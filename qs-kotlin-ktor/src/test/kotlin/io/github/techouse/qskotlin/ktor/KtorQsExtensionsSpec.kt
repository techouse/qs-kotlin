package io.github.techouse.qskotlin.ktor

import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.Delimiter
import io.github.techouse.qskotlin.models.EncodeOptions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication

class KtorQsExtensionsSpec :
    FunSpec({
        test("null value returns the same URLBuilder unchanged") {
            val original = Url("https://api.example.com/products")
            val builder = URLBuilder(original)

            val result = builder.appendQsQueryParameters(null)

            result shouldBeSameInstanceAs builder
            result.build().toString() shouldBe original.toString()
        }

        test("empty encode result returns the same URLBuilder unchanged") {
            val original = Url("https://api.example.com/products")
            val builder = URLBuilder(original)

            val result = builder.appendQsQueryParameters(emptyMap<String, Any?>())

            result shouldBeSameInstanceAs builder
            result.build().toString() shouldBe original.toString()
        }

        test("simple map serializes into query parameters") {
            val url =
                URLBuilder("https://api.example.com/products")
                    .appendQsQueryParameters(mapOf("q" to "kotlin", "page" to 2))
                    .build()

            url.toString() shouldBe "https://api.example.com/products?q=kotlin&page=2"
            url.parameters["q"] shouldBe "kotlin"
            url.parameters["page"] shouldBe "2"
        }

        test("nested map serializes into qs-style bracket notation") {
            val url =
                URLBuilder("https://api.example.com/products")
                    .appendQsQueryParameters(
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
                URLBuilder("https://api.example.com/products")
                    .appendQsQueryParameters(mapOf("tags" to listOf("a", "b")))
                    .build()

            url.encodedQuery shouldBe "tags%5B0%5D=a&tags%5B1%5D=b"
        }

        test("list of complex objects uses structured qs-style keys") {
            val url =
                URLBuilder("https://api.example.com/products")
                    .appendQsQueryParameters(
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
                URLBuilder("https://api.example.com/products?existing=1")
                    .appendQsQueryParameters(mapOf("filter" to mapOf("name" to "John")))
                    .build()

            url.encodedQuery shouldBe "existing=1&filter%5Bname%5D=John"
        }

        test("duplicate keys are preserved") {
            val url =
                URLBuilder("https://api.example.com/products")
                    .appendQsQueryParameters(
                        mapOf("tag" to listOf("a", "b")),
                        EncodeOptions(listFormat = ListFormat.REPEAT),
                    )
                    .build()

            url.encodedQuery shouldBe "tag=a&tag=b"
            url.parameters.getAll("tag") shouldBe listOf("a", "b")
        }

        test("empty values are preserved") {
            val url =
                URLBuilder("https://api.example.com/products")
                    .appendQsQueryParameters(mapOf("foo" to ""))
                    .build()

            url.encodedQuery shouldBe "foo="
            url.parameters["foo"] shouldBe ""
        }

        test("name-only values are preserved") {
            val url =
                URLBuilder("https://api.example.com/products")
                    .appendQsQueryParameters(
                        mapOf("foo" to null),
                        EncodeOptions(strictNullHandling = true),
                    )
                    .build()

            url.encodedQuery shouldBe "foo"
        }

        test("generated query prefix and custom delimiter are handled") {
            val url =
                URLBuilder("https://api.example.com/products")
                    .appendQsQueryParameters(
                        mapOf("a" to "b", "c" to "d"),
                        EncodeOptions(addQueryPrefix = true, delimiter = Delimiter.SEMICOLON),
                    )
                    .build()

            url.encodedQuery shouldBe "a=b&c=d"
        }

        test("leading question mark in raw key is preserved when query prefix is disabled") {
            val url =
                URLBuilder("https://api.example.com/products")
                    .appendQsQueryParameters(mapOf("?foo" to "bar"), EncodeOptions(encode = false))
                    .build()

            url.encodedQuery shouldBe "?foo=bar"
            url.parameters["?foo"] shouldBe "bar"
        }

        test("encoded qs output is not double-encoded") {
            val url =
                URLBuilder("https://api.example.com/products")
                    .appendQsQueryParameters(mapOf("a" to mapOf("b" to "c")))
                    .build()

            url.toString() shouldBe "https://api.example.com/products?a%5Bb%5D=c"
            url.encodedQuery shouldBe "a%5Bb%5D=c"
        }

        test("URLBuilder extension returns the same builder instance") {
            val builder = URLBuilder("https://api.example.com/products")

            val result = builder.appendQsQueryParameters(mapOf("a" to "b"))

            result shouldBeSameInstanceAs builder
        }

        test("Url extension returns a new URL and leaves the original unchanged") {
            val original = Url("https://api.example.com/products?existing=1")

            val updated = original.appendQsQueryParameters(mapOf("a" to "b"))

            updated shouldNotBeSameInstanceAs original
            original.toString() shouldBe "https://api.example.com/products?existing=1"
            updated.toString() shouldBe "https://api.example.com/products?existing=1&a=b"
        }

        test("server helper parses raw query string") {
            testApplication {
                application {
                    routing {
                        get("/products") {
                            call.request.parseQsQuery() shouldBe
                                mapOf("filter" to mapOf("name" to "John Doe"))
                            call.respondText("ok")
                        }
                    }
                }

                client.get("/products?filter%5Bname%5D=John%20Doe").bodyAsText() shouldBe "ok"
            }
        }

        test("server helper parses nested bracket notation") {
            testApplication {
                application {
                    routing {
                        get("/products") {
                            call.request.parseQsQuery() shouldBe
                                mapOf("filter" to mapOf("age" to mapOf("gte" to "30")))
                            call.respondText("ok")
                        }
                    }
                }

                client.get("/products?filter%5Bage%5D%5Bgte%5D=30").bodyAsText() shouldBe "ok"
            }
        }

        test("server helper handles encoded brackets and values") {
            testApplication {
                application {
                    routing {
                        get("/products") {
                            call.request.parseQsQuery() shouldBe
                                mapOf("a" to mapOf("b" to "c+d"), "name" to "John Doe")
                            call.respondText("ok")
                        }
                    }
                }

                client.get("/products?a%5Bb%5D=c%2Bd&name=John+Doe").bodyAsText() shouldBe "ok"
            }
        }

        test("server helper parses empty query string consistently with qs-kotlin") {
            testApplication {
                application {
                    routing {
                        get("/products") {
                            call.request.parseQsQuery() shouldBe emptyMap<String, Any?>()
                            call.respondText("ok")
                        }
                    }
                }

                client.get("/products").bodyAsText() shouldBe "ok"
            }
        }

        test("server helper handles duplicate keys according to qs-kotlin options") {
            testApplication {
                application {
                    routing {
                        get("/products") {
                            call.request.parseQsQuery() shouldBe mapOf("tag" to listOf("a", "b"))
                            call.request.parseQsQuery(
                                DecodeOptions(strictNullHandling = true)
                            ) shouldBe mapOf("tag" to listOf("a", "b"))
                            call.respondText("ok")
                        }
                    }
                }

                client.get("/products?tag=a&tag=b").bodyAsText() shouldBe "ok"
            }
        }
    })
