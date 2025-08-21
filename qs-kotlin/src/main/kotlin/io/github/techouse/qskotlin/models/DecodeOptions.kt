package io.github.techouse.qskotlin.models

import io.github.techouse.qskotlin.enums.DecodeKind
import io.github.techouse.qskotlin.enums.Duplicates
import io.github.techouse.qskotlin.internal.Utils
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/** Unified scalar decoder. Implementations may ignore `charset` and/or `kind`. */
fun interface Decoder {
    fun decode(value: String?, charset: Charset?, kind: DecodeKind?): Any?
}

/** Back‑compat adapter for `(value, charset) -> Any?` decoders. */
@Deprecated(
    message =
        "Use Decoder fun interface; wrap your two‑arg lambda: Decoder { v, c, _ -> legacy(v, c) }",
    replaceWith = ReplaceWith("Decoder { value, charset, _ -> legacyDecoder(value, charset) }"),
    level = DeprecationLevel.WARNING,
)
typealias LegacyDecoder = (String?, Charset?) -> Any?

/** Options that configure the output of Qs.decode. */
data class DecodeOptions(
    /** Set to `true` to decode dot Map notation in the encoded input. */
    private val allowDots: Boolean? = null,

    /** Set a Decoder to affect the decoding of the input. */
    private val decoder: Decoder? = null,
    @Deprecated(
        message = "Use `decoder` fun interface; this will be removed in a future major release",
        replaceWith = ReplaceWith("decoder"),
        level = DeprecationLevel.WARNING,
    )
    @Suppress("DEPRECATION")
    private val legacyDecoder: LegacyDecoder? = null,

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
     * Set to `true` to allow sparse Lists in the encoded input.
     *
     * Note: If set to `true`, the lists will contain `null` values for missing values.
     */
    val allowSparseLists: Boolean = false,

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
     * Enforce the [depth] limit when parsing nested keys.
     *
     * When `true`, exceeding [depth] throws an `IndexOutOfBoundsException` during key splitting.
     * When `false` (default), any remainder beyond [depth] is treated as a single trailing segment
     * (matching the reference `qs` behavior).
     */
    val strictDepth: Boolean = false,

    /** Set to true to decode values without `=` to `null`. */
    val strictNullHandling: Boolean = false,

    /** Set to `true` to throw an error when the limit is exceeded. */
    val throwOnLimitExceeded: Boolean = false,
) {
    /**
     * Effective `allowDots` value.
     *
     * Returns `true` when `allowDots == true` **or** when `decodeDotInKeys == true` (since decoding
     * dots in keys implies dot‑splitting). Otherwise returns `false`.
     */
    val getAllowDots: Boolean
        get() = allowDots ?: (decodeDotInKeys == true)

    /**
     * Effective `decodeDotInKeys` value.
     *
     * Defaults to `false` when unspecified. When `true`, encoded dots (`%2E`/`%2e`) inside key
     * segments are mapped to `.` **after** splitting, without introducing extra dot‑splits.
     */
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

    /**
     * Unified scalar decode with key/value context.
     *
     * Uses the provided [decoder] when set; otherwise falls back to [Utils.decode]. For backward
     * compatibility, a [legacyDecoder] `(value, charset)` can be supplied and is adapted
     * internally. The [kind] will be [DecodeKind.KEY] for keys (and key segments) and
     * [DecodeKind.VALUE] for values.
     */
    internal fun decode(
        value: String?,
        charset: Charset? = null,
        kind: DecodeKind = DecodeKind.VALUE,
    ): Any? {
        @Suppress("DEPRECATION")
        val d = decoder ?: legacyDecoder?.let { legacy -> Decoder { v, c, _ -> legacy(v, c) } }
        return if (d != null) {
            d.decode(value, charset, kind) // honor nulls from user decoder
        } else {
            defaultDecode(value, charset, kind)
        }
    }

    /**
     * Default library decode.
     *
     * For [DecodeKind.KEY], protects encoded dots (`%2E`/`%2e`) **before** percent‑decoding so key
     * splitting and post‑split mapping run on the intended tokens.
     */
    private fun defaultDecode(value: String?, charset: Charset?, kind: DecodeKind): Any? {
        if (value == null) return null
        if (kind == DecodeKind.KEY) {
            val protected = protectEncodedDotsForKeys(value, includeOutsideBrackets = getAllowDots)
            return Utils.decode(protected, charset)
        }
        return Utils.decode(value, charset)
    }

    /**
     * Double‑encode %2E/%2e in KEY strings so the percent‑decoder does not turn them into '.' too
     * early.
     *
     * When [includeOutsideBrackets] is true, occurrences both inside and outside bracket segments
     * are protected. Otherwise, only those **inside** `[...]` are protected. Note: only literal
     * `[`/`]` affect depth; percent‑encoded brackets (`%5B`/`%5D`) are treated as content, not
     * structure.
     */
    private fun protectEncodedDotsForKeys(input: String, includeOutsideBrackets: Boolean): String {
        val pct = input.indexOf('%')
        if (pct < 0) return input
        if (input.indexOf("2E", pct) < 0 && input.indexOf("2e", pct) < 0) return input
        val n = input.length
        val sb = StringBuilder(n + 8)
        var depth = 0
        var i = 0
        while (i < n) {
            when (val ch = input[i]) {
                '[' -> {
                    depth++
                    sb.append(ch)
                    i++
                }
                ']' -> {
                    if (depth > 0) depth--
                    sb.append(ch)
                    i++
                }
                '%' -> {
                    if (
                        i + 2 < n &&
                            input[i + 1] == '2' &&
                            (input[i + 2] == 'E' || input[i + 2] == 'e')
                    ) {
                        val inside = depth > 0
                        if (inside || includeOutsideBrackets) {
                            sb.append("%25").append(if (input[i + 2] == 'E') "2E" else "2e")
                        } else {
                            sb.append('%').append('2').append(input[i + 2])
                        }
                        i += 3
                    } else {
                        sb.append(ch)
                        i++
                    }
                }
                else -> {
                    sb.append(ch)
                    i++
                }
            }
        }
        return sb.toString()
    }

    /**
     * Back‑compat helper: decode a value without key/value kind context.
     *
     * Prefer calling [decode] directly (or [decodeKey]/[decodeValue] for explicit context).
     */
    @Deprecated(
        message =
            "Deprecated: use decodeKey/decodeValue (or decode(value, charset, kind)) to honor key/value context. This will be removed in the next major.",
        replaceWith = ReplaceWith("decode(value, charset)"),
        level = DeprecationLevel.WARNING,
    )
    @Suppress("unused")
    @JvmOverloads
    fun getDecoder(value: String?, charset: Charset? = null): Any? = decode(value, charset)

    /** Convenience: decode a key to String? */
    internal fun decodeKey(value: String?, charset: Charset?): String? =
        decode(value, charset, DecodeKind.KEY)?.toString() // keys are always coerced to String

    /** Convenience: decode a value */
    internal fun decodeValue(value: String?, charset: Charset?): Any? =
        decode(value, charset, DecodeKind.VALUE)
}
