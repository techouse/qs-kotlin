package io.github.techouse.qskotlin.unit

import io.github.techouse.qskotlin.decode
import io.github.techouse.qskotlin.encode
import io.github.techouse.qskotlin.enums.Duplicates
import io.github.techouse.qskotlin.enums.Format
import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.models.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class ExampleSpec :
    DescribeSpec({
        describe("Simple examples") {
            it("decodes a simple query string") { decode("a=c") shouldBe mapOf("a" to "c") }

            it("encodes a simple Map to a query string") {
                encode(mapOf("a" to "c")) shouldBe "a=c"
            }
        }

        describe("Decoding") {
            describe("Maps") {
                it("allows you to create nested Maps within your query strings") {
                    // QS allows you to create nested Maps within your query strings,
                    // by surrounding the name of sub-keys with square brackets []. For example,
                    // the string 'foo[bar]=baz' converts to
                    decode("foo[bar]=baz") shouldBe mapOf("foo" to mapOf("bar" to "baz"))
                }

                it("works with URI encoded strings too") {
                    decode("a%5Bb%5D=c") shouldBe mapOf("a" to mapOf("b" to "c"))
                }

                it("can nest Maps like 'foo[bar][baz]=foobarbaz'") {
                    decode("foo[bar][baz]=foobarbaz") shouldBe
                        mapOf("foo" to mapOf("bar" to mapOf("baz" to "foobarbaz")))
                }

                it("only decodes up to 5 children deep by default") {
                    // By default, when nesting Maps QS will only decode up to 5 children deep.
                    // This means if you attempt to decode a string like
                    // 'a[b][c][d][e][f][g][h][i]=j'
                    // your resulting Map will be:
                    decode("a[b][c][d][e][f][g][h][i]=j") shouldBe
                        mapOf(
                            "a" to
                                mapOf(
                                    "b" to
                                        mapOf(
                                            "c" to
                                                mapOf(
                                                    "d" to
                                                        mapOf(
                                                            "e" to
                                                                mapOf(
                                                                    "f" to mapOf("[g][h][i]" to "j")
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                }

                it("can override depth with DecodeOptions.depth") {
                    decode("a[b][c][d][e][f][g][h][i]=j", DecodeOptions(depth = 1)) shouldBe
                        mapOf("a" to mapOf("b" to mapOf("[c][d][e][f][g][h][i]" to "j")))
                }

                it("only parses up to 1000 parameters by default") {
                    decode("a=b&c=d", DecodeOptions(parameterLimit = 1)) shouldBe mapOf("a" to "b")
                }

                it("can bypass leading question mark with ignoreQueryPrefix") {
                    decode("?a=b&c=d", DecodeOptions(ignoreQueryPrefix = true)) shouldBe
                        mapOf("a" to "b", "c" to "d")
                }

                it("accepts custom delimiter") {
                    decode("a=b;c=d", DecodeOptions(delimiter = StringDelimiter(";"))) shouldBe
                        mapOf("a" to "b", "c" to "d")
                }

                it("accepts regex delimiter") {
                    decode("a=b;c=d", DecodeOptions(delimiter = RegexDelimiter("[;,]"))) shouldBe
                        mapOf("a" to "b", "c" to "d")
                }

                it("can enable dot notation with allowDots") {
                    decode("a.b=c", DecodeOptions(allowDots = true)) shouldBe
                        mapOf("a" to mapOf("b" to "c"))
                }

                it("can decode dots in keys with decodeDotInKeys") {
                    decode(
                        "name%252Eobj.first=John&name%252Eobj.last=Doe",
                        DecodeOptions(decodeDotInKeys = true),
                    ) shouldBe mapOf("name.obj" to mapOf("first" to "John", "last" to "Doe"))
                }

                it("can allow empty lists with allowEmptyLists") {
                    decode("foo[]&bar=baz", DecodeOptions(allowEmptyLists = true)) shouldBe
                        mapOf("foo" to emptyList<String>(), "bar" to "baz")
                }

                it("handles duplicate keys by default") {
                    decode("foo=bar&foo=baz") shouldBe mapOf("foo" to listOf("bar", "baz"))
                }

                it("can combine duplicates explicitly") {
                    decode(
                        "foo=bar&foo=baz",
                        DecodeOptions(duplicates = Duplicates.COMBINE),
                    ) shouldBe mapOf("foo" to listOf("bar", "baz"))
                }

                it("can take first duplicate with Duplicates.FIRST") {
                    decode("foo=bar&foo=baz", DecodeOptions(duplicates = Duplicates.FIRST)) shouldBe
                        mapOf("foo" to "bar")
                }

                it("can take last duplicate with Duplicates.LAST") {
                    decode("foo=bar&foo=baz", DecodeOptions(duplicates = Duplicates.LAST)) shouldBe
                        mapOf("foo" to "baz")
                }

                it("supports latin1 charset for legacy browsers") {
                    decode("a=%A7", DecodeOptions(charset = StandardCharsets.ISO_8859_1)) shouldBe
                        mapOf("a" to "§")
                }

                it("supports charset sentinel with latin1") {
                    decode(
                        "utf8=%E2%9C%93&a=%C3%B8",
                        DecodeOptions(charset = StandardCharsets.ISO_8859_1, charsetSentinel = true),
                    ) shouldBe mapOf("a" to "ø")
                }

                it("supports charset sentinel with utf8") {
                    decode(
                        "utf8=%26%2310003%3B&a=%F8",
                        DecodeOptions(charset = StandardCharsets.UTF_8, charsetSentinel = true),
                    ) shouldBe mapOf("a" to "ø")
                }

                it("can interpret numeric entities") {
                    decode(
                        "a=%26%239786%3B",
                        DecodeOptions(
                            charset = StandardCharsets.ISO_8859_1,
                            interpretNumericEntities = true,
                        ),
                    ) shouldBe mapOf("a" to "☺")
                }
            }

            describe("Lists") {
                it("can parse lists using [] notation") {
                    decode("a[]=b&a[]=c") shouldBe mapOf("a" to listOf("b", "c"))
                }

                it("can specify an index") {
                    decode("a[1]=c&a[0]=b") shouldBe mapOf("a" to listOf("b", "c"))
                }

                it("compacts sparse lists preserving order") {
                    decode("a[1]=b&a[15]=c") shouldBe mapOf("a" to listOf("b", "c"))
                }

                it("preserves empty string values") {
                    decode("a[]=&a[]=b") shouldBe mapOf("a" to listOf("", "b"))

                    decode("a[0]=b&a[1]=&a[2]=c") shouldBe mapOf("a" to listOf("b", "", "c"))
                }

                it("converts high indices to Map keys") {
                    decode("a[100]=b") shouldBe mapOf("a" to mapOf(100 to "b"))
                }

                it("can override list limit") {
                    decode("a[1]=b", DecodeOptions(listLimit = 0)) shouldBe
                        mapOf("a" to mapOf(1 to "b"))
                }

                it("can disable list parsing entirely") {
                    decode("a[]=b", DecodeOptions(parseLists = false)) shouldBe
                        mapOf("a" to mapOf(0 to "b"))
                }

                it("merges mixed notations into Map") {
                    decode("a[0]=b&a[b]=c") shouldBe mapOf("a" to mapOf(0 to "b", "b" to "c"))
                }

                it("can create lists of Maps") {
                    decode("a[][b]=c") shouldBe mapOf("a" to listOf(mapOf("b" to "c")))
                }

                it("can parse comma-separated values") {
                    decode("a=b,c", DecodeOptions(comma = true)) shouldBe
                        mapOf("a" to listOf("b", "c"))
                }
            }

            describe("Primitive/Scalar values") {
                it("parses all values as strings by default") {
                    decode("a=15&b=true&c=null") shouldBe
                        mapOf("a" to "15", "b" to "true", "c" to "null")
                }
            }
        }

        describe("Encoding") {
            it("encodes Maps as you would expect") {
                encode(mapOf("a" to "b")) shouldBe "a=b"

                encode(mapOf("a" to mapOf("b" to "c"))) shouldBe "a%5Bb%5D=c"
            }

            it("can disable encoding with encode=false") {
                encode(mapOf("a" to mapOf("b" to "c")), EncodeOptions(encode = false)) shouldBe
                    "a[b]=c"
            }

            it("can encode values only with encodeValuesOnly=true") {
                encode(
                    mapOf(
                        "a" to "b",
                        "c" to listOf("d", "e=f"),
                        "f" to listOf(listOf("g"), listOf("h")),
                    ),
                    EncodeOptions(encodeValuesOnly = true),
                ) shouldBe "a=b&c[0]=d&c[1]=e%3Df&f[0][0]=g&f[1][0]=h"
            }

            it("can use custom encoder") {
                encode(
                    mapOf("a" to mapOf("b" to "č")),
                    EncodeOptions(
                        encoder = { str, _, _ -> if (str == "č") "c" else str.toString() }
                    ),
                ) shouldBe "a[b]=c"
            }

            it("encodes lists with indices by default") {
                encode(mapOf("a" to listOf("b", "c", "d")), EncodeOptions(encode = false)) shouldBe
                    "a[0]=b&a[1]=c&a[2]=d"
            }

            it("can disable indices with indices=false") {
                encode(
                    mapOf("a" to listOf("b", "c", "d")),
                    EncodeOptions(encode = false, indices = false),
                ) shouldBe "a=b&a=c&a=d"
            }

            it("supports different list formats") {
                encode(
                    mapOf("a" to listOf("b", "c")),
                    EncodeOptions(encode = false, listFormat = ListFormat.INDICES),
                ) shouldBe "a[0]=b&a[1]=c"

                encode(
                    mapOf("a" to listOf("b", "c")),
                    EncodeOptions(encode = false, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a[]=b&a[]=c"

                encode(
                    mapOf("a" to listOf("b", "c")),
                    EncodeOptions(encode = false, listFormat = ListFormat.REPEAT),
                ) shouldBe "a=b&a=c"

                encode(
                    mapOf("a" to listOf("b", "c")),
                    EncodeOptions(encode = false, listFormat = ListFormat.COMMA),
                ) shouldBe "a=b,c"
            }

            it("uses bracket notation for Maps by default") {
                encode(
                    mapOf("a" to mapOf("b" to mapOf("c" to "d", "e" to "f"))),
                    EncodeOptions(encode = false),
                ) shouldBe "a[b][c]=d&a[b][e]=f"
            }

            it("can use dot notation with allowDots=true") {
                encode(
                    mapOf("a" to mapOf("b" to mapOf("c" to "d", "e" to "f"))),
                    EncodeOptions(encode = false, allowDots = true),
                ) shouldBe "a.b.c=d&a.b.e=f"
            }

            it("can encode dots in keys with encodeDotInKeys=true") {
                encode(
                    mapOf("name.obj" to mapOf("first" to "John", "last" to "Doe")),
                    EncodeOptions(allowDots = true, encodeDotInKeys = true),
                ) shouldBe "name%252Eobj.first=John&name%252Eobj.last=Doe"
            }

            it("can allow empty lists with allowEmptyLists=true") {
                encode(
                    mapOf("foo" to emptyList<String>(), "bar" to "baz"),
                    EncodeOptions(encode = false, allowEmptyLists = true),
                ) shouldBe "foo[]&bar=baz"
            }

            it("handles empty strings and null values") { encode(mapOf("a" to "")) shouldBe "a=" }

            it("returns empty string for empty collections") {
                encode(mapOf("a" to emptyList<String>())) shouldBe ""
                encode(mapOf("a" to emptyMap<String, Any>())) shouldBe ""
                encode(mapOf("a" to listOf(emptyMap<String, Any>()))) shouldBe ""
                encode(mapOf("a" to mapOf("b" to emptyList<String>()))) shouldBe ""
                encode(mapOf("a" to mapOf("b" to emptyMap<String, Any>()))) shouldBe ""
            }

            it("omits undefined properties") {
                encode(mapOf("a" to null, "b" to Undefined())) shouldBe "a="
            }

            it("can add query prefix") {
                encode(mapOf("a" to "b", "c" to "d"), EncodeOptions(addQueryPrefix = true)) shouldBe
                    "?a=b&c=d"
            }

            it("can override delimiter") {
                encode(mapOf("a" to "b", "c" to "d"), EncodeOptions(delimiter = ";")) shouldBe
                    "a=b;c=d"
            }

            it("can serialize DateTime objects") {
                val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(7), ZoneOffset.UTC)

                encode(mapOf("a" to date), EncodeOptions(encode = false)) shouldBe
                    "a=1970-01-01T00:00:00.007"

                encode(
                    mapOf("a" to date),
                    EncodeOptions(
                        encode = false,
                        dateSerializer = { d ->
                            d.atZone(ZoneOffset.UTC).toInstant().toEpochMilli().toString()
                        },
                    ),
                ) shouldBe "a=7"
            }

            it("can sort parameter keys") {
                encode(
                    mapOf("a" to "c", "z" to "y", "b" to "f"),
                    EncodeOptions(
                        encode = false,
                        sort = { a, b -> a.toString().compareTo(b.toString()) },
                    ),
                ) shouldBe "a=c&b=f&z=y"
            }

            it("can filter keys with function filter") {
                encode(
                    mapOf(
                        "a" to "b",
                        "c" to "d",
                        "e" to mapOf("f" to Instant.ofEpochMilli(123), "g" to listOf(2)),
                    ),
                    EncodeOptions(
                        encode = false,
                        filter =
                            FunctionFilter { prefix, value ->
                                when (prefix) {
                                    "b" -> Undefined()
                                    "e[f]" -> (value as Instant).toEpochMilli()
                                    "e[g][0]" -> (value as Number).toInt() * 2
                                    else -> value
                                }
                            },
                    ),
                ) shouldBe "a=b&c=d&e[f]=123&e[g][0]=4"
            }

            it("can filter keys with iterable filter") {
                encode(
                    mapOf("a" to "b", "c" to "d", "e" to "f"),
                    EncodeOptions(encode = false, filter = IterableFilter(listOf("a", "e"))),
                ) shouldBe "a=b&e=f"

                encode(
                    mapOf("a" to listOf("b", "c", "d"), "e" to "f"),
                    EncodeOptions(encode = false, filter = IterableFilter(listOf("a", 0, 2))),
                ) shouldBe "a[0]=b&a[2]=d"
            }
        }

        describe("null values") {
            it("treats null values like empty strings by default") {
                encode(mapOf("a" to null, "b" to "")) shouldBe "a=&b="
            }

            it(
                "does not distinguish between parameters with and without equal signs when decoding"
            ) {
                // Decoding does not distinguish between parameters with and without equal signs.
                // Both are converted to empty strings.
                decode("a&b=") shouldBe mapOf("a" to "", "b" to "")
            }

            it(
                "can distinguish between null values and empty strings using strictNullHandling flag"
            ) {
                // To distinguish between null values and empty Strings use the
                // EncodeOptions.strictNullHandling flag.
                // In the result string the null values have no = sign:
                encode(
                    mapOf("a" to null, "b" to ""),
                    EncodeOptions(strictNullHandling = true),
                ) shouldBe "a&b="
            }

            it("can decode values without = back to null using strictNullHandling flag") {
                // To decode values without = back to null use the DecodeOptions.strictNullHandling
                // flag:
                decode("a&b=", DecodeOptions(strictNullHandling = true)) shouldBe
                    mapOf("a" to null, "b" to "")
            }

            it("can completely skip rendering keys with null values using skipNulls flag") {
                // To completely skip rendering keys with null values, use the
                // EncodeOptions.skipNulls flag:
                encode(mapOf("a" to "b", "c" to null), EncodeOptions(skipNulls = true)) shouldBe
                    "a=b"
            }
        }

        describe("Charset") {
            it("can encode using latin1 charset") {
                encode(
                    mapOf("æ" to "æ"),
                    EncodeOptions(charset = StandardCharsets.ISO_8859_1),
                ) shouldBe "%E6=%E6"
            }

            it("converts characters that don't exist in latin1 to numeric entities") {
                encode(
                    mapOf("a" to "☺"),
                    EncodeOptions(charset = StandardCharsets.ISO_8859_1),
                ) shouldBe "a=%26%239786%3B"
            }

            it("can announce charset using charsetSentinel option with UTF-8") {
                encode(mapOf("a" to "☺"), EncodeOptions(charsetSentinel = true)) shouldBe
                    "utf8=%E2%9C%93&a=%E2%98%BA"
            }

            it("can announce charset using charsetSentinel option with latin1") {
                encode(
                    mapOf("a" to "æ"),
                    EncodeOptions(charset = StandardCharsets.ISO_8859_1, charsetSentinel = true),
                ) shouldBe "utf8=%26%2310003%3B&a=%E6"
            }

            it("can use custom encoder for different character sets") {
                // Note: This example uses a mock encoder since Shift JIS support
                // would require additional dependencies in Kotlin
                encode(
                    mapOf("a" to "hello"),
                    EncodeOptions(
                        encoder = { str, _, _ ->
                            when (str) {
                                "a" -> "%61"
                                "hello" -> "%68%65%6c%6c%6f"
                                else -> str.toString()
                            }
                        }
                    ),
                ) shouldBe "%61=%68%65%6c%6c%6f"
            }

            it("can use custom decoder for different character sets") {
                // Note: This example uses a mock decoder
                decode(
                    "%61=%68%65%6c%6c%6f",
                    DecodeOptions(
                        decoder = { str, _ ->
                            when (str) {
                                "%61" -> "a"
                                "%68%65%6c%6c%6f" -> "hello"
                                else -> str
                            }
                        }
                    ),
                ) shouldBe mapOf("a" to "hello")
            }
        }

        describe("RFC 3986 and RFC 1738 space encoding") {
            it("encodes spaces as %20 by default (RFC 3986)") {
                encode(mapOf("a" to "b c")) shouldBe "a=b%20c"
            }

            it("encodes spaces as %20 with explicit RFC 3986 format") {
                encode(mapOf("a" to "b c"), EncodeOptions(format = Format.RFC3986)) shouldBe
                    "a=b%20c"
            }

            it("encodes spaces as + with RFC 1738 format") {
                encode(mapOf("a" to "b c"), EncodeOptions(format = Format.RFC1738)) shouldBe "a=b+c"
            }
        }
    })
