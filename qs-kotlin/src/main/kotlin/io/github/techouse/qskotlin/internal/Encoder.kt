package io.github.techouse.qskotlin.internal

import io.github.techouse.qskotlin.enums.Format
import io.github.techouse.qskotlin.enums.Formatter
import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.enums.ListFormatGenerator
import io.github.techouse.qskotlin.models.*
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.util.Collections
import java.util.IdentityHashMap

/** A helper object for encoding data into a query string format. */
internal object Encoder {
    private enum class Phase {
        START,
        ITERATE,
        WAIT_CHILD,
    }

    private class Frame(
        var obj: Any?,
        val undefined: Boolean,
        val prefix: String,
        val generateArrayPrefix: ListFormatGenerator,
        val commaRoundTrip: Boolean,
        val commaCompactNulls: Boolean,
        val allowEmptyLists: Boolean,
        val strictNullHandling: Boolean,
        val skipNulls: Boolean,
        val encodeDotInKeys: Boolean,
        val encoder: ValueEncoder?,
        val serializeDate: DateSerializer?,
        val sort: Sorter?,
        val filter: Filter?,
        val allowDots: Boolean,
        val format: Format,
        val formatter: Formatter,
        val encodeValuesOnly: Boolean,
        val charset: Charset,
        val addQueryPrefix: Boolean,
        var phase: Phase = Phase.START,
        var values: MutableList<Any?> = mutableListOf(),
        var objKeys: List<Any?> = emptyList(),
        var index: Int = 0,
        var adjustedPrefix: String = "",
        var effectiveCommaLength: Int? = null,
        var iterableList: List<Any?>? = null,
        var tracked: Boolean = false,
        var trackedObject: Any? = null,
    )

