@file:JvmName("QS")
@file:JvmMultifileClass

package io.github.techouse.qskotlin

import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.EncodeOptions
import java.net.URI

/**
 * Decode a query [String] into a [Map<String, Any?>].
 *
 * @param options [DecodeOptions] optional decoder settings
 * @return [Map<String, Any?>] the decoded Map
 */
@JvmOverloads
fun String.toQueryMap(options: DecodeOptions? = null): Map<String, Any?> = decode(this, options)

/**
 * Encode a [Map] into a query string.
 *
 * @param options [EncodeOptions] optional encoder settings
 * @return [String] the encoded query string
 */
@JvmOverloads
fun Map<*, *>.toQueryString(options: EncodeOptions? = null): String = encode(this, options)

/**
 * Decode this [URI]'s raw query component into a [Map<String, Any?>].
 *
 * This intentionally uses [URI.getRawQuery] instead of [URI.getQuery] so qs receives the original
 * percent escapes. An absent or explicitly empty query, and an opaque URI without a query
 * component, returns an empty Map.
 *
 * @param options [DecodeOptions] optional decoder settings
 * @return [Map<String, Any?>] the decoded Map
 */
@JvmOverloads
fun URI.decodeQsQuery(options: DecodeOptions? = null): Map<String, Any?> =
    rawQuery?.toQueryMap(options) ?: emptyMap()
