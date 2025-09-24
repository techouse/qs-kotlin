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
                result shouldBe "enc:?=enc:${data.toString()}"
                calls shouldBe listOf("?", data.toString())
            }

            it("ignores out-of-range array indices exposed by IterableFilter") {
                val data = arrayOf("only")
                val result =
                    Encoder.encode(
                        data = data,
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
                result shouldBe "arr=${data.toString()}"
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
        }
    })
