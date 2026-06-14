package io.github.techouse.qskotlin.okhttp

import io.github.techouse.qskotlin.encode
import io.github.techouse.qskotlin.models.EncodeOptions
import okhttp3.HttpUrl

/**
 * Adds qs-kotlin encoded query parameters to this OkHttp URL builder.
 *
 * qs-kotlin already percent-encodes generated names and values, so this uses OkHttp's encoded
 * query-parameter API to avoid double-encoding bracket notation and percent escapes.
 */
@JvmOverloads
fun HttpUrl.Builder.addQsQueryParameters(
    value: Any?,
    options: EncodeOptions = EncodeOptions(),
): HttpUrl.Builder {
    if (value == null) return this

    val query: String = encode(value, options)
    if (query.isEmpty()) return this

    val queryWithoutPrefix: String = if (options.addQueryPrefix) query.removePrefix("?") else query
    if (queryWithoutPrefix.isEmpty()) return this

    for (pair: String in options.delimiter.split(queryWithoutPrefix)) {
        if (pair.isEmpty()) continue

        val separatorIndex: Int = pair.indexOf('=')
        if (separatorIndex < 0) {
            addEncodedQueryParameter(pair, null)
        } else {
            addEncodedQueryParameter(
                pair.substring(0, separatorIndex),
                pair.substring(separatorIndex + 1),
            )
        }
    }

    return this
}

/**
 * Returns a new URL with qs-kotlin encoded query parameters appended.
 *
 * The original immutable [HttpUrl] is left unchanged.
 */
@JvmOverloads
fun HttpUrl.addQsQueryParameters(value: Any?, options: EncodeOptions = EncodeOptions()): HttpUrl =
    newBuilder().addQsQueryParameters(value, options).build()
