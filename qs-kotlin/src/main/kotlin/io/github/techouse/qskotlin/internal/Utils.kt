package io.github.techouse.qskotlin.internal

import io.github.techouse.qskotlin.constants.HexTable
import io.github.techouse.qskotlin.enums.Format
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.Undefined
import java.net.URI
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.collections.ArrayDeque

/** A collection of utility methods used by the library. */
internal object Utils {
    /**
     * Merges two objects, where the source object overrides the target object. If the source is a
     * Map, it will merge its entries into the target. If the source is an Iterable, it will append
     * its items to the target. If the source is a primitive, it will replace the target.
     *
     * @param target The target object to merge into.
     * @param source The source object to merge from.
     * @param options Optional decode options for merging behavior.
     * @return The merged object.
     */
    fun merge(target: Any?, source: Any?, options: DecodeOptions = DecodeOptions()): Any? {
        if (source == null) {
            return target
        }

        if (source !is Map<*, *>) {
            return when (target) {
                is Iterable<*> ->
                    when {
                        target.any { it is Undefined } -> {
                            val mutableTarget: MutableMap<String, Any?> =
                                target
                                    .withIndex()
                                    .associate { it.index.toString() to it.value }
                                    .toMutableMap()

                            when (source) {
                                is Iterable<*> ->
                                    source.forEachIndexed { i, item ->
                                        if (item !is Undefined) {
                                            mutableTarget[i.toString()] = item
                                        }
                                    }

                                else -> mutableTarget[mutableTarget.size.toString()] = source
                            }

                            when {
                                !options.parseLists &&
                                    mutableTarget.values.any { it is Undefined } ->
                                    mutableTarget.filterValues { it !is Undefined }

                                target is Set<*> -> mutableTarget.values.toSet()

                                else -> mutableTarget.values.toList()
                            }
                        }

                        else ->
                            when (source) {
                                is Iterable<*> ->
                                    when {
                                        target.all { it is Map<*, *> || it is Undefined } &&
                                            source.all { it is Map<*, *> || it is Undefined } -> {
                                            val mutableTarget: MutableMap<Int, Any?> =
                                                target
                                                    .withIndex()
                                                    .associate { it.index to it.value }
                                                    .toSortedMap()

                                            source.forEachIndexed { i, item ->
                                                mutableTarget[i] =
                                                    when {
                                                        mutableTarget.containsKey(i) ->
                                                            merge(mutableTarget[i], item, options)
                                                        else -> item
                                                    }
                                            }

                                            when (target) {
                                                is Set<*> -> mutableTarget.values.toSet()
                                                else -> mutableTarget.values.toList()
                                            }
                                        }

                                        else ->
                                            when (target) {
                                                is Set<*> ->
                                                    target + source.filterNot { it is Undefined }
                                                is List<*> ->
                                                    target + source.filterNot { it is Undefined }
                                                else ->
                                                    listOf(target) +
                                                        source.filterNot { it is Undefined }
                                            }
                                    }

                                else ->
                                    when (target) {
                                        is Set<*> -> target + source
                                        is List<*> -> target + source
                                        else -> listOf(target, source)
                                    }
                            }
                    }

                is Map<*, *> -> {
                    val mutableTarget = target.toMutableMap()

                    when (source) {
                        is Iterable<*> -> {
                            source.forEachIndexed { i, item ->
                                if (item !is Undefined) {
                                    mutableTarget[i.toString()] = item
                                }
                            }
                        }
                        is Undefined -> {
                            // ignore
                        }
                        else -> {
                            val k = source.toString()
                            if (k.isNotEmpty()) {
                                mutableTarget[k] = true
                            }
                        }
                    }

                    mutableTarget
                }

                else ->
                    when (source) {
                        is Iterable<*> -> listOf(target) + source.filterNot { it is Undefined }
                        else -> listOf(target, source)
                    }
            }
        }

        if (target == null || target !is Map<*, *>) {
            return when (target) {
                is Iterable<*> -> {
                    val mutableTarget: MutableMap<String, Any?> =
                        target
                            .withIndex()
                            .associate { it.index.toString() to it.value }
                            .filterValues { it !is Undefined }
                            .toMutableMap()

                    @Suppress("UNCHECKED_CAST")
                    (source as Map<Any, Any?>).forEach { (key, value) ->
                        mutableTarget[key.toString()] = value
                    }
                    mutableTarget
                }

                else -> {
                    val mutableTarget = listOfNotNull(target).toMutableList<Any?>()

                    when (source) {
                        is Iterable<*> ->
                            mutableTarget.addAll(
                                (source as Iterable<*>).filterNot { it is Undefined }.toList()
                            )

                        else -> mutableTarget.add(source)
                    }

                    mutableTarget
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        val mergeTarget: MutableMap<Any, Any?> =
            when {
                target is Iterable<*> && source !is Iterable<*> ->
                    target
                        .withIndex()
                        .associate { it.index.toString() to it.value }
                        .filterValues { it !is Undefined }
                        .toMutableMap()
                else -> (target as Map<Any, Any?>).toMutableMap()
            }

        @Suppress("UNCHECKED_CAST")
        (source as Map<Any, Any?>).forEach { (key, value) ->
            mergeTarget[key] =
                if (mergeTarget.containsKey(key)) {
                    merge(mergeTarget[key], value, options)
                } else {
                    value
                }
        }

        return mergeTarget
    }

    /**
     * A Kotlin representation of the deprecated JavaScript escape function
     * https://developer.mozilla.org/en-US/docs/web/javascript/reference/global_objects/escape
     */
    @Deprecated("Use URLEncoder.encode instead")
    fun escape(str: String, format: Format = Format.RFC3986): String {
        val buffer = StringBuilder()

        for (i in str.indices) {
            val c = str[i].code

            // These 69 characters are safe for escaping
            // ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@*_+-./
            if (
                (c in 0x30..0x39) || // 0-9
                    (c in 0x41..0x5A) || // A-Z
                    (c in 0x61..0x7A) || // a-z
                    c == 0x40 || // @
                    c == 0x2A || // *
                    c == 0x5F || // _
                    c == 0x2D || // -
                    c == 0x2B || // +
                    c == 0x2E || // .
                    c == 0x2F || // /
                    (format == Format.RFC1738 && (c == 0x28 || c == 0x29))
            ) { // ( )
                buffer.append(str[i])
                continue
            }

            if (c < 256) {
                buffer.append('%')
                buffer.append(c.toString(16).padStart(2, '0').uppercase())
                continue
            }

            buffer.append("%u")
            buffer.append(c.toString(16).padStart(4, '0').uppercase())
        }

        return buffer.toString()
    }

    /**
     * A Kotlin representation of the deprecated JavaScript unescape function
     * https://developer.mozilla.org/en-US/docs/web/javascript/reference/global_objects/unescape
     */
    @Deprecated("Use URLDecoder.decode instead")
    fun unescape(str: String): String {
        val buffer = StringBuilder()
        var i = 0

        while (i < str.length) {
            val c = str[i]

            if (c == '%') {
                // Ensure there's at least one character after '%'
                if (i + 1 < str.length) {
                    if (str[i + 1] == 'u') {
                        // Check that there are at least 6 characters for "%uXXXX"
                        if (i + 6 <= str.length) {
                            try {
                                val charCode = str.substring(i + 2, i + 6).toInt(16)
                                buffer.append(charCode.toChar())
                                i += 6
                                continue
                            } catch (_: NumberFormatException) {
                                // Not a valid %u escape: treat '%' as literal
                                buffer.append(str[i])
                                i++
                                continue
                            }
                        } else {
                            // Not enough characters for a valid %u escape: treat '%' as literal
                            buffer.append(str[i])
                            i++
                            continue
                        }
                    } else {
                        // For %XX escape: check that there are at least 3 characters
                        if (i + 3 <= str.length) {
                            try {
                                val charCode = str.substring(i + 1, i + 3).toInt(16)
                                buffer.append(charCode.toChar())
                                i += 3
                                continue
                            } catch (_: NumberFormatException) {
                                // Parsing failed: treat '%' as literal
                                buffer.append(str[i])
                                i++
                                continue
                            }
                        } else {
                            // Not enough characters for a valid %XX escape: treat '%' as literal
                            buffer.append(str[i])
                            i++
                            continue
                        }
                    }
                } else {
                    // '%' is the last character; treat it as literal
                    buffer.append(str[i])
                    i++
                    continue
                }
            }

            buffer.append(str[i])
            i++
        }

        return buffer.toString()
    }

    /** The maximum length of a segment to encode in a single pass. */
    private const val SEGMENT_LIMIT = 1024

    /**
     * Encodes a value into a URL-encoded string.
     *
     * @param value The value to encode.
     * @param charset The character set to use for encoding. Defaults to UTF-8.
     * @param format The encoding format to use. Defaults to RFC 3986.
     * @return The encoded string.
     */
    fun encode(
        value: Any?,
        charset: Charset? = StandardCharsets.UTF_8,
        format: Format? = Format.RFC3986,
    ): String {
        val charset = charset ?: StandardCharsets.UTF_8
        val format = format ?: Format.RFC3986

        // These cannot be encoded
        if (value is Iterable<*> || value is Map<*, *> || value is Undefined) {
            return ""
        }

        val str =
            when (value) {
                is ByteBuffer -> String(value.array(), charset)
                is ByteArray -> String(value, charset)
                else -> value?.toString()
            }

        if (str.isNullOrEmpty()) {
            return ""
        }

        if (charset == StandardCharsets.ISO_8859_1) {
            @Suppress("DEPRECATION")
            return escape(str, format).replace(Regex("%u[0-9a-f]{4}", RegexOption.IGNORE_CASE)) {
                match ->
                val code = match.value.substring(2).toInt(16)
                "%26%23$code%3B"
            }
        }

        val buffer = StringBuilder()

        var j = 0
        while (j < str.length) {
            val segment =
                if (str.length >= SEGMENT_LIMIT) {
                    str.substring(j, minOf(j + SEGMENT_LIMIT, str.length))
                } else {
                    str
                }

            var i = 0
            while (i < segment.length) {
                val c = segment[i].code

                when (c) {
                    0x2D, // -
                    0x2E, // .
                    0x5F, // _
                    0x7E, // ~
                    in 0x30..0x39, // 0-9
                    in 0x41..0x5A, // A-Z
                    in 0x61..0x7A -> { // a-z
                        buffer.append(segment[i])
                        i++
                        continue
                    }

                    0x28,
                    0x29 -> { // ( )
                        if (format == Format.RFC1738) {
                            buffer.append(segment[i])
                            i++
                            continue
                        }
                    }
                }

                when {
                    c < 0x80 -> { // ASCII
                        buffer.append(HexTable[c])
                        i++
                        continue
                    }

                    c < 0x800 -> { // 2 bytes
                        buffer.append(HexTable[0xC0 or (c shr 6)])
                        buffer.append(HexTable[0x80 or (c and 0x3F)])
                        i++
                        continue
                    }

                    c < 0xD800 || c >= 0xE000 -> { // 3 bytes
                        buffer.append(HexTable[0xE0 or (c shr 12)])
                        buffer.append(HexTable[0x80 or ((c shr 6) and 0x3F)])
                        buffer.append(HexTable[0x80 or (c and 0x3F)])
                        i++
                        continue
                    }

                    else -> { // 4 bytes (surrogate pair)
                        val nextC = if (i + 1 < segment.length) segment[i + 1].code else 0
                        val codePoint = 0x10000 + (((c and 0x3FF) shl 10) or (nextC and 0x3FF))
                        buffer.append(HexTable[0xF0 or (codePoint shr 18)])
                        buffer.append(HexTable[0x80 or ((codePoint shr 12) and 0x3F)])
                        buffer.append(HexTable[0x80 or ((codePoint shr 6) and 0x3F)])
                        buffer.append(HexTable[0x80 or (codePoint and 0x3F)])
                        i += 2 // Skip the next character as it's part of the surrogate pair
                        continue
                    }
                }
            }

            j += SEGMENT_LIMIT
        }

        return buffer.toString()
    }

    /**
     * Decodes a URL-encoded string into its original form.
     *
     * @param str The URL-encoded string to decode.
     * @param charset The character set to use for decoding. Defaults to UTF-8.
     * @return The decoded string, or null if the input is null.
     */
    fun decode(str: String?, charset: Charset? = StandardCharsets.UTF_8): String? {
        val strWithoutPlus = str?.replace('+', ' ')

        if (charset == StandardCharsets.ISO_8859_1) {
            return try {
                @Suppress("DEPRECATION")
                strWithoutPlus?.replace(Regex("%[0-9a-f]{2}", RegexOption.IGNORE_CASE)) { match ->
                    unescape(match.value)
                }
            } catch (_: Exception) {
                strWithoutPlus
            }
        }

        return try {
            strWithoutPlus?.let {
                URLDecoder.decode(it, charset?.name() ?: StandardCharsets.UTF_8.name())
            }
        } catch (_: Exception) {
            strWithoutPlus
        }
    }

    /**
     * Compact a nested Map or List structure by removing all Undefined values. This function
     * traverses the structure and removes any Undefined values, ensuring that the structure remains
     * intact.
     *
     * @param root The root of the Map or List structure to compact.
     * @param allowSparseLists If true, allows sparse Lists (i.e., Lists with Undefined values). If
     *   false, removes all Undefined values from Lists.
     * @return The compacted Map or List structure.
     */
    fun compact(
        root: MutableMap<String, Any?>,
        allowSparseLists: Boolean = false,
    ): MutableMap<String, Any?> {
        val stack = ArrayDeque<Any>()
        stack.add(root)

        // Identity-based visited set to prevent cycles
        val visited: MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap())

        visited.add(root)

        while (stack.isNotEmpty()) {
            when (val node = stack.removeLast()) {
                is MutableMap<*, *> -> {
                    @Suppress("UNCHECKED_CAST") val m = node as MutableMap<String, Any?>
                    val it = m.entries.iterator()
                    while (it.hasNext()) {
                        val e = it.next()
                        when (val v = e.value) {
                            is Undefined -> it.remove()

                            is MutableMap<*, *> -> {
                                if (visited.add(v)) {
                                    @Suppress("UNCHECKED_CAST")
                                    stack.add(v as MutableMap<String, Any?>)
                                }
                            }

                            is MutableList<*> -> {
                                if (visited.add(v)) {
                                    @Suppress("UNCHECKED_CAST") stack.add(v as MutableList<Any?>)
                                }
                            }
                        }
                    }
                }

                is MutableList<*> -> {
                    @Suppress("UNCHECKED_CAST") val list = node as MutableList<Any?>
                    val it = list.listIterator()
                    while (it.hasNext()) {
                        when (val v = it.next()) {
                            is Undefined -> if (allowSparseLists) it.set(null) else it.remove()

                            is MutableMap<*, *> -> {
                                if (visited.add(v)) {
                                    @Suppress("UNCHECKED_CAST")
                                    stack.add(v as MutableMap<String, Any?>)
                                }
                            }

                            is MutableList<*> -> {
                                if (visited.add(v)) {
                                    @Suppress("UNCHECKED_CAST") stack.add(v as MutableList<Any?>)
                                }
                            }
                        }
                    }
                }
            }
        }
        return root
    }

    /**
     * Combines two objects into a list. If either object is an Iterable, its elements are added to
     * the list. If either object is a primitive, it is added as a single element.
     *
     * @param a The first object to combine.
     * @param b The second object to combine.
     * @return A list containing the combined elements.
     */
    fun <T> combine(a: Any?, b: Any?): List<T> {
        val result = mutableListOf<T>()

        @Suppress("UNCHECKED_CAST")
        when (a) {
            is Iterable<*> -> result.addAll(a as Iterable<T>)
            else -> result.add(a as T)
        }

        @Suppress("UNCHECKED_CAST")
        when (b) {
            is Iterable<*> -> result.addAll(b as Iterable<T>)
            else -> result.add(b as T)
        }

        return result
    }

    /**
     * Applies a function to a value or each element in an Iterable. If the value is an Iterable,
     * the function is applied to each element. If the value is a single item, the function is
     * applied directly.
     *
     * @param value The value or Iterable to apply the function to.
     * @param fn The function to apply.
     * @return The result of applying the function, or null if the input is null.
     */
    fun <T> apply(value: Any?, fn: (T) -> T): Any? =
        when (value) {
            is Iterable<*> -> {
                @Suppress("UNCHECKED_CAST") (value as Iterable<T>).map(fn)
            }

            else -> {
                @Suppress("UNCHECKED_CAST") fn(value as T)
            }
        }

    /**
     * Checks if a value is a non-nullish primitive type. A non-nullish primitive is defined as a
     * String, Number, Boolean, Enum, Instant, LocalDateTime, or URI. If `skipNulls` is true, empty
     * Strings and URIs are also considered non-nullish.
     *
     * @param value The value to check.
     * @param skipNulls If true, empty Strings and URIs are not considered non-nullish.
     * @return True if the value is a non-nullish primitive, false otherwise.
     */
    fun isNonNullishPrimitive(value: Any?, skipNulls: Boolean = false): Boolean =
        when (value) {
            is String -> if (skipNulls) value.isNotEmpty() else true
            is Number,
            is Boolean,
            is Enum<*>,
            is Instant,
            is LocalDateTime -> true
            is URI -> if (skipNulls) value.toString().isNotEmpty() else true
            is Iterable<*>,
            is Map<*, *>,
            is Undefined -> false
            null -> false
            else -> true
        }

    /**
     * Checks if a value is empty. A value is considered empty if it is null, Undefined, an empty
     * String, an empty Iterable, or an empty Map.
     *
     * @param value The value to check.
     * @return True if the value is empty, false otherwise.
     */
    fun isEmpty(value: Any?): Boolean =
        when (value) {
            null,
            is Undefined -> true
            is String -> value.isEmpty()
            is Iterable<*> -> !value.iterator().hasNext()
            is Map<*, *> -> value.isEmpty()
            else -> false
        }

    /**
     * Interpret numeric entities in a string, converting them to their Unicode characters. This
     * function supports both decimal and hexadecimal numeric entities.
     *
     * @param str The input string potentially containing numeric entities.
     * @return A new string with numeric entities replaced by their corresponding characters.
     */
    fun interpretNumericEntities(str: String): String {
        if (str.length < 4) return str
        val first = str.indexOf("&#")
        if (first == -1) return str

        val sb = StringBuilder(str.length)
        var i = 0
        val n = str.length

        while (i < n) {
            val ch = str[i]
            if (ch == '&' && i + 2 < n && str[i + 1] == '#') {
                var j = i + 2
                // must have at least one digit
                if (j < n && str[j].isDigit()) {
                    var code = 0
                    val startDigits = j
                    while (j < n && str[j].isDigit()) {
                        code = code * 10 + (str[j] - '0')
                        j++
                    }
                    // must end with ';' and have at least one digit
                    if (j < n && str[j] == ';' && j > startDigits) {
                        // Keep behavior compatible with your current code:
                        // - For BMP values (<= 0xFFFF), append a single UTF-16 unit.
                        //   This includes surrogate halves like 55357/56489.
                        // - For > 0xFFFF, emit the surrogate pair.
                        if (code <= 0xFFFF) {
                            sb.append(code.toChar())
                        } else if (code <= 0x10FFFF) {
                            sb.append(String(Character.toChars(code)))
                        } else {
                            // Out-of-range: leave literal
                            sb.append('&')
                            i++
                            continue
                        }
                        i = j + 1
                        continue
                    }
                }
                // Not a valid numeric entity: emit literal '&' and continue
                sb.append('&')
                i++
            } else {
                sb.append(ch)
                i++
            }
        }
        return sb.toString()
    }
}
