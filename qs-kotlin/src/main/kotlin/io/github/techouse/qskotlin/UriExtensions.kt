package io.github.techouse.qskotlin

import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.EncodeOptions
import java.net.URI
import java.net.URLDecoder

/** Extension functions for URI to provide QS (Query String) functionality */

/**
 * The URI query split into a map. Providing custom [options] will override the default behavior.
 */
@JvmOverloads
fun URI.queryParametersQs(options: DecodeOptions? = null): Map<String, Any?> =
    if (!rawQuery.isNullOrEmpty()) QS.decode(rawQuery, options) else emptyMap()

/**
 * The normalized string representation of the URI. Providing custom [options] will override the
 * default behavior.
 */
@JvmOverloads
fun URI.toStringQs(
    options: EncodeOptions =
        EncodeOptions(listFormat = ListFormat.REPEAT, skipNulls = false, strictNullHandling = false)
): String {
    // Get all query parameters (including duplicates)
    val allQueryParams = getAllQueryParameters()

    val newQuery =
        if (allQueryParams.isNotEmpty()) {
            QS.encode(allQueryParams, options)
        } else {
            null
        }

    return buildUriString(newQuery)
}

/** Helper function to get all query parameters including duplicates */
private fun URI.getAllQueryParameters(): Map<String, Any?> {
    if (rawQuery.isNullOrEmpty()) return emptyMap()

    val params = mutableMapOf<String, MutableList<String>>()

    rawQuery.split("&").forEach { param ->
        val keyValue = param.split("=", limit = 2)
        if (keyValue.isNotEmpty()) {
            val rawKey = keyValue[0]
            val rawValue = if (keyValue.size > 1) keyValue[1] else ""

            // Decode keys and values more selectively
            val key =
                if (rawKey.contains("%")) {
                    URLDecoder.decode(rawKey, "UTF-8")
                } else {
                    rawKey
                }

            // For values, only decode URL-encoded sequences, preserve other characters
            val value =
                if (rawValue.contains("%")) {
                    // Replace only URL-encoded sequences, leave other characters as-is
                    rawValue.replace(Regex("%[0-9A-Fa-f]{2}")) { matchResult ->
                        val hex = matchResult.value.substring(1)
                        val byte = hex.toInt(16).toByte()
                        String(byteArrayOf(byte), Charsets.UTF_8)
                    }
                } else {
                    rawValue
                }

            params.getOrPut(key) { mutableListOf() }.add(value)
        }
    }

    return params.mapValues { (_, values) -> if (values.size == 1) values.first() else values }
}

/** Helper function to build URI string with new query */
private fun URI.buildUriString(newQuery: String?): String {
    val sb = StringBuilder()

    // Add scheme
    scheme?.let { sb.append("$it:") }

    // Add authority (user info, host, port)
    if (authority != null) {
        sb.append("//")
        userInfo?.let { sb.append("$it@") }
        host?.let { sb.append(it) }
        if (port != -1) {
            sb.append(":$port")
        }
    }

    // Add path
    path?.let { sb.append(it) }

    // Add query
    if (!newQuery.isNullOrEmpty()) {
        sb.append("?$newQuery")
    }

    // Add fragment
    fragment?.let { sb.append("#$it") }

    return sb.toString()
}
