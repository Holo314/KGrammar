package org.holo.kGrammar

import org.holo.kGrammar.parsers.parse
import kotlin.test.assertEquals

/**
 * Defining the [org.holo.kGrammar.Symbol.NonTerminal] symbols
 */
val start = Symbol.NonTerminal.Start
val addExp = Symbol.NonTerminal.generate()
val startMult = Symbol.NonTerminal.generate()
val multExp = Symbol.NonTerminal.generate()
val expression = Symbol.NonTerminal.generate()

/**
 * Defining the [org.holo.kGrammar.Symbol.Terminal] symbols
 */
val plus = Symbol.Terminal.from("\\+".toRegex())
val mult = Symbol.Terminal.from("\\*".toRegex())
val lb = Symbol.Terminal.from("\\(".toRegex())
val rb = Symbol.Terminal.from("\\)".toRegex())
val id = Symbol.Terminal.from("-?([1-9][0-9]*|0)(\\.[0-9]+)?".toRegex())

/**
 * [Λ] represent the empty sequence
 */
val Λ = listOf(Symbol.Terminal.Empty)

/**
 * Defining the rules.
 *
 * This rules define the grammar of a mathematical expression containing `+, *, [Double], (, )`.
 * The rules will be correctly parse any legal expression of those symbols, it will respect the precedence of `+`, `*` and `()`.
 * The parsing result will be *right associative*, so [Double] * [Double] * [Double] will be [Double] * ([Double] * [Double])
 */
val rules = Rule.rulesSet {
    +(start to listOf(startMult, addExp))
    +(addExp to Λ)
    +(addExp to listOf(plus, start))
    +(startMult to listOf(expression, multExp))
    +(multExp to Λ)
    +(multExp to listOf(mult, startMult))
    +(expression to listOf(lb, start, rb))
    +(expression to listOf(id))
}

/**
 * The AST class, after parsing the input into [ParseTree] (see [parse]), we will manually transform our [ParseTree] into this object(see [parseTreeToAST])
 */
sealed class AST {
    data class Number(val value: Double) : AST()
    data class AddExp(val left: AST, val right: AST) : AST()
    data class MulExp(val left: AST, val right: AST) : AST()
}

/**
 * Transform any [ParseTreeNonEmpty] (see [ParseTree.removeEmpty] if you have [ParseTree] instead) into [AST]
 */
fun parseTreeToAST(pTree: ParseTreeNonEmpty.NonTerminalNode): AST {
    return when (pTree.symbol) {
        start -> startToAST(pTree)
        addExp -> addExpToAST(pTree)
        startMult -> startMultToAST(pTree)
        multExp -> multExpToAST(pTree)
        expression -> expToAst(pTree)
        else -> error("Should not have reached here")
    }
}

/**
 * The function that evaluate [AST] into concrete result(in this case, a [Double])
 */
fun eval(ast: AST): Double = when (ast) {
    is AST.Number -> ast.value
    is AST.AddExp -> eval(ast.left) + eval(ast.right)
    is AST.MulExp -> eval(ast.left) * eval(ast.right)
}

/**
 * A complete process of evaluating an input
 */
fun main() {
    val tokens = tokenize("1+2*3+1.5+2*(2+1)*2+1", setOf(plus, mult, lb, rb, id))
    val parseTree = parse(tokens, rules).removeEmpty()
    val ast = parseTreeToAST(parseTree)
    assertEquals(1+2*3+1.5+2*(2+1)*2+1, eval(ast))
}

/**
 * The following section is the implementation of how we transform each [ParseTreeNonEmpty.NonTerminalNode] ([start], [addExp], [startMult], [multExp], [expression]) into it's equivalent [AST] node.
 *
 * Because the library is *not* code generating library, we cannot guarantee exhaustive `when` expression,
 * theoretically, one can just put the last case into `else` block([org.holo.kGrammar.ParseTreeNonEmpty.NonTerminalNode] have internal constructor,
 * unless someone construct a tree with reflection, you should never reach the else block in the following example),
 * but to make the logic clearer, I created a branch for each possible case, and am throwing an [IllegalStateException]
 * in the `else` block.
 */

fun startToAST(pTree: ParseTreeNonEmpty.NonTerminalNode): AST =
    when (pTree.children.size) {
        1 -> parseTreeToAST(pTree.children.first() as ParseTreeNonEmpty.NonTerminalNode)
        2 -> AST.AddExp(
            parseTreeToAST(pTree.children.first() as ParseTreeNonEmpty.NonTerminalNode),
            parseTreeToAST(pTree.children.last() as ParseTreeNonEmpty.NonTerminalNode)
        )
        else -> error("Nodes of \"Start\" can have only 1 or 2 children")
    }

fun addExpToAST(pTree: ParseTreeNonEmpty.NonTerminalNode): AST =
    parseTreeToAST(pTree.children.last() as ParseTreeNonEmpty.NonTerminalNode)

fun startMultToAST(pTree: ParseTreeNonEmpty.NonTerminalNode): AST =
    when (pTree.children.size) {
        1 -> parseTreeToAST(pTree.children.first() as ParseTreeNonEmpty.NonTerminalNode)
        2 -> AST.MulExp(
            parseTreeToAST(pTree.children.first() as ParseTreeNonEmpty.NonTerminalNode),
            parseTreeToAST(pTree.children.last() as ParseTreeNonEmpty.NonTerminalNode)
        )
        else -> error("Nodes of \"startMult\" can have only 1 or 2 children")
    }

fun multExpToAST(pTree: ParseTreeNonEmpty.NonTerminalNode): AST =
    parseTreeToAST(pTree.children.last() as ParseTreeNonEmpty.NonTerminalNode)

fun expToAst(pTree: ParseTreeNonEmpty.NonTerminalNode): AST =
    when (pTree.children.size) {
        1 -> AST.Number((pTree.children.first() as ParseTreeNonEmpty.TerminalNode).token.value.toDouble())
        3 -> parseTreeToAST(pTree.children.drop(1).first() as ParseTreeNonEmpty.NonTerminalNode)
        else -> error("Nodes of \"expression\" can have only 1 or 3 children")
    }
