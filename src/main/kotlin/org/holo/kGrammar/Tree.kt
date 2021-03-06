package org.holo.kGrammar

import java.util.*

/**
 * A class that represent the set of all possible symbols, it is partitioned into 2 classes:
 *
 *  [Terminal] - the class that represent all of the terminal symbols
 *
 *  [NonTerminal] - the class that represent all non-terminal symbols
 */
sealed class Symbol {
    sealed class Terminal : Symbol() {
        companion object {
            fun from(symbol: Regex) = CustomTemplate(symbol)
        }
        data class CustomTemplate internal constructor(val symbol: Regex) : Terminal() {
            init {
                require(symbol.pattern.isNotEmpty()) { "Empty symbols are not allowed, use org.holo.kGrammar.Symbol.Terminal.Empty to represent empty sequence" }
            }
        }

        /**
         * A symbol that represent the end of sequence, it is used only internally and should not be added to a rule
         */
        object EOS : Terminal()

        /**
         * A symbol that represent the empty sequence, if it appears in a rule it must appear alone and rules that it appears on represent "remove symbol" rules.
         *
         * For example: `A -> Empty` is the rule "when the non-terminal A appear, one can remove it"
         */
        object Empty : Terminal()
    }

    sealed class NonTerminal : Symbol() {
        companion object {
            fun generate() = Custom(UUID.randomUUID().toString())
            // This method is used for debugging, it helps following each symbol in the internal parts of the parsers
            internal fun generate(input: String) = Custom(input)
        }
        data class Custom internal constructor(val identifier: String) : NonTerminal()

        /**
         * A symbol that represent the starting point of the parsing, one should build his grammar starting from this symbol
         */
        object Start : NonTerminal()
    }
}


/**
 * A class that represent a production rule for the grammar
 */
data class Rule private constructor(val head: Symbol.NonTerminal, val body: List<Symbol>) {
    init {
        require(body.isNotEmpty()) { "The rule must not be empty, use org.holo.kGrammar.Symbol.Terminal.Empty to represent empty sequence" }
        require(Symbol.Terminal.EOS !in body) { "EOS should not be used inside of a rule, it is a terminating token used internally." +
                "It is exposed for those who wish to deal with Parse Tables" }
        if (body[0] is Symbol.NonTerminal) {
            require(body[0] != head) { "Left recursion is not allowed" }
        }
        if (body.filterNot { it is Symbol.Terminal.EOS }.size > 1) {
            require(Symbol.Terminal.Empty !in body) { "The empty terminal cannot appear with other rules" }
        }
    }

    companion object {
        fun from(definition: Pair<Symbol.NonTerminal, List<Symbol>>) = Rule(definition.first, definition.second)
        fun rulesSet(generator: RuleResource.() -> Unit) = RuleResource(mutableSetOf()).apply(generator).mutableSet.toSet()

        data class RuleResource(val mutableSet: MutableSet<Rule>) {
            operator fun Pair<Symbol.NonTerminal, List<Symbol>>.unaryPlus() = mutableSet.add(Rule(first, second))
        }
    }
}

data class Token(val symbol: Symbol.Terminal, val value: String, val position: IntRange)

/**
 * A class that represent CST
 */
sealed class ParseTree {
    data class TerminalNode internal constructor(val symbol: Symbol.Terminal, val value: Token) : ParseTree()
    object EmptyNode: ParseTree()
    data class NonTerminalNode internal constructor(val symbol: Symbol.NonTerminal, val children: List<ParseTree>) : ParseTree()

    companion object {
        /**
         * This function remove nodes whose direct child is an empty node
         * ```
         *      S
         *    /  \            S
         *   A    C           |
         *  / \   |    ==>    A
         * a   B  ε           |
         *     |              a
         *     ε
         * ```
         */
        fun removeEmpty(node: NonTerminalNode): ParseTreeNonEmpty.NonTerminalNode {
            require(node.children.first() !is EmptyNode) { "the node cannot have direct empty children" }
            val newChildren = node.children.filter {
                when(it) {
                    is TerminalNode -> true
                    is NonTerminalNode -> it.children.first() !is EmptyNode
                    is EmptyNode -> error("something went horribly wrong")
                }
            }.map {
                when(it) {
                    is TerminalNode -> ParseTreeNonEmpty.TerminalNode(it)
                    is NonTerminalNode -> removeEmpty(it)
                    is EmptyNode -> error("something went horribly wrong")
                }
            }

            return ParseTreeNonEmpty.NonTerminalNode(node.symbol, newChildren)
        }
    }
}

fun ParseTree.NonTerminalNode.removeEmpty(): ParseTreeNonEmpty.NonTerminalNode
        = ParseTree.removeEmpty(this)

/**
 * This sealed hierarchy exist because Kotlin does not support sum types, nor(at the time of writing this line) have sealed interfaces as stable feature,
 * so we need a separate hierarchy when we exclude the [ParseTree.EmptyNode] (otherwise one won't be able use pattern matching in a normal and natural way)
 */
sealed class ParseTreeNonEmpty {
    data class TerminalNode internal constructor(val symbol: Symbol.Terminal, val token: Token) : ParseTreeNonEmpty() {
        constructor(node: ParseTree.TerminalNode) : this(node.symbol, node.value)
    }
    data class NonTerminalNode internal constructor(val symbol: Symbol.NonTerminal, val children: List<ParseTreeNonEmpty>) : ParseTreeNonEmpty()
}