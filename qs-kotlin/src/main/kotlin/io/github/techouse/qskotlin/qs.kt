@file:JvmName("QS")
@file:JvmMultifileClass

package io.github.techouse.qskotlin

import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.enums.Sentinel
import io.github.techouse.qskotlin.internal.Decoder
import io.github.techouse.qskotlin.internal.Encoder
import io.github.techouse.qskotlin.internal.Utils
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.EncodeOptions
import io.github.techouse.qskotlin.models.FunctionFilter
import io.github.techouse.qskotlin.models.IterableFilter
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Decode a query [String] or a [Map] into a [Map<String, Any?>].
 *
 * @param input [Any?] the query string or Map to decode
 * @param options [DecodeOptions] optional decoder settings
 * @return [Map<String, Any?>] the decoded Map
 * @throws IllegalArgumentException if the input is not a String or Map
 */
@Throws(IllegalArgumentException::class, IndexOutOfBoundsException::class)
@JvmOverloads
fun decode(input: Any?, options: DecodeOptions? = null): Map<String, Any?> {
    val options = options ?: DecodeOptions()

    if (input !is String? && input !is Map<*, *>?) {
        throw IllegalArgumentException("The input must be a String or a Map<String, Any?>")
    }

    if (
        input == null ||
            (input is String && input.isEmpty()) ||
            (input is Map<*, *> && input.isEmpty())
    ) {
        return emptyMap()
    }

    val tempObj: MutableMap<String, Any?>? =
        when (input) {
            is String -> Decoder.parseQueryStringValues(input, options)
            is Map<*, *> -> input.mapKeys { it.key.toString() }.toMutableMap()
            else -> null
        }

    var finalOptions = options
    if (options.parseLists && options.listLimit > 0 && (tempObj?.size ?: 0) > options.listLimit) {
        finalOptions = options.copy(parseLists = false)
    }

    var obj = mutableMapOf<String, Any?>()

    if (tempObj?.isNotEmpty() == true) {
        for ((key, value) in tempObj) {
            val parsed = Decoder.parseKeys(key, value, finalOptions, input is String)

            if (obj.isEmpty() && parsed is MutableMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                obj = parsed as MutableMap<String, Any?>
                continue
            }

            @Suppress("UNCHECKED_CAST")
            obj = Utils.merge(obj, parsed, finalOptions) as MutableMap<String, Any?>
        }
    }

    return Utils.compact(obj, options.allowSparseLists)
}

/**
 * Encode a [Map] or [Iterable] into a query string.
 *
 * @param data [Any?] the data to encode
 * @param options [EncodeOptions] optional encoder settings
 * @return [String] the encoded query string
 */
@Throws(IndexOutOfBoundsException::class)
@JvmOverloads
fun encode(data: Any?, options: EncodeOptions? = null): String {
    val options = options ?: EncodeOptions()

    if (data == null) return ""

    var obj: Map<String, Any?> =
        when (data) {
            is Map<*, *> -> data.mapKeys { it.key.toString() }
            is Iterable<*> ->
                buildMap {
                    var i = 0
                    for (v in data) put((i++).toString(), v)
                }

            else -> emptyMap()
        }

    val keys = mutableListOf<Any?>()

    if (obj.isEmpty()) {
        return ""
    }

    var objKeys: List<*>? = null

    when (val filter = options.filter) {
        is FunctionFilter -> {
            try {
                val filtered = filter.function("", obj)
                @Suppress("UNCHECKED_CAST")
                if (filtered is Map<*, *>) {
                    obj = filtered as Map<String, Any?>
                }
            } catch (_: Exception) {
                // Handle function execution error
            }
        }

        is IterableFilter -> {
            objKeys = filter.iterable.toList()
        }

        else -> {
            // No filter applied
        }
    }

    if (objKeys == null) {
        objKeys = obj.keys.toList()
    }

    if (options.sort != null) {
        objKeys = objKeys.sortedWith(options.sort)
    }

    val sideChannel: MutableMap<Any?, Any?> = WeakHashMap()

    for (i: Int in 0 until objKeys.size) {
        val key: Any? = objKeys[i]

        if (key !is String? || (obj[key] == null && options.skipNulls)) {
            continue
        }

        val encoded: Any =
            Encoder.encode(
                data = obj[key],
                undefined = !obj.containsKey(key),
                prefix = key,
                generateArrayPrefix = options.getListFormat.generator,
                commaRoundTrip =
                    options.getListFormat.generator == ListFormat.COMMA.generator &&
                        options.commaRoundTrip == true,
                allowEmptyLists = options.allowEmptyLists,
                strictNullHandling = options.strictNullHandling,
                skipNulls = options.skipNulls,
                encodeDotInKeys = options.encodeDotInKeys,
                encoder =
                    if (options.encode)
                        { value, charset, format -> options.getEncoder(value, charset, format) }
                    else null,
                serializeDate = { date -> options.getDateSerializer(date) },
                filter = options.filter,
                sort = options.sort,
                allowDots = options.getAllowDots,
                format = options.format,
                formatter = options.formatter,
                encodeValuesOnly = options.encodeValuesOnly,
                charset = options.charset,
                addQueryPrefix = options.addQueryPrefix,
                sideChannel = sideChannel,
            )

        when (encoded) {
            is Iterable<*> -> keys.addAll(encoded)
            else -> keys.add(encoded)
        }
    }

    val joined: String = keys.joinToString(separator = options.delimiter.value)
    val out: StringBuilder = StringBuilder()

    if (options.addQueryPrefix) {
        out.append('?')
    }

    if (options.charsetSentinel) {
        when (options.charset) {
            // encodeURIComponent('&#10003') - numeric entity checkmark
            StandardCharsets.ISO_8859_1 -> out.append(Sentinel.ISO)
            // encodeURIComponent('✓')
            StandardCharsets.UTF_8 -> out.append(Sentinel.CHARSET)
        }
        if (joined.isNotEmpty()) out.append(options.delimiter.value)
    }

    if (joined.isNotEmpty()) {
        out.append(joined)
    }

    return out.toString()
}
