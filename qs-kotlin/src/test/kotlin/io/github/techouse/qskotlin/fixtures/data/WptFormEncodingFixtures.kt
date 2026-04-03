package io.github.techouse.qskotlin.fixtures.data

import io.github.techouse.qskotlin.enums.Duplicates
import io.github.techouse.qskotlin.enums.Format
import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.EncodeOptions
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class WptFixtureProvenance(val sourceFiles: List<String>, val note: String)

data class WptDecodeFixtureOptions(
    val charset: String? = null,
    val charsetSentinel: Boolean = false,
    val duplicates: String? = null,
    val ignoreQueryPrefix: Boolean = false,
    val strictNullHandling: Boolean = false,
) {
    fun toDecodeOptions(): DecodeOptions =
        DecodeOptions(
            charset = parseCharset(charset),
            charsetSentinel = charsetSentinel,
            duplicates = duplicates?.let(Duplicates::valueOf) ?: Duplicates.COMBINE,
            ignoreQueryPrefix = ignoreQueryPrefix,
            strictNullHandling = strictNullHandling,
        )
}

data class WptEncodeFixtureOptions(
    val charset: String? = null,
    val format: String? = null,
    val listFormat: String? = null,
    val strictNullHandling: Boolean = false,
) {
    fun toEncodeOptions(): EncodeOptions =
        EncodeOptions(
            charset = parseCharset(charset),
            format = format?.let(Format::valueOf) ?: Format.RFC3986,
            listFormat = listFormat?.let(ListFormat::valueOf),
            strictNullHandling = strictNullHandling,
        )
}

data class WptDecodeFixture(
    val name: String,
    val input: String,
    val options: WptDecodeFixtureOptions = WptDecodeFixtureOptions(),
    val expected: Map<String, Any?>? = null,
    val skipReason: String? = null,
)

data class WptEncodeFixture(
    val name: String,
    val input: Any?,
    val options: WptEncodeFixtureOptions = WptEncodeFixtureOptions(),
    val expected: String? = null,
    val skipReason: String? = null,
)

data class WptFormEncodingFixtureBundle(
    val provenance: WptFixtureProvenance,
    val decode: List<WptDecodeFixture>,
    val encode: List<WptEncodeFixture>,
)

object WptFormEncodingFixtures {
    private const val RESOURCE_PATH = "/wpt-form-encoding-fixtures.json"

    private val json = Json { ignoreUnknownKeys = false }

    val bundle: WptFormEncodingFixtureBundle by lazy(::load)

    fun load(): WptFormEncodingFixtureBundle {
        val text =
            checkNotNull(object {}.javaClass.getResourceAsStream(RESOURCE_PATH)) {
                    "Missing WPT fixture resource: $RESOURCE_PATH"
                }
                .bufferedReader()
                .use { it.readText() }

        val root = json.parseToJsonElement(text).jsonObject
        return WptFormEncodingFixtureBundle(
            provenance = parseProvenance(root.requireObject("provenance")),
            decode = root.requireArray("decode").map { parseDecodeFixture(it.jsonObject) },
            encode = root.requireArray("encode").map { parseEncodeFixture(it.jsonObject) },
        )
    }

    private fun parseProvenance(obj: JsonObject): WptFixtureProvenance =
        WptFixtureProvenance(
            sourceFiles = obj.requireArray("sourceFiles").map { it.jsonPrimitive.content },
            note = obj.requireString("note"),
        )

    private fun parseDecodeFixture(obj: JsonObject): WptDecodeFixture {
        val skipReason = obj.optionalString("skipReason")
        val expected =
            obj["expected"]?.let(::jsonElementToValue)?.let { value ->
                require(value is Map<*, *>) {
                    "Decode fixture '${obj.requireString("name")}' must have an object-valued expected payload."
                }
                @Suppress("UNCHECKED_CAST")
                value as Map<String, Any?>
            }

        return WptDecodeFixture(
            name = obj.requireString("name"),
            input = obj.requireString("input"),
            options = parseDecodeOptions(obj.optionalObject("options")),
            expected = expected,
            skipReason = skipReason,
        )
    }

    private fun parseEncodeFixture(obj: JsonObject): WptEncodeFixture =
        WptEncodeFixture(
            name = obj.requireString("name"),
            input = obj["input"]?.let(::jsonElementToValue),
            options = parseEncodeOptions(obj.optionalObject("options")),
            expected = obj.optionalString("expected"),
            skipReason = obj.optionalString("skipReason"),
        )

    private fun parseDecodeOptions(obj: JsonObject?): WptDecodeFixtureOptions =
        WptDecodeFixtureOptions(
            charset = obj?.optionalString("charset"),
            charsetSentinel = obj?.optionalBoolean("charsetSentinel") ?: false,
            duplicates = obj?.optionalString("duplicates"),
            ignoreQueryPrefix = obj?.optionalBoolean("ignoreQueryPrefix") ?: false,
            strictNullHandling = obj?.optionalBoolean("strictNullHandling") ?: false,
        )

    private fun parseEncodeOptions(obj: JsonObject?): WptEncodeFixtureOptions =
        WptEncodeFixtureOptions(
            charset = obj?.optionalString("charset"),
            format = obj?.optionalString("format"),
            listFormat = obj?.optionalString("listFormat"),
            strictNullHandling = obj?.optionalBoolean("strictNullHandling") ?: false,
        )

    private fun jsonElementToValue(element: JsonElement): Any? =
        when (element) {
            JsonNull -> null
            is JsonPrimitive ->
                if (element.isString) {
                    element.content
                } else {
                    element.booleanOrNull
                        ?: element.longOrNull
                        ?: element.doubleOrNull
                        ?: element.content
                }

            is JsonArray -> element.map(::jsonElementToValue)
            is JsonObject ->
                LinkedHashMap<String, Any?>(element.size).apply {
                    element.forEach { (key, value) -> put(key, jsonElementToValue(value)) }
                }
        }
}

private fun parseCharset(name: String?): Charset =
    when (name?.uppercase()) {
        null,
        "UTF-8",
        "UTF_8" -> StandardCharsets.UTF_8
        "ISO-8859-1",
        "ISO_8859_1",
        "LATIN1",
        "LATIN_1" -> StandardCharsets.ISO_8859_1
        else -> error("Unsupported fixture charset: $name")
    }

private fun JsonObject.requireArray(name: String): JsonArray =
    this[name]?.jsonArray ?: error("Missing array field '$name' in WPT fixture resource.")

private fun JsonObject.requireObject(name: String): JsonObject =
    this[name]?.jsonObject ?: error("Missing object field '$name' in WPT fixture resource.")

private fun JsonObject.requireString(name: String): String =
    this[name]?.jsonPrimitive?.content
        ?: error("Missing string field '$name' in WPT fixture resource.")

private fun JsonObject.optionalObject(name: String): JsonObject? = this[name]?.jsonObject

private fun JsonObject.optionalString(name: String): String? =
    this[name]?.jsonPrimitive?.contentOrNull

private fun JsonObject.optionalBoolean(name: String): Boolean? =
    this[name]?.jsonPrimitive?.booleanOrNull
