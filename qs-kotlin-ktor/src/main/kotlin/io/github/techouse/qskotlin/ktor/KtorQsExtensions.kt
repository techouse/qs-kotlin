package io.github.techouse.qskotlin.ktor

import io.github.techouse.qskotlin.decode
import io.github.techouse.qskotlin.encode
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.EncodeOptions
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.queryString

/**
 * Appends qs-kotlin encoded query parameters to this Ktor URL builder.
 *
 * qs-kotlin already percent-encodes generated names and values, so this uses Ktor's encoded
 * query-parameter API to avoid double-encoding bracket notation and percent escapes.
 */
@JvmOverloads
fun URLBuilder.appendQsQueryParameters(
    value: Any?,
    options: EncodeOptions = EncodeOptions(),
): URLBuilder {
    if (value == null) return this

    val query: String = encode(value, options)
    if (query.isEmpty()) return this

    val queryWithoutPrefix: String = if (options.addQueryPrefix) query.removePrefix("?") else query
    if (queryWithoutPrefix.isEmpty()) return this

    for (pair: String in options.delimiter.split(queryWithoutPrefix)) {
        if (pair.isEmpty()) continue

        val separatorIndex: Int = pair.indexOf('=')
        if (separatorIndex < 0) {
            encodedParameters.appendAll(pair, emptyList())
        } else {
            encodedParameters.append(
                pair.substring(0, separatorIndex),
                pair.substring(separatorIndex + 1),
            )
        }
    }

    return this
}

/**
 * Returns a new Ktor URL with qs-kotlin encoded query parameters appended.
 *
 * The original immutable [Url] is left unchanged.
 */
@JvmOverloads
fun Url.appendQsQueryParameters(value: Any?, options: EncodeOptions = EncodeOptions()): Url =
    URLBuilder(this).appendQsQueryParameters(value, options).build()

/**
 * Parses this request's raw query string with qs-kotlin.
 *
 * This intentionally reads `ApplicationRequest.queryString()` instead of Ktor's decoded query
 * parameter collections so qs-kotlin receives the original bracket notation and percent escapes.
 */
@JvmOverloads
fun ApplicationRequest.parseQsQuery(options: DecodeOptions = DecodeOptions()): Map<String, Any?> =
    decode(queryString(), options)
