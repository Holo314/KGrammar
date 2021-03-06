package org.holo.kGrammar

import org.holo.kGrammar.parsers.generateFirstFollowTable
import org.holo.kGrammar.parsers.generateParseTable
import org.holo.kGrammar.parsers.parse
import kotlin.test.Test
import kotlin.test.assertEquals

class LL1Tests {
    @Test
    fun parseTableTest(): Unit {
        val A = Symbol.NonTerminal.generate()
        val B = Symbol.NonTerminal.generate()

        val a = Symbol.Terminal.from("a".toRegex())
        val b = Symbol.Terminal.from("b".toRegex())
        val c = Symbol.Terminal.from("c".toRegex())


        val rules = Rule.rulesSet {
            +(Symbol.NonTerminal.Start to listOf(a, A, B, b))
            +(A to listOf(a, A, c))
            +(A to listOf(Symbol.Terminal.Empty))
            +(B to listOf(b, B))
            +(B to listOf(c))
        }


        val ffTable = generateFirstFollowTable(rules)
        assertEquals(Pair(setOf(a), setOf(Symbol.Terminal.EOS)), ffTable[Symbol.NonTerminal.Start])
        assertEquals(Pair(setOf(a, Symbol.Terminal.Empty), setOf(b, c)), ffTable[A])
        assertEquals(Pair(setOf(b, c), setOf(b)), ffTable[B])

        val pTable = generateParseTable(rules, ffTable)
        assertEquals(
            setOf(Rule.from(Symbol.NonTerminal.Start to listOf(a, A, B, b))),
            pTable[Pair(Symbol.NonTerminal.Start, a)]
        )
        assertEquals(setOf(), pTable[Pair(Symbol.NonTerminal.Start, b)])
        assertEquals(setOf(), pTable[Pair(Symbol.NonTerminal.Start, c)])
        assertEquals(setOf(), pTable[Pair(Symbol.NonTerminal.Start, Symbol.Terminal.EOS)])
        assertEquals(setOf(Rule.from(A to listOf(a, A, c))), pTable[Pair(A, a)])
        assertEquals(setOf(Rule.from(A to listOf(Symbol.Terminal.Empty))), pTable[Pair(A, b)])
        assertEquals(setOf(Rule.from(A to listOf(Symbol.Terminal.Empty))), pTable[Pair(A, c)])
        assertEquals(setOf(), pTable[Pair(A, Symbol.Terminal.EOS)])
        assertEquals(setOf(), pTable[Pair(B, a)])
        assertEquals(setOf(Rule.from(B to listOf(b, B))), pTable[Pair(B, b)])
        assertEquals(setOf(Rule.from(B to listOf(c))), pTable[Pair(B, c)])
        assertEquals(setOf(), pTable[Pair(B, Symbol.Terminal.EOS)])
    }

