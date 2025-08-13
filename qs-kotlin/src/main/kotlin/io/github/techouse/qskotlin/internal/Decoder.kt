package io.github.techouse.qskotlin.internal

import io.github.techouse.qskotlin.enums.Duplicates
import io.github.techouse.qskotlin.enums.Sentinel
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.Undefined
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/** A helper object for decoding query strings into structured data. */
internal object Decoder {
    /**
     * Parses a list value from a string or any other type, applying the options provided.
     *
     * @param value The value to parse, which can be a String or any other type.
     * @param options The decoding options that affect how the value is parsed.
     * @param currentListLength The current length of the list being parsed, used for limit checks.
     * @return The parsed value, which may be a List or the original value if no parsing is needed.
     */
    private fun parseListValue(value: Any?, options: DecodeOptions, currentListLength: Int): Any? {
        if (value is String && value.isNotEmpty() && options.comma && value.contains(',')) {
            val splitVal = value.split(',')
            if (options.throwOnLimitExceeded && splitVal.size > options.listLimit) {
                throw IndexOutOfBoundsException(
                    "List limit exceeded. " +
                        "Only ${options.listLimit} element${if (options.listLimit == 1) "" else "s"} allowed in a list."
                )
            }
            return splitVal
        }

        if (options.throwOnLimitExceeded && currentListLength >= options.listLimit) {
            throw IndexOutOfBoundsException(
                "List limit exceeded. " +
                    "Only ${options.listLimit} element${if (options.listLimit == 1) "" else "s"} allowed in a list."
            )
        }

        return value
    }

    /**
     * Parses a query string into a map of key-value pairs, handling various options for decoding.
     *
     * @param str The query string to parse.
     * @param options The decoding options that affect how the string is parsed.
     * @return A mutable map containing the parsed key-value pairs.
     * @throws IllegalArgumentException if the parameter limit is not a positive integer.
     * @throws IndexOutOfBoundsException if the parameter limit is exceeded and
     *   `throwOnLimitExceeded` is true.
     */
    internal fun parseQueryStringValues(
        str: String,
        options: DecodeOptions = DecodeOptions(),
    ): MutableMap<String, Any?> {
        val obj = mutableMapOf<String, Any?>()

        val cleanStr =
            (if (options.ignoreQueryPrefix) str.removePrefix("?") else str)
                .replace("%5B", "[", ignoreCase = true)
                .replace("%5D", "]", ignoreCase = true)

        val limit = if (options.parameterLimit == Int.MAX_VALUE) null else options.parameterLimit

        if (limit != null && limit <= 0) {
            throw IllegalArgumentException("Parameter limit must be a positive integer.")
        }

        val parts =
            if (limit != null) {
                val allParts: List<String> = options.delimiter.split(cleanStr)
                val takeCount: Int = if (options.throwOnLimitExceeded) limit + 1 else limit
                allParts.take(takeCount)
            } else {
                options.delimiter.split(cleanStr)
            }

        if (options.throwOnLimitExceeded && limit != null && parts.size > limit) {
            throw IndexOutOfBoundsException(
                "Parameter limit exceeded. Only $limit parameter${if (limit == 1) "" else "s"} allowed."
            )
        }

        var skipIndex = -1 // Keep track of where the utf8 sentinel was found
        var charset: Charset = options.charset

        if (options.charsetSentinel) {
            for (i: Int in parts.indices) {
                if (parts[i].startsWith("utf8=")) {
                    charset =
                        when (parts[i]) {
                            Sentinel.CHARSET.toString() -> StandardCharsets.UTF_8
                            Sentinel.ISO.toString() -> StandardCharsets.ISO_8859_1
                            else -> charset
                        }
                    skipIndex = i
                    break
                }
            }
        }

        for (i in parts.indices) {
            if (i == skipIndex) continue

            val part = parts[i]
            val bracketEqualsPos = part.indexOf("]=")
            val pos = if (bracketEqualsPos == -1) part.indexOf('=') else bracketEqualsPos + 1

            val key: String
            var value: Any?

            if (pos == -1) {
                key = options.getDecoder(part, charset).toString()
                value = if (options.strictNullHandling) null else ""
            } else {
                key = options.getDecoder(part.take(pos), charset).toString()
                value =
                    Utils.apply<Any?>(
                        parseListValue(
                            part.substring(pos + 1),
                            options,
                            if (obj.containsKey(key) && obj[key] is List<*>) {
                                (obj[key] as List<*>).size
                            } else 0,
                        )
                    ) { v: Any? ->
                        options.getDecoder((v as String?), charset)
                    }
            }

            if (
                value != null &&
                    !Utils.isEmpty(value) &&
                    options.interpretNumericEntities &&
                    charset == StandardCharsets.ISO_8859_1
            ) {
                value =
                    Utils.interpretNumericEntities(
                        if (value is Iterable<*>) value.joinToString(",") { it.toString() }
                        else value.toString()
                    )
            }

            if (part.contains("[]=")) {
                value = if (value is Iterable<*>) listOf(value) else value
            }

            val existing = obj.containsKey(key)
            when {
                existing && options.duplicates == Duplicates.COMBINE -> {
                    obj[key] = Utils.combine<Any?>(obj[key], value)
                }

                !existing || options.duplicates == Duplicates.LAST -> {
                    obj[key] = value
                }
            }
        }

        return obj
    }

