package io.github.techouse.qskotlin.models

/** Represents a delimiter used for splitting key-value pairs. */
sealed class Delimiter {
    abstract fun split(input: String): List<String>

    /** Java-friendly factories and common constants. */
    companion object {
        @JvmStatic fun string(value: String): Delimiter = StringDelimiter(value)

        @JvmStatic fun regex(pattern: String): Delimiter = RegexDelimiter(pattern)

        @JvmField val AMPERSAND: Delimiter = StringDelimiter("&")
        @JvmField val COMMA: Delimiter = StringDelimiter(",")
        @JvmField val SEMICOLON: Delimiter = StringDelimiter(";")
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

    // Cache the compiled regex to avoid recompiling on every split.
    private val regex: Regex by lazy(LazyThreadSafetyMode.NONE) { Regex(pattern) }

    override fun split(input: String): List<String> = regex.split(input)
}
