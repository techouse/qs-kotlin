package io.github.techouse.qskotlin.models

import io.github.techouse.qskotlin.enums.Format
import io.github.techouse.qskotlin.enums.Formatter
import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.internal.Utils
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

/**
 * ValueEncoder function typealias that takes a value, charset, and format, and returns a String
 * representation of the value.
 *
 * The encoder can be used to customize how values are encoded in the query string. If no encoder is
 * provided, Utils.encode will be used by default.
 */
typealias ValueEncoder = (value: Any?, charset: Charset?, format: Format?) -> String

/** Java-friendly functional interface for value encoding (Tri-function). */
fun interface JValueEncoder {
    fun apply(value: Any?, charset: Charset?, format: Format?): String
}

/**
 * DateSerializer function typealias that takes a LocalDateTime and returns a String representation
 * of the date.
 *
 * This can be used to customize how LocalDateTime objects are serialized in the encoded output. If
 * no serializer is provided, the default ISO format will be used.
 */
typealias DateSerializer = (date: LocalDateTime) -> String

/** Java-friendly functional interface for date serialization. */
fun interface JDateSerializer {
    fun apply(date: LocalDateTime): String
}

/**
 * Sorter function typealias that takes two values (a and b) and returns an Int indicating their
 * order.
 *
 * This can be used to customize the sorting of keys in the encoded output. If no sorter is
 * provided, the default order will be used.
 */
typealias Sorter = (a: Any?, b: Any?) -> Int

