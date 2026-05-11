---
name: qs-kotlin
description: Use this skill whenever a user wants to install, configure, troubleshoot, or write Kotlin, Java, JVM, or Android application code for encoding and decoding nested query strings with the qs-kotlin package. This skill helps produce practical QS.decode, QS.encode, decode, encode, toQueryMap, and toQueryString snippets, choose DecodeOptions and EncodeOptions, explain option tradeoffs, and avoid qs-kotlin edge-case pitfalls around lists, dot notation, duplicates, null handling, charset sentinels, depth limits, Java interop, Android coordinates, and untrusted input.
---

# qs-kotlin Usage Assistant

Help users parse and build query strings with the Kotlin/JVM `qs-kotlin`
package and the Android `qs-kotlin-android` wrapper.
Focus on user application code and interoperability outcomes, not repository
maintenance.

## Start With Inputs

Before producing a final snippet, collect only the missing details that change
the code:

- Runtime: Kotlin/JVM, Android, Java, tests, backend code, or generated example.
- Direction: decode an incoming query string, encode Kotlin or Java data, or
  normalize query-string handling around an existing URL/request object.
- The actual query string or data structure when available.
- Target API convention for lists: indexed brackets, empty brackets, repeated
  keys, or comma-separated values.
- Whether the query may include a leading `?`, dot notation, literal dots in
  keys, duplicate keys, custom delimiters, comma-separated lists, `null` flags,
  ISO-8859-1/legacy charset behavior, Android AAR requirements, or untrusted
  user input.

Do not over-ask when the desired behavior is obvious. State assumptions in the
answer and give the user a concrete snippet they can paste.

## Installation

Use the core JVM artifact for normal Kotlin/JVM and most Android projects:

```kotlin
dependencies {
    implementation("io.github.techouse:qs-kotlin:<version>")
}
```

Use the Android AAR wrapper when the project specifically wants an AAR
coordinate and Android Gradle Plugin metadata:

```kotlin
dependencies {
    implementation("io.github.techouse:qs-kotlin-android:<version>")
}
```

The library targets Java 17. Android apps with `minSdk < 26` that use
`java.time` transitively may need core library desugaring in the app module.

## Public API

Prefer package-level functions in Kotlin:

```kotlin
import io.github.techouse.qskotlin.decode
import io.github.techouse.qskotlin.encode
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.EncodeOptions
```

For Java, use the generated `QS` facade and Java-friendly builders:

```java
import io.github.techouse.qskotlin.QS;
import io.github.techouse.qskotlin.models.DecodeOptions;
import io.github.techouse.qskotlin.models.EncodeOptions;
```

Kotlin extension helpers are also available:

```kotlin
import io.github.techouse.qskotlin.toQueryMap
import io.github.techouse.qskotlin.toQueryString

val params = "a[b]=c".toQueryMap()
val query = mapOf("a" to mapOf("b" to "c")).toQueryString()
```

## Base Patterns

Decode a query string into nested Kotlin values:

```kotlin
import io.github.techouse.qskotlin.decode

val params =
    decode("a[b][c]=d&tags[]=kotlin&tags[]=android")

check(
    params ==
        mapOf(
            "a" to mapOf("b" to mapOf("c" to "d")),
            "tags" to listOf("kotlin", "android"),
        )
)
```

Encode nested Kotlin values into a query string:

```kotlin
import io.github.techouse.qskotlin.encode

val query =
    encode(
        mapOf(
            "a" to mapOf("b" to mapOf("c" to "d")),
            "tags" to listOf("kotlin", "android"),
        )
    )

check(query == "a%5Bb%5D%5Bc%5D=d&tags%5B0%5D=kotlin&tags%5B1%5D=android")
```

Java callers should prefer builders for options:

