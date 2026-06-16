package io.github.techouse.qskotlin.spring.web

import io.github.techouse.qskotlin.encode
import io.github.techouse.qskotlin.models.EncodeOptions
import org.springframework.web.util.UriComponentsBuilder

/**
 * Adds qs-kotlin encoded query parameters to this Spring Web URI components builder.
 *
 * qs-kotlin already percent-encodes generated names and values. Callers must finish URI
 * construction with `build(true).toUri()` so Spring treats the appended query components as already
 * encoded. Calling `build().toUri()` or `encode().build()` is unsafe for this helper because Spring
 * will encode `%` again, turning bracket notation such as `%5B` into `%255B`.
 *
 * This helper requires [EncodeOptions.encode] to remain `true`. Raw unencoded qs output cannot be
 * safely combined with the required Spring `build(true)` path.
 */
@JvmOverloads
fun UriComponentsBuilder.queryQs(
    value: Any?,
    options: EncodeOptions = EncodeOptions(),
): UriComponentsBuilder {
    if (value == null) return this

    require(options.encode) {
        "UriComponentsBuilder.queryQs requires EncodeOptions.encode to be true; finish URI construction with build(true).toUri()."
    }

    val query: String = encode(value, options)
    if (query.isEmpty()) return this

    val queryWithoutPrefix: String = if (options.addQueryPrefix) query.removePrefix("?") else query
    if (queryWithoutPrefix.isEmpty()) return this

    for (pair: String in options.delimiter.split(queryWithoutPrefix)) {
        if (pair.isEmpty()) continue

        val separatorIndex: Int = pair.indexOf('=')
        if (separatorIndex < 0) {
            queryParam(pair)
        } else {
            queryParam(pair.substring(0, separatorIndex), pair.substring(separatorIndex + 1))
        }
    }

    return this
}
