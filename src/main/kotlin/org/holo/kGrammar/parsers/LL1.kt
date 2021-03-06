package org.holo.kGrammar.parsers

import org.holo.kGrammar.*

/**
 * Type aliases for mapping from [Symbol.NonTerminal] to set of [Symbol.Terminal]
 *
 *  - [FirstSets] represent the FIRST set
 *  - [FollowSets] represent the FOLLOW set
 */
internal typealias FirstSets = Map<Symbol.NonTerminal, Set<Symbol.Terminal>>
internal typealias FollowSets = Map<Symbol.NonTerminal, Set<Symbol.Terminal>>

/**
 * A typealias that represent a table who column are `[Symbol.NonTerminal] symbols | FIRST | FOLLOW`
 */
internal typealias FirstFollowTable = Map<Symbol.NonTerminal, Pair<Set<Symbol.Terminal>, Set<Symbol.Terminal>>>

/**
 * A typealias that represent a parse table [Symbol.NonTerminal]×[Symbol.Terminal] to set of [Rule]
 */
internal typealias ParseTable = Map<Pair<Symbol.NonTerminal, Symbol.Terminal>, Set<Rule>>


/**
 * Get the FIRST set of all the [Symbol.NonTerminal] from [rules]
 */
internal fun getFirstSets(rules: Collection<Rule>): FirstSets {
    val rulesMap = rules
        .groupBy { it.head }
        .toMap()
        .mapValues { it.value.map { rule -> rule.body } }

    val initialSets: FirstSets = rulesMap.mapValues { emptySet() }

    fun nextStage(currentState: FirstSets): FirstSets {
        fun nextInnerStage(body: List<Symbol>, state: FirstSets): Set<Symbol.Terminal> {
            return when (val firstElement = body.plus(Symbol.Terminal.Empty)[0]) {
                is Symbol.Terminal -> setOf(firstElement)
                is Symbol.NonTerminal -> when (Symbol.Terminal.Empty) {
                    !in state[firstElement]!! -> state[firstElement]!!
                    else -> (state[firstElement]!! - Symbol.Terminal.Empty) + nextInnerStage(body.drop(1), state)
                }
            }
        }

        return currentState.mapValues { symbolState ->
            symbolState.value + rulesMap[symbolState.key]!!.fold(emptySet()) { acc, ruleBody ->
                acc + nextInnerStage(ruleBody, currentState)
            }
        }
    }

    return ::nextStage.fixPointFrom(initialSets)
}

/**
 * Get the FIRST set of a tail of a body of a [Rule]
 */
internal fun getFirstSetOfBody(firstSetsOfNonTerminals: FirstSets, body: List<Symbol>): Set<Symbol.Terminal> {
    return when (val first = body.plus(Symbol.Terminal.Empty)[0]) {
        is Symbol.Terminal -> setOf(first)
        is Symbol.NonTerminal -> when (Symbol.Terminal.Empty) {
            in firstSetsOfNonTerminals[first]!! -> getFirstSetOfBody(firstSetsOfNonTerminals, body.drop(1)) +
                    firstSetsOfNonTerminals[first]!!
            else -> firstSetsOfNonTerminals[first]!!
        }
    }
}

/**
 * Get the FIRST set of the tail that appear after each [Symbol.NonTerminal], that is, given the rule `A -> aAbBCc`
 *
 * [getFirstSetAfterNonTerminal] of `aAbBc` will return a map of:
 *
 *  - `A` to [getFirstSetOfBody] of `bBc`
 *  - `B` to [getFirstSetOfBody] of `Cc`
 *  - `C` to [getFirstSetOfBody] of `c`
 */
internal fun getFirstSetAfterNonTerminal(body: List<Symbol>, firstSetsOfNonTerminals: FirstSets): FollowSets {
    return body.asSequence()
        //.filter { it is Symbol.NonTerminal }//TODO: !!!
        .mapIndexed { i, symbol -> symbol to i }
        .filter { it.first is Symbol.NonTerminal }
        .map {
            it.first as Symbol.NonTerminal to when {
                it.second < body.size -> getFirstSetOfBody(firstSetsOfNonTerminals, body.drop(it.second + 1))
                else -> getFirstSetOfBody(firstSetsOfNonTerminals, listOf(Symbol.Terminal.Empty))
            }
        }
        .toMap()
}

/**
 * Get the FIRST set of all the [Symbol.NonTerminal] from [rules] where it assumes [firstSets] = [getFirstSets] of [rules]
 */
