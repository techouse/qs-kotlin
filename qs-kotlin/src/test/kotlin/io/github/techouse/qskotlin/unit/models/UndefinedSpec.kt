package io.github.techouse.qskotlin.unit.models

import io.github.techouse.qskotlin.models.Undefined
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class UndefinedSpec :
    DescribeSpec({
        describe("Undefined") {
            it("copy with no modifications") {
                val undefined = Undefined()
                val newUndefined = Undefined()

                newUndefined shouldBe undefined
            }
        }
    })
