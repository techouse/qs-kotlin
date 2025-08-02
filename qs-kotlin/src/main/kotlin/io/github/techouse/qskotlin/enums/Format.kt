package io.github.techouse.qskotlin.enums

typealias Formatter = (value: String) -> String

/** An enum of all available format options. */
enum class Format(val formatter: Formatter) {
    /** RFC 3986 format (default) https://datatracker.ietf.org/doc/html/rfc3986 */
    RFC3986({ value: String -> value }),

    /** RFC 1738 format https://datatracker.ietf.org/doc/html/rfc1738 */
    RFC1738({ value: String -> value.replace("%20", "+") });
}