```java
import io.github.techouse.qskotlin.QS;
import io.github.techouse.qskotlin.enums.ListFormat;
import io.github.techouse.qskotlin.models.EncodeOptions;
import java.util.List;
import java.util.Map;

String query =
    QS.encode(
        Map.of("tags", List.of("kotlin", "android")),
        EncodeOptions.builder().listFormat(ListFormat.REPEAT).build());

assert query.equals("tags=kotlin&tags=android");
```

## Decode Recipes

Use these options with `decode(query, DecodeOptions(...))` in Kotlin or
`QS.decode(query, DecodeOptions.builder()...build())` in Java:

- Leading question mark: `ignoreQueryPrefix = true`.
- Dot notation such as `a.b=c`: `allowDots = true`.
- Double-encoded literal dots in keys such as `name%252Eobj.first=John`:
  `decodeDotInKeys = true`.
- Duplicate keys: `duplicates = Duplicates.COMBINE` keeps all values as a list;
  use `Duplicates.FIRST` or `Duplicates.LAST` to collapse.
- Bracket lists: enabled by default; set `parseLists = false` to treat list
  syntax as map keys.
- Empty list tokens such as `foo[]`: `allowEmptyLists = true`.
- Large or sparse list indices: default `listLimit` is `20`; indices above the
  limit become map keys. A negative `listLimit` disables numeric-index list
  parsing.
- Comma-separated values such as `a=b,c`: `comma = true`.
- Tokens without `=` as `null`: `strictNullHandling = true`.
- Custom delimiters: `delimiter = Delimiter.SEMICOLON`,
  `delimiter = StringDelimiter(";")`, or `delimiter = RegexDelimiter("[;,]")`.
- Legacy charset input: `charset = StandardCharsets.ISO_8859_1`; use
  `charsetSentinel = true` when a form may include `utf8=...` to signal the
  real charset.
- HTML numeric entities: `interpretNumericEntities = true`, usually with
  ISO-8859-1 or charset sentinel handling.
- Untrusted input: keep `depth`, `parameterLimit`, and `listLimit` bounded; use
  `strictDepth = true` and `throwOnLimitExceeded = true` when callers need hard
  failures instead of soft limiting.

Example for a request query:

```kotlin
import io.github.techouse.qskotlin.decode
import io.github.techouse.qskotlin.enums.Duplicates
import io.github.techouse.qskotlin.models.DecodeOptions

val params =
    decode(
        "?filter.status=open&tag=kotlin&tag=android",
        DecodeOptions(
            ignoreQueryPrefix = true,
            allowDots = true,
            duplicates = Duplicates.COMBINE,
        ),
    )

check(params == mapOf("filter" to mapOf("status" to "open"), "tag" to listOf("kotlin", "android")))
```

## Encode Recipes

Use these options with `encode(data, EncodeOptions(...))` in Kotlin or
`QS.encode(data, EncodeOptions.builder()...build())` in Java:

- List style defaults to `ListFormat.INDICES`:
  `tags%5B0%5D=kotlin&tags%5B1%5D=android`.
- Empty brackets: `listFormat = ListFormat.BRACKETS`.
- Repeated keys: `listFormat = ListFormat.REPEAT`.
- Comma-separated values: `listFormat = ListFormat.COMMA`.
- Single-item comma lists that must round-trip as lists:
  `commaRoundTrip = true`.
- Drop `null` items before comma-joining lists: `commaCompactNulls = true`.
- Dot notation for nested maps: `allowDots = true`.
- Literal dots in keys: `encodeDotInKeys = true`; set `allowDots = true` when
  nested paths should use dot notation.
- Add a leading `?`: `addQueryPrefix = true`.
- Custom pair delimiter: `delimiter = Delimiter.SEMICOLON` or
  `delimiter = StringDelimiter(";")`.
- Preserve readable bracket/dot keys while encoding values:
  `encodeValuesOnly = true`.
- Disable percent encoding entirely for debugging or documented examples:
  `encode = false`.
- Emit `null` without `=`: `strictNullHandling = true`.
- Omit `null` keys: `skipNulls = true`.
- Emit empty lists as `foo[]`: `allowEmptyLists = true`.
- Omit selected values: use `skipNulls = true` for `null` values, return `null`
  from a `FunctionFilter`, or remove those entries before calling `encode`.
