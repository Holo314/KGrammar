package org.holo.kGrammar

/**
 * Create a (lazy-infinite) sequence of the form: `V, F(V), F(F(V)),...`
 * Where V=[startValue] and F=[this]
 */
fun <T> ((T) -> T).recursiveSeqFrom(startValue: T): Sequence<T> {
    return recursiveSeqFrom(startValue, this@recursiveSeqFrom)
}


/**
 * Create a (lazy-infinite) sequence of the form: `V, F(V), F(F(V)),...`
 * Where V=[startValue] and F=[func]
 */
fun <T> recursiveSeqFrom(startValue: T, func: (T) -> T) = sequence {
    var cur = startValue
    while (true) {
        yield(cur)
        cur = func(cur)
    }
}

/**
 * Find the fixed point of the sequence `V, F(V), F(F(V)),...`
 * Where V=[startValue] and F=[this]
 */
fun <T> ((T) -> T).fixPointFrom(startValue: T): T {
    return fixPointFrom(startValue, this)
}

/**
 * Find the fixed point of the sequence `V, F(V), F(F(V)),...`
 * Where V=[startValue] and F=[func]
 */
fun <T> fixPointFrom(startValue: T, func: (T) -> T): T {
    return func.recursiveSeqFrom(startValue)
        .zipWithNext()
        .takeWhile { it.first != it.second }
        .last().second
}

internal inline fun <reified T> Any.cast(): T = this as T
