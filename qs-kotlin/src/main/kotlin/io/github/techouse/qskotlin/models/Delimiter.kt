package io.github.techouse.qskotlin.models

/** Represents a delimiter used for splitting key-value pairs. */
sealed class Delimiter {
    abstract fun split(input: String): List<String>

    /** Java-friendly factories and common constants. */
    companion object {
        @JvmStatic fun string(value: String): StringDelimiter = StringDelimiter(value)

        @JvmStatic fun regex(pattern: String): RegexDelimiter = RegexDelimiter(pattern)

        @JvmField val AMPERSAND: StringDelimiter = StringDelimiter("&")
        @JvmField val COMMA: StringDelimiter = StringDelimiter(",")
        @JvmField val SEMICOLON: StringDelimiter = StringDelimiter(";")
    }
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
    constructor(pattern: java.util.regex.Pattern) : this(pattern.pattern())

    // Eagerly compile for thread-safe reuse across threads.
    private val regex: Regex = Regex(pattern)

    override fun split(input: String): List<String> = regex.split(input)
}
