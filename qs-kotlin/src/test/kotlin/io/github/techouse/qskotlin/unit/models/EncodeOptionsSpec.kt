package io.github.techouse.qskotlin.unit.models

import io.github.techouse.qskotlin.enums.Format
import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.models.Delimiter
import io.github.techouse.qskotlin.models.EncodeOptions
import io.github.techouse.qskotlin.models.FunctionFilter
import io.github.techouse.qskotlin.models.StringDelimiter
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.nio.charset.StandardCharsets

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
                        filter = FunctionFilter { _: String, map: Any? -> emptyMap<String, Any?>() },
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
            }
        }
    })
