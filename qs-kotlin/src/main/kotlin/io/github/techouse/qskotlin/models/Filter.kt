package io.github.techouse.qskotlin.models

import java.util.function.BiFunction

/**
 * Marker interface for filters used by encoding.
 *
 * A filter can either:
 * - **Transform** values on a per key/value basis ([FunctionFilter]).
 * - **Select** a subset of keys/indices to include ([IterableFilter]).
 *
 * See: `EncodeOptions.Builder.filter(...)` for how filters are applied during encoding.
 */
sealed interface Filter

/**
 * A filter that applies a function to each key-value pair.
 *
 * The function receives the map/list **key** (as a `String`) and its **value** (`Any?`), and
 * returns either a transformed value **or** `null` to omit the entry.
 *
 * ### Examples
 * Kotlin:
 * ```kotlin
 * val f = FunctionFilter { key, value ->
 *   if (key.startsWith("password")) null else value // drop secrets
 * }
 * ```
 *
 * Java:
 * ```java
 * FunctionFilter f = new FunctionFilter((k, v) ->
 *     k.startsWith("password") ? null : v);
 * ```
 */
class FunctionFilter(
    /** Function invoked for every key/value; return `null` to exclude the entry. */
    val function: (String, Any?) -> Any?
) : Filter {
    /**
     * Java-friendly constructor accepting a {@link java.util.function.BiFunction}.
     *
     * The function is invoked for every `(key, value)` during encoding. Return the transformed
     * value, or `null` to exclude that entry from the output.
     */
    constructor(fn: BiFunction<String, Any?, Any?>) : this({ key, value -> fn.apply(key, value) })

    companion object {
        /**
         * Java-friendly factory.
         *
         * Example:
         * ```java
         * FunctionFilter f = FunctionFilter.from((k, v) -> v == null ? "<null>" : v);
         * ```
         */
        @JvmStatic
        fun from(fn: BiFunction<String, Any?, Any?>): FunctionFilter =
            FunctionFilter { key, value ->
                fn.apply(key, value)
            }
    }
}

/**
 * A filter backed by an [Iterable] that **selects** which keys/indices to include.
 *
 * Pass a sequence of property names (for objects) and/or indices (for lists). Any entries whose key
 * (or index) are **not** present in the iterable are omitted.
 *
 * ### Examples
 * Kotlin:
 * ```kotlin
 * // Only include keys "a" and index 0 from list "x"
 * val f = IterableFilter(listOf("a", 0))
 * ```
 *
 * Java:
 * ```java
 * IterableFilter f = IterableFilter.of("a", 0, 2);
 * ```
 */
class IterableFilter(val iterable: Iterable<*>) : Filter {
    /** Java-friendly constructor for `Object[]` / Kotlin array varargs. */
    constructor(array: Array<out Any?>) : this(array.asIterable())

    /** Java-friendly constructor for {@link java.util.Collection}; preserves iteration order. */
    constructor(collection: Collection<*>) : this(collection as Iterable<*>)

    companion object {
        /** Java-friendly factory with varargs; e.g., `IterableFilter.of("a", 0, 2)`. */
        @JvmStatic fun of(vararg values: Any?): IterableFilter = IterableFilter(values.asList())

        /** Java-friendly factory from a {@link java.util.Collection}. */
        @JvmStatic fun from(collection: Collection<*>): IterableFilter = IterableFilter(collection)
    }
}
