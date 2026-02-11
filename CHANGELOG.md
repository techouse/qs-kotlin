## 1.5.0-wip

* [FIX] ignore empty query segments before applying `parameterLimit` so delimiters do not consume the limit budget
* [FIX] skip empty keys during decode to match `qs`
* [FIX] enforce comma list limits with truncation or throwing, including duplicate key accumulation
* [FIX] correct UTF-16 surrogate encoding and prevent segment-boundary splits in `Utils.encode`
* [FIX] decode `ByteArray`/`ByteBuffer` values via charset even when `encode=false`
* [FIX] ensure `FunctionFilter` results still pass through date serialization and COMMA temporal normalization
* [FIX] replace undefined holes during list merges and normalize when `parseLists=false`
* [FIX] detect cycles introduced by filters during encoding
* [FIX] append scalars to overflow maps during merge to preserve list-limit semantics
* [FIX] preserve overflow indices/maxIndex when merging `OverflowMap` into `null` targets
* [FIX] skip `Undefined` values when appending iterables into `OverflowMap` via `combine`
* [FIX] preserve overflow semantics in merge/combine (overflow sources, negative `listLimit`)
* [FIX] COMMA list encoding honors ByteArray/ByteBuffer decoding when `encode=false`
* [CHORE] refactor encode/merge internals to stack-based traversal for deep-nesting safety
* [CHORE] expand tests for empty segments, comma limits, surrogates, byte buffers, filter date normalization, and overflow edge cases

## 1.4.4

* [CHORE] update Kotlin to 2.3.10

## 1.4.3

* [CHORE] update Android Gradle Plugin to 9.0.0

## 1.4.2

* [FIX] implement `DecodeOptions.ListLimit` handling in `Utils.combine` function to prevent DoS via memory exhaustion

## 1.4.1

* [CHORE] update Kotlin to 2.3.0

## 1.4.0

* [FEAT] add `EncodeOptions.commaCompactNulls` to drop `null` values when using comma list format

## 1.3.4

* [CHORE] update Kotlin to 2.2.21

## 1.3.3

* [CHORE] update Android Gradle Plugin to 8.13.0

## 1.3.2

* [CHORE] update Kotlin to 2.2.20
* [CHORE] update Android Gradle Plugin to 8.12.3
* [CHORE] migrate to Gradle version catalog for dependency management

## 1.3.1

* [CHORE] update Android compileSdk to 36

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