    @Test
    fun parseTableTest2(): Unit {
        val E_ = Symbol.NonTerminal.generate()
        val T = Symbol.NonTerminal.generate()
        val T_ = Symbol.NonTerminal.generate()
        val F = Symbol.NonTerminal.generate()

        val plus = Symbol.Terminal.from("\\+".toRegex())
        val mul = Symbol.Terminal.from("\\*".toRegex())
        val lb = Symbol.Terminal.from("\\(".toRegex())
        val rb = Symbol.Terminal.from("\\)".toRegex())
        val id = Symbol.Terminal.from("id".toRegex())

        val rules = Rule.rulesSet {
            +(Symbol.NonTerminal.Start to listOf(T, E_))
            +(E_ to listOf(plus, T, E_))
            +(E_ to listOf(Symbol.Terminal.Empty))
            +(T to listOf(F, T_))
            +(T_ to listOf(mul, F, T_))
            +(T_ to listOf(Symbol.Terminal.Empty))
            +(F to listOf(lb, Symbol.NonTerminal.Start, rb))
            +(F to listOf(id))
        }

        val ffTable = generateFirstFollowTable(rules)
        assertEquals(Pair(setOf(lb, id), setOf(Symbol.Terminal.EOS, rb)), ffTable[Symbol.NonTerminal.Start])
        assertEquals(Pair(setOf(plus, Symbol.Terminal.Empty), setOf(Symbol.Terminal.EOS, rb)), ffTable[E_])
        assertEquals(Pair(setOf(lb, id), setOf(Symbol.Terminal.EOS, rb, plus)), ffTable[T])
        assertEquals(Pair(setOf(mul, Symbol.Terminal.Empty), setOf(Symbol.Terminal.EOS, rb, plus)), ffTable[T_])
        assertEquals(Pair(setOf(lb, id), setOf(Symbol.Terminal.EOS, rb, plus, mul)), ffTable[F])

        val pTable = generateParseTable(rules, ffTable)
        assertEquals(setOf(Rule.from(Symbol.NonTerminal.Start to listOf(T, E_))), pTable[Pair(Symbol.NonTerminal.Start, lb)])
        assertEquals(setOf(Rule.from(E_ to listOf(Symbol.Terminal.Empty))), pTable[Pair(E_, Symbol.Terminal.EOS)])
        assertEquals(setOf(), pTable[Pair(T, mul)])
        assertEquals(setOf(Rule.from(T_ to listOf(mul, F, T_))), pTable[Pair(T_, mul)])
        assertEquals(setOf(Rule.from(F to listOf(id))), pTable[Pair(F, id)])
    }

    @Test
    fun parserTest(): Unit {
        val E_ = Symbol.NonTerminal.generate()
        val T = Symbol.NonTerminal.generate()
        val T_ =Symbol.NonTerminal.generate()
        val F = Symbol.NonTerminal.generate()

        val plus = Symbol.Terminal.from("\\+".toRegex())
        val mul = Symbol.Terminal.from("\\*".toRegex())
        val lb = Symbol.Terminal.from("\\(".toRegex())
        val rb = Symbol.Terminal.from("\\)".toRegex())
        val id = Symbol.Terminal.from("([1-9][0-9]*|0)(\\.[0-9]+)?".toRegex())

        val rules = Rule.rulesSet {
            +(Symbol.NonTerminal.Start to listOf(T, E_))
            +(E_ to listOf(plus, T, E_))
            +(E_ to listOf(Symbol.Terminal.Empty))
            +(T to listOf(F, T_))
            +(T_ to listOf(mul, F, T_))
            +(T_ to listOf(Symbol.Terminal.Empty))
            +(F to listOf(lb, Symbol.NonTerminal.Start, rb))
            +(F to listOf(id))
        }

        val ffTable = generateFirstFollowTable(rules)
        val pTable = generateParseTable(rules, ffTable)
        val tokenSequence = tokenize(
            "1+1",
            setOf(plus, mul, lb, rb, id)
        )

        val result = parse(tokenSequence, pTable)
        val expected =
            ParseTree.NonTerminalNode(
                Symbol.NonTerminal.Start,
                listOf(
                    ParseTree.NonTerminalNode(
                        T,
                        listOf(
                            ParseTree.NonTerminalNode(
                                F,
                                listOf(
                                    ParseTree.TerminalNode(id, Token(id, "1", 0..0))
                                )
                            ),
                            ParseTree.NonTerminalNode(
                                T_,
                                listOf(
                                    ParseTree.EmptyNode
                                )
                            )
                        )
                    ),
                    ParseTree.NonTerminalNode(
                        E_,
                        listOf(
                            ParseTree.TerminalNode(plus, Token(plus, "+", 1..1)),
                            ParseTree.NonTerminalNode(
                                T,
                                listOf(
                                    ParseTree.NonTerminalNode(
                                        F,
                                        listOf(
                                            ParseTree.TerminalNode(id, Token(id, "1", 2..2))
                                        )
                                    ),
                                    ParseTree.NonTerminalNode(
                                        T_,
                                        listOf(
                                            ParseTree.EmptyNode
                                        )
                                    )
                                )
                            ),
                            ParseTree.NonTerminalNode(
                                E_,
                                listOf(
                                    ParseTree.EmptyNode
                                )
                            )
                        )
                    )
                )
            )

        assertEquals(expected, result)
    }

