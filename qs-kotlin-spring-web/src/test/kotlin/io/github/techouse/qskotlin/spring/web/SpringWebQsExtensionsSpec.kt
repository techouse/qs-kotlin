package io.github.techouse.qskotlin.spring.web

import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.models.Delimiter
import io.github.techouse.qskotlin.models.EncodeOptions
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.springframework.web.util.UriComponentsBuilder

class SpringWebQsExtensionsSpec :
    FunSpec({
        test("null value returns the same builder unchanged") {
            val builder = UriComponentsBuilder.fromUriString("https://api.example.com/products")

            val result = builder.queryQs(null)

            result shouldBeSameInstanceAs builder
            result.build(true).toUriString() shouldBe "https://api.example.com/products"
        }

        test("empty encode result returns the same builder unchanged") {
            val builder = UriComponentsBuilder.fromUriString("https://api.example.com/products")

            val result = builder.queryQs(emptyMap<String, Any?>())

            result shouldBeSameInstanceAs builder
            result.build(true).toUriString() shouldBe "https://api.example.com/products"
        }

        test("simple map serializes into query parameters") {
            val uri =
                UriComponentsBuilder.fromUriString("https://api.example.com/products")
                    .queryQs(mapOf("q" to "kotlin", "page" to 2))
                    .build(true)
                    .toUri()

            uri.toString() shouldBe "https://api.example.com/products?q=kotlin&page=2"
        }

        test("nested map serializes into qs-style bracket notation") {
            val query =
                UriComponentsBuilder.fromUriString("https://api.example.com/products")
                    .queryQs(
                        mapOf(
                            "filter" to
                                mapOf(
                                    "where" to mapOf("name" to "John", "age" to mapOf("gte" to 30))
                                )
                        )
                    )
                    .build(true)
                    .query

            query shouldBe "filter%5Bwhere%5D%5Bname%5D=John&filter%5Bwhere%5D%5Bage%5D%5Bgte%5D=30"
        }

        test("list serialization uses qs-kotlin default indices") {
            val query =
                UriComponentsBuilder.fromUriString("https://api.example.com/products")
                    .queryQs(mapOf("tags" to listOf("a", "b")))
                    .build(true)
                    .query

            query shouldBe "tags%5B0%5D=a&tags%5B1%5D=b"
        }

        test("list of complex objects uses structured qs-style keys") {
            val query =
                UriComponentsBuilder.fromUriString("https://api.example.com/products")
                    .queryQs(
                        mapOf(
                            "items" to
                                listOf(
                                    mapOf("id" to 1, "name" to "first"),
                                    mapOf("id" to 2, "name" to "second"),
                                )
                        )
                    )
                    .build(true)
                    .query

            query shouldBe
                "items%5B0%5D%5Bid%5D=1&items%5B0%5D%5Bname%5D=first&items%5B1%5D%5Bid%5D=2&items%5B1%5D%5Bname%5D=second"
        }

        test("existing query parameters are preserved and qs parameters append") {
            val query =
                UriComponentsBuilder.fromUriString("https://api.example.com/products?existing=1")
                    .queryQs(mapOf("filter" to mapOf("name" to "John")))
                    .build(true)
                    .query

            query shouldBe "existing=1&filter%5Bname%5D=John"
        }

        test("duplicate keys are preserved") {
            val query =
                UriComponentsBuilder.fromUriString("https://api.example.com/products")
                    .queryQs(
                        mapOf("tag" to listOf("a", "b")),
                        EncodeOptions(listFormat = ListFormat.REPEAT),
                    )
                    .build(true)
                    .query

            query shouldBe "tag=a&tag=b"
        }

        test("empty values are preserved") {
            val query =
                UriComponentsBuilder.fromUriString("https://api.example.com/products")
                    .queryQs(mapOf("foo" to ""))
                    .build(true)
                    .query

            query shouldBe "foo="
        }

        test("name-only values are preserved") {
            val query =
                UriComponentsBuilder.fromUriString("https://api.example.com/products")
                    .queryQs(mapOf("foo" to null), EncodeOptions(strictNullHandling = true))
                    .build(true)
                    .query

            query shouldBe "foo"
        }

        test("generated query prefix and custom delimiter are handled") {
            val query =
                UriComponentsBuilder.fromUriString("https://api.example.com/products")
                    .queryQs(
                        mapOf("a" to "b", "c" to "d"),
                        EncodeOptions(addQueryPrefix = true, delimiter = Delimiter.SEMICOLON),
                    )
                    .build(true)
                    .query

            query shouldBe "a=b&c=d"
        }

        test("encoded output is preserved with build true") {
            val uri =
                UriComponentsBuilder.fromUriString("https://api.example.com/products")
                    .queryQs(mapOf("filter" to mapOf("where" to mapOf("name" to "John Doe"))))
                    .build(true)
                    .toUri()

            uri.toString() shouldBe
                "https://api.example.com/products?filter%5Bwhere%5D%5Bname%5D=John%20Doe"
        }

        test("build to URI double-encodes already encoded qs output") {
            val uri =
                UriComponentsBuilder.fromUriString("https://api.example.com/products")
                    .queryQs(mapOf("filter" to mapOf("where" to mapOf("name" to "John Doe"))))
                    .build()
                    .toUri()
                    .toString()

            uri shouldContain "%255B"
            uri shouldContain "%2520"
        }

        test("encode then build double-encodes already encoded qs output") {
            val uri =
                UriComponentsBuilder.fromUriString("https://api.example.com/products")
                    .queryQs(mapOf("filter" to mapOf("where" to mapOf("name" to "John Doe"))))
                    .encode()
                    .build()
                    .toUriString()

            uri shouldContain "%255B"
            uri shouldContain "%2520"
        }

        test("raw unencoded output is rejected") {
            val error =
                shouldThrow<IllegalArgumentException> {
                    UriComponentsBuilder.fromUriString("https://api.example.com/products")
                        .queryQs(
                            mapOf("filter" to mapOf("where" to mapOf("name" to "John Doe"))),
                            EncodeOptions(encode = false),
                        )
                }

            error.message shouldBe
                "UriComponentsBuilder.queryQs requires EncodeOptions.encode to be true; finish URI construction with build(true).toUri()."
        }

        test("builder extension returns the same builder instance") {
            val builder = UriComponentsBuilder.fromUriString("https://api.example.com/products")

            val result = builder.queryQs(mapOf("a" to "b"))

            result shouldBeSameInstanceAs builder
        }
    })
