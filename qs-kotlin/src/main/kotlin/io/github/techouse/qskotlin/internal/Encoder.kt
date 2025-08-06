package io.github.techouse.qskotlin.internal

import io.github.techouse.qskotlin.enums.Format
import io.github.techouse.qskotlin.enums.Formatter
import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.enums.ListFormatGenerator
import io.github.techouse.qskotlin.models.DateSerializer
import io.github.techouse.qskotlin.models.Filter
import io.github.techouse.qskotlin.models.FunctionFilter
import io.github.techouse.qskotlin.models.IterableFilter
import io.github.techouse.qskotlin.models.Sorter
import io.github.techouse.qskotlin.models.Undefined
import io.github.techouse.qskotlin.models.ValueEncoder
import io.github.techouse.qskotlin.models.WeakWrapper
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.util.WeakHashMap
import kotlin.collections.get

/** A helper object for encoding data into a query string format. */
internal object Encoder {
    // Top-level unique token
    private val SENTINEL = Any()

    /**
     * Encodes the given data into a query string format.
     *
     * @param data The data to encode; can be any type.
     * @param undefined If true, will not encode undefined values.
     * @param sideChannel A mutable map for tracking cyclic references.
     * @param prefix An optional prefix for the encoded string.
     * @param generateArrayPrefix A generator for array prefixes.
     * @param commaRoundTrip If true, uses comma for array encoding.
     * @param allowEmptyLists If true, allows empty lists in the output.
     * @param strictNullHandling If true, handles nulls strictly.
     * @param skipNulls If true, skips null values in the output.
     * @param encodeDotInKeys If true, encodes dots in keys.
     * @param encoder An optional custom encoder function.
     * @param serializeDate An optional date serializer function.
     * @param sort An optional sorter for keys.
     * @param filter An optional filter to apply to the data.
     * @param allowDots If true, allows dots in keys.
     * @param format The format to use for encoding (default is RFC3986).
     * @param formatter A custom formatter function.
     * @param encodeValuesOnly If true, only encodes values without keys.
     * @param charset The character set to use (default is UTF-8).
     * @param addQueryPrefix If true, adds a '?' prefix to the output.
     */
    fun encode(
        data: Any?,
        undefined: Boolean,
        sideChannel: MutableMap<Any?, Any?>,
        prefix: String? = null,
        generateArrayPrefix: ListFormatGenerator? = null,
        commaRoundTrip: Boolean? = null,
        allowEmptyLists: Boolean = false,
        strictNullHandling: Boolean = false,
        skipNulls: Boolean = false,
        encodeDotInKeys: Boolean = false,
        encoder: ValueEncoder? = null,
        serializeDate: DateSerializer? = null,
        sort: Sorter? = null,
        filter: Filter? = null,
        allowDots: Boolean = false,
        format: Format = Format.RFC3986,
        formatter: Formatter = format.formatter,
        encodeValuesOnly: Boolean = false,
        charset: Charset = StandardCharsets.UTF_8,
        addQueryPrefix: Boolean = false,
    ): Any? {
        val prefix: String = prefix ?: if (addQueryPrefix) "?" else ""
        val generateArrayPrefix: ListFormatGenerator =
            generateArrayPrefix ?: ListFormat.INDICES.generator
        val commaRoundTrip: Boolean =
            commaRoundTrip ?: (generateArrayPrefix == ListFormat.COMMA.generator)

        var obj: Any? = data

        val objWrapper = data?.let { WeakWrapper(it) }
        var tmpSc: MutableMap<Any?, Any?>? = sideChannel
        var step = 0
        var findFlag = false

        // Walk ancestors
        while (!findFlag) {
            @Suppress("UNCHECKED_CAST")
            tmpSc = tmpSc?.get(SENTINEL) as? MutableMap<Any?, Any?> ?: break
            step++
            val pos: Int? = objWrapper?.let { tmpSc[it] as? Int }
            if (pos != null) {
                if (pos == step) {
                    throw IndexOutOfBoundsException("Cyclic object value")
                } else {
                    findFlag = true
                }
            }
            if (tmpSc[SENTINEL] == null) {
                step = 0
            }
        }

        if (filter is FunctionFilter) {
            obj = filter.function(prefix, obj)
        } else if (obj is LocalDateTime) {
            obj =
                when (serializeDate) {
                    null -> obj.toString() // Default ISO format
                    else -> serializeDate(obj)
                }
        } else if (generateArrayPrefix == ListFormat.COMMA.generator && obj is Iterable<*>) {
            obj =
                obj.map { value ->
                    when (value) {
                        is Instant -> value.toString()
                        is LocalDateTime -> serializeDate?.invoke(value) ?: value.toString()
                        else -> value
                    }
                }
        }

        if (!undefined && obj == null) {
            if (strictNullHandling)
                return when {
                    (encoder != null && !encodeValuesOnly) -> encoder(prefix, charset, format)
                    else -> prefix
                }

            obj = ""
        }

        if (Utils.isNonNullishPrimitive(obj, skipNulls) || obj is ByteArray)
            return when {
                (encoder != null) -> {
                    val keyValue: String =
                        if (encodeValuesOnly) prefix else encoder(prefix, null, null)

                    "${formatter(keyValue)}=${formatter(encoder(obj, null, null))}"
                }

                else -> "${formatter(prefix)}=${formatter(obj.toString())}"
            }

        val values = mutableListOf<Any?>()

        if (undefined) {
            return values
        }

        val objKeys: List<Any?> =
            when {
                generateArrayPrefix == ListFormat.COMMA.generator && obj is Iterable<*> -> {
                    // we need to join elements in
                    if (encodeValuesOnly && encoder != null) {
                        obj = obj.map { el -> el?.let { encoder(it.toString(), null, null) } ?: "" }
                    }

                    if (obj.iterator().hasNext()) {
                        val objKeysValue = obj.joinToString(",") { el -> el?.toString() ?: "" }

                        listOf(mapOf("value" to objKeysValue.ifEmpty { null }))
                    } else {
                        listOf(mapOf("value" to Undefined.Companion()))
                    }
                }

                filter is IterableFilter -> filter.iterable.toList()

                else -> {
                    val keys: Iterable<Any?> =
                        when (obj) {
                            is Map<*, *> -> obj.keys
                            is List<*> -> obj.indices
                            is Array<*> -> obj.indices
                            is Iterable<*> -> obj.mapIndexed { index, _ -> index }
                            else -> emptyList()
                        }

                    if (sort != null) {
                        keys.toMutableList().apply { sortWith(sort) }
                    } else {
                        keys.toList()
                    }
                }
            }

        val encodedPrefix: String = if (encodeDotInKeys) prefix.replace(".", "%2E") else prefix

        val adjustedPrefix: String =
            if ((commaRoundTrip && obj is Iterable<*> && obj.count() == 1)) "$encodedPrefix[]"
            else encodedPrefix

        if (allowEmptyLists && obj is Iterable<*> && !obj.iterator().hasNext()) {
            return "$adjustedPrefix[]"
        }

        for (i: Int in 0 until objKeys.size) {
            val key = objKeys[i]
            val (value: Any?, valueUndefined: Boolean) =
                when {
                    key is Map<*, *> && key.containsKey("value") && key["value"] !is Undefined -> {
                        Pair(key["value"], false)
                    }

                    else ->
                        when (obj) {
                            is Map<*, *> -> {
                                Pair(obj[key], !obj.containsKey(key))
                            }

                            is Iterable<*> -> {
                                val index = key as? Int
                                if (index != null && index >= 0 && index < obj.count()) {
                                    Pair(obj.elementAt(index), false)
                                } else {
                                    Pair(null, true)
                                }
                            }

                            is Array<*> -> {
                                val index = key as? Int
                                if (index != null && index >= 0 && index < obj.size) {
                                    Pair(obj[index], false)
                                } else {
                                    Pair(null, true)
                                }
                            }

                            else -> {
                                Pair(null, true) // Handle unsupported object types gracefully
                            }
                        }
                }

            if (skipNulls && value == null) {
                continue
            }

            val encodedKey: String =
                if (allowDots && encodeDotInKeys) key.toString().replace(".", "%2E")
                else key.toString()

            val keyPrefix: String =
                if (obj is Iterable<*>) generateArrayPrefix(adjustedPrefix, encodedKey)
                else "$adjustedPrefix${if (allowDots) ".$encodedKey" else "[$encodedKey]"}"

            // Record the current container in this frame so children can detect cycles.
            if (obj is Map<*, *> || obj is Iterable<*>) {
                objWrapper?.let { sideChannel[it] = step }
            }

            // Create child side-channel and link to the parent
            val valueSideChannel = WeakHashMap<Any?, Any?>()
            valueSideChannel[SENTINEL] = sideChannel

            val encoded: Any? =
                encode(
                    data = value,
                    undefined = valueUndefined,
                    prefix = keyPrefix,
                    generateArrayPrefix = generateArrayPrefix,
                    commaRoundTrip = commaRoundTrip,
                    allowEmptyLists = allowEmptyLists,
                    strictNullHandling = strictNullHandling,
                    skipNulls = skipNulls,
                    encodeDotInKeys = encodeDotInKeys,
                    encoder =
                        when {
                            generateArrayPrefix == ListFormat.COMMA.generator &&
                                encodeValuesOnly &&
                                obj is Iterable<*> -> null
                            else -> encoder
                        },
                    serializeDate = serializeDate,
                    filter = filter,
                    sort = sort,
                    allowDots = allowDots,
                    format = format,
                    formatter = formatter,
                    encodeValuesOnly = encodeValuesOnly,
                    charset = charset,
                    addQueryPrefix = addQueryPrefix,
                    sideChannel = valueSideChannel,
                )

            when (encoded) {
                is Iterable<*> -> values.addAll(encoded)
                else -> values.add(encoded)
            }
        }

        return values
    }
}