    @Test
    fun parserTest2(): Unit {
        val a = Symbol.Terminal.from("a".toRegex())
        val b = Symbol.Terminal.from("b".toRegex())
        val A = Symbol.NonTerminal.generate()
        val rules = Rule.rulesSet {
            +(Symbol.NonTerminal.Start to listOf(A, b))
            +(A to listOf(a))
        }
        val ffTable = generateFirstFollowTable(rules)
        val pTable = generateParseTable(rules, ffTable)
        val tokenSequence = tokenize(
            "ab",
            setOf(b, a)
        )
        val result = parse(tokenSequence, pTable)
        val expected = ParseTree.NonTerminalNode(
            Symbol.NonTerminal.Start,
            listOf(
                ParseTree.NonTerminalNode(
                    A,
                    listOf(ParseTree.TerminalNode(a, Token(a, "a", 0..0)))
                ),
                ParseTree.TerminalNode(b, Token(b, "b", 1..1))
            )
        )
        assertEquals(expected, result)
    }

    @Test
    fun removeEmptyEntries(): Unit {
        val E_ = Symbol.NonTerminal.generate()
        val T = Symbol.NonTerminal.generate()
        val T_ = Symbol.NonTerminal.generate()
        val F = Symbol.NonTerminal.generate()

        val plus = Symbol.Terminal.from("\\+".toRegex())
        val mul = Symbol.Terminal.from("\\*".toRegex())
        val lb = Symbol.Terminal.from("\\(".toRegex())
        val rb = Symbol.Terminal.from("\\)".toRegex())
        val id = Symbol.Terminal.from("([1-9][0-9]*|0)(\\.[0-9]+)?".toRegex())

        val tree =
            ParseTree.NonTerminalNode(
                Symbol.NonTerminal.Start,
                listOf(
                    ParseTree.NonTerminalNode(
                        T,
                        listOf(
                            ParseTree.NonTerminalNode(
                                F,
                                listOf(
                                    ParseTree.TerminalNode(id, Token(id, "1", 0..0))
                                )
                            ),
                            ParseTree.NonTerminalNode(
                                T_,
                                listOf(
                                    ParseTree.EmptyNode
                                )
                            )
                        )
                    ),
                    ParseTree.NonTerminalNode(
                        E_,
                        listOf(
                            ParseTree.TerminalNode(plus, Token(plus, "+", 1..1)),
                            ParseTree.NonTerminalNode(
                                T,
                                listOf(
                                    ParseTree.NonTerminalNode(
                                        F,
                                        listOf(
                                            ParseTree.TerminalNode(id, Token(id, "1", 2..2))
                                        )
                                    ),
                                    ParseTree.NonTerminalNode(
                                        T_,
                                        listOf(
                                            ParseTree.EmptyNode
                                        )
                                    )
                                )
                            ),
                            ParseTree.NonTerminalNode(
                                E_,
                                listOf(
                                    ParseTree.EmptyNode
                                )
                            )
                        )
                    )
                )
            )
        val result = ParseTree.removeEmpty(tree)

        val expected =
            ParseTreeNonEmpty.NonTerminalNode(
                Symbol.NonTerminal.Start,
                listOf(
                    ParseTreeNonEmpty.NonTerminalNode(
                        T,
                        listOf(
                            ParseTreeNonEmpty.NonTerminalNode(
                                F,
                                listOf(
                                    ParseTreeNonEmpty.TerminalNode(id, Token(id, "1", 0..0))
                                )
                            )
                        )
                    ),
                    ParseTreeNonEmpty.NonTerminalNode(
                        E_,
                        listOf(
                            ParseTreeNonEmpty.TerminalNode(plus, Token(plus, "+", 1..1)),
                            ParseTreeNonEmpty.NonTerminalNode(
                                T,
                                listOf(
                                    ParseTreeNonEmpty.NonTerminalNode(
                                        F,
                                        listOf(
                                            ParseTreeNonEmpty.TerminalNode(id, Token(id, "1", 2..2))
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )

        assertEquals(expected, result)
    }
}