package io.github.techouse.qskotlin.models

/** Represents a delimiter used for splitting key-value pairs. */
sealed class Delimiter {
    abstract fun split(input: String): List<String>
}

/**
 * String-based delimiter for better performance with simple delimiters.
 *
 * This is suitable for common delimiters like `&`, `,`, or `;`. It uses the `String.split` method
 * for efficient splitting.
 */
data class StringDelimiter(val value: String) : Delimiter() {
    override fun split(input: String): List<String> = input.split(value)
}

/**
 * Regex-based delimiter for complex pattern matching.
 *
 * This is useful for delimiters that require regular expression matching, such as `\\s*;\\s*` for
 * semicolon-separated values with optional whitespace. It uses the `Regex.split` method for
 * splitting the input string.
 */
data class RegexDelimiter(val pattern: String) : Delimiter() {
    override fun split(input: String): List<String> = Regex(pattern).split(input)
}
