package io.github.techouse.qskotlin.unit

import io.github.techouse.qskotlin.decode
import io.github.techouse.qskotlin.encode
import io.github.techouse.qskotlin.fixtures.data.WptFormEncodingFixtures
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class WptFormEncodingSpec :
    FreeSpec({
        val fixtures = WptFormEncodingFixtures.bundle

        "fixture provenance is documented" {
            fixtures.provenance.sourceFiles shouldBe
                listOf(
                    "wpt/url/urlencoded-parser.any.js",
                    "wpt/url/urlsearchparams-stringifier.any.js",
                )
            fixtures.provenance.note shouldContain "qs-kotlin"
            fixtures.provenance.note shouldContain "WHATWG URLSearchParams behavior"
        }

        "decode fixtures" -
            {
                fixtures.decode.forEach { fixture ->
                    fixture.name {
                        if (fixture.skipReason != null) {
                            fixture.skipReason.isNotBlank() shouldBe true
                        } else {
                            withClue("input=${fixture.input}, options=${fixture.options}") {
                                decode(fixture.input, fixture.options.toDecodeOptions()) shouldBe
                                    requireNotNull(fixture.expected)
                            }
                        }
                    }
                }
            }

        "encode fixtures" -
            {
                fixtures.encode.forEach { fixture ->
                    fixture.name {
                        if (fixture.skipReason != null) {
                            fixture.skipReason.isNotBlank() shouldBe true
                        } else {
                            withClue("input=${fixture.input}, options=${fixture.options}") {
                                encode(fixture.input, fixture.options.toEncodeOptions()) shouldBe
                                    requireNotNull(fixture.expected)
                            }
                        }
                    }
                }
            }
    })
