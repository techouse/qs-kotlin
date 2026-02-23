package io.github.techouse.qskotlin.unit.internal

import io.github.techouse.qskotlin.enums.Format
import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.internal.Encoder
import io.github.techouse.qskotlin.models.FunctionFilter
import io.github.techouse.qskotlin.models.IterableFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime

class EncoderInternalSpec :
    DescribeSpec({
        describe("Encoder.encode internals") {
            it("applies defaults when optional parameters are null") {
                val calls = mutableListOf<String>()
                val data = arrayOf("v1")
                val result =
                    Encoder.encode(
                        data = data,
                        undefined = false,
                        sideChannel = mutableMapOf(),
                        prefix = null,
                        generateArrayPrefix = null,
                        commaRoundTrip = null,
                        encoder = { value, _, _ ->
                            val text = value?.toString() ?: "<null>"
                            calls += text
                            "enc:$text"
                        },
                        format = Format.RFC3986,
                        formatter = Format.RFC3986.formatter,
                        charset = StandardCharsets.UTF_8,
                        addQueryPrefix = true,
                    )

                result.shouldBeInstanceOf<String>()
                result shouldBe "enc:?=enc:$data"
                calls shouldBe listOf("?", data.toString())
            }

            it("encodes arrays using indices generator") {
                val data = arrayOf("v0", "v1")
                val result =
                    Encoder.encode(
                        data = data,
                        undefined = false,
                        sideChannel = mutableMapOf(),
                        prefix = "arr",
                        generateArrayPrefix = ListFormat.INDICES.generator,
                        encoder = { value, _, _ -> value?.toString() ?: "" },
                        format = Format.RFC3986,
                        formatter = Format.RFC3986.formatter,
                        charset = StandardCharsets.UTF_8,
                    )

                result.shouldBeInstanceOf<String>()
                result.startsWith("arr=") shouldBe true
            }

            it("skips out-of-range array indices exposed by IterableFilter") {
                val result =
                    Encoder.encode(
                        data = arrayOf("only"),
                        undefined = false,
                        sideChannel = mutableMapOf(),
                        prefix = "arr",
                        generateArrayPrefix = ListFormat.INDICES.generator,
                        filter = IterableFilter(listOf(2)),
                        encoder = { value, _, _ -> value?.toString() ?: "" },
                        format = Format.RFC3986,
                        formatter = Format.RFC3986.formatter,
                        charset = StandardCharsets.UTF_8,
                    )

                result.shouldBeInstanceOf<String>()
                result.startsWith("arr=") shouldBe true
            }

            it("handles unsupported object types by skipping values") {
                val result =
                    Encoder.encode(
                        data = Plain("value"),
                        undefined = false,
                        sideChannel = mutableMapOf(),
                        prefix = "plain",
                        generateArrayPrefix = ListFormat.INDICES.generator,
                        filter = IterableFilter(listOf("prop")),
                        encoder = { value, _, _ -> value?.toString() ?: "" },
                        format = Format.RFC3986,
                        formatter = Format.RFC3986.formatter,
                        charset = StandardCharsets.UTF_8,
                    )

                result.shouldBeInstanceOf<String>()
                result shouldBe "plain=value"
            }

            it("serializes LocalDateTime values with custom serializer") {
                val stamp = LocalDateTime.parse("2024-05-01T10:15:00")
                val result =
                    Encoder.encode(
                        data = stamp,
                        undefined = false,
                        sideChannel = mutableMapOf(),
                        prefix = "ts",
                        serializeDate = { dt -> "X${dt.toLocalDate()}" },
                        encoder = { value, _, _ -> value?.toString() ?: "" },
                        format = Format.RFC3986,
                        formatter = Format.RFC3986.formatter,
                        charset = StandardCharsets.UTF_8,
                    )

                result.shouldBeInstanceOf<String>()
                result shouldBe "ts=X2024-05-01"
            }

            it("pre-encodes comma lists when encodeValuesOnly is true") {
                val seenValues = mutableListOf<String>()
                val result =
                    Encoder.encode(
                        data = listOf("alpha"),
                        undefined = false,
                        sideChannel = mutableMapOf(),
                        prefix = "tags",
                        generateArrayPrefix = ListFormat.COMMA.generator,
                        encodeValuesOnly = true,
                        encoder = { value, _, _ ->
                            val text = value?.toString() ?: ""
                            seenValues += text
                            "enc:$text"
                        },
                    )

                seenValues shouldBe listOf("alpha")
                result shouldBe listOf("tags[]=enc:alpha")
            }

            it("uses iterable snapshot for non-list COMMA iterables") {
                val data =
                    object : Iterable<String> {
                        override fun iterator(): Iterator<String> = listOf("a", "b").iterator()
                    }

                val result =
                    Encoder.encode(
                        data = data,
                        undefined = false,
                        sideChannel = mutableMapOf(),
                        prefix = "tags",
                        generateArrayPrefix = ListFormat.COMMA.generator,
                        encoder = { value, _, _ -> value?.toString() ?: "" },
                        formatter = { value -> value },
                    )

                result shouldBe listOf("tags=a,b")
            }

            it("handles nullable COMMA items when encodeValuesOnly pre-encodes list values") {
                val seenValues = mutableListOf<String>()
                val data =
                    object : Iterable<String?> {
                        override fun iterator(): Iterator<String?> = listOf("a", null).iterator()
                    }

                val result =
                    Encoder.encode(
                        data = data,
                        undefined = false,
                        sideChannel = mutableMapOf(),
                        prefix = "tags",
                        generateArrayPrefix = ListFormat.COMMA.generator,
                        encodeValuesOnly = true,
                        encoder = { value, _, _ ->
                            value?.toString()?.also { seenValues += it }?.let { "enc:$it" } ?: ""
                        },
                        formatter = { value -> value },
                    )

                seenValues shouldBe listOf("a")
                result shouldBe listOf("tags=enc:a,")
            }

            it("coerces binary values inside COMMA joins without custom encoder") {
                val result =
                    Encoder.encode(
                        data =
                            listOf(
                                "A".toByteArray(StandardCharsets.UTF_8),
                                ByteBuffer.wrap("B".toByteArray(StandardCharsets.UTF_8)),
                                null,
                                "C",
                            ),
                        undefined = false,
                        sideChannel = mutableMapOf(),
                        prefix = "bytes",
                        generateArrayPrefix = ListFormat.COMMA.generator,
                        formatter = { value -> value },
                    )

                result shouldBe listOf("bytes=A,B,,C")
            }

            it("supports custom list prefix generators for iterable children") {
                val customGenerator = { prefix: String, key: String? -> "$prefix<$key>" }

                val result =
                    Encoder.encode(
                        data = listOf("x"),
                        undefined = false,
                        sideChannel = mutableMapOf(),
                        prefix = "arr",
                        generateArrayPrefix = customGenerator,
                        encoder = { value, _, _ -> value?.toString() ?: "" },
                        formatter = { value -> value },
                    )

                result shouldBe listOf("arr<0>=x")
            }

            it("returns empty suffix when allowEmptyLists enabled for empty iterable") {
                val result =
                    Encoder.encode(
                        data = emptyList<Any?>(),
                        undefined = false,
                        sideChannel = mutableMapOf(),
                        prefix = "items",
                        allowEmptyLists = true,
                    )

                result shouldBe "items[]"
            }

            it(
                "returns empty suffix for empty non-collection iterable when allowEmptyLists enabled"
            ) {
                val result =
                    Encoder.encode(
                        data =
                            object : Iterable<String> {
                                override fun iterator(): Iterator<String> =
                                    emptyList<String>().iterator()
                            },
                        undefined = false,
                        sideChannel = mutableMapOf(),
                        prefix = "items",
                        allowEmptyLists = true,
                    )

                result shouldBe "items[]"
            }

            it("uses iterableList size for commaRoundTrip with non-collection iterables") {
                val result =
                    Encoder.encode(
                        data =
                            object : Iterable<String> {
                                override fun iterator(): Iterator<String> =
                                    listOf("solo").iterator()
                            },
                        undefined = false,
                        sideChannel = mutableMapOf(),
                        prefix = "items",
                        generateArrayPrefix = ListFormat.INDICES.generator,
                        commaRoundTrip = true,
                        encoder = { value, _, _ -> value?.toString() ?: "" },
                        formatter = { v -> v },
                    )

                result shouldBe listOf("items[][0]=solo")
            }

            it("stringifies temporal comma lists when no serializer supplied") {
                val instant = Instant.parse("2020-01-01T00:00:00Z")
                val date = LocalDateTime.parse("2020-01-01T00:00:00")

                val result =
                    Encoder.encode(
                        data = listOf(instant, date),
                        undefined = false,
                        sideChannel = mutableMapOf(),
                        prefix = "ts",
                        generateArrayPrefix = ListFormat.COMMA.generator,
                        encoder = { value, _, _ -> value?.toString() ?: "" },
                    )

                result shouldBe listOf("ts=2020-01-01T00:00:00Z,2020-01-01T00:00")
            }

            it("serializes LocalDateTime with default ISO formatting when serializer null") {
                val stamp = LocalDateTime.parse("2024-01-02T03:04:05")

                val result =
                    Encoder.encode(
                        data = stamp,
                        undefined = false,
                        sideChannel = mutableMapOf(),
                        prefix = "ts",
                    )

                result shouldBe "ts=2024-01-02T03:04:05"
            }

            it("propagates undefined flag by returning empty mutable list") {
                val result =
                    Encoder.encode(
                        data = null,
                        undefined = true,
                        sideChannel = mutableMapOf(),
                        prefix = "ignored",
                    )

                result.shouldBeInstanceOf<MutableList<*>>().isEmpty() shouldBe true
            }

            it("detects cyclic references and throws") {
                val cycle = mutableMapOf<String, Any?>()
                cycle["self"] = cycle

                shouldThrow<IndexOutOfBoundsException> {
                        Encoder.encode(
                            data = cycle,
                            undefined = false,
                            sideChannel = mutableMapOf(),
                            prefix = "self",
                        )
                    }
                    .message shouldBe "Cyclic object value"
            }

            it("detects cycles introduced by filter") {
                val root = mutableMapOf<String, Any?>()
                root["a"] = mutableMapOf("b" to "c")

                val filter = FunctionFilter { prefix, value ->
                    if (prefix.contains("a")) root else value
                }

                shouldThrow<IndexOutOfBoundsException> {
                        Encoder.encode(
                            data = root,
                            undefined = false,
                            sideChannel = mutableMapOf(),
                            prefix = "root",
                            filter = filter,
                        )
                    }
                    .message shouldBe "Cyclic object value"
            }
        }
    })

private class Plain(private val value: String) {
    override fun toString(): String = value
}
