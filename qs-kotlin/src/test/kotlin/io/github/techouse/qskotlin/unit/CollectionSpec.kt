package io.github.techouse.qskotlin.unit

import io.github.techouse.qskotlin.models.Undefined
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CollectionSpec :
    DescribeSpec({
        describe("mimic Dart's SplayTreeMap behavior in Kotlin with TreeMap") {
            it("indices are ordered in value") {
                sortedMapOf("1" to "a", "0" to "b", "2" to "c").values shouldBe
                    listOf("b", "a", "c")
            }

            it("indices are ordered in value 2") {
                val array = sortedMapOf<String, String>()
                array["1"] = "c"
                array["0"] = "b"
                array["2"] = "d"

                array.values shouldBe listOf("b", "c", "d")
            }
        }

        describe("mimic Dart's List.filled behavior in Kotlin") {
            it("fill with single item") {
                val array = MutableList<String?>(1) { null }
                array[0] = "b"

                array shouldBe listOf("b")
            }

            it("fill with Undefined") {
                val array = MutableList<Any?>(3) { Undefined() }
                array[0] = "a"
                array[2] = "c"

                array shouldBe listOf("a", Undefined(), "c")
            }
        }
    })
