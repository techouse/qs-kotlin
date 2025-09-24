package io.github.techouse.qskotlin.unit

import io.github.techouse.qskotlin.enums.Sentinel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SentinelSpec :
    FunSpec({
        test("sentinel encoded values are exposed") {
            Sentinel.ISO.asQueryParam() shouldBe Sentinel.ISO.toString()
            Sentinel.CHARSET.toEntry().key shouldBe Sentinel.PARAM_NAME
        }
    })
