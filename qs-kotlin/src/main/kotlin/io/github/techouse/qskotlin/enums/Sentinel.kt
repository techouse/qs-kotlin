package io.github.techouse.qskotlin.enums

/**
 * An enum of available charset sentinels.
 * - [value] is the raw sentinel value (e.g., "✓" or "&#10003;").
 * - [encoded] is the pre-encoded query pair including the parameter name, e.g. "utf8=%E2%9C%93".
 */
enum class Sentinel(val value: String, val encoded: String) {
    /**
     * This is what browsers will submit when the ✓ character occurs in an
     * application/x-www-form-urlencoded body and the encoding of the page containing the form is
     * iso-8859-1, or when the submitted form has an accept-charset attribute of iso-8859-1.
     * Presumably also with other charsets that do not contain the ✓ character, such as us-ascii.
     */
    ISO(value = "&#10003;", encoded = "utf8=%26%2310003%3B"),

    /**
     * These are the percent-encoded utf-8 octets representing a checkmark, indicating that the
     * request actually is utf-8 encoded.
     */
    CHARSET(value = "✓", encoded = "utf8=%E2%9C%93");

    /** Return the encoded query parameter (same as [toString]). */
    fun asQueryParam(): String = encoded

    /** Java-friendly: return ("utf8", rawValue) as an immutable entry. */
    fun toEntry(): Map.Entry<String, String> = mapOf(PARAM_NAME to value).entries.first()

    override fun toString(): String = encoded

    companion object {
        /** The query parameter name used by charset sentinels. */
        const val PARAM_NAME: String = "utf8"
    }
}
