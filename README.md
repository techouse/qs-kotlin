# qs-kotlin

<p align="center">
    <img src="logo.png?raw=true" width="256" alt="RxDart" />
</p>

A query string encoding and decoding library for Android and Kotlin/JVM.

Ported from [qs](https://www.npmjs.com/package/qs) for JavaScript.

[![Kotlin 2.0.21](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/whatsnew2020.html)
[![JVM 17](https://img.shields.io/badge/JVM-17-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.techouse/qs-kotlin)](https://central.sonatype.com/artifact/io.github.techouse/qs-kotlin)
[![Android AAR](https://img.shields.io/badge/Android-AAR-3DDC84?logo=android&logoColor=white)](https://central.sonatype.com/artifact/io.github.techouse/qs-kotlin-android)
[![Test](https://github.com/techouse/qs-kotlin/actions/workflows/test.yml/badge.svg)](https://github.com/techouse/qs-kotlin/actions/workflows/test.yml)
[![codecov](https://codecov.io/gh/techouse/qs-kotlin/graph/badge.svg?token=ClCDNcsxqQ)](https://codecov.io/gh/techouse/qs-kotlin)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/4e9ab7e1fe40412bb3f7709a7d3fff23)](https://app.codacy.com/gh/techouse/qs-kotlin/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![GitHub](https://img.shields.io/github/license/techouse/qs-kotlin)](LICENSE)
[![GitHub Sponsors](https://img.shields.io/github/sponsors/techouse)](https://github.com/sponsors/techouse)
[![GitHub Repo stars](https://img.shields.io/github/stars/techouse/qs-kotlin)](https://github.com/techouse/qs-kotlin/stargazers)

This repo provides:

- **`qs-kotlin`** – the core JVM library (Jar)
- **`qs-kotlin-android`** – a thin Android AAR wrapper that re-exports the same API

> If you only target the JVM (including Android projects that are fine with a plain Jar), just use `qs-kotlin`. The Android wrapper is provided for teams that prefer an AAR coordinate and AGP metadata.

---

## Highlights

- Nested maps and lists: `foo[bar][baz]=qux` ⇄ `{ foo: { bar: { baz: "qux" } } }`
- Multiple list formats (indices, brackets, repeat, comma)
- Dot-notation support (`a.b=c`) and `"."`-encoding toggles
- UTF-8 and ISO-8859-1 charsets, plus optional charset sentinel (`utf8=✓`)
- Custom encoders/decoders, key sorting, filtering, and strict null handling
- Supports `LocalDateTime`/`Instant` serialization via a pluggable serializer
- Extensive tests (Kotest), performance-minded implementation

---

## Installation

### JVM (Jar)

```kotlin
dependencies {
    implementation("io.github.techouse:qs-kotlin:<version>")
}
```

### Android (AAR wrapper)

```kotlin
dependencies {
    implementation("io.github.techouse:qs-kotlin-android:<version>")
}
```

> The Android AAR depends on Java 17 APIs. If your app’s `minSdk < 26` and you use `java.time` transitively, enable **core library desugaring** in your app:

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
```

---

## Requirements

- Kotlin **2.0.20+**
- Java **17+**
- Android wrapper: AGP **8.7+**, `compileSdk 35`, `minSdk 25`

---

## Quick start

```kotlin
import io.github.techouse.qskotlin.QS

// Decode
val obj: Map<String, Any?> = QS.decode("foo[bar]=baz&foo[list][]=a&foo[list][]=b")
// -> mapOf("foo" to mapOf("bar" to "baz", "list" to listOf("a", "b")))

// Encode
val qs: String = QS.encode(mapOf("foo" to mapOf("bar" to "baz")))
// -> "foo%5Bbar%5D=baz"
```

---

## Usage

### Simple

```kotlin
// Decode
val decoded: Map<String, Any?> = QS.decode("a=c")
// => mapOf("a" to "c")

// Encode
val encoded: String = QS.encode(mapOf("a" to "c"))
// => "a=c"
```

---

## Decoding

### Nested maps

```kotlin
QS.decode("foo[bar]=baz")
// => mapOf("foo" to mapOf("bar" to "baz"))

QS.decode("a%5Bb%5D=c")
// => mapOf("a" to mapOf("b" to "c"))

QS.decode("foo[bar][baz]=foobarbaz")
// => mapOf("foo" to mapOf("bar" to mapOf("baz" to "foobarbaz")))
```

### Depth (default: 5)

Beyond the configured depth, remaining bracket content is kept as literal text:

```kotlin
QS.decode("a[b][c][d][e][f][g][h][i]=j")
// => mapOf("a" to mapOf("b" to mapOf("c" to mapOf("d" to mapOf("e" to mapOf("f" to mapOf("[g][h][i]" to "j")))))))
```

Override depth:

```kotlin
QS.decode("a[b][c][d][e][f][g][h][i]=j", DecodeOptions(depth = 1))
// => mapOf("a" to mapOf("b" to mapOf("[c][d][e][f][g][h][i]" to "j")))
```

### Parameter limit

```kotlin
QS.decode("a=b&c=d", DecodeOptions(parameterLimit = 1))
// => mapOf("a" to "b")
```

### Ignore leading `?`

```kotlin
QS.decode("?a=b&c=d", DecodeOptions(ignoreQueryPrefix = true))
// => mapOf("a" to "b", "c" to "d")
```

### Custom delimiter (string or regex)

```kotlin
QS.decode("a=b;c=d", DecodeOptions(delimiter = StringDelimiter(";")))
// => mapOf("a" to "b", "c" to "d")

QS.decode("a=b;c=d", DecodeOptions(delimiter = RegexDelimiter("[;,]")))
// => mapOf("a" to "b", "c" to "d")
```

### Dot-notation and “decode dots in keys”

```kotlin
QS.decode("a.b=c", DecodeOptions(allowDots = true))
// => mapOf("a" to mapOf("b" to "c"))

QS.decode(
  "name%252Eobj.first=John&name%252Eobj.last=Doe",
  DecodeOptions(decodeDotInKeys = true)
)
// => mapOf("name.obj" to mapOf("first" to "John", "last" to "Doe"))
```

### Empty lists

```kotlin
QS.decode("foo[]&bar=baz", DecodeOptions(allowEmptyLists = true))
// => mapOf("foo" to emptyList<String>(), "bar" to "baz")
```

### Duplicates

```kotlin
QS.decode("foo=bar&foo=baz")
// => mapOf("foo" to listOf("bar", "baz"))

QS.decode("foo=bar&foo=baz", DecodeOptions(duplicates = Duplicates.COMBINE))
// => same as above

QS.decode("foo=bar&foo=baz", DecodeOptions(duplicates = Duplicates.FIRST))
// => mapOf("foo" to "bar")

QS.decode("foo=bar&foo=baz", DecodeOptions(duplicates = Duplicates.LAST))
// => mapOf("foo" to "baz")
```

### Charset and sentinel

```kotlin
// latin1
QS.decode("a=%A7", DecodeOptions(charset = StandardCharsets.ISO_8859_1))
// => mapOf("a" to "§")

// Sentinels
QS.decode("utf8=%E2%9C%93&a=%C3%B8", DecodeOptions(charset = StandardCharsets.ISO_8859_1, charsetSentinel = true))
// => mapOf("a" to "ø")

QS.decode("utf8=%26%2310003%3B&a=%F8", DecodeOptions(charset = StandardCharsets.UTF_8, charsetSentinel = true))
// => mapOf("a" to "ø")
```

### Interpret numeric entities (`&#1234;`)

```kotlin
QS.decode(
  "a=%26%239786%3B",
  DecodeOptions(charset = StandardCharsets.ISO_8859_1, interpretNumericEntities = true)
)
// => mapOf("a" to "☺")
```

### Lists

```kotlin
QS.decode("a[]=b&a[]=c")
// => mapOf("a" to listOf("b", "c"))

QS.decode("a[1]=c&a[0]=b")
// => mapOf("a" to listOf("b", "c"))

QS.decode("a[1]=b&a[15]=c")
// => mapOf("a" to listOf("b", "c"))

QS.decode("a[]=&a[]=b")
// => mapOf("a" to listOf("", "b"))
```

Large indices convert to a map by default:

```kotlin
QS.decode("a[100]=b")
// => mapOf("a" to mapOf(100 to "b"))
```

Disable list parsing:

```kotlin
QS.decode("a[]=b", DecodeOptions(parseLists = false))
// => mapOf("a" to mapOf(0 to "b"))
```

Mixing notations merges into a map:

```kotlin
QS.decode("a[0]=b&a[b]=c")
// => mapOf("a" to mapOf(0 to "b", "b" to "c"))
```

Comma-separated values:

```kotlin
QS.decode("a=b,c", DecodeOptions(comma = true))
// => mapOf("a" to listOf("b", "c"))
```

### Primitive/scalar values

All values decode as strings by default:

```kotlin
QS.decode("a=15&b=true&c=null")
// => mapOf("a" to "15", "b" to "true", "c" to "null")
```

---

## Encoding

### Basics

```kotlin
QS.encode(mapOf("a" to "b"))
// => "a=b"

QS.encode(mapOf("a" to mapOf("b" to "c")))
// => "a%5Bb%5D=c"
```

Disable URI encoding for readability:

```kotlin
QS.encode(mapOf("a" to mapOf("b" to "c")), EncodeOptions(encode = false))
// => "a[b]=c"
```

Values-only encoding:

```kotlin
QS.encode(
  mapOf(
    "a" to "b",
    "c" to listOf("d", "e=f"),
    "f" to listOf(listOf("g"), listOf("h")),
  ),
  EncodeOptions(encodeValuesOnly = true)
)
// => "a=b&c[0]=d&c[1]=e%3Df&f[0][0]=g&f[1][0]=h"
```

Custom encoder:

```kotlin
QS.encode(
  mapOf("a" to mapOf("b" to "č")),
  EncodeOptions(encoder = { v, _, _ -> if (v == "č") "c" else v.toString() })
)
// => "a[b]=c"   (with encode=false would be unescaped)
```

### List formats

```kotlin
// default (indices)
QS.encode(mapOf("a" to listOf("b", "c")), EncodeOptions(encode = false))
// => "a[0]=b&a[1]=c"

// brackets
QS.encode(mapOf("a" to listOf("b", "c")), EncodeOptions(encode = false, listFormat = ListFormat.BRACKETS))
// => "a[]=b&a[]=c"

// repeat
QS.encode(mapOf("a" to listOf("b", "c")), EncodeOptions(encode = false, listFormat = ListFormat.REPEAT))
// => "a=b&a=c"

// comma
QS.encode(mapOf("a" to listOf("b", "c")), EncodeOptions(encode = false, listFormat = ListFormat.COMMA))
// => "a=b,c"
```

### Nested maps

```kotlin
QS.encode(
  mapOf("a" to mapOf("b" to mapOf("c" to "d", "e" to "f"))),
  EncodeOptions(encode = false)
)
// => "a[b][c]=d&a[b][e]=f"
```

Dot notation:

```kotlin
QS.encode(
  mapOf("a" to mapOf("b" to mapOf("c" to "d", "e" to "f"))),
  EncodeOptions(encode = false, allowDots = true)
)
// => "a.b.c=d&a.b.e=f"
```

Encode dots in keys:

```kotlin
QS.encode(
  mapOf("name.obj" to mapOf("first" to "John", "last" to "Doe")),
  EncodeOptions(allowDots = true, encodeDotInKeys = true)
)
// => "name%252Eobj.first=John&name%252Eobj.last=Doe"
```

Allow empty lists:

```kotlin
QS.encode(
  mapOf("foo" to emptyList<String>(), "bar" to "baz"),
  EncodeOptions(encode = false, allowEmptyLists = true)
)
// => "foo[]&bar=baz"
```

Empty strings & nulls:

```kotlin
QS.encode(mapOf("a" to ""))
// => "a="
```

Return empty string for empty containers:

```kotlin
QS.encode(mapOf("a" to emptyList<String>()))        // => ""
QS.encode(mapOf("a" to emptyMap<String, Any>()))    // => ""
QS.encode(mapOf("a" to listOf(emptyMap<String, Any>()))) // => ""
QS.encode(mapOf("a" to mapOf("b" to emptyList<String>()))) // => ""
QS.encode(mapOf("a" to mapOf("b" to emptyMap<String, Any>()))) // => ""
```

Omit `Undefined`:

```kotlin
QS.encode(mapOf("a" to null, "b" to Undefined()))
// => "a="
```

Add query prefix:

```kotlin
QS.encode(mapOf("a" to "b", "c" to "d"), EncodeOptions(addQueryPrefix = true))
// => "?a=b&c=d"
```

Custom delimiter:

```kotlin
QS.encode(mapOf("a" to "b", "c" to "d"), EncodeOptions(delimiter = ";"))
// => "a=b;c=d"
```

### Dates

By default, `LocalDateTime` is serialized using `toString()`.

```kotlin
val date = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(7), java.time.ZoneId.systemDefault())

QS.encode(mapOf("a" to date), EncodeOptions(encode = false))
// => "a=1970-01-01T01:00:00.007"   // (example output depends on system zone)

QS.encode(
  mapOf("a" to date),
  EncodeOptions(
    encode = false,
    dateSerializer = { d -> d.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli().toString() }
  )
)
// => "a=7"
```

### Sorting & filtering

```kotlin
// Sort keys
QS.encode(
  mapOf("a" to "c", "z" to "y", "b" to "f"),
  EncodeOptions(encode = false, sort = { a, b -> a.toString().compareTo(b.toString()) })
)
// => "a=c&b=f&z=y"

// Filter by function (drop/transform values)
QS.encode(
  mapOf("a" to "b", "c" to "d", "e" to mapOf("f" to java.time.Instant.ofEpochMilli(123), "g" to listOf(2))),
  EncodeOptions(
    encode = false,
    filter = FunctionFilter { prefix, value ->
      when (prefix) {
        "b" -> Undefined()
        "e[f]" -> (value as java.time.Instant).toEpochMilli()
        "e[g][0]" -> (value as Number).toInt() * 2
        else -> value
      }
    }
  )
)
// => "a=b&c=d&e[f]=123&e[g][0]=4"

// Filter by explicit list of keys/indices
QS.encode(
  mapOf("a" to "b", "c" to "d", "e" to "f"),
  EncodeOptions(encode = false, filter = IterableFilter(listOf("a", "e")))
)
// => "a=b&e=f"

QS.encode(
  mapOf("a" to listOf("b", "c", "d"), "e" to "f"),
  EncodeOptions(encode = false, filter = IterableFilter(listOf("a", 0, 2)))
)
// => "a[0]=b&a[2]=d"
```

### RFC 3986 vs RFC 1738 space encoding

```kotlin
QS.encode(mapOf("a" to "b c"))
// => "a=b%20c"   (RFC 3986 default)

QS.encode(mapOf("a" to "b c"), EncodeOptions(format = Format.RFC3986))
// => "a=b%20c"

QS.encode(mapOf("a" to "b c"), EncodeOptions(format = Format.RFC1738))
// => "a=b+c"
```

---

## Design notes

- **Performance:** The implementation mirrors qs semantics but is optimized for Kotlin/JVM. Deep parsing, list compaction, and cycle-safe compaction are implemented iteratively where it matters.
- **Safety:** Defaults (depth, parameterLimit) help mitigate abuse in user-supplied inputs; you can loosen them when you fully trust the source.
- **Interop:** Exposes knobs similar to qs (filters, sorters, custom encoders/decoders) to make migrations straightforward.

---

Special thanks to the authors of [qs](https://www.npmjs.com/package/qs) for JavaScript:
- [Jordan Harband](https://github.com/ljharb)
- [TJ Holowaychuk](https://github.com/visionmedia/node-querystring)

---

## License

BSD 3-Clause © [techouse](https://github.com/techouse)
