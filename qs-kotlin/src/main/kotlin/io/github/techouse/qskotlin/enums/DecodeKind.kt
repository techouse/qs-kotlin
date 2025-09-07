package io.github.techouse.qskotlin.enums

/**
 * Decoding context for a scalar token.
 * - [KEY]: the token is a key or key segment. Callers may want to preserve percent-encoded dots
 *   (%2E / %2e) until after key-splitting.
 * - [VALUE]: the token is a value; typically fully percent-decode.
 */
enum class DecodeKind {
    KEY,
    VALUE,
}