    /**
     * Encodes the given data into a query string format.
     *
     * @param data The data to encode; can be any type.
     * @param undefined If true, will not encode undefined values.
     * @param sideChannel Reserved for compatibility; unused (cycle tracking is internal).
     * @param prefix An optional prefix for the encoded string.
     * @param generateArrayPrefix A generator for array prefixes.
     * @param commaRoundTrip If true, uses comma for array encoding.
     * @param commaCompactNulls If true, compacts nulls in comma-separated lists.
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
        @Suppress("UNUSED_PARAMETER") sideChannel: MutableMap<Any?, Any?>,
        prefix: String? = null,
        generateArrayPrefix: ListFormatGenerator? = null,
        commaRoundTrip: Boolean? = null,
        commaCompactNulls: Boolean = false,
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
    ): Any {
        val prefixValue: String = prefix ?: if (addQueryPrefix) "?" else ""
        val generator: ListFormatGenerator = generateArrayPrefix ?: ListFormat.INDICES.generator
        val isCommaGenerator = generator == ListFormat.COMMA.generator
        val effectiveCommaRoundTrip: Boolean = commaRoundTrip ?: isCommaGenerator

        // Use identity-based tracking for the current traversal path to detect cycles.
        val seen = Collections.newSetFromMap(IdentityHashMap<Any?, Boolean>())

        val stack = ArrayDeque<Frame>()
        stack.add(
            Frame(
                obj = data,
                undefined = undefined,
                prefix = prefixValue,
                generateArrayPrefix = generator,
                commaRoundTrip = effectiveCommaRoundTrip,
                commaCompactNulls = commaCompactNulls,
                allowEmptyLists = allowEmptyLists,
                strictNullHandling = strictNullHandling,
                skipNulls = skipNulls,
                encodeDotInKeys = encodeDotInKeys,
                encoder = encoder,
                serializeDate = serializeDate,
                sort = sort,
                filter = filter,
                allowDots = allowDots,
                format = format,
                formatter = formatter,
                encodeValuesOnly = encodeValuesOnly,
                charset = charset,
                addQueryPrefix = addQueryPrefix,
            )
        )

        var lastResult: Any? = null

        while (stack.isNotEmpty()) {
            val frame = stack.last()

            when (frame.phase) {
                Phase.START -> {
                    var obj: Any? = frame.obj

                    when (val f = frame.filter) {
                        is FunctionFilter -> {
                            obj = f.function(frame.prefix, obj)
                        }
                        else -> Unit
                    }

                    if (obj is LocalDateTime) {
                        obj = frame.serializeDate?.invoke(obj) ?: obj.toString()
                    } else if (isCommaGenerator && obj is Iterable<*>) {
                        obj =
                            obj.map { value ->
                                when (value) {
                                    is Instant -> value.toString()
                                    is LocalDateTime ->
                                        frame.serializeDate?.invoke(value) ?: value.toString()
                                    else -> value
                                }
                            }
                    }

                    if (!frame.undefined && obj == null) {
                        if (frame.strictNullHandling) {
                            val keyOnly =
                                if (frame.encoder != null && !frame.encodeValuesOnly) {
                                    frame.encoder.invoke(frame.prefix, frame.charset, frame.format)
                                } else {
                                    frame.prefix
                                }
                            if (frame.tracked) {
                                frame.trackedObject?.let { seen.remove(it) }
                            }
                            stack.removeLast()
                            lastResult = keyOnly
                            continue
                        }
                        obj = ""
                    }

                    val trackObject = obj is Map<*, *> || obj is Array<*> || obj is Iterable<*>

                    if (trackObject) {
                        val objRef = obj
                        if (seen.contains(objRef)) {
                            throw IndexOutOfBoundsException("Cyclic object value")
                        }
                        seen.add(objRef)
                        frame.tracked = true
                        frame.trackedObject = objRef
                    }

                    if (
                        Utils.isNonNullishPrimitive(obj, frame.skipNulls) ||
                            obj is ByteArray ||
                            obj is ByteBuffer
                    ) {
                        val fragment =
                            if (frame.encoder != null) {
                                val keyValue =
                                    if (frame.encodeValuesOnly) frame.prefix
                                    else
                                        frame.encoder.invoke(
                                            frame.prefix,
                                            frame.charset,
                                            frame.format,
                                        )
                                val encodedValue =
                                    frame.encoder.invoke(obj, frame.charset, frame.format)
                                "${frame.formatter(keyValue)}=${frame.formatter(encodedValue)}"
                            } else {
                                val rawValue =
                                    Utils.bytesToString(obj, frame.charset) ?: obj.toString()
                                "${frame.formatter(frame.prefix)}=${frame.formatter(rawValue)}"
                            }

                        if (frame.tracked) {
                            frame.trackedObject?.let { seen.remove(it) }
                        }
                        stack.removeLast()
                        lastResult = fragment
                        continue
                    }

                    frame.obj = obj
                    if (frame.undefined) {
                        if (frame.tracked) {
                            frame.trackedObject?.let { seen.remove(it) }
                        }
                        stack.removeLast()
                        lastResult = mutableListOf<Any?>()
                        continue
                    }

                    if (obj is Iterable<*> && obj !is List<*>) {
                        frame.iterableList = obj.toList()
                    }

                    val objKeys: List<Any?> =
                        when {
                            isCommaGenerator && obj is Iterable<*> -> {
                                val items =
                                    when {
                                        obj is List<*> -> obj
                                        frame.iterableList != null -> frame.iterableList!!
                                        else -> obj.toList()
                                    }

                                val filtered =
                                    if (frame.commaCompactNulls) items.filterNotNull() else items

                                frame.effectiveCommaLength = filtered.size

                                val joinSource =
                                    if (frame.encodeValuesOnly && frame.encoder != null) {
                                        filtered.map { el ->
                                            el?.let {
                                                frame.encoder.invoke(it.toString(), null, null)
                                            } ?: ""
                                        }
                                    } else {
                                        filtered.map { el ->
                                            when (el) {
                                                is ByteArray,
                                                is ByteBuffer ->
                                                    Utils.bytesToString(el, frame.charset) ?: ""
                                                else -> el?.toString() ?: ""
                                            }
                                        }
                                    }

                                if (joinSource.isNotEmpty()) {
                                    val joined = joinSource.joinToString(",")
                                    listOf(mapOf("value" to joined.ifEmpty { null }))
                                } else {
                                    listOf(mapOf("value" to Undefined.Companion()))
                                }
                            }

                            frame.filter is IterableFilter -> {
                                frame.filter.iterable.toList()
                            }

                            else -> {
                                val keys: Iterable<Any?> =
                                    when (obj) {
                                        is Map<*, *> -> obj.keys
                                        is List<*> -> obj.indices
                                        is Array<*> -> obj.indices
                                        is Iterable<*> -> {
                                            val list = frame.iterableList ?: obj.toList()
                                            list.indices
                                        }
                                        else -> emptyList()
                                    }

                                if (frame.sort != null) {
                                    keys.toMutableList().apply { sortWith(frame.sort) }
                                } else {
                                    keys.toList()
                                }
                            }
                        }

                    val encodedPrefix: String =
                        if (frame.encodeDotInKeys) frame.prefix.replace(".", "%2E")
                        else frame.prefix

                    val adjustedPrefix: String =
                        if (
                            frame.commaRoundTrip &&
                                obj is Iterable<*> &&
                                (if (isCommaGenerator && frame.effectiveCommaLength != null) {
                                    frame.effectiveCommaLength == 1
                                } else {
                                    val count =
                                        when (obj) {
                                            is Collection<*> -> obj.size
                                            else -> frame.iterableList?.size ?: obj.count()
                                        }
                                    count == 1
                                })
                        )
                            "$encodedPrefix[]"
                        else encodedPrefix

                    val iterableEmpty =
                        if (obj is Iterable<*>) {
                            when (obj) {
                                is Collection<*> -> obj.isEmpty()
                                else -> frame.iterableList?.isEmpty() ?: !obj.iterator().hasNext()
                            }
                        } else {
                            false
                        }

                    if (frame.allowEmptyLists && obj is Iterable<*> && iterableEmpty) {
                        if (frame.tracked) {
                            frame.trackedObject?.let { seen.remove(it) }
                        }
                        stack.removeLast()
                        lastResult = "$adjustedPrefix[]"
                        continue
                    }

                    frame.objKeys = objKeys
                    frame.adjustedPrefix = adjustedPrefix
                    frame.phase = Phase.ITERATE
                    continue
                }

                Phase.ITERATE -> {
                    if (frame.index >= frame.objKeys.size) {
                        if (frame.tracked) {
                            frame.trackedObject?.let { seen.remove(it) }
                        }
                        stack.removeLast()
                        lastResult = frame.values
                        continue
                    }

                    val key = frame.objKeys[frame.index++]
                    val obj = frame.obj

                    val (value: Any?, valueUndefined: Boolean) =
                        when {
                            key is Map<*, *> &&
                                key.containsKey("value") &&
                                key["value"] !is Undefined -> {
                                Pair(key["value"], false)
                            }
                            else ->
                                when (obj) {
                                    is Map<*, *> -> Pair(obj[key], !obj.containsKey(key))
                                    is Iterable<*> -> {
                                        val index = key as? Int
                                        val list =
                                            when (obj) {
                                                is List<*> -> obj
                                                else -> frame.iterableList ?: obj.toList()
                                            }
                                        if (index != null && index >= 0 && index < list.size) {
                                            Pair(list[index], false)
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
                                    else -> Pair(null, true)
                                }
                        }

                    if (frame.skipNulls && value == null) {
                        continue
                    }

                    val encodedKey: String =
                        if (frame.allowDots && frame.encodeDotInKeys)
                            key.toString().replace(".", "%2E")
                        else key.toString()

                    val keyPrefix: String =
                        if (obj is Iterable<*>)
                            frame.generateArrayPrefix(frame.adjustedPrefix, encodedKey)
                        else
                            "${frame.adjustedPrefix}${if (frame.allowDots) ".$encodedKey" else "[$encodedKey]"}"

                    val childEncoder =
                        if (
                            frame.generateArrayPrefix == ListFormat.COMMA.generator &&
                                frame.encodeValuesOnly &&
                                obj is Iterable<*>
                        )
                            null
                        else frame.encoder

                    frame.phase = Phase.WAIT_CHILD
                    stack.add(
                        Frame(
                            obj = value,
                            undefined = valueUndefined,
                            prefix = keyPrefix,
                            generateArrayPrefix = frame.generateArrayPrefix,
                            commaRoundTrip = frame.commaRoundTrip,
                            commaCompactNulls = frame.commaCompactNulls,
                            allowEmptyLists = frame.allowEmptyLists,
                            strictNullHandling = frame.strictNullHandling,
                            skipNulls = frame.skipNulls,
                            encodeDotInKeys = frame.encodeDotInKeys,
                            encoder = childEncoder,
                            serializeDate = frame.serializeDate,
                            sort = frame.sort,
                            filter = frame.filter,
                            allowDots = frame.allowDots,
                            format = frame.format,
                            formatter = frame.formatter,
                            encodeValuesOnly = frame.encodeValuesOnly,
                            charset = frame.charset,
                            addQueryPrefix = frame.addQueryPrefix,
                        )
                    )
                    continue
                }

                Phase.WAIT_CHILD -> {
                    val encoded = lastResult
                    when (encoded) {
                        is Iterable<*> -> frame.values.addAll(encoded)
                        else -> frame.values.add(encoded)
                    }
                    frame.phase = Phase.ITERATE
                    continue
                }
            }
        }

        return lastResult ?: emptyList<Any?>()
    }
}
