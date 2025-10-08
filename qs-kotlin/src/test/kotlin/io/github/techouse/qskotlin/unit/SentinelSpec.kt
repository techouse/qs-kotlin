package io.github.techouse.qskotlin.unit

import io.github.techouse.qskotlin.enums.Sentinel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SentinelSpec :
    FunSpec({
        test("sentinel encoded values are exposed") {
            Sentinel.ISO.asQueryParam() shouldBe Sentinel.ISO.toString()
            Sentinel.ISO.encoded shouldBe "utf8=%26%2310003%3B"
            Sentinel.CHARSET.toEntry().key shouldBe Sentinel.PARAM_NAME
            Sentinel.CHARSET.encoded shouldBe "utf8=%E2%9C%93"
        }
    })
