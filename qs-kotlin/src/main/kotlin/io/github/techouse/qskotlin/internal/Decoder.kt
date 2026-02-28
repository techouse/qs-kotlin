package io.github.techouse.qskotlin.internal

import io.github.techouse.qskotlin.enums.Duplicates
import io.github.techouse.qskotlin.enums.Sentinel
import io.github.techouse.qskotlin.internal.Decoder.dotToBracketTopLevel
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.Delimiter
import io.github.techouse.qskotlin.models.RegexDelimiter
import io.github.techouse.qskotlin.models.StringDelimiter
import io.github.techouse.qskotlin.models.Undefined
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/** A helper object for decoding query strings into structured data. */
internal object Decoder {
    private const val MAX_PREALLOCATED_SPLIT_PARTS = 1_024

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
            if (options.listLimit >= 0) {
                val remaining = options.listLimit - currentListLength
                if (options.throwOnLimitExceeded) {
                    if (remaining < 0) {
                        throw IndexOutOfBoundsException(listLimitExceededMessage(options.listLimit))
                    }
                    val splitVal =
                        if (remaining == Int.MAX_VALUE) {
                            splitCommaValue(value)
                        } else {
                            splitCommaValue(value, remaining + 1)
                        }
                    if (splitVal.size > remaining) {
                        throw IndexOutOfBoundsException(listLimitExceededMessage(options.listLimit))
                    }
                    return splitVal
                }
                if (remaining <= 0) return emptyList<String>()
                return splitCommaValue(value, remaining)
            }
            return splitCommaValue(value)
        }

        if (
            options.listLimit >= 0 &&
                options.throwOnLimitExceeded &&
                currentListLength >= options.listLimit
        ) {
            throw IndexOutOfBoundsException(listLimitExceededMessage(options.listLimit))
        }

        return value
    }

    private fun listLimitExceededMessage(limit: Int): String {
        return "List limit exceeded. Only $limit element${if (limit == 1) "" else "s"} allowed in a list."
    }

    private fun splitCommaValue(value: String, maxParts: Int? = null): List<String> {
        if (maxParts != null && maxParts <= 0) return emptyList()

        val parts = newSplitBuffer(maxParts)
        var start = 0
        while (true) {
            if (maxParts != null && parts.size >= maxParts) break

            val comma = value.indexOf(',', start)
            val end = if (comma == -1) value.length else comma
            parts.add(value.substring(start, end))

            if (comma == -1) break
            start = comma + 1
        }

        return parts
    }

    private fun collectNonEmptyParts(
        input: String,
        delimiter: Delimiter,
        maxParts: Int? = null,
    ): List<String> {
        return when (delimiter) {
            is StringDelimiter -> collectNonEmptyStringParts(input, delimiter.value, maxParts)
            is RegexDelimiter -> collectNonEmptyIterableParts(delimiter.split(input), maxParts)
        }
    }

    private fun collectNonEmptyStringParts(
        input: String,
        delimiter: String,
        maxParts: Int?,
    ): List<String> {
        if (delimiter.isEmpty()) {
            throw IllegalArgumentException(
                "collectNonEmptyStringParts received an invalid empty delimiter."
            )
        }
        if (maxParts != null && maxParts <= 0) return emptyList()

        val parts = newSplitBuffer(maxParts)
        var start = 0
        while (true) {
            if (maxParts != null && parts.size >= maxParts) break

            val next = input.indexOf(delimiter, start)
            val end = if (next == -1) input.length else next

            if (end > start) {
                parts.add(input.substring(start, end))
            }

            if (next == -1) break
            start = next + delimiter.length
        }

        return parts
    }

    private fun collectNonEmptyIterableParts(
        parts: Iterable<String>,
        maxParts: Int?,
    ): List<String> {
        if (maxParts != null && maxParts <= 0) return emptyList()

        val out = newSplitBuffer(maxParts)
        for (part in parts) {
            if (part.isEmpty()) continue
            out.add(part)
            if (maxParts != null && out.size >= maxParts) break
        }
        return out
    }

    private fun newSplitBuffer(maxParts: Int?): ArrayList<String> {
        if (maxParts == null) return ArrayList()
        if (maxParts <= 0) return ArrayList()
        return ArrayList(minOf(maxParts, MAX_PREALLOCATED_SPLIT_PARTS))
    }

    /**
     * Parses a query string into a map of key-value pairs, handling various options for decoding.
     * Percent-encoded brackets `%5B`/`%5D` are normalized to literal `[`/`]` before splitting.
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

        val baseStr = if (options.ignoreQueryPrefix) str.removePrefix("?") else str
        val cleanStr =
            if (baseStr.indexOf('%') >= 0) {
                baseStr
                    .replace("%5B", "[", ignoreCase = true)
                    .replace("%5D", "]", ignoreCase = true)
            } else {
                baseStr
            }

        val limit = if (options.parameterLimit == Int.MAX_VALUE) null else options.parameterLimit

        if (limit != null && limit <= 0) {
            throw IllegalArgumentException("Parameter limit must be a positive integer.")
        }

        val takeCount =
            if (limit != null) {
                if (options.throwOnLimitExceeded) limit + 1 else limit
            } else {
                null
            }
        val parts = collectNonEmptyParts(cleanStr, options.delimiter, takeCount)

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
            if (part.isEmpty()) continue
            val bracketEqualsPos = part.indexOf("]=")
            val pos = if (bracketEqualsPos == -1) part.indexOf('=') else bracketEqualsPos + 1

            val key: String
            var value: Any?

            if (pos == -1) {
                // Decode a bare key (no '=') using key-aware decoding
                key = options.decodeKey(part, charset).orEmpty()
                value = if (options.strictNullHandling) null else ""
            } else {
                // Decode the key slice as a key; values decode as values
                key = options.decodeKey(part.take(pos), charset).orEmpty()
                value =
                    Utils.apply(
                        parseListValue(
                            part.substring(pos + 1),
                            options,
                            if (obj.containsKey(key) && obj[key] is List<*>) {
                                (obj[key] as List<*>).size
                            } else 0,
                        )
                    ) { v: Any? ->
                        options.decodeValue(v as String?, charset)
                    }
            }
            if (key.isEmpty()) continue

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
                    obj[key] = Utils.combine(obj[key], value, options.listLimit)
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
                        Utils.isOverflow(leaf) -> leaf
                        else -> Utils.combine(emptyList<Any?>(), leaf, options.listLimit)
                    }
            } else {
                // Always build *string-keyed* maps here
                val mutableObj = LinkedHashMap<String, Any?>(1)

                val cleanRoot =
                    if (root.startsWith("[")) {
                        val last = root.lastIndexOf(']')
                        if (last > 0) root.substring(1, last) else root.substring(1)
                    } else root

                val decodedRoot =
                    if (options.getDecodeDotInKeys && cleanRoot.contains("%2E", ignoreCase = true))
                        cleanRoot.replace("%2E", ".", ignoreCase = true)
                    else cleanRoot

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
                        mutableObj[decodedRoot] = leaf
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
     * Convert top-level dot segments into bracket segments, preserving dots inside brackets and
     * ignoring degenerate top-level dots.
     *
     * Rules:
     * - Only dots at depth == 0 split. Dots inside `\[\]` are preserved.
     * - Percent-encoded dots (`%2E`/`%2e`) never split here (they may map to '.' later).
     * - Degenerates:
     *     * leading '.' → preserved (e.g., `".a"` stays `".a"`),
     *     * double dots `"a..b"` → the first dot is preserved (`"a.\[b]"`),
     *     * trailing dot `"a."` → trailing '.' is preserved and ignored by the splitter.
     *
     * Examples:
     * - `user.email.name` → `user\[email]\[name]`
     * - `a\[b].c` → `a\[b]\[c]`
     * - `a\[.].c` → `a\[.]\[c]`
     * - `a%2E\[b]` → remains `a%2E\[b]` (no split here)
     */
    private fun dotToBracketTopLevel(s: String): String {
        val sb = StringBuilder(s.length)
        var depth = 0
        var i = 0
        while (i < s.length) {
            when (val ch = s[i]) {
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
                '.' -> {
                    if (depth == 0) {
                        // Look ahead to decide what to do with a top‑level dot
                        val hasNext = i + 1 < s.length
                        val next = if (hasNext) s[i + 1] else '\u0000'
                        when {
                            // Degenerate ".[" → skip the dot so "a.[b]" behaves like "a[b]"
                            next == '[' -> {
                                i++ // consume the '.'
                            }
                            // Preserve literal dot for "a." (trailing) and for "a..b" (the first
                            // dot)
                            !hasNext || next == '.' -> {
                                sb.append('.')
                                i++
                            }
                            else -> {
                                // Normal split: convert a.b → a[b] at top level
                                val start = ++i
                                var j = start
                                while (j < s.length && s[j] != '.' && s[j] != '[') j++
                                sb.append('[').append(s, start, j).append(']')
                                i = j
                            }
                        }
                    } else {
                        sb.append('.')
                        i++
                    }
                }
                '%' -> {
                    // Preserve percent sequences verbatim at top level. Encoded dots (%2E/%2e)
                    // are *not* used as separators here; they may be mapped to '.' later
                    // when parsing segments (see DecodeOptions.defaultDecode/parseObject).
                    sb.append('%')
                    i++
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
     * Split a key into segments based on balanced brackets.
     *
     * Notes:
     * - Top-level dot splitting (`a.b` → `a\[b]`) happens earlier via [dotToBracketTopLevel] when
     *   [allowDots] is true.
     * - Unterminated '[': the entire key is treated as a single literal segment (qs semantics).
     * - If [strictDepth] is false and depth is exceeded, the remainder is kept as one final bracket
     *   segment.
     *
     * @param originalKey The original key to split.
     * @param allowDots Whether to allow top-level dot splitting (already applied upstream).
     * @param maxDepth The maximum number of bracket segments to collect.
     * @param strictDepth When true, exceeding [maxDepth] throws; when false, the remainder is a
     *   single trailing segment.
     */
    internal fun splitKeyIntoSegments(
        originalKey: String,
        allowDots: Boolean,
        maxDepth: Int,
        strictDepth: Boolean,
    ): List<String> {
        // Depth 0 semantics: use the original key as a single segment; never throw.
        if (maxDepth <= 0) {
            return listOf(originalKey)
        }

        // Apply dot→bracket *before* splitting, but when depth == 0, we do NOT split at all and do
        // NOT throw.
        val key: String = if (allowDots) dotToBracketTopLevel(originalKey) else originalKey

        val segments = ArrayList<String>(key.count { it == '[' } + 1)

        val first = key.indexOf('[')
        val parent = if (first >= 0) key.take(first) else key
        if (parent.isNotEmpty()) segments.add(parent)

        var open = first
        var unterminated = false
        var depth = 0
        while (open >= 0 && depth < maxDepth) {
            var i2 = open + 1
            var level = 1
            var close = -1

            // Balance nested '[' and ']' within the same group,
            // so "[with[inner]]" is treated as one segment.
            while (i2 < key.length) {
                val ch2 = key[i2]
                if (ch2 == '[') {
                    level++
                } else if (ch2 == ']') {
                    level--
                    if (level == 0) {
                        close = i2
                        break
                    }
                }
                i2++
            }

            if (close < 0) {
                unterminated = true
                break // unterminated group; stop collecting
            }

            segments.add(key.substring(open, close + 1)) // includes the surrounding [ ]
            depth++
            open = key.indexOf('[', close + 1)
        }

        if (open >= 0) {
            // When depth > 0, strictDepth can apply to a *well-formed* remainder.
            // Unterminated remainder is wrapped without throwing.
            if (strictDepth && !unterminated) {
                throw IndexOutOfBoundsException(
                    "Input depth exceeded depth option of $maxDepth and strictDepth is true"
                )
            }
            // Stash the remainder—unterminated or overflow—as a single segment.
            segments.add("[" + key.substring(open) + "]")
        }

        return segments
    }
}
