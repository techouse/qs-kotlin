## 1.3.0

* [BREAKING] `EncodeOptions.delimiter` now expects a `StringDelimiter` instead of a `String` for improved type safety
* [FEAT] add Java-friendly functional interfaces and factories for encoding, decoding, and filtering

## 1.2.3

* [CHORE] improve build reproducibility and enhance publication metadata

## 1.2.2

* [FIX] handle unterminated group when stashing remainder in key segmentation
* [CHORE] add tests for `splitKeyIntoSegments` remainder wrapping and `strictDepth` behavior

## 1.2.1

* [FIX] fix key decoding to treat dots consistently with values and update `DecodeOptions.decodeKey`/`DecodeOptions.decodeValue` visibility
* [FIX] handle encoded dots and nested brackets in key parsing for dot notation
* [FIX] remove unused `protectEncodedDotsForKeys` utility from `DecodeOptions`
* [CHORE] clarify `decodeDotInKeys` documentation and improve `DecodeOptions.decodeKey`/`DecodeOptions.decodeValue` convenience methods with default charset
* [CHORE] refactor `defaultDecode` signature to remove unused `DecodeKind` parameter in key decoding
* [CHORE]️ refactor dot-to-bracket conversion and key splitting to improve handling of top-level dots and bracket segments
* [CHORE] suppress deprecation warnings in `DecodeOptionsSpec`
* [CHORE] clarify documentation for encoded dot handling in key decoding and parser logic
* [CHORE] expand tests for key decoding with encoded dots and custom decoder behavior
* [CHORE] add comprehensive tests for encoded dot behavior in keys to ensure C# qs port (`QsNet`) parity and edge case coverage
* [CHORE] update `DecodeOptionsSpec` to use public decode method instead of `callDefaultDecode` reflection helper
* [CHORE] expand `DecodeSpec` coverage for encoded dot behavior in keys and C# qs port (`QsNet`) parity scenarios

## 1.2.0

* [FEAT] add `DecodeKind` enum to distinguish decoding context for keys and values
* [FEAT] add `LegacyDecoder` typealias and deprecate legacy decoder support in `DecodeOptions` for backward compatibility
* [FIX] protect encoded dots in key decoding to prevent premature conversion to '.' and ensure correct parsing
* [FIX] handle lowercase '%2e' in key decoding and improve bracketed key parsing for accurate dot conversion
* [FIX] fix key segment handling for depth 0 to preserve original key with encoded dots
* [FIX] optimize `protectEncodedDotsForKeys` to skip processing when no encoded dots are present; update deprecation message for `getDecoder` to clarify removal timeline
* [FIX] replace regex-based dot-to-bracket conversion with top-level parser to correctly handle encoded dots in key segments
* [FIX] fix `allowDots` logic to ensure `decodeDotInKeys` requires `allowDots` not explicitly false
* [CHORE] update deprecation annotation for indices option in `EncodeOptions` with message, replacement, and level
* [CHORE] add tests for key coercion and `depth=0` behavior with `allowDots` in `decode`
* [CHORE] update decoder tests to handle `DecodeKind` for selective key/value decoding
* [CHORE] remove explicit `Decoder` type annotations in custom decoder test cases for improved readability
* [CHORE] add tests for `defaultDecode` to verify encoded dot handling in keys with `allowDots` and `decodeDotInKeys` options
* [CHORE] clarify deprecation message for legacy decoder adapter and document bracket handling in `protectEncodedDotsForKeys`
* [CHORE] reformat deprecation and documentation comments for improved readability in `DecodeOptions`
* [CHORE] add comprehensive tests for encoded dot handling in keys with `allowDots` and `decodeDotInKeys` options
* [CHORE]️ deprecate `getDecoder` in favor of context-aware decode methods for value decoding
* [CHORE] update `Decoder` interface documentation to use code formatting for parameter names
* [CHORE] rename local variable for custom decoder in encoding test for clarity
* [CHORE] add tests for dot-to-bracket conversion guardrails in decode with `allowDots` option

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
