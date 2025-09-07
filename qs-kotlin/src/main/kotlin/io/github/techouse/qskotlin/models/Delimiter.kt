package io.github.techouse.qskotlin.models

import java.util.regex.Pattern

/**
 * Strategy for splitting a query string into `key=value` pairs.
 *
 * Use [StringDelimiter] for simple, single-character separators (fast path), and [RegexDelimiter]
 * for more advanced patterns (e.g., optional whitespace or multiple alternatives).
 *
 * ### Semantics
 * - Splitting follows JVM defaults (Kotlin/JDK): **trailing empty segments are discarded**.
 * - Empty middle segments are preserved (e.g., `a=b&&c=d` → `["a=b", "", "c=d"]`).
 * - Implementations are **thread-safe** and immutable; reuse instances when possible.
 *
 * ### Examples
 * Kotlin:
 * ```kotlin
 * val d1: Delimiter = Delimiter.string("&")
 * val parts = d1.split("a=b&c=d") // ["a=b", "c=d"]
 *
 * val d2: Delimiter = Delimiter.regex("\\s*[;&]\\s*")
 * val parts2 = d2.split("a=b ; c=d") // ["a=b", "c=d"]
 * ```
 *
 * Java:
 * ```java
 * Delimiter d1 = Delimiter.string("&");
 * List<String> parts = d1.split("a=b&c=d");
 *
 * Delimiter d2 = Delimiter.regex(java.util.regex.Pattern.compile("[;&]"));
 * List<String> parts2 = d2.split("a=b;c=d");
 * ```
 */
sealed class Delimiter {
    /**
     * Split [input] into a list of `key=value` segments.
     *
     * Implementation notes:
     * - Matches JDK/Kotlin `split` behavior: trailing empty results are omitted (limit=0).
     * - Mid-string empty segments are retained.
     */
    abstract fun split(input: String): List<String>

    /** Java-friendly factories and common constants. */
    companion object {
        /** Create a [StringDelimiter] for a literal separator (e.g., "&" or ";"). */
        @JvmStatic fun string(value: String): StringDelimiter = StringDelimiter(value)

        /** Create a [RegexDelimiter] from a pattern string (no flags). */
        @JvmStatic fun regex(pattern: String): RegexDelimiter = RegexDelimiter(pattern)

        /** Create a [RegexDelimiter] from a precompiled [Pattern], preserving its flags. */
        @JvmStatic fun regex(pattern: Pattern): RegexDelimiter = RegexDelimiter(pattern)

        /** Create a [RegexDelimiter] by compiling [pattern] with the given Java regex [flags]. */
        @JvmStatic
        fun regex(pattern: String, flags: Int): RegexDelimiter =
            RegexDelimiter(Pattern.compile(pattern, flags))

        /** Literal `&` separator. */
        @JvmField val AMPERSAND: StringDelimiter = StringDelimiter("&")
        /** Literal `,` separator. */
        @JvmField val COMMA: StringDelimiter = StringDelimiter(",")
        /** Literal `;` separator. */
        @JvmField val SEMICOLON: StringDelimiter = StringDelimiter(";")
    }
}

/**
 * String-based delimiter optimized for simple separators.
 *
 * Uses `String.split(value)` with the default `limit = 0`, which **discards trailing empty
 * segments**.
 */
data class StringDelimiter(val value: String) : Delimiter() {
    init {
        require(value.isNotEmpty()) { "Delimiter must not be empty" }
    }

    override fun split(input: String): List<String> = input.split(value)
}

/**
 * Regex-based delimiter for complex matching.
 *
 * Stores the underlying Java [Pattern] to preserve flags (e.g., CASE_INSENSITIVE, DOTALL) and
 * delegates to `Pattern.split`, which mirrors JDK behavior (trailing empty segments removed).
 * Immutable and thread-safe; reuse for repeated parsing.
 */
class RegexDelimiter(private val jPattern: Pattern) : Delimiter() {
    /** Construct from a raw pattern string (compiled with no flags). */
    constructor(pattern: String) : this(Pattern.compile(pattern))

    /** The raw regex source of the underlying [Pattern]. */
    val pattern: String
        get() = jPattern.pattern()

    /** The Java regex flags on the underlying [Pattern] (e.g., CASE_INSENSITIVE). */
    val flags: Int
        get() = jPattern.flags()

    override fun split(input: String): List<String> = jPattern.split(input).toList()

    /** Two [RegexDelimiter]s are equal when both pattern text and flags are equal. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RegexDelimiter) return false
        return jPattern.pattern() == other.jPattern.pattern() &&
            jPattern.flags() == other.jPattern.flags()
    }

    /** Hash is derived from pattern text and flags. */
    override fun hashCode(): Int = 31 * jPattern.pattern().hashCode() + jPattern.flags()

    /** Human-readable representation including pattern text and flags. */
    override fun toString(): String =
        "RegexDelimiter(pattern='${jPattern.pattern()}', flags=${jPattern.flags()})"
}
