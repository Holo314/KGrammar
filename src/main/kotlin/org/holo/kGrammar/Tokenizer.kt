package org.holo.kGrammar

/**
 * Return a (lazy) sequence of [Token], the sequence is infinite, [Symbol.Terminal.EOS] represent the end of the stream, and will be the
 * return value of [Iterator.next] will stay [Symbol.Terminal.EOS] forever.
 */
fun tokenize(source: CharSequence, terminals: Set<Symbol.Terminal.CustomTemplate>): Sequence<Token> = sequence {
    var curPos = 0
    while (curPos < source.length) {
        @Suppress("UNCHECKED_CAST")
        val result = terminals
            .map { it to it.symbol.find(source, curPos) }
            .filter { it.second != null }
            .filter { it.second!!.range.first == curPos }
            .maxByOrNull { it.second!!.range.last }!!
        curPos = result.second!!.range.last + 1
        yield(Token(result.first, result.second!!.value, result.second!!.range))
    }
    while (true) {
        yield(Token(Symbol.Terminal.EOS, "", source.length until source.length))
    }
}