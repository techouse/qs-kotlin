package io.github.techouse.qskotlin.unit.models

import io.github.techouse.qskotlin.models.Delimiter
import io.github.techouse.qskotlin.models.RegexDelimiter
import io.github.techouse.qskotlin.models.StringDelimiter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.regex.Pattern

class DelimiterSpec :
    DescribeSpec({
        describe("Delimiter factories") {
            it("string factory splits using literal separator") {
                val d = Delimiter.string("&")
                d.shouldBeInstanceOf<StringDelimiter>().value shouldBe "&"
                d.split("a=b&c=d&").filter { it.isNotEmpty() } shouldBe listOf("a=b", "c=d")
            }

            it("regex factories preserve patterns and flags") {
                val compiled = Pattern.compile("[;&]", Pattern.CASE_INSENSITIVE)
                val fromPattern = Delimiter.regex(compiled)
                val fromString = Delimiter.regex("[;&]")
                val fromFlags = Delimiter.regex("[;&]", Pattern.CASE_INSENSITIVE)
                val fromOptions = Delimiter.regex("[;&]", setOf(RegexOption.DOT_MATCHES_ALL))
                val fromKOptions =
                    Delimiter.regex("[;&]", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
                val fromEmptyOptions = Delimiter.regex("[;&]", emptySet())

                fromPattern.shouldBeInstanceOf<RegexDelimiter>().apply {
                    val source = this.pattern
                    source.contains(';') shouldBe true
                    source.contains('&') shouldBe true
                    flags shouldBe Pattern.CASE_INSENSITIVE
                }
                fromString.shouldBeInstanceOf<RegexDelimiter>().apply {
                    val source = this.pattern
                    source.contains(';') shouldBe true
                    source.contains('&') shouldBe true
                    flags shouldBe 0
                }
                fromFlags.shouldBeInstanceOf<RegexDelimiter>().flags shouldBe
                    Pattern.CASE_INSENSITIVE
                fromOptions.shouldBeInstanceOf<RegexDelimiter>().flags shouldBe
                    RegexOption.DOT_MATCHES_ALL.value
                fromKOptions.shouldBeInstanceOf<RegexDelimiter>().apply {
                    flags and RegexOption.IGNORE_CASE.value shouldBe RegexOption.IGNORE_CASE.value
                    flags and RegexOption.MULTILINE.value shouldBe RegexOption.MULTILINE.value
                }
                fromEmptyOptions.shouldBeInstanceOf<RegexDelimiter>().flags shouldBe 0

                Delimiter.regex(pattern = "[;&]", options = setOf(RegexOption.LITERAL))
                    .shouldBeInstanceOf<RegexDelimiter>()

                fromFlags.split("a=b;c=d").filter { it.isNotEmpty() } shouldBe listOf("a=b", "c=d")
            }

            it("predefined delimiters use literal separators") {
                Delimiter.AMPERSAND.split("a=b&c=d").filter { it.isNotEmpty() } shouldBe
                    listOf("a=b", "c=d")
                Delimiter.COMMA.split("a=b,c=d").filter { it.isNotEmpty() } shouldBe
                    listOf("a=b", "c=d")
                Delimiter.SEMICOLON.split("a=b;c=d").filter { it.isNotEmpty() } shouldBe
                    listOf("a=b", "c=d")
            }

            it("enforces non-empty literal delimiter") {
                shouldThrow<IllegalArgumentException> { StringDelimiter("") }
            }

            it("regex delimiter equality handles non delimiter instances") {
                val delimiter = RegexDelimiter("[;&]")
                delimiter.equals("[;&]") shouldBe false
            }
        }
    })
