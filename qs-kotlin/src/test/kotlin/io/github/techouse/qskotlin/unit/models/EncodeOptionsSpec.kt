package io.github.techouse.qskotlin.unit.models

import io.github.techouse.qskotlin.enums.Format
import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.models.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

class EncodeOptionsSpec :
    DescribeSpec({
        describe("EncodeOptions") {
            it("copyWith no modifications") {
                val options =
                    EncodeOptions(
                        addQueryPrefix = true,
                        allowDots = true,
                        allowEmptyLists = true,
                        listFormat = ListFormat.INDICES,
                        charset = StandardCharsets.ISO_8859_1,
                        charsetSentinel = true,
                        delimiter = Delimiter.COMMA,
                        encode = true,
                        encodeDotInKeys = true,
                        encodeValuesOnly = true,
                        format = Format.RFC1738,
                        skipNulls = true,
                        strictNullHandling = true,
                        commaRoundTrip = true,
                        commaCompactNulls = true,
                    )

                val newOptions = options.copy()

                newOptions.addQueryPrefix shouldBe true
                newOptions.getAllowDots shouldBe true
                newOptions.allowEmptyLists shouldBe true
                newOptions.getListFormat shouldBe ListFormat.INDICES
                newOptions.charset shouldBe StandardCharsets.ISO_8859_1
                newOptions.charsetSentinel shouldBe true
                newOptions.delimiter shouldBe Delimiter.COMMA
                newOptions.encode shouldBe true
                newOptions.encodeDotInKeys shouldBe true
                newOptions.encodeValuesOnly shouldBe true
                newOptions.format shouldBe Format.RFC1738
                newOptions.skipNulls shouldBe true
                newOptions.strictNullHandling shouldBe true
                newOptions.commaRoundTrip shouldBe true
                newOptions.commaCompactNulls shouldBe true
                newOptions shouldBe options
            }

            it("copyWith modifications") {
                val options =
                    EncodeOptions(
                        addQueryPrefix = true,
                        allowDots = true,
                        allowEmptyLists = true,
                        listFormat = ListFormat.INDICES,
                        charset = StandardCharsets.ISO_8859_1,
                        charsetSentinel = true,
                        delimiter = Delimiter.COMMA,
                        encode = true,
                        encodeDotInKeys = true,
                        encodeValuesOnly = true,
                        format = Format.RFC1738,
                        skipNulls = true,
                        strictNullHandling = true,
                        commaRoundTrip = true,
                        commaCompactNulls = true,
                    )

                val newOptions =
                    options.copy(
                        addQueryPrefix = false,
                        allowDots = false,
                        allowEmptyLists = false,
                        listFormat = ListFormat.BRACKETS,
                        charset = StandardCharsets.UTF_8,
                        charsetSentinel = false,
                        delimiter = StringDelimiter("&"),
                        encode = false,
                        encodeDotInKeys = false,
                        encodeValuesOnly = false,
                        format = Format.RFC3986,
                        skipNulls = false,
                        strictNullHandling = false,
                        commaRoundTrip = false,
                        commaCompactNulls = false,
                        filter = FunctionFilter { _: String, _: Any? -> emptyMap<String, Any?>() },
                    )

                newOptions.addQueryPrefix shouldBe false
                newOptions.getAllowDots shouldBe false
                newOptions.allowEmptyLists shouldBe false
                newOptions.getListFormat shouldBe ListFormat.BRACKETS
                newOptions.charset shouldBe StandardCharsets.UTF_8
                newOptions.charsetSentinel shouldBe false
                newOptions.delimiter shouldBe StringDelimiter("&")
                newOptions.encode shouldBe false
                newOptions.encodeDotInKeys shouldBe false
                newOptions.encodeValuesOnly shouldBe false
                newOptions.format shouldBe Format.RFC3986
                newOptions.skipNulls shouldBe false
                newOptions.strictNullHandling shouldBe false
                newOptions.commaRoundTrip shouldBe false
                newOptions.commaCompactNulls shouldBe false
            }

            it("builder produces java-friendly configuration") {
                val calls = mutableListOf<String>()
                val options =
                    @Suppress("DEPRECATION")
                    EncodeOptions.builder()
                        .encoder(
                            JValueEncoder { value, charset, format ->
                                calls += "encoder"
                                "${charset?.displayName()}-${format?.name}-${value ?: ""}"
                            }
                        )
                        .dateSerializer(
                            JDateSerializer {
                                calls += "dateSerializer"
                                "serialized-${it.toLocalDate()}"
                            }
                        )
                        .listFormat(ListFormat.BRACKETS)
                        .indices(true)
                        .allowDots(true)
                        .addQueryPrefix(true)
                        .allowEmptyLists(true)
                        .charset(StandardCharsets.ISO_8859_1)
                        .charsetSentinel(true)
                        .delimiter(";")
                        .delimiter(Delimiter.COMMA)
                        .encode(false)
                        .encodeDotInKeys(true)
                        .encodeValuesOnly(true)
                        .format(Format.RFC1738)
                        .filter(FunctionFilter { key, value -> "$key=$value" })
                        .skipNulls(true)
                        .strictNullHandling(true)
                        .commaRoundTrip(true)
                        .commaCompactNulls(true)
                        .sort { a, b -> (a.toString()).compareTo(b.toString()) }
                        .build()

                val now = LocalDateTime.parse("2024-01-01T00:00:00")

                options.addQueryPrefix shouldBe true
                options.allowEmptyLists shouldBe true
                options.charset shouldBe StandardCharsets.ISO_8859_1
                options.charsetSentinel shouldBe true
                options.delimiter shouldBe Delimiter.COMMA
                options.encode shouldBe false
                options.encodeDotInKeys shouldBe true
                options.encodeValuesOnly shouldBe true
                options.format shouldBe Format.RFC1738
                val filter = options.filter as FunctionFilter
                filter.function("key", 5) shouldBe "key=5"
                options.skipNulls shouldBe true
                options.strictNullHandling shouldBe true
                options.commaRoundTrip shouldBe true
                options.commaCompactNulls shouldBe true
                options.getAllowDots shouldBe true
                options.getListFormat shouldBe ListFormat.BRACKETS
                options.sort!!("b", "a") shouldBe "b".compareTo("a")
                options.getEncoder("value", StandardCharsets.ISO_8859_1, Format.RFC1738) shouldBe
                    "ISO-8859-1-RFC1738-value"
                options.getDateSerializer(now) shouldBe "serialized-2024-01-01"
                calls.contains("encoder") shouldBe true
                calls.contains("dateSerializer") shouldBe true
            }

            it("defaults exposes baseline instance") {
                EncodeOptions.defaults() shouldBe EncodeOptions()
                EncodeOptions.builder().build() shouldBe EncodeOptions()
            }

            it("listFormat falls back to indices when explicit format absent") {
                val options = EncodeOptions(listFormat = null, indices = false)
                options.getListFormat shouldBe ListFormat.REPEAT
            }

            it("custom encoder and serializer override defaults") {
                val options =
                    EncodeOptions(
                        encoder = { value, _, _ -> "K:$value" },
                        dateSerializer = { "D:${it.toLocalDate()}" },
                    )

                options.getEncoder("x") shouldBe "K:x"
                options.getDateSerializer(LocalDateTime.parse("2024-12-31T23:59:59")) shouldBe
                    "D:2024-12-31"
            }

            it("exposes deprecated indices property for compatibility") {
                val options = EncodeOptions(indices = true)
                @Suppress("DEPRECATION")
                options.indices shouldBe true
            }

            it("builder accepts kotlin lambdas for encoder and date serializer") {
                val options =
                    EncodeOptions.builder()
                        .encoder { value, _, _ -> "wrapped-${value ?: ""}" }
                        .dateSerializer { dt -> "d-${dt.toLocalDate()}" }
                        .addQueryPrefix(false)
                        .build()

                options.getEncoder("value") shouldBe "wrapped-value"
                options.getDateSerializer(LocalDateTime.parse("2020-05-01T10:15:00")) shouldBe
                    "d-2020-05-01"
            }

            it("rejects unsupported charsets") {
                val error =
                    shouldThrow<IllegalArgumentException> {
                        EncodeOptions(charset = Charset.forName("US-ASCII"))
                    }
                error.message.shouldContain("Invalid charset")
            }
        }
    })