- Legacy form spaces as `+`: `format = Format.RFC1738`; the default is
  `Format.RFC3986`, which emits spaces as `%20`.
- Legacy charset output: `charset = StandardCharsets.ISO_8859_1`; use
  `charsetSentinel = true` to prepend the `utf8=...` sentinel.
- Custom behavior: use `encoder`, `dateSerializer`, `sort`, or `filter` when
  the target API needs special scalar encoding, date formatting, stable key
  order, or selected fields.

Example for an API that expects repeated keys:

```kotlin
import io.github.techouse.qskotlin.encode
import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.models.EncodeOptions

val query =
    encode(
        mapOf(
            "q" to "query strings",
            "tag" to listOf("kotlin", "android"),
        ),
        EncodeOptions(
            listFormat = ListFormat.REPEAT,
            addQueryPrefix = true,
        ),
    )

check(query == "?q=query%20strings&tag=kotlin&tag=android")
```

## Java Interop

Use builders from Java to avoid long Kotlin data-class constructors:

```java
import io.github.techouse.qskotlin.QS;
import io.github.techouse.qskotlin.enums.Duplicates;
import io.github.techouse.qskotlin.models.DecodeOptions;
import java.util.List;
import java.util.Map;

Map<String, Object> params =
    QS.decode(
        "?tag=kotlin&tag=android",
        DecodeOptions.builder()
            .ignoreQueryPrefix(true)
            .duplicates(Duplicates.COMBINE)
            .build());

assert params.equals(Map.of("tag", List.of("kotlin", "android")));
```

Use Java-friendly functional interfaces for custom callbacks:

- `JDecoder` for decode customization.
- `JValueEncoder` for encode customization.
- `JDateSerializer` for `LocalDateTime` serialization.
- Java `Comparator` instances through `EncodeOptions.builder().sort(...)`.

## Combinations To Check

Warn or adjust before giving code for these cases:

- `DecodeOptions(decodeDotInKeys = true, allowDots = false)` is invalid.
- `parameterLimit` must be positive, or `Int.MAX_VALUE` for effectively
  unlimited parsing.
- `throwOnLimitExceeded = true` turns parameter and list limit violations into
  `IndexOutOfBoundsException`; without it, parsing truncates or falls back.
- `strictDepth = true` throws on well-formed depth overflow; with the default
  `false`, the remainder beyond `depth` is kept as a trailing key segment.
- Built-in charset handling supports only `StandardCharsets.UTF_8` and
  `StandardCharsets.ISO_8859_1`; other encodings require a custom `encoder` or
  `decoder`.
- `EncodeOptions.encoder` is ignored when `encode = false`.
- Combining `encodeValuesOnly = true` and `encodeDotInKeys = true` encodes only
  dots in keys; values remain otherwise unchanged.
- `DecodeOptions.comma` parses simple comma-separated values, but does not
  decode nested map syntax such as `a={b:1},{c:d}`.
- `encode(null)`, scalar roots, empty maps, and empty containers generally
  produce an empty string.
- The JDK and many web frameworks flatten duplicates or nested query syntax.
  Prefer `decode` or `QS.decode` on the raw query string when qs-style nested or
  repeated values matter.

## Response Shape

For code-generation requests, answer with:

1. A short statement of assumptions, especially language, artifact coordinate,
   list format, null handling, charset, prefix handling, and whether input is
   trusted.
2. One concrete Kotlin or Java snippet using `decode`, `encode`, `toQueryMap`,
   `toQueryString`, or `QS`.
3. A brief explanation of only the options used.
4. A small verification example, such as an expected map, expected query string,
   JUnit assertion, Kotest assertion, or `check(...)`.

Keep snippets application-oriented. Prefer public API imports from
`io.github.techouse.qskotlin`; do not ask users to import from
`io.github.techouse.qskotlin.internal`.
