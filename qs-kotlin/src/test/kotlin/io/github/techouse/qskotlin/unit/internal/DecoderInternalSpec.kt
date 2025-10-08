package io.github.techouse.qskotlin.unit.internal

import io.github.techouse.qskotlin.enums.Duplicates
import io.github.techouse.qskotlin.internal.Decoder
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.Undefined
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
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
