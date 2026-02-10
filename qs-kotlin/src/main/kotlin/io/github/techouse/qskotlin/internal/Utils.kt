package io.github.techouse.qskotlin.internal

import io.github.techouse.qskotlin.constants.HexTable
import io.github.techouse.qskotlin.enums.Format
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.Undefined
import java.net.URI
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.collections.ArrayDeque

/** A collection of utility methods used by the library. */
internal object Utils {
    private enum class MergePhase {
        START,
        LIST_ITER,
        MAP_ITER,
    }

    private data class MergeFrame(
        var target: Any?,
        var source: Any?,
        val options: DecodeOptions,
        val onResult: (Any?) -> Unit,
        var phase: MergePhase = MergePhase.START,
        var indexedTarget: MutableMap<Int, Any?>? = null,
        var sourceList: List<Any?>? = null,
        var listIndex: Int = 0,
        var targetIsSet: Boolean = false,
        var mergeTarget: MutableMap<Any?, Any?>? = null,
        var mapIterator: Iterator<Map.Entry<Any?, Any?>>? = null,
        var overflowMax: Int? = null,
    )

    /**
     * Merges two objects, where the source object overrides or extends the target object.
     * - If the source is a Map, it will merge its entries into the target.
     * - If the source is an Iterable, it will append its items to the target.
     * - If the source is a primitive, it will combine with the target following qs semantics
     *   (including OverflowMap append behavior).
     *
     * @param target The target object to merge into.
     * @param source The source object to merge from.
     * @param options Optional decode options for merging behavior.
     * @return The merged object.
     */
    fun merge(target: Any?, source: Any?, options: DecodeOptions = DecodeOptions()): Any? {
        var result: Any? = null
        val stack = ArrayDeque<MergeFrame>()

        stack.add(
            MergeFrame(
                target = target,
                source = source,
                options = options,
                onResult = { value -> result = value },
            )
        )

        fun toIndexedMap(iterable: Iterable<*>): MutableMap<Int, Any?> {
            val map = java.util.TreeMap<Int, Any?>()
            var i = 0
            for (v in iterable) {
                map[i++] = v
            }
            return map
        }

        fun updateOverflowMax(current: Int, key: Any?): Int {
            val parsed =
                when (key) {
                    is Int -> key
                    is Long -> if (key in Int.MIN_VALUE..Int.MAX_VALUE) key.toInt() else null
                    is String -> key.toIntOrNull()
                    else -> null
                }
            return if (parsed == null || parsed < 0) current else maxOf(current, parsed)
        }

        while (stack.isNotEmpty()) {
            val frame = stack.last()

            when (frame.phase) {
                MergePhase.START -> {
                    val currentTarget = frame.target
                    val currentSource = frame.source

                    if (currentSource == null) {
                        stack.removeLast()
                        frame.onResult(currentTarget)
                        continue
                    }

                    if (currentSource !is Map<*, *>) {
                        when (currentTarget) {
                            is Iterable<*> -> {
                                if (currentTarget.any { it is Undefined }) {
                                    val mutableTarget: MutableMap<String, Any?> =
                                        currentTarget
                                            .withIndex()
                                            .associate { it.index.toString() to it.value }
                                            .toMutableMap()

                                    when (currentSource) {
                                        is Iterable<*> ->
                                            currentSource.forEachIndexed { i, item ->
                                                if (item !is Undefined) {
                                                    mutableTarget[i.toString()] = item
                                                }
                                            }

                                        else ->
                                            mutableTarget[mutableTarget.size.toString()] =
                                                currentSource
                                    }

                                    val merged =
                                        when {
                                            !options.parseLists &&
                                                mutableTarget.values.any { it is Undefined } ->
                                                mutableTarget.filterValues { it !is Undefined }
                                            currentTarget is Set<*> -> mutableTarget.values.toSet()
                                            else -> mutableTarget.values.toList()
                                        }

                                    stack.removeLast()
                                    frame.onResult(merged)
                                    continue
                                }

                                if (currentSource is Iterable<*>) {
                                    val targetMaps =
                                        currentTarget.all { it is Map<*, *> || it is Undefined }
                                    val sourceMaps =
                                        currentSource.all { it is Map<*, *> || it is Undefined }

                                    if (targetMaps && sourceMaps) {
                                        frame.indexedTarget = toIndexedMap(currentTarget)
                                        frame.sourceList = currentSource.toList()
                                        frame.targetIsSet = currentTarget is Set<*>
                                        frame.listIndex = 0
                                        frame.phase = MergePhase.LIST_ITER
                                        continue
                                    }

                                    val filtered = currentSource.filterNot { it is Undefined }
                                    val merged =
                                        when (currentTarget) {
                                            is Set<*> -> currentTarget + filtered
                                            is List<*> -> currentTarget + filtered
                                            else -> listOf(currentTarget) + filtered
                                        }
                                    stack.removeLast()
                                    frame.onResult(merged)
                                    continue
                                }

                                val merged =
                                    when (currentTarget) {
                                        is Set<*> -> currentTarget + currentSource
                                        is List<*> -> currentTarget + currentSource
                                        else -> listOf(currentTarget, currentSource)
                                    }
                                stack.removeLast()
                                frame.onResult(merged)
                                continue
                            }

                            is Map<*, *> -> {
                                if (currentTarget is OverflowMap && currentSource !is Iterable<*>) {
                                    val newIndex = currentTarget.maxIndex + 1
                                    currentTarget[newIndex.toString()] = currentSource
                                    currentTarget.maxIndex = newIndex
                                    stack.removeLast()
                                    frame.onResult(currentTarget)
                                    continue
                                }
                                if (currentTarget is OverflowMap && currentSource is Iterable<*>) {
                                    var newIndex = currentTarget.maxIndex
                                    for (item in currentSource) {
                                        if (item is Undefined) continue
                                        newIndex += 1
                                        currentTarget[newIndex.toString()] = item
                                    }
                                    currentTarget.maxIndex = newIndex
                                    stack.removeLast()
                                    frame.onResult(currentTarget)
                                    continue
                                }
                                val mutableTarget = currentTarget.toMutableMap()

                                when (currentSource) {
                                    is Iterable<*> -> {
                                        currentSource.forEachIndexed { i, item ->
                                            if (item !is Undefined) {
                                                mutableTarget[i.toString()] = item
                                            }
                                        }
                                    }
                                    is Undefined -> {
                                        // ignore
                                    }
                                    else -> {
                                        val k = currentSource.toString()
                                        if (k.isNotEmpty()) {
                                            mutableTarget[k] = true
                                        }
                                    }
                                }

                                stack.removeLast()
                                frame.onResult(mutableTarget)
                                continue
                            }

                            else -> {
                                val merged =
                                    when (currentSource) {
                                        is Iterable<*> ->
                                            listOf(currentTarget) +
                                                currentSource.filterNot { it is Undefined }
                                        else -> listOf(currentTarget, currentSource)
                                    }
                                stack.removeLast()
                                frame.onResult(merged)
                                continue
                            }
                        }
                    }

                    // Source is a Map
                    if (currentTarget == null || currentTarget !is Map<*, *>) {
                        if (currentTarget is Iterable<*>) {
                            val mutableTarget: MutableMap<String, Any?> =
                                currentTarget
                                    .withIndex()
                                    .associate { it.index.toString() to it.value }
                                    .filterValues { it !is Undefined }
                                    .toMutableMap()

                            @Suppress("UNCHECKED_CAST")
                            (currentSource as Map<Any?, Any?>).forEach { (key, value) ->
                                mutableTarget[key.toString()] = value
                            }
                            stack.removeLast()
                            frame.onResult(mutableTarget)
                            continue
                        }

                        if (currentSource is OverflowMap) {
                            val sourceMax = currentSource.maxIndex
                            val resultMap = OverflowMap()
                            if (currentTarget != null) {
                                resultMap["0"] = currentTarget
                            }
                            for ((key, value) in currentSource) {
                                val keyStr = key
                                val oldIndex = keyStr.toIntOrNull()
                                if (oldIndex == null) {
                                    resultMap[keyStr] = value
                                } else {
                                    resultMap[(oldIndex + 1).toString()] = value
                                }
                            }
                            resultMap.maxIndex = sourceMax + 1
                            stack.removeLast()
                            frame.onResult(resultMap)
                            continue
                        }

                        val mutableTarget = listOfNotNull(currentTarget).toMutableList<Any?>()
                        when (currentSource) {
                            is Iterable<*> ->
                                mutableTarget.addAll(
                                    (currentSource as Iterable<*>)
                                        .filterNot { it is Undefined }
                                        .toList()
                                )
                            else -> mutableTarget.add(currentSource)
                        }
                        stack.removeLast()
                        frame.onResult(mutableTarget)
                        continue
                    }

                    @Suppress("UNCHECKED_CAST")
                    val mergeTarget: MutableMap<Any?, Any?> =
                        when {
                            currentTarget is Iterable<*> && currentSource !is Iterable<*> ->
                                currentTarget
                                    .withIndex()
                                    .associate { it.index.toString() to it.value }
                                    .filterValues { it !is Undefined }
                                    .toMutableMap() as MutableMap<Any?, Any?>
                            currentTarget is OverflowMap ->
                                OverflowMap().apply {
                                    putAll(currentTarget)
                                    maxIndex = currentTarget.maxIndex
                                } as MutableMap<Any?, Any?>
                            else -> (currentTarget as Map<Any?, Any?>).toMutableMap()
                        }

                    frame.mergeTarget = mergeTarget
                    @Suppress("UNCHECKED_CAST")
                    frame.mapIterator = (currentSource as Map<Any?, Any?>).entries.iterator()
                    frame.overflowMax = (mergeTarget as? OverflowMap)?.maxIndex
                    frame.phase = MergePhase.MAP_ITER
                    continue
                }

                MergePhase.MAP_ITER -> {
                    if (frame.mapIterator?.hasNext() == true) {
                        val entry = frame.mapIterator!!.next()
                        val key = entry.key

                        if (frame.overflowMax != null) {
                            frame.overflowMax = updateOverflowMax(frame.overflowMax!!, key)
                        }

                        val mergeTarget = frame.mergeTarget!!
                        if (mergeTarget.containsKey(key)) {
                            val childTarget = mergeTarget[key]
                            stack.add(
                                MergeFrame(
                                    target = childTarget,
                                    source = entry.value,
                                    options = frame.options,
                                    onResult = { value -> mergeTarget[key] = value },
                                )
                            )
                            continue
                        }

                        mergeTarget[key] = entry.value
                        continue
                    }

                    if (frame.overflowMax != null && frame.mergeTarget is OverflowMap) {
                        (frame.mergeTarget as OverflowMap).maxIndex = frame.overflowMax!!
                    }

                    stack.removeLast()
                    frame.onResult(frame.mergeTarget!!)
                    continue
                }

                MergePhase.LIST_ITER -> {
                    if (frame.listIndex >= frame.sourceList!!.size) {
                        if (
                            frame.options.parseLists == false &&
                                frame.indexedTarget!!.values.any { it is Undefined }
                        ) {
                            val normalized = mutableMapOf<String, Any?>()
                            for ((index, value) in frame.indexedTarget!!) {
                                if (value !is Undefined) {
                                    normalized[index.toString()] = value
                                }
                            }
                            stack.removeLast()
                            frame.onResult(normalized)
                            continue
                        }

                        val merged =
                            if (frame.targetIsSet) {
                                frame.indexedTarget!!.values.toSet()
                            } else {
                                frame.indexedTarget!!.values.toList()
                            }
                        stack.removeLast()
                        frame.onResult(merged)
                        continue
                    }

                    val idx = frame.listIndex++
                    val item = frame.sourceList!![idx]
                    val indexedTarget = frame.indexedTarget!!

                    if (indexedTarget.containsKey(idx)) {
                        val childTarget = indexedTarget[idx]
                        if (childTarget is Undefined) {
                            if (item !is Undefined) {
                                indexedTarget[idx] = item
                            }
                            continue
                        }
                        if (item is Undefined) {
                            continue
                        }
                        stack.add(
                            MergeFrame(
                                target = childTarget,
                                source = item,
                                options = frame.options,
                                onResult = { value -> indexedTarget[idx] = value },
                            )
                        )
                        continue
                    }

                    indexedTarget[idx] = item
                    continue
                }
            }
        }

        return result
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

    /** Decode raw bytes to a String using the supplied charset, replacing malformed input. */
    private fun decodeBytes(bytes: ByteArray, charset: Charset): String =
        if (charset == StandardCharsets.UTF_8) {
            val decoder =
                charset
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } else {
            String(bytes, charset)
        }

    /** Extract bytes from a ByteBuffer without mutating its position. */
    private fun byteBufferToArray(buffer: ByteBuffer): ByteArray {
        val dup = buffer.duplicate()
        val out = ByteArray(dup.remaining())
        dup.get(out)
        return out
    }

    /** Coerce ByteArray/ByteBuffer to String using the supplied charset; null otherwise. */
    internal fun bytesToString(value: Any?, charset: Charset): String? =
        when (value) {
            is ByteArray -> decodeBytes(value, charset)
            is ByteBuffer -> decodeBytes(byteBufferToArray(value), charset)
            else -> null
        }

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
                is ByteBuffer -> bytesToString(value, charset)
                is ByteArray -> bytesToString(value, charset)
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
            var end = minOf(j + SEGMENT_LIMIT, str.length)
            if (end < str.length) {
                val last = str[end - 1]
                val next = str[end]
                if (Character.isHighSurrogate(last) && Character.isLowSurrogate(next)) {
                    end -= 1 // keep surrogate pair together
                }
            }
            val segment = str.substring(j, end)

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

                    c in 0xD800..0xDBFF -> { // high surrogate
                        if (i + 1 < segment.length) {
                            val nextC = segment[i + 1].code
                            if (nextC in 0xDC00..0xDFFF) {
                                val codePoint =
                                    0x10000 + (((c - 0xD800) shl 10) or (nextC - 0xDC00))
                                buffer.append(HexTable[0xF0 or (codePoint shr 18)])
                                buffer.append(HexTable[0x80 or ((codePoint shr 12) and 0x3F)])
                                buffer.append(HexTable[0x80 or ((codePoint shr 6) and 0x3F)])
                                buffer.append(HexTable[0x80 or (codePoint and 0x3F)])
                                i += 2
                                continue
                            }
                        }
                        // Lone high surrogate: encode code unit as 3-byte sequence.
                        buffer.append(HexTable[0xE0 or (c shr 12)])
                        buffer.append(HexTable[0x80 or ((c shr 6) and 0x3F)])
                        buffer.append(HexTable[0x80 or (c and 0x3F)])
                        i++
                        continue
                    }

                    c in 0xDC00..0xDFFF -> { // lone low surrogate
                        buffer.append(HexTable[0xE0 or (c shr 12)])
                        buffer.append(HexTable[0x80 or ((c shr 6) and 0x3F)])
                        buffer.append(HexTable[0x80 or (c and 0x3F)])
                        i++
                        continue
                    }

                    else -> { // 3 bytes
                        buffer.append(HexTable[0xE0 or (c shr 12)])
                        buffer.append(HexTable[0x80 or ((c shr 6) and 0x3F)])
                        buffer.append(HexTable[0x80 or (c and 0x3F)])
                        i++
                        continue
                    }
                }
            }

            j = end
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
     * An internal Map implementation used to track objects that have exceeded the array limit. It
     * maintains the maximum numeric index to allow O(1) appending of new elements.
     */
    internal class OverflowMap : LinkedHashMap<String, Any?>() {
        var maxIndex: Int = -1
    }

    /**
     * Checks if the given object is an overflow map.
     *
     * @param value The object to check.
     * @return True if the object is an overflow map, false otherwise.
     */
    fun isOverflow(value: Any?): Boolean = value is OverflowMap

    /**
     * Combines two objects into a list or a map if the list limit is exceeded.
     *
     * @param a The first object to combine.
     * @param b The second object to combine.
     * @param limit The maximum number of elements allowed in a list.
     * @return A list or a map containing the combined elements.
     */
    fun combine(a: Any?, b: Any?, limit: Int): Any? {
        // If 'a' is already an overflow object, add to it
        if (a is OverflowMap) {
            var newIndex = a.maxIndex
            if (b is Iterable<*>) {
                for (item in b) {
                    newIndex += 1
                    a[newIndex.toString()] = item
                }
            } else {
                newIndex += 1
                a[newIndex.toString()] = b
            }
            a.maxIndex = newIndex
            return a
        }

        val result = mutableListOf<Any?>()

        @Suppress("UNCHECKED_CAST")
        when (a) {
            is Iterable<*> -> result.addAll(a)
            else -> result.add(a)
        }

        @Suppress("UNCHECKED_CAST")
        when (b) {
            is Iterable<*> -> result.addAll(b)
            else -> result.add(b)
        }

        if (limit >= 0 && result.size > limit) {
            val map = OverflowMap()
            result.forEachIndexed { index, item -> map[index.toString()] = item }
            map.maxIndex = result.size - 1
            return map
        }

        return result
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
