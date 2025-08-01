package io.github.techouse.qskotlin.e2e

import io.github.techouse.qskotlin.QS
import io.github.techouse.qskotlin.fixtures.data.EndToEndTestCases
import io.github.techouse.qskotlin.models.EncodeOptions
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class EndToEndTest :
    FreeSpec({
        "e2e tests" -
            {
                EndToEndTestCases.forEach { testCase ->
                    "${testCase.data} <-> ${testCase.encoded}" {
                        QS.encode(testCase.data, EncodeOptions(encode = false)) shouldBe
                            testCase.encoded
                        QS.decode(testCase.encoded) shouldBe testCase.data
                    }
                }
            }
    })
