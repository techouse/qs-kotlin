package io.github.techouse.qskotlin.extensions

import kotlin.math.min

/** Extensions for Iterable to provide additional functionality */

/** Returns a new [Iterable] without elements of type [Q]. */
inline fun <T, reified Q> Iterable<T>.whereNotType(): Iterable<T> = filter { it !is Q }

/** Extensions for List to provide additional functionality */

/**
 * Extracts a section of a list and returns a new list.
 *
 * Modeled after JavaScript's `Array.prototype.slice()` method.
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/slice
 */
fun <T> List<T>.slice(start: Int = 0, end: Int? = null): List<T> {
    val actualStart = if (start < 0) (size + start).coerceIn(0, size) else start.coerceIn(0, size)
    val actualEnd =
        if (end == null) size
        else {
            if (end < 0) (size + end).coerceIn(0, size) else end.coerceIn(0, size)
        }
    return subList(actualStart, actualEnd)
}

/** Extensions for String to provide additional functionality */

/**
 * Extracts a section of a string and returns a new string.
 *
 * Modeled after JavaScript's `String.prototype.slice()` method.
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/slice
 */
fun String.slice(start: Int, end: Int? = null): String {
    val actualEnd = end ?: length
    val normalizedEnd = if (actualEnd < 0) length + actualEnd else actualEnd
    val normalizedStart = if (start < 0) length + start else start

    return substring(normalizedStart, min(normalizedEnd, length))
}
