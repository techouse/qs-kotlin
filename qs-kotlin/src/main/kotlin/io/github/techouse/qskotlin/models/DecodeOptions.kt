package io.github.techouse.qskotlin.models

import io.github.techouse.qskotlin.Utils
import io.github.techouse.qskotlin.enums.Duplicates
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * A function that decodes a value from a query string or form data. It takes a value and an
 * optional charset, returning the decoded value.
 *
 * @param value The encoded value to decode.
 * @param charset The character set to use for decoding, if any.
 * @return The decoded value, or null if the value is not present.
 */
typealias Decoder = (value: String?, charset: Charset?) -> Any?

/** Options that configure the output of Qs.decode. */
data class DecodeOptions(
    /** Set to `true` to decode dot Map notation in the encoded input. */
    private val allowDots: Boolean? = null,

    /** Set a Decoder to affect the decoding of the input. */
    private val decoder: Decoder? = null,

    /**
     * Set to `true` to decode dots in keys.
     *
     * Note: it implies allowDots, so QS.decode will error if you set decodeDotInKeys to `true`, and
     * allowDots to `false`.
     */
    private val decodeDotInKeys: Boolean? = null,

    /** Set to `true` to allow empty List values inside Maps in the encoded input. */
    val allowEmptyLists: Boolean = false,

    /**
     * QS will limit specifying indices in a List to a maximum index of `20`. Any List members with
     * an index of greater than `20` will instead be converted to a Map with the index as the key.
     * This is needed to handle cases when someone sent, for example, `a[999999999]` and it will
     * take significant time to iterate over this huge List. This limit can be overridden by passing
     * a listLimit option.
     */
    val listLimit: Int = 20,

    /** The character encoding to use when decoding the input. */
    val charset: Charset = StandardCharsets.UTF_8,

    /**
     * Some services add an initial `utf8=✓` value to forms so that old InternetExplorer versions
     * are more likely to submit the form as UTF-8. Additionally, the server can check the value
     * against wrong encodings of the checkmark character and detect that a query string or
     * `application/x-www-form-urlencoded` body was *not* sent as UTF-8, eg. if the form had an
     * `accept-charset` parameter or the containing page had a different character set.
     *
     * QS supports this mechanism via the charsetSentinel option. If specified, the UTF-8 parameter
     * will be omitted from the returned Map. It will be used to switch to ISO-8859-1/UTF-8 mode
     * depending on how the checkmark is encoded.
     *
     * Important: When you specify both the charset option and the charsetSentinel option, the
     * charset will be overridden when the request contains a UTF-8 parameter from which the actual
     * charset can be deduced. In that sense the charset will behave as the default charset rather
     * than the authoritative charset.
     */
    val charsetSentinel: Boolean = false,

    /**
     * Set to `true` to parse the input as a comma-separated value.
     *
     * Note: nested Maps, such as `'a={b:1},{c:d}'` are not supported.
     */
    val comma: Boolean = false,

    /** The delimiter to use when splitting key-value pairs in the encoded input. */
    val delimiter: Delimiter = StringDelimiter("&"),

    /**
     * By default, when nesting Maps QS will only decode up to 5 children deep. This depth can be
     * overridden by setting the depth. The depth limit helps mitigate abuse when qs is used to
     * parse user input, and it is recommended to keep it a reasonably small number.
     */
    val depth: Int = 5,

    /**
     * For similar reasons, by default QS will only parse up to 1000 parameters. This can be
     * overridden by passing a parameterLimit option.
     */
    val parameterLimit: Int = 1000,

    /** Change the duplicate key handling strategy */
    val duplicates: Duplicates = Duplicates.COMBINE,

    /** Set to `true` to ignore the leading question mark query prefix in the encoded input. */
    val ignoreQueryPrefix: Boolean = false,

    /** Set to `true` to interpret HTML numeric entities (`&#...;`) in the encoded input. */
    val interpretNumericEntities: Boolean = false,

    /** To disable List parsing entirely, set parseLists to `false`. */
    val parseLists: Boolean = true,

    /**
     * Set to `true` to add a layer of protection by throwing an error when the limit is exceeded,
     * allowing you to catch and handle such cases.
     */
    val strictDepth: Boolean = false,

    /** Set to true to decode values without `=` to `null`. */
    val strictNullHandling: Boolean = false,

    /** Set to `true` to throw an error when the limit is exceeded. */
    val throwOnLimitExceeded: Boolean = false,
) {
    /** The List encoding format to use. */
    val getAllowDots: Boolean
        get() = allowDots ?: (decodeDotInKeys == true)

    /** The List encoding format to use. */
    val getDecodeDotInKeys: Boolean
        get() = decodeDotInKeys ?: false

    init {
        require(charset == StandardCharsets.UTF_8 || charset == StandardCharsets.ISO_8859_1) {
            "Invalid charset"
        }
        require(parameterLimit > 0) { "Parameter limit must be positive" }
        require(!getDecodeDotInKeys || getAllowDots) {
            "decodeDotInKeys requires allowDots to be true"
        }
    }

    /** Decode the input using the specified Decoder. */
    fun getDecoder(value: String?, charset: Charset? = null): Any? =
        if (decoder != null) decoder.invoke(value, charset) else Utils.decode(value, charset)
}
