package io.github.techouse.qskotlin.unit

import io.github.techouse.qskotlin.decodeQsQuery
import io.github.techouse.qskotlin.fixtures.data.EndToEndTestCases
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.Delimiter
import io.github.techouse.qskotlin.models.EncodeOptions
import io.github.techouse.qskotlin.toQueryMap
import io.github.techouse.qskotlin.toQueryString
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.net.URI

class ExtensionsSpec :
    FunSpec({
        context("String.toQueryMap") {
            EndToEndTestCases.forEach { testCase ->
                test("should decode: ${testCase.encoded}") {
                    testCase.encoded.toQueryMap() shouldBe testCase.data
                }
            }
        }

        context("Map.toQueryString") {
            EndToEndTestCases.forEach { testCase ->
                test("should encode: ${testCase.data}") {
                    testCase.data.toQueryString(EncodeOptions(encode = false)) shouldBe
                        testCase.encoded
                }
            }

            test("uses default EncodeOptions overload when omitted") {
                mapOf("a" to "b").toQueryString() shouldBe "a=b"
            }
        }

        context("URI.decodeQsQuery") {
            test("decodes the raw query without losing qs semantics") {
                val uri =
                    URI(
                        "https://example.com/search?" +
                            "filter%5Bwhere%5D%5Bname%5D=John%20Doe&" +
                            "tag=a&tag=b&flag&empty=&escaped=x%26y&pct=%2525&" +
                            "plus=a+b&space=a%20b#results"
                    )

                uri.decodeQsQuery() shouldBe
                    mapOf(
                        "filter" to mapOf("where" to mapOf("name" to "John Doe")),
                        "tag" to listOf("a", "b"),
                        "flag" to "",
                        "empty" to "",
                        "escaped" to "x&y",
                        "pct" to "%25",
                        "plus" to "a b",
                        "space" to "a b",
                    )
            }

            test("forwards decoder options") {
                val uri = URI("search?flag;tags=kotlin,android")

                uri.decodeQsQuery(
                    DecodeOptions(
                        comma = true,
                        delimiter = Delimiter.SEMICOLON,
                        strictNullHandling = true,
                    )
                ) shouldBe mapOf("flag" to null, "tags" to listOf("kotlin", "android"))
            }

            test("decodes relative hierarchical URIs") {
                URI("search?filter%5Bname%5D=Jane").decodeQsQuery() shouldBe
                    mapOf("filter" to mapOf("name" to "Jane"))
            }

            test("returns an empty map for absent, empty, and opaque query components") {
                val absentQuery = URI("https://example.com/path")
                val emptyQuery = URI("https://example.com/path?")

                absentQuery.rawQuery shouldBe null
                emptyQuery.rawQuery shouldBe ""
                absentQuery.decodeQsQuery() shouldBe emptyMap()
                emptyQuery.decodeQsQuery() shouldBe emptyMap()
                URI("mailto:user@example.com?subject=hello").decodeQsQuery() shouldBe emptyMap()
            }
        }
    })
