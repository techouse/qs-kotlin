package io.github.techouse.qskotlin.internal

import io.github.techouse.qskotlin.enums.Format
import io.github.techouse.qskotlin.enums.Formatter
import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.enums.ListFormatGenerator
import io.github.techouse.qskotlin.models.*
import io.github.techouse.qskotlin.models.Undefined
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.util.Collections
import java.util.IdentityHashMap

/** A helper object for encoding data into a query string format. */
internal object Encoder {
    private val indicesGenerator: ListFormatGenerator = ListFormat.INDICES.generator
    private val bracketsGenerator: ListFormatGenerator = ListFormat.BRACKETS.generator
    private val repeatGenerator: ListFormatGenerator = ListFormat.REPEAT.generator
    private val commaGenerator: ListFormatGenerator = ListFormat.COMMA.generator

    // Traversal phases for the encoder's explicit stack.
    private enum class Phase {
        START,
        ITERATE,
        WAIT_CHILD,
    }

    private data class TraversalContext(
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
    ) {
        val isCommaGenerator: Boolean = generateArrayPrefix === commaGenerator

        fun withEncoder(value: ValueEncoder?): TraversalContext {
            return if (value === encoder) this else copy(encoder = value)
        }
    }

    // Mutable traversal frame; kept local to avoid leaking internal state.
    private class Frame(
        var obj: Any?,
        val undefined: Boolean,
        val path: KeyPathNode,
        val context: TraversalContext,
        var phase: Phase = Phase.START,
        var values: MutableList<Any?>? = null,
        var objKeys: List<Any?> = emptyList(),
        var index: Int = 0,
        var adjustedPath: KeyPathNode = path,
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
        val generator: ListFormatGenerator = generateArrayPrefix ?: indicesGenerator
        val effectiveCommaRoundTrip: Boolean = commaRoundTrip ?: (generator === commaGenerator)
        val rootContext =
            TraversalContext(
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
            )

        tryEncodeLinearChain(data, undefined, prefixValue, rootContext)?.let {
            return it
        }

        // Use identity-based tracking for the current traversal path to detect cycles.
        val seen = Collections.newSetFromMap(IdentityHashMap<Any?, Boolean>())

        val stack = ArrayDeque<Frame>()
        stack.add(
            Frame(
                obj = data,
                undefined = undefined,
                path = KeyPathNode.fromMaterialized(prefixValue),
                context = rootContext,
            )
        )

        var lastResult: Any? = null

        fun finishFrame(result: Any?) {
            val completed = stack.removeLast()
            if (completed.tracked) {
                completed.trackedObject?.let { seen.remove(it) }
            }
            lastResult = result
        }

        fun appendFrameValues(frame: Frame, encoded: Any?) {
            if (encoded is Iterable<*>) {
                var values: MutableList<Any?>? = frame.values
                for (item in encoded) {
                    if (values == null) {
                        values = mutableListOf()
                        frame.values = values
                    }
                    values.add(item)
                }
            } else {
                val values = frame.values ?: mutableListOf<Any?>().also { frame.values = it }
                values.add(encoded)
            }
        }

        while (stack.isNotEmpty()) {
            val frame = stack.last()

            when (frame.phase) {
                Phase.START -> {
                    var obj: Any? = frame.obj
                    val context = frame.context
                    var pathText: String? = null
                    fun materializedPath(): String {
                        return pathText ?: frame.path.materialize().also { pathText = it }
                    }

                    when (val f = context.filter) {
                        is FunctionFilter -> {
                            obj = f.function(materializedPath(), obj)
                        }
                        else -> Unit
                    }

                    if (obj is LocalDateTime) {
                        obj = context.serializeDate?.invoke(obj) ?: obj.toString()
                    } else if (context.isCommaGenerator && obj is Iterable<*>) {
                        obj =
                            obj.map { value ->
                                when (value) {
                                    is Instant -> value.toString()
                                    is LocalDateTime ->
                                        context.serializeDate?.invoke(value) ?: value.toString()
                                    else -> value
                                }
                            }
                    }

                    if (!frame.undefined && obj == null) {
                        if (context.strictNullHandling) {
                            val keyOnly =
                                if (context.encoder != null && !context.encodeValuesOnly) {
                                    context.encoder.invoke(
                                        materializedPath(),
                                        context.charset,
                                        context.format,
                                    )
                                } else {
                                    materializedPath()
                                }
                            finishFrame(keyOnly)
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
                        Utils.isNonNullishPrimitive(obj, context.skipNulls) ||
                            obj is ByteArray ||
                            obj is ByteBuffer
                    ) {
                        val fragment =
                            if (context.encoder != null) {
                                val keyValue =
                                    if (context.encodeValuesOnly) materializedPath()
                                    else
                                        context.encoder.invoke(
                                            materializedPath(),
                                            context.charset,
                                            context.format,
                                        )
                                val encodedValue =
                                    context.encoder.invoke(obj, context.charset, context.format)
                                "${context.formatter(keyValue)}=${context.formatter(encodedValue)}"
                            } else {
                                val rawValue =
                                    Utils.bytesToString(obj, context.charset) ?: obj.toString()
                                "${context.formatter(materializedPath())}=${context.formatter(rawValue)}"
                            }

                        finishFrame(fragment)
                        continue
                    }

                    frame.obj = obj
                    if (frame.undefined) {
                        finishFrame(mutableListOf<Any?>())
                        continue
                    }

                    if (obj is Iterable<*> && obj !is List<*>) {
                        frame.iterableList = obj.toList()
                    }

                    val objKeys: List<Any?> =
                        when {
                            context.isCommaGenerator && obj is Iterable<*> -> {
                                val items =
                                    when {
                                        obj is List<*> -> obj
                                        frame.iterableList != null -> frame.iterableList!!
                                        else -> obj.toList()
                                    }

                                val filtered =
                                    if (context.commaCompactNulls) items.filterNotNull() else items

                                frame.effectiveCommaLength = filtered.size

                                val joinSource =
                                    if (context.encodeValuesOnly && context.encoder != null) {
                                        filtered.map { el ->
                                            el?.let {
                                                context.encoder.invoke(it.toString(), null, null)
                                            } ?: ""
                                        }
                                    } else {
                                        filtered.map { el ->
                                            when (el) {
                                                is ByteArray,
                                                is ByteBuffer ->
                                                    Utils.bytesToString(el, context.charset) ?: ""
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

                            context.filter is IterableFilter -> {
                                context.filter.iterable.toList()
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

                                if (context.sort != null) {
                                    keys.toMutableList().apply { sortWith(context.sort) }
                                } else {
                                    keys.toList()
                                }
                            }
                        }

                    val pathForChildren: KeyPathNode =
                        if (context.encodeDotInKeys) frame.path.asDotEncoded() else frame.path

                    val adjustedPath: KeyPathNode =
                        if (
                            context.commaRoundTrip &&
                                obj is Iterable<*> &&
                                (if (
                                    context.isCommaGenerator && frame.effectiveCommaLength != null
                                ) {
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
                            pathForChildren.append("[]")
                        else pathForChildren

                    val iterableEmpty =
                        if (obj is Iterable<*>) {
                            when (obj) {
                                is Collection<*> -> obj.isEmpty()
                                else -> frame.iterableList?.isEmpty() ?: !obj.iterator().hasNext()
                            }
                        } else {
                            false
                        }

                    if (context.allowEmptyLists && obj is Iterable<*> && iterableEmpty) {
                        finishFrame(adjustedPath.append("[]").materialize())
                        continue
                    }

                    frame.objKeys = objKeys
                    frame.adjustedPath = adjustedPath
                    frame.phase = Phase.ITERATE
                    continue
                }

                Phase.ITERATE -> {
                    val context = frame.context
                    if (frame.index >= frame.objKeys.size) {
                        finishFrame(frame.values ?: emptyList<Any?>())
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

                    if (context.skipNulls && value == null) {
                        continue
                    }

                    val encodedKey: String =
                        if (context.allowDots && context.encodeDotInKeys)
                            key.toString().replace(".", "%2E")
                        else key.toString()

                    val adjustedPath = frame.adjustedPath
                    val keyPath: KeyPathNode =
                        if (obj is Iterable<*>) {
                            buildSequenceChildPath(
                                adjustedPath = adjustedPath,
                                encodedKey = encodedKey,
                                generator = context.generateArrayPrefix,
                            )
                        } else if (context.allowDots) {
                            adjustedPath.append(".$encodedKey")
                        } else {
                            adjustedPath.append("[$encodedKey]")
                        }

                    val childEncoder =
                        if (
                            context.isCommaGenerator &&
                                context.encodeValuesOnly &&
                                obj is Iterable<*>
                        )
                            null
                        else context.encoder
                    val childContext = context.withEncoder(childEncoder)

                    frame.phase = Phase.WAIT_CHILD
                    stack.add(
                        Frame(
                            obj = value,
                            undefined = valueUndefined,
                            path = keyPath,
                            context = childContext,
                        )
                    )
                    continue
                }

                Phase.WAIT_CHILD -> {
                    appendFrameValues(frame, lastResult)
                    frame.phase = Phase.ITERATE
                    continue
                }
            }
        }

        return lastResult ?: emptyList<Any?>()
    }

    private fun buildSequenceChildPath(
        adjustedPath: KeyPathNode,
        encodedKey: String,
        generator: ListFormatGenerator,
    ): KeyPathNode {
        return when {
            generator === indicesGenerator -> adjustedPath.append("[$encodedKey]")
            generator === bracketsGenerator -> adjustedPath.append("[]")
            generator === repeatGenerator || generator === commaGenerator -> adjustedPath
            else -> KeyPathNode.fromMaterialized(generator(adjustedPath.materialize(), encodedKey))
        }
    }

    private fun tryEncodeLinearChain(
        data: Any?,
        undefined: Boolean,
        prefix: String,
        context: TraversalContext,
    ): Any? {
        if (undefined) return null
        if (context.filter != null || context.sort != null) return null
        if (context.allowEmptyLists) return null
        if (context.commaRoundTrip || context.commaCompactNulls || context.isCommaGenerator)
            return null
        if (data !is Map<*, *>) return null

        val seen = Collections.newSetFromMap(IdentityHashMap<Any?, Boolean>())
        var current: Any? = data
        var path = KeyPathNode.fromMaterialized(prefix)

        while (current is Map<*, *>) {
            if (!seen.add(current)) {
                throw IndexOutOfBoundsException("Cyclic object value")
            }
            if (current.size != 1) {
                return null
            }

            val entry = current.entries.first()
            val key = entry.key.toString()
            val encodedKey =
                if (context.allowDots && context.encodeDotInKeys) key.replace(".", "%2E") else key
            val pathForChildren = if (context.encodeDotInKeys) path.asDotEncoded() else path

            path =
                if (context.allowDots) {
                    pathForChildren.append(".$encodedKey")
                } else {
                    pathForChildren.append("[$encodedKey]")
                }

            current = entry.value
        }

        var leaf = current
        if (leaf is LocalDateTime) {
            leaf = context.serializeDate?.invoke(leaf) ?: leaf.toString()
        }

        if (leaf == null) {
            if (context.strictNullHandling) {
                val keyOnly =
                    if (context.encoder != null && !context.encodeValuesOnly) {
                        context.encoder.invoke(path.materialize(), context.charset, context.format)
                    } else {
                        path.materialize()
                    }
                return listOf(keyOnly)
            }
            leaf = ""
        }

        if (leaf is Undefined) {
            return emptyList<Any?>()
        }

        if (
            Utils.isNonNullishPrimitive(leaf, context.skipNulls) ||
                leaf is ByteArray ||
                leaf is ByteBuffer
        ) {
            val fragment =
                if (context.encoder != null) {
                    val keyValue =
                        if (context.encodeValuesOnly) path.materialize()
                        else
                            context.encoder.invoke(
                                path.materialize(),
                                context.charset,
                                context.format,
                            )
                    val encodedValue = context.encoder.invoke(leaf, context.charset, context.format)
                    "${context.formatter(keyValue)}=${context.formatter(encodedValue)}"
                } else {
                    val rawValue = Utils.bytesToString(leaf, context.charset) ?: leaf.toString()
                    "${context.formatter(path.materialize())}=${context.formatter(rawValue)}"
                }
            return listOf(fragment)
        }

        return null
    }
}
