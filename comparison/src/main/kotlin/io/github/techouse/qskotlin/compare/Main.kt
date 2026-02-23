package io.github.techouse.qskotlin.compare

import io.github.techouse.qskotlin.decode
import io.github.techouse.qskotlin.encode
import kotlinx.serialization.json.*
import java.io.InputStream

fun main(args: Array<String>) {
    if (args.firstOrNull() in setOf("perf", "--perf")) {
        runPerfSnapshot()
        return
    }

    val json = Json { ignoreUnknownKeys = true }
    val cases = json.parseToJsonElement(resourceText("/comparison/test_cases.json")).jsonArray

    val percentEncodeBrackets = true // set to false if your encode() already escapes [ ]

    cases.forEach { c ->
        val obj = c.jsonObject
        val encodedIn = obj["encoded"]!!.jsonPrimitive.content
        val dataIn = fromJsonValue(obj["data"]!!)

        var encodedOut = encode(dataIn)
        if (percentEncodeBrackets) {
            encodedOut = encodedOut.replace("[", "%5B").replace("]", "%5D")
        }
        println("Encoded: $encodedOut")

        val decodedOut = decode(encodedIn)
        val decodedJson = canonJson(decodedOut)
        println("Decoded: $decodedJson")
    }
}

private fun resourceStream(path: String): InputStream =
    object {}.javaClass.getResourceAsStream(path) ?: error("Resource not found on classpath: $path")

private fun resourceText(path: String): String =
    resourceStream(path).bufferedReader().use { it.readText() }

private fun fromJsonValue(e: JsonElement): Any? =
    when (e) {
        is JsonNull -> null
        is JsonPrimitive ->
            if (e.isString) e.content
            else e.booleanOrNull ?: e.longOrNull ?: e.doubleOrNull ?: e.content
        is JsonArray -> e.map { fromJsonValue(it) }
        is JsonObject -> e.entries.associate { it.key to fromJsonValue(it.value) }
    }

private fun canonJson(v: Any?): JsonElement =
    when (v) {
        null -> JsonNull
        is String -> JsonPrimitive(v)
        is Boolean -> JsonPrimitive(v)
        is Number -> JsonPrimitive(v.toString())
        is Map<*, *> -> {
            val ordered = LinkedHashMap<String, JsonElement>(v.size)
            v.forEach { (k, value) -> ordered[k?.toString() ?: "null"] = canonJson(value) }
            JsonObject(ordered)
        }
        is Iterable<*> -> JsonArray(v.map { canonJson(it) })
        else -> JsonPrimitive(v.toString())
    }
