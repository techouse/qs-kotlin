@file:JvmName("QS")
@file:JvmMultifileClass

package io.github.techouse.qskotlin

import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.EncodeOptions

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
