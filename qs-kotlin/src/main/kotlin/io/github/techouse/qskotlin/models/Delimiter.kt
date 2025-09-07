package io.github.techouse.qskotlin.models

import java.util.regex.Pattern

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
 * Stores the underlying Java [Pattern] to preserve flags (e.g., CASE_INSENSITIVE, DOTALL) and
 * delegates splitting to `Pattern.split`.
 */
class RegexDelimiter(private val jPattern: Pattern) : Delimiter() {
    /** Construct from a raw pattern string (no flags). */
    constructor(pattern: String) : this(Pattern.compile(pattern))

    /** Expose the raw pattern text for compatibility. */
    val pattern: String
        get() = jPattern.pattern()

    override fun split(input: String): List<String> = jPattern.split(input).toList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RegexDelimiter) return false
        return jPattern.pattern() == other.jPattern.pattern() &&
            jPattern.flags() == other.jPattern.flags()
    }

    override fun hashCode(): Int = 31 * jPattern.pattern().hashCode() + jPattern.flags()

    override fun toString(): String =
        "RegexDelimiter(pattern='${jPattern.pattern()}', flags=${jPattern.flags()})"
}
