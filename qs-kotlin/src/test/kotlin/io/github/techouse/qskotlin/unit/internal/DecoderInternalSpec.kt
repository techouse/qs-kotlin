package io.github.techouse.qskotlin.unit.internal

import io.github.techouse.qskotlin.enums.Duplicates
import io.github.techouse.qskotlin.internal.Decoder
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.RegexDelimiter
import io.github.techouse.qskotlin.models.StringDelimiter
import io.github.techouse.qskotlin.models.Undefined
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.lang.reflect.InvocationTargetException
import java.nio.charset.StandardCharsets

class DecoderInternalSpec :
    DescribeSpec({
        describe("Decoder.parseQueryStringValues") {
            it("rejects non-positive parameter limits") {
                shouldThrow<IllegalArgumentException> {
                    Decoder.parseQueryStringValues("a=1", DecodeOptions(parameterLimit = 0))
                }
            }

            it("throws when parameter limit exceeded and throwOnLimitExceeded=true") {
                val options = DecodeOptions(parameterLimit = 1, throwOnLimitExceeded = true)
                shouldThrow<IndexOutOfBoundsException> {
                    Decoder.parseQueryStringValues("a=1&b=2", options)
                }
            }

            it("splits comma-delimited lists respecting limits") {
                val options = DecodeOptions(comma = true)
                val result = Decoder.parseQueryStringValues("list=a,b", options)
                result["list"] shouldBe listOf("a", "b")
            }

            it("string delimiter path ignores empty segments while preserving valid pairs") {
                val result = Decoder.parseQueryStringValues("a=1&&b=2&&", DecodeOptions())
                result shouldBe mutableMapOf("a" to "1", "b" to "2")
            }

            it("string delimiter path handles multi-character delimiters with adjacent empties") {
                val result =
                    Decoder.parseQueryStringValues(
                        "a=1&&&&b=2&&",
                        DecodeOptions(delimiter = StringDelimiter("&&")),
                    )

                result shouldBe mutableMapOf("a" to "1", "b" to "2")
            }

            it("regex delimiter path preserves parsing semantics") {
                val result =
                    Decoder.parseQueryStringValues(
                        "a=1;b=2,,c=3;;",
                        DecodeOptions(delimiter = RegexDelimiter("[;,]")),
                    )
                result shouldBe mutableMapOf("a" to "1", "b" to "2", "c" to "3")
            }

            it("respects charset sentinel and numeric entities") {
                val options =
                    DecodeOptions(
                        interpretNumericEntities = true,
                        charset = StandardCharsets.ISO_8859_1,
                        charsetSentinel = true,
                        ignoreQueryPrefix = true,
                    )

                val result =
                    Decoder.parseQueryStringValues("?utf8=%26%2310003%3B&name=%26%2365%3B", options)
                result.containsKey("name") shouldBe true
                result["name"] shouldBe "A"
            }

            it("uses default options overload when omitted") {
                Decoder.parseQueryStringValues("foo=bar") shouldBe mutableMapOf("foo" to "bar")
            }

            it("decodes bare keys honoring strictNullHandling") {
                val result =
                    Decoder.parseQueryStringValues("flag", DecodeOptions(strictNullHandling = true))

                result shouldBe mutableMapOf<String, Any?>("flag" to null)
            }

            it("wraps bracket suffix comma values as nested lists") {
                val options = DecodeOptions(comma = true)
                val result = Decoder.parseQueryStringValues("tags[]=a,b", options)

                result["tags[]"] shouldBe listOf(listOf("a", "b"))
            }

            it("combines duplicate keys when duplicates=COMBINE") {
                val options = DecodeOptions(duplicates = Duplicates.COMBINE)
                val result = Decoder.parseQueryStringValues("k=1&k=2", options)

                result["k"] shouldBe listOf("1", "2")
            }

            it("parameter limits count only non-empty parts for string delimiters") {
                val result =
                    Decoder.parseQueryStringValues(
                        "&&a=1&&b=2",
                        DecodeOptions(parameterLimit = 1, throwOnLimitExceeded = false),
                    )
                result shouldBe mutableMapOf("a" to "1")
            }

            it("parameter limits throw when non-empty parts exceed the window") {
                shouldThrow<IndexOutOfBoundsException> {
                    Decoder.parseQueryStringValues(
                        "&&a=1&&b=2",
                        DecodeOptions(parameterLimit = 1, throwOnLimitExceeded = true),
                    )
                }
            }

            it("comma parsing preserves empty boundary tokens") {
                val result = Decoder.parseQueryStringValues("a=,", DecodeOptions(comma = true))
                result["a"] shouldBe listOf("", "")
            }

            it("comma parsing truncates when listLimit is exceeded and throw disabled") {
                val result =
                    Decoder.parseQueryStringValues(
                        "a=1,2,3",
                        DecodeOptions(comma = true, listLimit = 2, throwOnLimitExceeded = false),
                    )
                result["a"] shouldBe listOf("1", "2")
            }

            it("comma parsing throws when listLimit is exceeded and throw enabled") {
                shouldThrow<IndexOutOfBoundsException> {
                    Decoder.parseQueryStringValues(
                        "a=1,2",
                        DecodeOptions(comma = true, listLimit = 1, throwOnLimitExceeded = true),
                    )
                }
            }

            it("rejects empty string delimiters") {
                shouldThrow<IllegalArgumentException> {
                    Decoder.parseQueryStringValues(
                        "a=1",
                        DecodeOptions(delimiter = StringDelimiter("")),
                    )
                }
            }

            it("uses unbounded comma split when listLimit is Int.MAX_VALUE") {
                val parseListValue =
                    Decoder::class
                        .java
                        .getDeclaredMethod(
                            "parseListValue",
                            Any::class.java,
                            DecodeOptions::class.java,
                            Int::class.javaPrimitiveType,
                        )
                parseListValue.isAccessible = true

                val result =
                    parseListValue.invoke(
                        Decoder,
                        "a,b",
                        DecodeOptions(
                            comma = true,
                            listLimit = Int.MAX_VALUE,
                            throwOnLimitExceeded = true,
                        ),
                        0,
                    )

                result shouldBe listOf("a", "b")
            }

            it("throws when comma parsing starts with negative remaining allowance") {
                val parseListValue =
                    Decoder::class
                        .java
                        .getDeclaredMethod(
                            "parseListValue",
                            Any::class.java,
                            DecodeOptions::class.java,
                            Int::class.javaPrimitiveType,
                        )
                parseListValue.isAccessible = true

                val thrown =
                    shouldThrow<InvocationTargetException> {
                        parseListValue.invoke(
                            Decoder,
                            "a,b",
                            DecodeOptions(comma = true, listLimit = 1, throwOnLimitExceeded = true),
                            2,
                        )
                    }

                thrown.cause.shouldBeInstanceOf<IndexOutOfBoundsException>()
            }
        }

        describe("Decoder.parseKeys") {
            it("handles nested list chains while respecting parent indices") {
                val options = DecodeOptions(parseLists = true)
                val value = listOf(listOf("x", "y"))

                @Suppress("UNCHECKED_CAST")
                val parsed =
                    Decoder.parseKeys(
                        givenKey = "0[]",
                        value = value,
                        options = options,
                        valuesParsed = true,
                    ) as Map<String, Any?>

                parsed["0"] shouldBe listOf(listOf("x", "y"))
            }

            it("produces empty list when allowEmptyLists consumes blank value") {
                val options =
                    DecodeOptions(
                        parseLists = true,
                        allowEmptyLists = true,
                        strictNullHandling = false,
                    )

                val parsed =
                    Decoder.parseKeys(
                        givenKey = "list[]",
                        value = "",
                        options = options,
                        valuesParsed = true,
                    ) as Map<*, *>

                parsed["list"] shouldBe mutableListOf<Any?>()
            }

            it("falls back to map entries when parseLists disabled") {
                val options = DecodeOptions(parseLists = false)

                val parsed =
                    Decoder.parseKeys(
                        givenKey = "arr[3]",
                        value = "v",
                        options = options,
                        valuesParsed = true,
                    ) as Map<*, *>

                parsed["arr"] shouldBe mapOf("3" to "v")
            }

            it("enforces listLimit for nested list growth") {
                val options = DecodeOptions(listLimit = 1, throwOnLimitExceeded = true)
                val nested = listOf(listOf("a", "b"))

                shouldThrow<IndexOutOfBoundsException> {
                    Decoder.parseKeys(
                        givenKey = "0[]",
                        value = nested,
                        options = options,
                        valuesParsed = false,
                    )
                }
            }

            it("creates indexed lists for bracketed numeric keys within limit") {
                val parsed =
                    Decoder.parseKeys(
                        givenKey = "items[2]",
                        value = "x",
                        options = DecodeOptions(),
                        valuesParsed = true,
                    ) as Map<*, *>

                parsed["items"] shouldBe mutableListOf(Undefined(), Undefined(), "x")
            }
        }
    })
