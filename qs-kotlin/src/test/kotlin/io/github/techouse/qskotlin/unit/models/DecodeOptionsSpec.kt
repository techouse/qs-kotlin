package io.github.techouse.qskotlin.unit.models

import io.github.techouse.qskotlin.enums.Duplicates
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.RegexDelimiter
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.nio.charset.StandardCharsets

class DecodeOptionsSpec :
    DescribeSpec({
        describe("DecodeOptions") {
            it("copyWith no modifications") {
                val options =
                    DecodeOptions(
                        allowDots = true,
                        allowEmptyLists = true,
                        listLimit = 20,
                        charset = StandardCharsets.UTF_8,
                        charsetSentinel = true,
                        comma = true,
                        delimiter = RegexDelimiter("&"),
                        depth = 20,
                        duplicates = Duplicates.LAST,
                        ignoreQueryPrefix = true,
                        interpretNumericEntities = true,
                        parameterLimit = 200,
                        parseLists = true,
                        strictNullHandling = true,
                    )

                val newOptions = options.copy()

                newOptions.getAllowDots shouldBe true
                newOptions.allowEmptyLists shouldBe true
                newOptions.listLimit shouldBe 20
                newOptions.charset shouldBe StandardCharsets.UTF_8
                newOptions.charsetSentinel shouldBe true
                newOptions.comma shouldBe true
                newOptions.delimiter shouldBe RegexDelimiter("&")
                newOptions.depth shouldBe 20
                newOptions.duplicates shouldBe Duplicates.LAST
                newOptions.ignoreQueryPrefix shouldBe true
                newOptions.interpretNumericEntities shouldBe true
                newOptions.parameterLimit shouldBe 200
                newOptions.parseLists shouldBe true
                newOptions.strictNullHandling shouldBe true
                newOptions shouldBe options
            }

            it("copyWith modifications") {
                val options =
                    DecodeOptions(
                        allowDots = true,
                        allowEmptyLists = true,
                        listLimit = 10,
                        charset = StandardCharsets.ISO_8859_1,
                        charsetSentinel = true,
                        comma = true,
                        delimiter = RegexDelimiter(","),
                        depth = 10,
                        duplicates = Duplicates.COMBINE,
                        ignoreQueryPrefix = true,
                        interpretNumericEntities = true,
                        parameterLimit = 100,
                        parseLists = false,
                        strictNullHandling = true,
                    )

                val newOptions =
                    options.copy(
                        allowDots = false,
                        allowEmptyLists = false,
                        listLimit = 20,
                        charset = StandardCharsets.UTF_8,
                        charsetSentinel = false,
                        comma = false,
                        delimiter = RegexDelimiter("&"),
                        depth = 20,
                        duplicates = Duplicates.LAST,
                        ignoreQueryPrefix = false,
                        interpretNumericEntities = false,
                        parameterLimit = 200,
                        parseLists = true,
                        strictNullHandling = false,
                    )

                newOptions.getAllowDots shouldBe false
                newOptions.allowEmptyLists shouldBe false
                newOptions.listLimit shouldBe 20
                newOptions.charset shouldBe StandardCharsets.UTF_8
                newOptions.charsetSentinel shouldBe false
                newOptions.comma shouldBe false
                newOptions.delimiter shouldBe RegexDelimiter("&")
                newOptions.depth shouldBe 20
                newOptions.duplicates shouldBe Duplicates.LAST
                newOptions.ignoreQueryPrefix shouldBe false
                newOptions.interpretNumericEntities shouldBe false
                newOptions.parameterLimit shouldBe 200
                newOptions.parseLists shouldBe true
                newOptions.strictNullHandling shouldBe false
            }
        }
    })
