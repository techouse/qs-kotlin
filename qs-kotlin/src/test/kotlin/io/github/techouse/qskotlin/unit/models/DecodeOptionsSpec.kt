@file:Suppress("DEPRECATION")

package io.github.techouse.qskotlin.unit.models

import io.github.techouse.qskotlin.enums.DecodeKind
import io.github.techouse.qskotlin.enums.Duplicates
import io.github.techouse.qskotlin.internal.Utils
import io.github.techouse.qskotlin.models.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

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

        val charsets = listOf(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1)

        describe(
            "DecodeOptions.defaultDecode: KEY protects encoded dots prior to percent-decoding"
        ) {
            it("KEY maps %2E/%2e inside brackets to '.' when allowDots=true (UTF-8/ISO-8859-1)") {
                for (cs in charsets) {
                    val opts = DecodeOptions(allowDots = true)
                    opts.decode("a[%2E]", cs, DecodeKind.KEY) shouldBe "a[.]"
                    opts.decode("a[%2e]", cs, DecodeKind.KEY) shouldBe "a[.]"
                }
            }

            it(
                "KEY maps %2E outside brackets to '.' when allowDots=true; independent of decodeDotInKeys (UTF-8/ISO)"
            ) {
                for (cs in charsets) {
                    val opts1 = DecodeOptions(allowDots = true, decodeDotInKeys = false)
                    val opts2 = DecodeOptions(allowDots = true, decodeDotInKeys = true)
                    opts1.decode("a%2Eb", cs, DecodeKind.KEY) shouldBe "a.b"
                    opts2.decode("a%2Eb", cs, DecodeKind.KEY) shouldBe "a.b"
                }
            }

            it("non-KEY decodes %2E to '.' (control)") {
                for (cs in charsets) {
                    val opts = DecodeOptions(allowDots = true)
                    opts.decode("a%2Eb", cs, DecodeKind.VALUE) shouldBe "a.b"
                }
            }

            it("KEY maps %2E/%2e inside brackets even when allowDots=false") {
                for (cs in charsets) {
                    val opts = DecodeOptions(allowDots = false)
                    opts.decode("a[%2E]", cs, DecodeKind.KEY) shouldBe "a[.]"
                    opts.decode("a[%2e]", cs, DecodeKind.KEY) shouldBe "a[.]"
                }
            }

            it(
                "KEY outside %2E decodes to '.' when allowDots=false (no protection outside brackets)"
            ) {
                for (cs in charsets) {
                    val opts = DecodeOptions(allowDots = false)
                    opts.decode("a%2Eb", cs, DecodeKind.KEY) shouldBe "a.b"
                    opts.decode("a%2eb", cs, DecodeKind.KEY) shouldBe "a.b"
                }
            }
        }

        describe("DecodeOptions: allowDots / decodeDotInKeys interplay") {
            it(
                "decodeDotInKeys=true implies getAllowDots=true when allowDots not explicitly false"
            ) {
                val opts = DecodeOptions(decodeDotInKeys = true)
                opts.getAllowDots shouldBe true
            }
        }

        describe("DecodeOptions: key/value decoding + custom decoder behavior (C# parity)") {
            it("DecodeKey should throw when decodeDotInKeys=true and allowDots=false") {
                // In Kotlin we validate at construction time.
                shouldThrow<IllegalArgumentException> {
                        DecodeOptions(allowDots = false, decodeDotInKeys = true)
                    }
                    .message!!
                    .lowercase()
                    .let { msg ->
                        msg.contains("decodedotinkeys") shouldBe true
                        msg.contains("allowdots") shouldBe true
                    }
            }

            it(
                "DecodeKey decodes percent sequences like values (allowDots=true, decodeDotInKeys=false)"
            ) {
                val opts = DecodeOptions(allowDots = true, decodeDotInKeys = false)
                opts.decode("a%2Eb", StandardCharsets.UTF_8, DecodeKind.KEY) shouldBe "a.b"
                opts.decode("a%2eb", StandardCharsets.UTF_8, DecodeKind.KEY) shouldBe "a.b"
            }

            it("DecodeValue decodes percent sequences normally") {
                val opts = DecodeOptions()
                opts.decode("%2E", StandardCharsets.UTF_8, DecodeKind.VALUE) shouldBe "."
            }

            it("decodeKey/decodeValue return null when input null") {
                val opts = DecodeOptions()
                opts.decodeKey(null) shouldBe null
                opts.decodeValue(null) shouldBe null
            }

            it("Decoder is used for KEY and for VALUE") {
                val calls = mutableListOf<Pair<String?, DecodeKind>>()
                val opts =
                    DecodeOptions(
                        decoder =
                            Decoder { s, _, kind ->
                                calls += (s to (kind ?: DecodeKind.VALUE))
                                s // echo back
                            }
                    )

                opts.decode("x", StandardCharsets.UTF_8, DecodeKind.KEY) shouldBe "x"
                opts.decode("y", StandardCharsets.UTF_8, DecodeKind.VALUE) shouldBe "y"

                calls.size shouldBe 2
                calls[0].second shouldBe DecodeKind.KEY
                calls[0].first shouldBe "x"
                calls[1].second shouldBe DecodeKind.VALUE
                calls[1].first shouldBe "y"
            }

            it("Decoder null return is honored (no fallback to default)") {
                val opts = DecodeOptions(decoder = Decoder { _, _, _ -> null })

                opts.decode("foo", StandardCharsets.UTF_8, DecodeKind.VALUE) shouldBe null
                opts.decode("bar", StandardCharsets.UTF_8, DecodeKind.KEY) shouldBe null
            }

            it("Single decoder acts like 'legacy' when ignoring kind (no default applied first)") {
                // Emulates C# “legacy decoder” that uppercases the raw token without
                // percent-decoding.
                val opts = DecodeOptions(decoder = Decoder { s, _, _ -> s?.uppercase() })

                opts.decode("abc", StandardCharsets.UTF_8, DecodeKind.VALUE) shouldBe "ABC"
                // For keys, custom decoder gets the raw token; no default percent-decoding happens
                // first.
                opts.decode("a%2Eb", StandardCharsets.UTF_8, DecodeKind.KEY) shouldBe "A%2EB"
            }

            it("copy() preserves and overrides the decoder") {
                val original =
                    DecodeOptions(
                        decoder = Decoder { s, _, k -> s?.let { "K:${k ?: DecodeKind.VALUE}:$it" } }
                    )

                // Copy without overrides preserves decoder
                val copy = original.copy()
                copy.decode("v", StandardCharsets.UTF_8, DecodeKind.VALUE) shouldBe "K:VALUE:v"
                copy.decode("k", StandardCharsets.UTF_8, DecodeKind.KEY) shouldBe "K:KEY:k"

                // Override the decoder
                val copy2 =
                    original.copy(
                        decoder =
                            Decoder { s, _, k -> s?.let { "K2:${k ?: DecodeKind.VALUE}:$it" } }
                    )
                copy2.decode("v", StandardCharsets.UTF_8, DecodeKind.VALUE) shouldBe "K2:VALUE:v"
                copy2.decode("k", StandardCharsets.UTF_8, DecodeKind.KEY) shouldBe "K2:KEY:k"
            }

            it("decoder wins over legacyDecoder when both are provided") {
                @Suppress("DEPRECATION") val legacy: LegacyDecoder = { v, _ -> "L:${v ?: "null"}" }
                val dec = Decoder { v, _, k -> "K:${(k ?: DecodeKind.VALUE)}:${v ?: "null"}" }
                val opts = DecodeOptions(decoder = dec, legacyDecoder = legacy)

                opts.decode("x", StandardCharsets.UTF_8, DecodeKind.KEY) shouldBe "K:KEY:x"
                opts.decode("y", StandardCharsets.UTF_8, DecodeKind.VALUE) shouldBe "K:VALUE:y"
            }

            it("decodeKey coerces non-string decoder result via toString") {
                val opts = DecodeOptions(decoder = Decoder { _, _, _ -> 42 })
                opts.decodeKey("anything", StandardCharsets.UTF_8) shouldBe "42"
            }

            it(
                "copy() to an inconsistent combination (allowDots=false with decodeDotInKeys=true) throws"
            ) {
                val original = DecodeOptions(decodeDotInKeys = true)
                shouldThrow<IllegalArgumentException> { original.copy(allowDots = false) }
            }
        }

        describe("DecodeOptions builder and defaults") {
            it("builder wires kotlin and java overloads") {
                @Suppress("DEPRECATION") val legacy: LegacyDecoder = { v, _ -> "L:${v ?: ""}" }
                val jLegacy = JLegacyDecoder { v, _ -> "JL:${v ?: ""}" }
                val options =
                    DecodeOptions.builder()
                        .decoder(Decoder { v, _, k -> "K:${k ?: DecodeKind.VALUE}:${v ?: ""}" })
                        .legacyDecoder(legacy)
                        .decoder(JDecoder { v, _, k -> "J:${k ?: DecodeKind.VALUE}:${v ?: ""}" })
                        .legacyDecoder(jLegacy)
                        .allowDots(true)
                        .decodeDotInKeys(true)
                        .allowEmptyLists(true)
                        .allowSparseLists(true)
                        .listLimit(42)
                        .charset(StandardCharsets.ISO_8859_1)
                        .charsetSentinel(true)
                        .comma(true)
                        .delimiter("&")
                        .delimiter(Delimiter.SEMICOLON)
                        .delimiter(Pattern.compile("[;&]"))
                        .delimiterRegex("\\s*[;&]\\s*", Pattern.CASE_INSENSITIVE)
                        .depth(7)
                        .parameterLimit(123)
                        .duplicates(Duplicates.FIRST)
                        .ignoreQueryPrefix(true)
                        .interpretNumericEntities(true)
                        .parseLists(false)
                        .strictDepth(true)
                        .strictNullHandling(true)
                        .throwOnLimitExceeded(true)
                        .build()

                val delimiter = options.delimiter as RegexDelimiter
                delimiter.pattern shouldBe "\\s*[;&]\\s*"
                delimiter.flags shouldBe Pattern.CASE_INSENSITIVE
                options.getAllowDots shouldBe true
                options.getDecodeDotInKeys shouldBe true
                options.allowEmptyLists shouldBe true
                options.allowSparseLists shouldBe true
                options.listLimit shouldBe 42
                options.charset shouldBe StandardCharsets.ISO_8859_1
                options.charsetSentinel shouldBe true
                options.comma shouldBe true
                options.depth shouldBe 7
                options.parameterLimit shouldBe 123
                options.duplicates shouldBe Duplicates.FIRST
                options.ignoreQueryPrefix shouldBe true
                options.interpretNumericEntities shouldBe true
                options.parseLists shouldBe false
                options.strictDepth shouldBe true
                options.strictNullHandling shouldBe true
                options.throwOnLimitExceeded shouldBe true
                options.decode("token", StandardCharsets.ISO_8859_1, DecodeKind.KEY) shouldBe
                    "J:KEY:token"
            }

            it("defaults exposes baseline instance") {
                DecodeOptions.defaults() shouldBe DecodeOptions()
                DecodeOptions.builder().build() shouldBe DecodeOptions()
            }

            it("java-friendly aliases mirror computed flags") {
                val implied = DecodeOptions(decodeDotInKeys = true)
                implied.isAllowDotsEffective() shouldBe true
                implied.isDecodeDotInKeysEffective() shouldBe true

                val explicit = DecodeOptions(allowDots = false)
                explicit.isAllowDotsEffective() shouldBe false
                explicit.isDecodeDotInKeysEffective() shouldBe false
            }

            it("decode uses defaults when charset and kind omitted") {
                val opts = DecodeOptions()
                opts.decode("a%20b") shouldBe Utils.decode("a%20b")
                opts.decodeKey("foo%20bar") shouldBe "foo bar"
                opts.decodeValue("foo%20bar") shouldBe "foo bar"
            }

            it("rejects unsupported charset") {
                val error =
                    shouldThrow<IllegalArgumentException> {
                        DecodeOptions(charset = Charset.forName("US-ASCII"))
                    }
                error.message.shouldContain("Invalid charset")
            }
        }
    })
