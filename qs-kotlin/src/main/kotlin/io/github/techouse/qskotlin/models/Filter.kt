package io.github.techouse.qskotlin.models

sealed interface Filter

/**
 * A filter that applies a function to a key-value pair. The function takes the key as a String and
 * the value as Any?, and returns a transformed value.
 */
class FunctionFilter(val function: (String, Any?) -> Any?) : Filter {
    /** Java-friendly ctor: accept a java.util.function.BiFunction */
    constructor(
        fn: java.util.function.BiFunction<String, Any?, Any?>
    ) : this({ key, value -> fn.apply(key, value) })

    companion object {
        /** Java-friendly factory */
        @JvmStatic
        fun from(fn: java.util.function.BiFunction<String, Any?, Any?>): FunctionFilter =
            FunctionFilter { key, value ->
                fn.apply(key, value)
            }
    }
}

/**
 * A filter that applies to an Iterable. This can be used to filter or transform the elements of the
 * Iterable.
 */
class IterableFilter(val iterable: Iterable<*>) : Filter {
    /** Java-friendly ctor: accept an Object[] */
    constructor(array: Array<out Any?>) : this(array.asIterable())

    /** Java-friendly ctor: accept a java.util.Collection */
    constructor(collection: java.util.Collection<*>) : this(collection as Iterable<*>)

    companion object {
        /** Java-friendly factory: varargs */
        @JvmStatic fun of(vararg values: Any?): IterableFilter = IterableFilter(values.asList())

        /** Java-friendly factory: collection */
        @JvmStatic
        fun from(collection: java.util.Collection<*>): IterableFilter = IterableFilter(collection)
    }
}
