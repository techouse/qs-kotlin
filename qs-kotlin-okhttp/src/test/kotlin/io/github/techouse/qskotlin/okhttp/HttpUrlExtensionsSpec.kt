package io.github.techouse.qskotlin.okhttp

import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.models.Delimiter
import io.github.techouse.qskotlin.models.EncodeOptions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import okhttp3.HttpUrl.Companion.toHttpUrl

class HttpUrlExtensionsSpec :
    FunSpec({
        test("null value returns the same builder unchanged") {
            val original = "https://api.example.com/products".toHttpUrl()
            val builder = original.newBuilder()

            val result = builder.addQsQueryParameters(null)

            result shouldBeSameInstanceAs builder
            result.build() shouldBe original
        }

        test("empty encode result returns the same builder unchanged") {
            val original = "https://api.example.com/products".toHttpUrl()
            val builder = original.newBuilder()

            val result = builder.addQsQueryParameters(emptyMap<String, Any?>())

            result shouldBeSameInstanceAs builder
            result.build() shouldBe original
        }

        test("simple map serializes into query parameters") {
            val url =
                "https://api.example.com/products"
                    .toHttpUrl()
                    .newBuilder()
                    .addQsQueryParameters(mapOf("q" to "kotlin", "page" to 2))
                    .build()

            url.toString() shouldBe "https://api.example.com/products?q=kotlin&page=2"
            url.queryParameter("q") shouldBe "kotlin"
            url.queryParameter("page") shouldBe "2"
        }

        test("nested map serializes into qs-style bracket notation") {
            val url =
                "https://api.example.com/products"
                    .toHttpUrl()
                    .newBuilder()
                    .addQsQueryParameters(
                        mapOf(
                            "filter" to
                                mapOf(
                                    "where" to mapOf("name" to "John", "age" to mapOf("gte" to 30))
                                )
                        )
                    )
                    .build()

            url.encodedQuery shouldBe
                "filter%5Bwhere%5D%5Bname%5D=John&" + "filter%5Bwhere%5D%5Bage%5D%5Bgte%5D=30"
        }

        test("list serialization uses qs-kotlin default indices") {
            val url =
                "https://api.example.com/products"
                    .toHttpUrl()
                    .newBuilder()
                    .addQsQueryParameters(mapOf("tags" to listOf("a", "b")))
                    .build()

            url.encodedQuery shouldBe "tags%5B0%5D=a&tags%5B1%5D=b"
        }

        test("list of complex objects uses structured qs-style keys") {
            val url =
                "https://api.example.com/products"
                    .toHttpUrl()
                    .newBuilder()
                    .addQsQueryParameters(
                        mapOf(
                            "items" to
                                listOf(
                                    mapOf("id" to 1, "name" to "first"),
                                    mapOf("id" to 2, "name" to "second"),
                                )
                        )
                    )
                    .build()

            url.encodedQuery shouldBe
                "items%5B0%5D%5Bid%5D=1&items%5B0%5D%5Bname%5D=first&" +
                    "items%5B1%5D%5Bid%5D=2&items%5B1%5D%5Bname%5D=second"
        }

        test("existing query parameters are preserved and qs parameters append") {
            val url =
                "https://api.example.com/products?existing=1"
                    .toHttpUrl()
                    .newBuilder()
                    .addQsQueryParameters(mapOf("filter" to mapOf("name" to "John")))
                    .build()

            url.encodedQuery shouldBe "existing=1&filter%5Bname%5D=John"
        }

        test("duplicate keys are preserved") {
            val url =
                "https://api.example.com/products"
                    .toHttpUrl()
                    .newBuilder()
                    .addQsQueryParameters(
                        mapOf("tag" to listOf("a", "b")),
                        EncodeOptions(listFormat = ListFormat.REPEAT),
                    )
                    .build()

            url.encodedQuery shouldBe "tag=a&tag=b"
            url.queryParameterValues("tag") shouldBe listOf("a", "b")
        }

        test("empty values are preserved") {
            val url =
                "https://api.example.com/products"
                    .toHttpUrl()
                    .newBuilder()
                    .addQsQueryParameters(mapOf("foo" to ""))
                    .build()

            url.encodedQuery shouldBe "foo="
            url.queryParameter("foo") shouldBe ""
        }

        test("name-only values are preserved") {
            val url =
                "https://api.example.com/products"
                    .toHttpUrl()
                    .newBuilder()
                    .addQsQueryParameters(
                        mapOf("foo" to null),
                        EncodeOptions(strictNullHandling = true),
                    )
                    .build()

            url.encodedQuery shouldBe "foo"
            url.queryParameterValue(0) shouldBe null
        }

        test("generated query prefix and custom delimiter are handled") {
            val url =
                "https://api.example.com/products"
                    .toHttpUrl()
                    .newBuilder()
                    .addQsQueryParameters(
                        mapOf("a" to "b", "c" to "d"),
                        EncodeOptions(addQueryPrefix = true, delimiter = Delimiter.SEMICOLON),
                    )
                    .build()

            url.encodedQuery shouldBe "a=b&c=d"
        }

        test("leading question mark in raw key is preserved when query prefix is disabled") {
            val url =
                "https://api.example.com/products"
                    .toHttpUrl()
                    .newBuilder()
                    .addQsQueryParameters(mapOf("?foo" to "bar"), EncodeOptions(encode = false))
                    .build()

            url.encodedQuery shouldBe "?foo=bar"
            url.queryParameter("?foo") shouldBe "bar"
        }

        test("encoded qs output is not double-encoded") {
            val url =
                "https://api.example.com/products"
                    .toHttpUrl()
                    .newBuilder()
                    .addQsQueryParameters(mapOf("a" to mapOf("b" to "c")))
                    .build()

            url.toString() shouldBe "https://api.example.com/products?a%5Bb%5D=c"
            url.encodedQuery shouldBe "a%5Bb%5D=c"
        }

        test("builder extension returns the same builder instance") {
            val builder = "https://api.example.com/products".toHttpUrl().newBuilder()

            val result = builder.addQsQueryParameters(mapOf("a" to "b"))

            result shouldBeSameInstanceAs builder
        }

        test("HttpUrl extension returns a new URL and leaves the original unchanged") {
            val original = "https://api.example.com/products?existing=1".toHttpUrl()

            val updated = original.addQsQueryParameters(mapOf("a" to "b"))

            updated shouldNotBeSameInstanceAs original
            original.toString() shouldBe "https://api.example.com/products?existing=1"
            updated.toString() shouldBe "https://api.example.com/products?existing=1&a=b"
        }
    })
