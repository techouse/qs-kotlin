## 1.1.3

* [FIX] ensure consistent string key usage for maps and correct numeric key handling in `Decoder`

## 1.1.2

* [FIX] change `HashMap` to `LinkedHashMap` in `Decoder.parseObject` for consistent iteration order
* [CHORE] update Android Gradle Plugin to v8.11.1
* [CHORE] add comparison tests with Node.js `qs` library to ensure compatibility with the original library

## 1.1.1

* [FEAT] add support for sparse Lists in `DecodeOptions` - if set to `true`, the lists will contain `null` values for missing values
* [FIX] fix parsing of negative integer indices in query strings by replacing `decodedRoot.all(Char::isDigit)` with `decodedRoot.toIntOrNull() != null` to properly handle negative numbers
* [FIX] fix merge semantics for `Map` targets: ignore `null`/`Undefined` sources and treat scalar sources as flag keys (`map[k] = true`) instead of merging—prevents accidental `"Undefined"` keys and duplicate `[true, true]` entries
* [CHORE] add comprehensive unit tests for query string encoding and decoding ported from https://github.com/atek-software/qsparser

## 1.1.0

* [CHORE] replace `QS` with top-level `encode` and `decode` functions for improved clarity
* [CHORE] refactor to use internal `Utils`, `Encoder` and `Decoder` for improved structure

## 1.0.4

* [FEAT] add `@JvmOverloads` annotation to decoder and encoder functions for improved Java interoperability

## 1.0.3

* [FEAT] add `@JvmStatic` to `QS.encode` and `QS.decode` methods to enable Java-friendly static calls

## 1.0.2

* [FIX] replace `URLDecoder.decode(String, Charset)` (API 33+) with `URLDecoder.decode(String, String)` to restore Android compatibility (minSdk 25)
* [CHORE] improve code readability by simplifying conditional expressions and comments

## 1.0.1

* [CHORE] Update Kotlin version to 2.0.21.

## 1.0.0

* [CHORE] Initial release of the project.
