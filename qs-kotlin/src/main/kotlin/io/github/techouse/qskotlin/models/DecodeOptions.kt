package io.github.techouse.qskotlin.models

import io.github.techouse.qskotlin.enums.DecodeKind
import io.github.techouse.qskotlin.enums.Duplicates
import io.github.techouse.qskotlin.internal.Utils
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/** Unified scalar decoder. Implementations may ignore `charset` and/or `kind`. */
fun interface Decoder {
    fun decode(value: String?, charset: Charset?, kind: DecodeKind?): Any?
}

/** Java-friendly functional interface for Decoder (SAM for Java callers). */
@FunctionalInterface
fun interface JDecoder {
    fun decode(value: String?, charset: Charset?, kind: DecodeKind?): Any?
}

/** Java-friendly functional interface for the legacy two-arg decoder. */
@FunctionalInterface
fun interface JLegacyDecoder {
    fun decode(value: String?, charset: Charset?): Any?
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
    val delimiter: Delimiter = Delimiter.AMPERSAND,

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
     * Builder for [DecodeOptions]. Prefer this from Java to avoid long, ambiguous constructors.
     *
     * Defaults mirror the primary constructor. Set only the options you need and call [build].
     *
     * **Example (Java):**
     * <pre>{@code
     * DecodeOptions opts = DecodeOptions.builder()
     *     .ignoreQueryPrefix(true)
     *     .delimiter("&")
     *     .depth(10)
     *     .decoder((value, cs, kind) -> value) // JDecoder
     *     .build();
     * }</pre>
     */
    class Builder {
        private var allowDots: Boolean? = null
        private var decoder: Decoder? = null
        @Suppress("DEPRECATION") private var legacyDecoder: LegacyDecoder? = null
        private var decodeDotInKeys: Boolean? = null
        private var allowEmptyLists: Boolean = false
        private var allowSparseLists: Boolean = false
        private var listLimit: Int = 20
        private var charset: Charset = StandardCharsets.UTF_8
        private var charsetSentinel: Boolean = false
        private var comma: Boolean = false
        private var delimiter: Delimiter = Delimiter.AMPERSAND
        private var depth: Int = 5
        private var parameterLimit: Int = 1000
        private var duplicates: Duplicates = Duplicates.COMBINE
        private var ignoreQueryPrefix: Boolean = false
        private var interpretNumericEntities: Boolean = false
        private var parseLists: Boolean = true
        private var strictDepth: Boolean = false
        private var strictNullHandling: Boolean = false
        private var throwOnLimitExceeded: Boolean = false

        /**
         * Provide a unified Kotlin decoder. If set, this takes precedence over [legacyDecoder]. The
         * decoder receives the raw token, the effective charset, and the [DecodeKind].
         */
        fun decoder(decoder: Decoder) = apply { this.decoder = decoder }

        /**
         * Back-compat: supply a two-arg decoder `(value, charset)`. Prefer [decoder] or the Java
         * SAM [.decoder(JDecoder)].
         */
        @Suppress("DEPRECATION")
        fun legacyDecoder(decoder: LegacyDecoder) = apply { this.legacyDecoder = decoder }

        /** Java-friendly SAM for the unified decoder; adapted to Kotlin's [Decoder]. */
        fun decoder(decoder: JDecoder) = apply {
            this.decoder = Decoder { v, c, k -> decoder.decode(v, c, k) }
        }

        /** Java-friendly SAM for the legacy two-arg decoder; adapted internally. */
        fun legacyDecoder(decoder: JLegacyDecoder) = apply {
            @Suppress("DEPRECATION")
            this.legacyDecoder = { v, c ->
                decoder.decode(v, c)
            }
        }

        /**
         * Enable dot-notation splitting in keys. When `true`, `a.b=c` becomes `{a:{b:"c"}}`. If
         * unspecified, it is implied by [decodeDotInKeys] when that is `true`.
         */
        fun allowDots(value: Boolean?) = apply { this.allowDots = value }

        /**
         * Treat encoded dots at the top level as split points (e.g. `a%2Eb`). Requires [allowDots]
         * not be explicitly `false`.
         */
        fun decodeDotInKeys(value: Boolean?) = apply { this.decodeDotInKeys = value }

        /** Allow list parameters with no values (e.g., `a[]` → empty list). */
        fun allowEmptyLists(value: Boolean) = apply { this.allowEmptyLists = value }

        /** Allow sparse lists; missing indices are represented as `null`. */
        fun allowSparseLists(value: Boolean) = apply { this.allowSparseLists = value }

        /**
         * Maximum explicit index allowed when parsing lists. Higher indices trigger a map fallback
         * to avoid huge sparse allocations. Default is 20.
         */
        fun listLimit(value: Int) = apply { this.listLimit = value }

        /** Set the decoding charset (UTF-8 or ISO-8859-1). */
        fun charset(value: Charset) = apply { this.charset = value }

        /**
         * Honor an initial `utf8=✓` parameter to auto-detect charset per qs conventions. When
         * enabled, the sentinel is consumed and not included in output.
         */
        fun charsetSentinel(value: Boolean) = apply { this.charsetSentinel = value }

        /** Parse values as comma-separated sequences when appropriate (flat only). */
        fun comma(value: Boolean) = apply { this.comma = value }

        /** Set the pair delimiter using a literal string (e.g., "&" or ";"). */
        fun delimiter(value: String) = apply { this.delimiter = StringDelimiter(value) }

        /** Provide a prebuilt [Delimiter] (e.g., [StringDelimiter] or [RegexDelimiter]). */
        fun delimiter(value: Delimiter) = apply { this.delimiter = value }

        /**
         * Use a Java [Pattern] as a regex delimiter; flags (CASE_INSENSITIVE, DOTALL, …) are
         * preserved.
         */
        fun delimiter(value: Pattern) = apply { this.delimiter = RegexDelimiter(value) }

        /** Compile and use a regex delimiter from pattern + flags, preserving those flags. */
        fun delimiterRegex(pattern: String, flags: Int) = apply {
            this.delimiter = RegexDelimiter(Pattern.compile(pattern, flags))
        }

        /** Maximum nesting depth for parsed objects (default 5). */
        fun depth(value: Int) = apply { this.depth = value }

        /** Maximum number of parameters to parse (default 1000). */
        fun parameterLimit(value: Int) = apply { this.parameterLimit = value }

        /** Strategy for handling duplicate keys; see [Duplicates] (COMBINE, LAST, FIRST). */
        fun duplicates(value: Duplicates) = apply { this.duplicates = value }

        /** Ignore a leading `?` in the input (useful for raw URLs). */
        fun ignoreQueryPrefix(value: Boolean) = apply { this.ignoreQueryPrefix = value }

        /** Convert HTML numeric entities (e.g., `&#9786;`) during decoding. */
        fun interpretNumericEntities(value: Boolean) = apply {
            this.interpretNumericEntities = value
        }

        /** Disable or enable list parsing entirely (default enabled). */
        fun parseLists(value: Boolean) = apply { this.parseLists = value }

        /**
         * When `true`, exceeding [depth] throws; when `false`, remainder becomes a trailing
         * segment.
         */
        fun strictDepth(value: Boolean) = apply { this.strictDepth = value }

        /** When `true`, parameters without `=` decode to `null` (rather than empty string). */
        fun strictNullHandling(value: Boolean) = apply { this.strictNullHandling = value }

        /** Throw when any parsing limit is exceeded (e.g., [parameterLimit]). */
        fun throwOnLimitExceeded(value: Boolean) = apply { this.throwOnLimitExceeded = value }

        /** Build an immutable [DecodeOptions] with the configured values. */
        fun build(): DecodeOptions =
            DecodeOptions(
                allowDots = allowDots,
                decoder = decoder,
                legacyDecoder = legacyDecoder,
                decodeDotInKeys = decodeDotInKeys,
                allowEmptyLists = allowEmptyLists,
                allowSparseLists = allowSparseLists,
                listLimit = listLimit,
                charset = charset,
                charsetSentinel = charsetSentinel,
                comma = comma,
                delimiter = delimiter,
                depth = depth,
                parameterLimit = parameterLimit,
                duplicates = duplicates,
                ignoreQueryPrefix = ignoreQueryPrefix,
                interpretNumericEntities = interpretNumericEntities,
                parseLists = parseLists,
                strictDepth = strictDepth,
                strictNullHandling = strictNullHandling,
                throwOnLimitExceeded = throwOnLimitExceeded,
            )
    }

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
     * Defaults to `false` when unspecified. Inside bracket segments, percent-decoding will
     * naturally yield '.' from `%2E/%2e`. `decodeDotInKeys` controls whether encoded dots at the
     * top level are treated as additional split points; it does not affect the literal '.' produced
     * by percent-decoding inside bracket segments.
     */
    val getDecodeDotInKeys: Boolean
        get() = decodeDotInKeys ?: false

    // Java-friendly aliases (non-breaking):
    @JvmName("isAllowDotsEffective") fun isAllowDotsEffective(): Boolean = getAllowDots

    @JvmName("isDecodeDotInKeysEffective")
    fun isDecodeDotInKeysEffective(): Boolean = getDecodeDotInKeys

    init {
        require(charset == StandardCharsets.UTF_8 || charset == StandardCharsets.ISO_8859_1) {
            "Invalid charset: only UTF-8 and ISO-8859-1 (Latin1) are supported"
        }
        require(parameterLimit > 0) { "Parameter limit must be positive" }
        require(depth >= 0) { "Depth must be non-negative" }
        // If decodeDotInKeys is enabled, allowDots must not be explicitly false.
        require(!getDecodeDotInKeys || allowDots != false) {
            "decodeDotInKeys requires allowDots to be true"
        }
    }

    /**
     * Unified scalar decode with key/value context.
     *
     * Uses the provided [decoder] when set; otherwise falls back to [Utils.decode]. For backward
     * compatibility, a [legacyDecoder] `(value, charset)` can be supplied and is adapted
     * internally. The [kind] will be [DecodeKind.KEY] for keys (and key segments) and
     * [DecodeKind.VALUE] for values, and is forwarded to custom decoders. The library default does
     * not vary decoding based on [kind].
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
            defaultDecode(value, charset)
        }
    }

    /**
     * Default library decode.
     *
     * Keys are decoded identically to values via [Utils.decode], which percent‑decodes `%2E/%2e` to
     * '.'. Whether a '.' participates in key splitting is decided by the parser (based on options).
     */
    private fun defaultDecode(value: String?, charset: Charset?): Any? {
        if (value == null) return null
        // Keys decode exactly like values; do NOT “protect” encoded dots.
        return Utils.decode(value, charset)
    }

    /** Convenience: decode a key to String? */
    @JvmOverloads
    fun decodeKey(value: String?, charset: Charset? = this.charset): String? =
        decode(value, charset, DecodeKind.KEY)?.toString() // keys are always coerced to String

    /** Convenience: decode a value */
    @JvmOverloads
    fun decodeValue(value: String?, charset: Charset? = this.charset): Any? =
        decode(value, charset, DecodeKind.VALUE)

    companion object {
        /** Obtain a Java-friendly builder. */
        @JvmStatic fun builder(): Builder = Builder()

        /** A handy defaults instance for Java call sites. */
        @JvmStatic fun defaults(): DecodeOptions = DecodeOptions()
    }
}
