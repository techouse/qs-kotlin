package io.github.techouse.qskotlin.unit.internal

import io.github.techouse.qskotlin.enums.Format
import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.internal.Encoder
import io.github.techouse.qskotlin.models.IterableFilter
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.nio.charset.StandardCharsets
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
        }
    })

private class Plain(private val value: String) {
    override fun toString(): String = value
}
