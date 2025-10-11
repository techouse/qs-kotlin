package io.github.techouse.qskotlin.unit

import io.github.techouse.qskotlin.models.EncodeOptions
import io.github.techouse.qskotlin.toQueryMap
import io.github.techouse.qskotlin.toQueryString
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ExtensionsSpec :
    DescribeSpec({
        describe("Kotlin extension wrappers") {
            it("String.toQueryMap delegates to decode") {
                "a=1&b=2".toQueryMap() shouldBe mapOf("a" to "1", "b" to "2")
            }

            it("Map.toQueryString delegates to encode") {
                linkedMapOf("a" to 1, "b" to 2)
                    .toQueryString(EncodeOptions(encode = false)) shouldBe "a=1&b=2"
            }
        }
    })
