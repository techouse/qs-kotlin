package io.github.techouse.qskotlin.enums

typealias ListFormatGenerator = (prefix: String, key: String?) -> String

/** An enum of all available list format options. */
enum class ListFormat(val generator: ListFormatGenerator) {
    /** Use brackets to represent list items, for example `foo[]=123&foo[]=456&foo[]=789` */
    BRACKETS({ prefix: String, _ -> "$prefix[]" }),

    /** Use commas to represent list items, for example `foo=123,456,789` */
    COMMA({ prefix: String, _ -> prefix }),

    /** Repeat the same key to represent list items, for example `foo=123&foo=456&foo=789` */
    REPEAT({ prefix: String, _ -> prefix }),

    /**
     * Use indices in brackets to represent list items, for example
     * `foo[0]=123&foo[1]=456&foo[2]=789`
     */
    INDICES({ prefix: String, key: String? -> "$prefix[$key]" });
}