internal fun getFollowSets(rules: Collection<Rule>, firstSets: FirstSets): FollowSets {
    val symbols = rules.map { it.head }.toSet()
    val initialSets: FollowSets = symbols.map {
        it to when (it) {
            is Symbol.NonTerminal.Start -> setOf(Symbol.Terminal.EOS)
            else -> emptySet()
        }
    }.toMap()

    fun nextStageByRule(rule: Rule, state: FollowSets): FollowSets {
        val followingTerminals = getFirstSetAfterNonTerminal(rule.body, firstSets)
        val withEmptyFollow = followingTerminals
            .filter { Symbol.Terminal.Empty in it.value }
            .mapValues { state[rule.head]!! + it.value - Symbol.Terminal.Empty }

        return state.mapValues {
            when (val head = it.key) {
                in withEmptyFollow -> it.value + withEmptyFollow[head]!!
                in followingTerminals -> it.value + followingTerminals[head]!!
                else -> it.value
            }
        }
    }

    return fixPointFrom(initialSets) { value ->
        rules.fold(value) { acc, rule ->
            acc.mapValues { it.value + nextStageByRule(rule, value)[it.key]!! }
        }
    }
}

/**
 * Get the FIRST set of all the [Symbol.NonTerminal] from [rules]
 */
internal fun getFollowSets(rules: Collection<Rule>): FollowSets = getFollowSets(rules, getFirstSets(rules))

/**
 * Generates a table [FirstFollowTable] using specific [rules]
 */
internal fun generateFirstFollowTable(rules: Collection<Rule>): FirstFollowTable {
    require(rules.any { it.head == Symbol.NonTerminal.Start }) { "The rules must contain at least one rule that start with the starter non-terminal" }
    val firstSets = getFirstSets(rules)
    val followSets = getFollowSets(rules, firstSets)
    return firstSets.mapValues { Pair(it.value, followSets[it.key]!!) }
}

/**
 * Generates a parse table [ParseTable] to set of [Rule] using specific [rules] and assuming [firstFollowTable] = [generateFirstFollowTable] of [rules]
 */
internal fun generateParseTable(rules: Collection<Rule>, firstFollowTable: FirstFollowTable): ParseTable {
    val firstSets = firstFollowTable.mapValues { it.value.first }
    val followSets = firstFollowTable.mapValues { it.value.second }

    @Suppress("UNCHECKED_CAST")
    val symbols: Pair<List<Symbol.Terminal>, List<Symbol.NonTerminal>> = rules
        .flatMap { it.body + it.head }
        .partition { it is Symbol.Terminal } as Pair<List<Symbol.Terminal>, List<Symbol.NonTerminal>>

    val terminals = symbols.first.toSet() + Symbol.Terminal.EOS - Symbol.Terminal.Empty
    val nonTerminal = symbols.second.toSet()
    val combinations: List<Pair<Symbol.NonTerminal, Symbol.Terminal>> =
        nonTerminal.flatMap { lhs -> terminals.map { rhs -> lhs to rhs } }

    return combinations.map {
        it to rules.filter { rule -> rule.head == it.first }
            .filter { rule ->
                val firstOfBody = getFirstSetOfBody(firstSets, rule.body)

                Symbol.Terminal.Empty in firstOfBody && it.second in followSets[it.first]!!
                        || it.second in firstOfBody
            }.toSet()
    }.toMap()
}

/**
 * Generates a parse table [ParseTable] to set of [Rule] using specific [rules]
 */
internal fun generateParseTable(rules: Collection<Rule>): ParseTable =
    generateParseTable(rules, generateFirstFollowTable(rules))

/**
 * Parse the [tokenSequence] by the rules specified at [parseTable]
 */
internal fun parse(tokenSequence: Sequence<Token>, parseTable: ParseTable): ParseTree.NonTerminalNode {
    require(parseTable.values.all { it.size < 2 }) { "There is a conflict in the parse table" }
    val pTable = parseTable.mapValues {
        val iter = it.value.iterator()
        when (iter.hasNext()) {
            true -> iter.next()
            false -> null
        }
    }
    val tokenIterator = tokenSequence.iterator()
    val stack = mutableListOf<Symbol>(Symbol.NonTerminal.Start)
    var cToken = tokenIterator.next()
    fun innerRulesList(): ParseTree {
        if (stack.first() is Symbol.Terminal.EOS) error("Something went horribly wrong")
        return when (val head = stack.removeFirst()) {
            is Symbol.NonTerminal -> {
                val rule = pTable[head to cToken.symbol]
                stack.addAll(0, rule!!.body)
                ParseTree.NonTerminalNode(
                    head,
                    rule.body.map { innerRulesList() }
                )
            }
            is Symbol.Terminal.Empty -> ParseTree.EmptyNode
            is Symbol.Terminal -> {
                val token = cToken
                cToken = tokenIterator.next()
                ParseTree.TerminalNode(head, token)
            }
        }
    }

    return innerRulesList() as ParseTree.NonTerminalNode
}

/**
 * Parse the [tokenSequence] by the [rules]
 */
fun parse(tokenSequence: Sequence<Token>, rules: Collection<Rule>): ParseTree.NonTerminalNode =
    parse(tokenSequence, generateParseTable(rules))