package io.github.techouse.qskotlin.unit.internal

import io.github.techouse.qskotlin.enums.Format
import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.internal.Encoder
import io.github.techouse.qskotlin.models.IterableFilter
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.nio.charset.StandardCharsets

class EncoderInternalSpec :
    DescribeSpec({
        describe("Encoder.encode internals") {
            it("applies defaults when optional parameters are null") {
                val calls = mutableListOf<String>()
                val result =
                    Encoder.encode(
                        data = listOf("v1"),
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

                result.shouldBeInstanceOf<List<*>>()
                result shouldBe listOf("enc:?[0]=enc:v1")
                calls shouldBe listOf("?[0]", "v1")
            }

            it("ignores out-of-range array indices exposed by IterableFilter") {
                val result =
                    Encoder.encode(
                        data = listOf("only"),
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

                result.shouldBeInstanceOf<List<*>>()
                result.shouldBeEmpty()
            }
        }
    })
