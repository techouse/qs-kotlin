package io.github.techouse.qskotlin.models

sealed interface Filter

/**
 * A filter that applies a function to a key-value pair. The function takes the key as a String and
 * the value as Any?, and returns a transformed value.
 */
class FunctionFilter(val function: (String, Any?) -> Any?) : Filter

/**
 * A filter that applies to an Iterable. This can be used to filter or transform the elements of the
 * Iterable.
 */
class IterableFilter(val iterable: Iterable<*>) : Filter