/** Options that configure the output of Qs.encode. */
data class EncodeOptions(
    /**
     * Set an Encoder to affect the encoding of values. Note: the encoder option does not apply if
     * encode is `false`
     */
    private val encoder: ValueEncoder? = null,

    /**
     * If you only want to override the serialization of DateTime objects, you can provide a custom
     * DateSerializer.
     */
    private val dateSerializer: DateSerializer? = null,

    /** The List encoding format to use. */
    private val listFormat: ListFormat? = null,
    @Deprecated(
        message = "Use listFormat instead",
        replaceWith = ReplaceWith("listFormat"),
        level = DeprecationLevel.WARNING,
    )
    val indices: Boolean? = null,

    /** Set to `true` to use dot Map notation in the encoded output. */
    private val allowDots: Boolean? = null,

    /** Set to `true` to add a question mark `?` prefix to the encoded output. */
    val addQueryPrefix: Boolean = false,

    /** Set to `true` to allow empty Lists in the encoded output. */
    val allowEmptyLists: Boolean = false,

    /** The character encoding to use. */
    val charset: Charset = StandardCharsets.UTF_8,

    /**
     * Set to `true` to announce the character by including an `utf8=✓` parameter with the proper
     * encoding of the checkmark, similar to what Ruby on Rails and others do when submitting forms.
     */
    val charsetSentinel: Boolean = false,

    /** The delimiter to use when joining key-value pairs in the encoded output. */
    val delimiter: StringDelimiter = Delimiter.AMPERSAND,

    /** Set to `false` to disable encoding. */
    val encode: Boolean = true,

    /**
     * Encode Map keys using dot notation by setting encodeDotInKeys to `true`:
     *
     * Caveat: When encodeValuesOnly is `true` as well as encodeDotInKeys, only dots in keys and
     * nothing else will be encoded.
     */
    val encodeDotInKeys: Boolean = false,

    /** Encoding can be disabled for keys by setting the encodeValuesOnly to `true` */
    val encodeValuesOnly: Boolean = false,

    /**
     * The encoding format to use. The default format is Format.RFC3986 which encodes `' '` to `%20`
     * which is backward compatible. You can also set format to Format.RFC1738 which encodes `' '`
     * to `+`.
     */
    val format: Format = Format.RFC3986,

    /**
     * Use the filter option to restrict which keys will be included in the encoded output. If you
     * pass a Function, it will be called for each key to obtain the replacement value. If you pass
     * a List, it will be used to select properties and List indices to be encoded.
     */
    val filter: Filter? = null,

    /** Set to `true` to completely skip encoding keys with `null` values */
    val skipNulls: Boolean = false,

    /**
     * Set to `true` to distinguish between `null` values and empty Strings. This way the encoded
     * string `null` values will have no `=` sign.
     */
    val strictNullHandling: Boolean = false,

    /**
     * When listFormat is set to ListFormat.COMMA, you can also set commaRoundTrip option to `true`
     * or `false`, to append `[]` on single-item Lists, so that they can round trip through a parse.
     */
    val commaRoundTrip: Boolean? = null,

    /** Set a Sorter to affect the order of parameter keys. */
    val sort: Sorter? = null,
) {
    /** Convenience getter: effective allowDots (fallbacks to encodeDotInKeys when null). */
    val getAllowDots: Boolean
        get() = allowDots ?: encodeDotInKeys

    /**
     * Convenience getter for accessing the ListFormat.
     *
     * If `listFormat` is not set, it will use `indices` if available, otherwise defaults to
     * ListFormat.INDICES.
     */
    @Suppress("DEPRECATION") // Use listFormat instead of indices
    val getListFormat: ListFormat
        get() =
            listFormat
                ?: indices?.let { if (it) ListFormat.INDICES else ListFormat.REPEAT }
                ?: ListFormat.INDICES

    /** Convenience getter for accessing the format's formatter */
    val formatter: Formatter
        get() = format.formatter

    init {
        // Validate charset
        require(charset == StandardCharsets.UTF_8 || charset == StandardCharsets.ISO_8859_1) {
            "Invalid charset"
        }
    }

    /**
     * Encodes a value to a String.
     *
     * Uses the provided encoder if available, otherwise uses Utils.encode.
     */
    @JvmOverloads
    fun getEncoder(value: Any?, charset: Charset? = null, format: Format? = null): String =
        encoder?.invoke(value, charset ?: this.charset, format ?: this.format)
            ?: Utils.encode(value, charset ?: this.charset, format ?: this.format)

    /**
     * Serializes a LocalDateTime instance to a String.
     *
     * Uses the provided serializeDate function if available, otherwise uses LocalDateTime ISO
     * format.
     */
    fun getDateSerializer(date: LocalDateTime): String =
        dateSerializer?.invoke(date) ?: date.toString()

    /**
     * Java-friendly builder to construct [EncodeOptions] without passing a long argument list.
     *
     * Usage from Java:
     * <pre>{@code
     *   EncodeOptions opts = EncodeOptions.builder()
     *       .addQueryPrefix(true)
     *       .listFormat(ListFormat.BRACKETS)
     *       .encodeValuesOnly(false)
     *       .encoder((value, cs, fmt) -> ... ) // JValueEncoder
     *       .sort(Comparator.naturalOrder())
     *       .build();
     * }</pre>
     */
    class Builder {
        private var encoder: ValueEncoder? = null
        private var dateSerializer: DateSerializer? = null
        private var listFormat: ListFormat? = null
        @Deprecated(
            message = "Use listFormat instead",
            replaceWith = ReplaceWith("listFormat"),
            level = DeprecationLevel.WARNING,
        )
        private var indices: Boolean? = null
        private var allowDots: Boolean? = null
        private var addQueryPrefix: Boolean = false
        private var allowEmptyLists: Boolean = false
        private var charset: Charset = StandardCharsets.UTF_8
        private var charsetSentinel: Boolean = false
        private var delimiter: StringDelimiter = Delimiter.AMPERSAND
        private var encode: Boolean = true
        private var encodeDotInKeys: Boolean = false
        private var encodeValuesOnly: Boolean = false
        private var format: Format = Format.RFC3986
        private var filter: Filter? = null
        private var skipNulls: Boolean = false
        private var strictNullHandling: Boolean = false
        private var commaRoundTrip: Boolean? = null
        private var sort: Sorter? = null

        // Kotlin-friendly setters
        fun encoder(encoder: ValueEncoder) = apply { this.encoder = encoder }

        fun dateSerializer(serializer: DateSerializer) = apply { this.dateSerializer = serializer }

        // Java-friendly setters
        fun encoder(encoder: JValueEncoder) = apply {
            this.encoder = { v, cs, fmt -> encoder.apply(v, cs, fmt) }
        }

        fun dateSerializer(serializer: JDateSerializer) = apply {
            this.dateSerializer = { dt -> serializer.apply(dt) }
        }

        fun listFormat(listFormat: ListFormat?) = apply { this.listFormat = listFormat }

        @Deprecated(
            message = "Use listFormat instead",
            replaceWith = ReplaceWith("listFormat"),
            level = DeprecationLevel.WARNING,
        )
        @Suppress("DEPRECATION")
        fun indices(indices: Boolean?) = apply { this.indices = indices }

        fun allowDots(allowDots: Boolean?) = apply { this.allowDots = allowDots }

        fun addQueryPrefix(add: Boolean) = apply { this.addQueryPrefix = add }

        fun allowEmptyLists(allow: Boolean) = apply { this.allowEmptyLists = allow }

        fun charset(charset: Charset) = apply { this.charset = charset }

        fun charsetSentinel(enabled: Boolean) = apply { this.charsetSentinel = enabled }

        fun delimiter(value: String) = apply { this.delimiter = StringDelimiter(value) }

        fun delimiter(value: StringDelimiter) = apply { this.delimiter = value }

        fun encode(enabled: Boolean) = apply { this.encode = enabled }

        fun encodeDotInKeys(enabled: Boolean) = apply { this.encodeDotInKeys = enabled }

        fun encodeValuesOnly(enabled: Boolean) = apply { this.encodeValuesOnly = enabled }

        fun format(format: Format) = apply { this.format = format }

        fun filter(filter: Filter?) = apply { this.filter = filter }

        fun skipNulls(skip: Boolean) = apply { this.skipNulls = skip }

        fun strictNullHandling(strict: Boolean) = apply { this.strictNullHandling = strict }

        fun commaRoundTrip(value: Boolean?) = apply { this.commaRoundTrip = value }

        fun sort(comparator: java.util.Comparator<Any?>) = apply {
            this.sort = { a, b -> comparator.compare(a, b) }
        }

        @Suppress("DEPRECATION")
        fun build(): EncodeOptions =
            EncodeOptions(
                encoder = encoder,
                dateSerializer = dateSerializer,
                listFormat = listFormat,
                indices = indices,
                allowDots = allowDots,
                addQueryPrefix = addQueryPrefix,
                allowEmptyLists = allowEmptyLists,
                charset = charset,
                charsetSentinel = charsetSentinel,
                delimiter = delimiter,
                encode = encode,
                encodeDotInKeys = encodeDotInKeys,
                encodeValuesOnly = encodeValuesOnly,
                format = format,
                filter = filter,
                skipNulls = skipNulls,
                strictNullHandling = strictNullHandling,
                commaRoundTrip = commaRoundTrip,
                sort = sort,
            )
    }

    companion object {
        /** Obtain a Java-friendly builder. */
        @JvmStatic fun builder(): Builder = Builder()

        /** A handy defaults instance for Java call sites. */
        @JvmStatic fun defaults(): EncodeOptions = EncodeOptions()
    }
}