    /**
     * Parses a chain of keys into an object, handling nested structures and lists.
     *
     * @param chain The list of keys representing the structure to parse.
     * @param value The value to assign to the last key in the chain.
     * @param options The decoding options that affect how the object is parsed.
     * @param valuesParsed Indicates whether the values have already been parsed.
     * @return The resulting object after parsing the chain.
     */
    private fun parseObject(
        chain: List<String>,
        value: Any?,
        options: DecodeOptions,
        valuesParsed: Boolean,
    ): Any? {
        val currentListLength =
            if (chain.isNotEmpty() && chain.last() == "[]") {
                val parentKey = chain.dropLast(1).joinToString("").toIntOrNull()
                if (parentKey != null && value is List<*> && value.indices.contains(parentKey)) {
                    (value[parentKey] as? List<*>)?.size ?: 0
                } else 0
            } else 0

        var leaf = if (valuesParsed) value else parseListValue(value, options, currentListLength)

        for (i in chain.size - 1 downTo 0) {
            val root: String = chain[i]
            val obj: Any?

            if (root == "[]" && options.parseLists) {
                obj =
                    when {
                        options.allowEmptyLists &&
                            (leaf == "" || (options.strictNullHandling && leaf == null)) ->
                            mutableListOf<Any?>()
                        else -> Utils.combine<Any?>(emptyList<Any?>(), leaf)
                    }
            } else {
                // Always build *string-keyed* maps here
                val mutableObj = LinkedHashMap<String, Any?>(1)

                val cleanRoot =
                    if (root.startsWith("[") && root.endsWith("]")) {
                        root.substring(1, root.length - 1)
                    } else root

                val decodedRoot =
                    if (options.getDecodeDotInKeys) cleanRoot.replace("%2E", ".") else cleanRoot

                val isPureNumeric = decodedRoot.isNotEmpty() && decodedRoot.all { it.isDigit() }
                val idx: Int? = if (isPureNumeric) decodedRoot.toInt() else null
                val isBracketedNumeric =
                    idx != null && root != decodedRoot && idx.toString() == decodedRoot

                when {
                    // If list parsing is disabled OR listLimit < 0: always make a map with string
                    // key
                    !options.parseLists || options.listLimit < 0 -> {
                        val keyForMap = if (decodedRoot == "") "0" else decodedRoot
                        mutableObj[keyForMap] = leaf
                        obj = mutableObj
                    }

                    // Proper list index (e.g., "[3]") and allowed by listLimit -> build a list
                    isBracketedNumeric && idx >= 0 && idx <= options.listLimit -> {
                        val list = MutableList<Any?>(idx + 1) { Undefined.Companion() }
                        list[idx] = leaf
                        obj = list
                    }

                    // Otherwise, treat it as a map with *string* key (even if numeric)
                    else -> {
                        val keyForMap = decodedRoot
                        mutableObj[keyForMap] = leaf
                        obj = mutableObj
                    }
                }
            }

            leaf = obj
        }

        return leaf
    }

    /**
     * Parses a key and its associated value into an object, handling nested structures and lists.
     *
     * @param givenKey The key to parse, which may contain nested structures.
     * @param value The value associated with the key.
     * @param options The decoding options that affect how the key-value pair is parsed.
     * @param valuesParsed Indicates whether the values have already been parsed.
     * @return The resulting object after parsing the key-value pair.
     */
    internal fun parseKeys(
        givenKey: String?,
        value: Any?,
        options: DecodeOptions,
        valuesParsed: Boolean,
    ): Any? {
        if (givenKey.isNullOrEmpty()) return null

        val segments =
            splitKeyIntoSegments(
                originalKey = givenKey,
                allowDots = options.getAllowDots,
                maxDepth = options.depth,
                strictDepth = options.strictDepth,
            )

        return parseObject(segments, value, options, valuesParsed)
    }

    /**
     * Regular expression to match dots followed by non-dot and non-bracket characters. This is used
     * to replace dots in keys with brackets for parsing.
     */
    private val DOT_TO_BRACKET = Regex("""\.([^.\[]+)""")

    /**
     * Splits a key into segments based on brackets and dots, handling depth and strictness.
     *
     * @param originalKey The original key to split.
     * @param allowDots Whether to allow dots in the key.
     * @param maxDepth The maximum depth for splitting.
     * @param strictDepth Whether to enforce strict depth limits.
     * @return A list of segments derived from the original key.
     * @throws IndexOutOfBoundsException if the depth exceeds maxDepth and strictDepth is true.
     */
    internal fun splitKeyIntoSegments(
        originalKey: String,
        allowDots: Boolean,
        maxDepth: Int,
        strictDepth: Boolean,
    ): List<String> {
        // Apply dot→bracket *before* splitting, but when depth == 0, we do NOT split at all and do
        // NOT throw.
        val key: String =
            if (allowDots) originalKey.replace(DOT_TO_BRACKET) { "[${it.groupValues[1]}]" }
            else originalKey

        // Depth 0 semantics: use the original key as a single segment; never throw.
        if (maxDepth <= 0) {
            return listOf(key)
        }

        val segments = ArrayList<String>(key.count { it == '[' } + 1)

        val first = key.indexOf('[')
        val parent = if (first >= 0) key.take(first) else key
        if (parent.isNotEmpty()) segments.add(parent)

        var open = first
        var depth = 0
        while (open >= 0 && depth < maxDepth) {
            val close = key.indexOf(']', open + 1)
            if (close < 0) break
            segments.add(key.substring(open, close + 1)) // e.g. "[p]" or "[]"
            depth++
            open = key.indexOf('[', close + 1)
        }

        if (open >= 0) {
            // When depth > 0, strictDepth can apply to the remainder.
            if (strictDepth) {
                throw IndexOutOfBoundsException(
                    "Input depth exceeded depth option of $maxDepth and strictDepth is true"
                )
            }
            // Stash the remainder as a single segment.
            segments.add("[" + key.substring(open) + "]")
        }

        return segments
    }
}
