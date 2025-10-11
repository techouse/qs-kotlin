package io.github.techouse.qskotlin.unit

import io.github.techouse.qskotlin.fixtures.data.EndToEndTestCases
import io.github.techouse.qskotlin.models.EncodeOptions
import io.github.techouse.qskotlin.toQueryMap
import io.github.techouse.qskotlin.toQueryString
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ExtensionsSpec :
    FunSpec({
        context("String.toQueryMap") {
            EndToEndTestCases.forEach { testCase ->
                test("should decode: ${testCase.encoded}") {
                    testCase.encoded.toQueryMap() shouldBe testCase.data
                }
            }
        }

        context("Map.toQueryString") {
            EndToEndTestCases.forEach { testCase ->
                test("should encode: ${testCase.data}") {
                    testCase.data.toQueryString(EncodeOptions(encode = false)) shouldBe
                        testCase.encoded
                }
            }

            test("uses default EncodeOptions overload when omitted") {
                mapOf("a" to "b").toQueryString() shouldBe "a=b"
            }
        }
    })
