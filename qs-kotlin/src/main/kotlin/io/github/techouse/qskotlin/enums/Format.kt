package io.github.techouse.qskotlin.enums

typealias Formatter = (value: String) -> String

/** Java-friendly functional interface for supplying a formatter from Java. */
fun interface JFormatter {
    fun format(value: String): String
}

/** An enum of all available format options. */
enum class Format(val formatter: Formatter) {
    /** RFC 3986 format (default) https://datatracker.ietf.org/doc/html/rfc3986 */
    RFC3986({ value: String -> value }),

    /** RFC 1738 format https://datatracker.ietf.org/doc/html/rfc1738 */
    RFC1738({ value: String -> value.replace("%20", "+") });

    /**
     * Apply this format to a percent-encoded value. Java-friendly helper so callers can do:
     * `Format.RFC1738.format(value)`.
     */
    fun format(value: String): String = formatter(value)

    companion object {
        /**
         * Adapt a Java 1-arg Function into a Kotlin [Formatter]. Usage (Java): `Formatter f =
         * Format.formatter(v -> v.replace(" ", "+"));`
         */
        @JvmStatic
        fun formatter(fn: java.util.function.Function<String, String>): Formatter = { v ->
            fn.apply(v)
        }

        /**
         * Adapt a Java-friendly SAM [JFormatter] into a Kotlin [Formatter]. Usage (Java):
         * `Formatter f = Format.formatter((JFormatter) v -> v);`
         */
        @JvmStatic fun formatter(fn: JFormatter): Formatter = { v -> fn.format(v) }

        /** Java ergonomic overload. */
        @JvmStatic
        fun formatter(fn: java.util.function.UnaryOperator<String>): Formatter = { v ->
            fn.apply(v)
        }
    }
}
