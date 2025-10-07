package io.github.techouse.qskotlin.unit.internal

import io.github.techouse.qskotlin.internal.Decoder
import io.github.techouse.qskotlin.models.DecodeOptions
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
        }
    })
