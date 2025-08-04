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
