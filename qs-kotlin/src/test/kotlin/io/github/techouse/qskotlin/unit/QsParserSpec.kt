package io.github.techouse.qskotlin.unit

import io.github.techouse.qskotlin.decode
import io.github.techouse.qskotlin.encode
import io.github.techouse.qskotlin.enums.DecodeKind
import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.internal.Utils
import io.github.techouse.qskotlin.models.*
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.charset.StandardCharsets

/** Test cases ported from the QsParser Java library https://github.com/atek-software/qsparser */
class QsParserSpec :
    DescribeSpec({
        /// Ported from
        // https://github.com/atek-software/qsparser/blob/main/src/test/java/ro/atek/qsparser/QueryStringParserTest.java
        describe("parsing") {
            it("should parse a simple string") {
                val options = DecodeOptions()
                val optionsStrictNullHandling = DecodeOptions(strictNullHandling = true)

                decode("0=foo", options) shouldBe mapOf("0" to "foo")

                decode("foo=c++", options) shouldBe mapOf("foo" to "c  ")

                decode("a[>=]=23", options) shouldBe mapOf("a" to mapOf(">=" to "23"))

                decode("a[<=>]==23", options) shouldBe mapOf("a" to mapOf("<=>" to "=23"))

                decode("a[==]=23", options) shouldBe mapOf("a" to mapOf("==" to "23"))

                decode("foo", optionsStrictNullHandling) shouldBe mapOf("foo" to null)

                decode("foo", options) shouldBe mapOf("foo" to "")

                decode("foo=", options) shouldBe mapOf("foo" to "")

                decode("foo=bar", options) shouldBe mapOf("foo" to "bar")

                decode(" foo = bar = baz ", options) shouldBe mapOf(" foo " to " bar = baz ")

                decode("foo=bar=baz", options) shouldBe mapOf("foo" to "bar=baz")

                decode("foo=bar&bar=baz", options) shouldBe mapOf("foo" to "bar", "bar" to "baz")

                decode("foo2=bar2&baz2=", options) shouldBe mapOf("foo2" to "bar2", "baz2" to "")

                decode("foo=bar&baz", optionsStrictNullHandling) shouldBe
                    mapOf("foo" to "bar", "baz" to null)

                decode("foo=bar&baz", options) shouldBe mapOf("foo" to "bar", "baz" to "")

                decode("cht=p3&chd=t:60,40&chs=250x100&chl=Hello|World", options) shouldBe
                    mapOf(
                        "cht" to "p3",
                        "chd" to "t:60,40",
                        "chs" to "250x100",
                        "chl" to "Hello|World",
                    )
            }

            it("should handle arrays on the same key") {
                val options = DecodeOptions()

                decode("a[]=b&a[]=c", options) shouldBe mapOf("a" to listOf("b", "c"))

                decode("a[0]=b&a[1]=c", options) shouldBe mapOf("a" to listOf("b", "c"))

                decode("a=b,c", options) shouldBe mapOf("a" to "b,c")

                decode("a=b&a=c", options) shouldBe mapOf("a" to listOf("b", "c"))
            }

            it("should allow dot notation") {
                val options = DecodeOptions()
                val optionsAllowDots = DecodeOptions(allowDots = true)

                decode("a.b=c", options) shouldBe mapOf("a.b" to "c")

                decode("a.b=c", optionsAllowDots) shouldBe mapOf("a" to mapOf("b" to "c"))
            }

            it("should handle depth parsing") {
                val options = DecodeOptions()
                val optionsDepth1 = DecodeOptions(depth = 1)
                val optionsDepth0 = DecodeOptions(depth = 0)

                decode("a[b]=c", options) shouldBe mapOf("a" to mapOf("b" to "c"))

                decode("a[b][c]=d", options) shouldBe mapOf("a" to mapOf("b" to mapOf("c" to "d")))

                decode("a[b][c][d][e][f][g][h]=i", options) shouldBe
                    mapOf(
                        "a" to
                            mapOf(
                                "b" to
                                    mapOf(
                                        "c" to
                                            mapOf(
                                                "d" to
                                                    mapOf(
                                                        "e" to mapOf("f" to mapOf("[g][h]" to "i"))
                                                    )
                                            )
                                    )
                            )
                    )

                decode("a[b][c]=d", optionsDepth1) shouldBe
                    mapOf("a" to mapOf("b" to mapOf("[c]" to "d")))

                decode("a[b][c][d]=e", optionsDepth1) shouldBe
                    mapOf("a" to mapOf("b" to mapOf("[c][d]" to "e")))

                decode("a[0]=b&a[1]=c", optionsDepth0) shouldBe mapOf("a[0]" to "b", "a[1]" to "c")

                decode("a[0][0]=b&a[0][1]=c&a[1]=d&e=2", optionsDepth0) shouldBe
                    mapOf("a[0][0]" to "b", "a[0][1]" to "c", "a[1]" to "d", "e" to "2")
            }

            it("should parse an explicit array") {
                val options = DecodeOptions()

                decode("a[]=b", options) shouldBe mapOf("a" to listOf("b"))

                decode("a[]=b&a[]=c", options) shouldBe mapOf("a" to listOf("b", "c"))

                decode("a[]=b&a[]=c&a[]=d", options) shouldBe mapOf("a" to listOf("b", "c", "d"))
            }

            it("should parse a mix of simple and explicit arrays") {
                val options = DecodeOptions()
                val options20 = DecodeOptions(listLimit = 20)
                val options0 = DecodeOptions(listLimit = 0)

                decode("a=b&a[]=c", options) shouldBe mapOf("a" to listOf("b", "c"))

                decode("a[]=b&a=c", options) shouldBe mapOf("a" to listOf("b", "c"))

                decode("a[0]=b&a=c", options) shouldBe mapOf("a" to listOf("b", "c"))

                decode("a=b&a[0]=c", options) shouldBe mapOf("a" to listOf("b", "c"))

                decode("a[1]=b&a=c", options20) shouldBe mapOf("a" to listOf("b", "c"))

                decode("a[]=b&a=c", options0) shouldBe mapOf("a" to mapOf("0" to "b", "1" to "c"))

                decode("a=b&a[1]=c", options20) shouldBe mapOf("a" to listOf("b", "c"))

                decode("a=b&a[]=c", options0) shouldBe mapOf("a" to mapOf("0" to "b", "1" to "c"))
            }

            it("should parse a nested array") {
                val options = DecodeOptions()

                decode("a[b][]=c&a[b][]=d", options) shouldBe
                    mapOf("a" to mapOf("b" to listOf("c", "d")))

                decode("a[>=]=25", options) shouldBe mapOf("a" to mapOf(">=" to "25"))
            }

            it("should allow specifying array indices") {
                val options = DecodeOptions()
                val options20 = DecodeOptions(listLimit = 20)
                val options0 = DecodeOptions(listLimit = 0)

                decode("a[1]=c&a[0]=b&a[2]=d", options) shouldBe mapOf("a" to listOf("b", "c", "d"))

                decode("a[1]=c&a[0]=b", options) shouldBe mapOf("a" to listOf("b", "c"))

                decode("a[1]=c", options20) shouldBe mapOf("a" to listOf("c"))

                decode("a[1]=c", options0) shouldBe mapOf("a" to mapOf("1" to "c"))

                decode("a[1]=c", options) shouldBe mapOf("a" to listOf("c"))
            }

            it("should limit specific array indices to listLimit") {
                val options = DecodeOptions()
                val options20 = DecodeOptions(listLimit = 20)

                decode("a[20]=a", options20) shouldBe mapOf("a" to listOf("a"))

                decode("a[21]=a", options20) shouldBe mapOf("a" to mapOf("21" to "a"))

                decode("a[20]=a", options) shouldBe mapOf("a" to listOf("a"))

                decode("a[21]=a", options) shouldBe mapOf("a" to mapOf("21" to "a"))
            }

            it("should support keys that begin with a number") {
                val options = DecodeOptions()

                decode("a[12b]=c", options) shouldBe mapOf("a" to mapOf("12b" to "c"))
            }

            it("should support encoded equal signs") {
                val options = DecodeOptions()

                decode("he%3Dllo=th%3Dere", options) shouldBe mapOf("he=llo" to "th=ere")
            }

            it("should handle URL encoded strings") {
                val options = DecodeOptions()

                decode("a[b%20c]=d", options) shouldBe mapOf("a" to mapOf("b c" to "d"))

                decode("a[b]=c%20d", options) shouldBe mapOf("a" to mapOf("b" to "c d"))
            }

            it("should allow brackets in the value") {
                val options = DecodeOptions()

                decode("pets=[\"tobi\"]", options) shouldBe mapOf("pets" to "[\"tobi\"]")

                decode("operators=[\">=\", \"<=\"]", options) shouldBe
                    mapOf("operators" to "[\">=\", \"<=\"]")
            }

            it("should allow empty values") {
                val options = DecodeOptions()

                decode("", options) shouldBe emptyMap()

                decode(null, options) shouldBe emptyMap()
            }

            it("should transform arrays to objects") {
                val options = DecodeOptions()

                decode("foo[0]=bar&foo[bad]=baz", options) shouldBe
                    mapOf("foo" to mapOf("0" to "bar", "bad" to "baz"))

                decode("foo[bad]=baz&foo[0]=bar", options) shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", "0" to "bar"))

                decode("foo[bad]=baz&foo[]=bar", options) shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", "0" to "bar"))

                decode("foo[]=bar&foo[bad]=baz", options) shouldBe
                    mapOf("foo" to mapOf("0" to "bar", "bad" to "baz"))

                decode("foo[bad]=baz&foo[]=bar&foo[]=foo", options) shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", "0" to "bar", "1" to "foo"))

                decode("foo[0][a]=a&foo[0][b]=b&foo[1][a]=aa&foo[1][b]=bb", options) shouldBe
                    mapOf(
                        "foo" to
                            listOf(mapOf("a" to "a", "b" to "b"), mapOf("a" to "aa", "b" to "bb"))
                    )
            }

            it("should transform arrays to objects with dot notation") {
                val optionsAllowDots = DecodeOptions(allowDots = true)

                decode("foo[0].baz=bar&fool.bad=baz", optionsAllowDots) shouldBe
                    mapOf("foo" to listOf(mapOf("baz" to "bar")), "fool" to mapOf("bad" to "baz"))

                decode("foo[0].baz=bar&fool.bad.boo=baz", optionsAllowDots) shouldBe
                    mapOf(
                        "foo" to listOf(mapOf("baz" to "bar")),
                        "fool" to mapOf("bad" to mapOf("boo" to "baz")),
                    )

                decode("foo[0][0].baz=bar&fool.bad=baz", optionsAllowDots) shouldBe
                    mapOf(
                        "foo" to listOf(listOf(mapOf("baz" to "bar"))),
                        "fool" to mapOf("bad" to "baz"),
                    )

                decode("foo[0].baz[0]=15&foo[0].bar=2", optionsAllowDots) shouldBe
                    mapOf("foo" to listOf(mapOf("baz" to listOf("15"), "bar" to "2")))

                decode("foo[0].baz[0]=15&foo[0].baz[1]=16&foo[0].bar=2", optionsAllowDots) shouldBe
                    mapOf("foo" to listOf(mapOf("baz" to listOf("15", "16"), "bar" to "2")))

                decode("foo.bad=baz&foo[0]=bar", optionsAllowDots) shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", "0" to "bar"))

                decode("foo.bad=baz&foo[]=bar", optionsAllowDots) shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", "0" to "bar"))

                decode("foo[]=bar&foo.bad=baz", optionsAllowDots) shouldBe
                    mapOf("foo" to mapOf("0" to "bar", "bad" to "baz"))

                decode("foo.bad=baz&foo[]=bar&foo[]=foo", optionsAllowDots) shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", "0" to "bar", "1" to "foo"))

                decode("foo[0].a=a&foo[0].b=b&foo[1].a=aa&foo[1].b=bb", optionsAllowDots) shouldBe
                    mapOf(
                        "foo" to
                            listOf(mapOf("a" to "a", "b" to "b"), mapOf("a" to "aa", "b" to "bb"))
                    )
            }

            it("should correctly prune undefined values") {
                val options = DecodeOptions()

                decode("a[2]=b&a[99999999]=c", options) shouldBe
                    mapOf("a" to mapOf("2" to "b", "99999999" to "c"))
            }

            it("should support malformed URI characters") {
                val options = DecodeOptions()
                val optionsStrictNullHandling = DecodeOptions(strictNullHandling = true)

                decode("{%:%}", optionsStrictNullHandling) shouldBe mapOf("{%:%}" to null)

                decode("{%:%}=", options) shouldBe mapOf("{%:%}" to "")

                decode("foo=%:%}", options) shouldBe mapOf("foo" to "%:%}")
            }

            it("should not produce empty keys") {
                val options = DecodeOptions()

                decode("_r=1&", options) shouldBe mapOf("_r" to "1")
            }

            it("should parse arrays of objects") {
                val options = DecodeOptions()

                decode("a[][b]=c", options) shouldBe mapOf("a" to listOf(mapOf("b" to "c")))

                decode("a[0][b]=c", options) shouldBe mapOf("a" to listOf(mapOf("b" to "c")))
            }

            it("should allow for empty strings in arrays") {
                val options = DecodeOptions()
                val optionsStrictNullHandling20 =
                    DecodeOptions(strictNullHandling = true, listLimit = 20)
                val optionsStrictNullHandling0 =
                    DecodeOptions(strictNullHandling = true, listLimit = 0)

                decode("a[]=b&a[]=&a[]=c", options) shouldBe mapOf("a" to listOf("b", "", "c"))

                decode("a[0]=b&a[1]&a[2]=c&a[19]=", optionsStrictNullHandling20) shouldBe
                    mapOf("a" to listOf("b", null, "c", ""))

                decode("a[]=b&a[]&a[]=c&a[]=", optionsStrictNullHandling0) shouldBe
                    mapOf("a" to mapOf("0" to "b", "1" to null, "2" to "c", "3" to ""))

                decode("a[0]=b&a[1]=&a[2]=c&a[19]", optionsStrictNullHandling20) shouldBe
                    mapOf("a" to listOf("b", "", "c", null))

                decode("a[]=b&a[]=&a[]=c&a[]", optionsStrictNullHandling0) shouldBe
                    mapOf("a" to mapOf("0" to "b", "1" to "", "2" to "c", "3" to null))

                decode("a[]=&a[]=b&a[]=c", optionsStrictNullHandling0) shouldBe
                    mapOf("a" to mapOf("0" to "", "1" to "b", "2" to "c"))
            }

            it("should compact sparse arrays") {
                val options = DecodeOptions(listLimit = 20)

                decode("a[10]=1&a[2]=2", options) shouldBe mapOf("a" to listOf("2", "1"))

                decode("a[1][b][2][c]=1", options) shouldBe
                    mapOf("a" to listOf(mapOf("b" to listOf(mapOf("c" to "1")))))

                decode("a[1][2][3][c]=1", options) shouldBe
                    mapOf("a" to listOf(listOf(listOf(mapOf("c" to "1")))))

                decode("a[1][2][3][c][1]=1", options) shouldBe
                    mapOf("a" to listOf(listOf(listOf(mapOf("c" to listOf("1"))))))
            }

            it("should parse sparse arrays") {
                val optionsAllowSparse = DecodeOptions(allowSparseLists = true)

                decode("a[4]=1&a[1]=2", optionsAllowSparse) shouldBe
                    mapOf("a" to listOf(null, "2", null, null, "1"))

                decode("a[1][b][2][c]=1", optionsAllowSparse) shouldBe
                    mapOf("a" to listOf(null, mapOf("b" to listOf(null, null, mapOf("c" to "1")))))

                decode("a[1][2][3][c]=1", optionsAllowSparse) shouldBe
                    mapOf(
                        "a" to
                            listOf(
                                null,
                                listOf(null, null, listOf(null, null, null, mapOf("c" to "1"))),
                            )
                    )

                decode("a[1][2][3][c][1]=1", optionsAllowSparse) shouldBe
                    mapOf(
                        "a" to
                            listOf(
                                null,
                                listOf(
                                    null,
                                    null,
                                    listOf(null, null, null, mapOf("c" to listOf(null, "1"))),
                                ),
                            )
                    )
            }

            it("should parse jQuery param strings") {
                val options = DecodeOptions()

                decode(
                    "filter%5B0%5D%5B%5D=int1&filter%5B0%5D%5B%5D=%3D&filter%5B0%5D%5B%5D=77&filter%5B%5D=and&filter%5B2%5D%5B%5D=int2&filter%5B2%5D%5B%5D=%3D&filter%5B2%5D%5B%5D=8",
                    options,
                ) shouldBe
                    mapOf(
                        "filter" to
                            listOf(listOf("int1", "=", "77"), "and", listOf("int2", "=", "8"))
                    )
            }

            it("should continue parsing when no parent is found") {
                val options = DecodeOptions()
                val optionsStrictNullHandling = DecodeOptions(strictNullHandling = true)

                decode("[]=&a=b", options) shouldBe mapOf("0" to "", "a" to "b")

                decode("[]&a=b", optionsStrictNullHandling) shouldBe mapOf("0" to null, "a" to "b")

                decode("[foo]=bar", optionsStrictNullHandling) shouldBe mapOf("foo" to "bar")
            }

            it("should not error when parsing a very long array") {
                val options = DecodeOptions()

                var atom = "a[]=a"
                while (atom.length < 120 * 1024) {
                    atom += "&$atom"
                }

                shouldNotThrow<Exception> { decode(atom, options) }
            }

            it("should parse a string with an alternative string delimiter") {
                val optionsSemicolon = DecodeOptions(delimiter = Delimiter.SEMICOLON)
                val optionsRegex = DecodeOptions(delimiter = RegexDelimiter("[;,] *"))

                decode("a=b;c=d", optionsSemicolon) shouldBe mapOf("a" to "b", "c" to "d")

                decode("a=b; c=d", optionsRegex) shouldBe mapOf("a" to "b", "c" to "d")
            }

            it("should allow overriding parameter limit") {
                val options1 = DecodeOptions(parameterLimit = 1)
                val optionsMax = DecodeOptions(parameterLimit = Int.MAX_VALUE)

                decode("a=b&c=d", options1) shouldBe mapOf("a" to "b")

                decode("a=b&c=d", optionsMax) shouldBe mapOf("a" to "b", "c" to "d")
            }

            it("should allow overriding list limit") {
                val optionsNegative = DecodeOptions(listLimit = -1)

                decode("a[0]=b", optionsNegative) shouldBe mapOf("a" to mapOf("0" to "b"))

                decode("a[-1]=b", optionsNegative) shouldBe mapOf("a" to mapOf("-1" to "b"))

                decode("a[0]=b&a[1]=c", optionsNegative) shouldBe
                    mapOf("a" to mapOf("0" to "b", "1" to "c"))
            }

            it("should allow disabling list parsing") {
                val options = DecodeOptions(parseLists = false)

                decode("a[0]=b&a[1]=c", options) shouldBe
                    mapOf("a" to mapOf("0" to "b", "1" to "c"))

                decode("a[]=b", options) shouldBe mapOf("a" to mapOf("0" to "b"))
            }

            it("should allow for query string prefix") {
                val optionsIgnorePrefix = DecodeOptions(ignoreQueryPrefix = true)
                val optionsKeepPrefix = DecodeOptions(ignoreQueryPrefix = false)

                decode("?foo=bar", optionsIgnorePrefix) shouldBe mapOf("foo" to "bar")

                decode("foo=bar", optionsIgnorePrefix) shouldBe mapOf("foo" to "bar")

                decode("?foo=bar", optionsKeepPrefix) shouldBe mapOf("?foo" to "bar")
            }

            it("should parse string with comma as array divider") {
                val simpleOptions = DecodeOptions()
                val commaOptions = DecodeOptions(comma = true)
                val commaStrictNullOptions = DecodeOptions(comma = true, strictNullHandling = true)

                decode("foo=bar,tee", commaOptions) shouldBe mapOf("foo" to listOf("bar", "tee"))

                decode("foo[bar]=coffee,tee", commaOptions) shouldBe
                    mapOf("foo" to mapOf("bar" to listOf("coffee", "tee")))

                decode("foo=", commaOptions) shouldBe mapOf("foo" to "")

                decode("foo", commaOptions) shouldBe mapOf("foo" to "")

                decode("foo", commaStrictNullOptions) shouldBe mapOf("foo" to null)

                decode("a[0]=c", simpleOptions) shouldBe mapOf("a" to listOf("c"))

                decode("a[]=c", simpleOptions) shouldBe mapOf("a" to listOf("c"))

                decode("a[]=c", commaOptions) shouldBe mapOf("a" to listOf("c"))

                decode("a[0]=c&a[1]=d", simpleOptions) shouldBe mapOf("a" to listOf("c", "d"))

                decode("a[]=c&a[]=d", simpleOptions) shouldBe mapOf("a" to listOf("c", "d"))

                decode("a[]=c&a[]=d", commaOptions) shouldBe mapOf("a" to listOf("c", "d"))
            }

            it("should use number decoder") {
                val numberDecoder = Decoder { value, _, kind ->
                    if (kind == DecodeKind.VALUE) {
                        try {
                            value?.toInt()?.let { "[$it]" } ?: value
                        } catch (_: NumberFormatException) {
                            value
                        }
                    } else {
                        // Leave keys untouched
                        value
                    }
                }
                val options = DecodeOptions(decoder = numberDecoder)

                decode("foo=1", options) shouldBe mapOf("foo" to "[1]")

                decode("foo=1.0", options) shouldBe mapOf("foo" to "1.0")

                decode("foo=0", options) shouldBe mapOf("foo" to "[0]")
            }

            it("should parse comma delimited array") {
                val options = DecodeOptions(comma = true)

                decode("foo=a%2Cb", options) shouldBe mapOf("foo" to "a,b")

                decode("foo=a%2C%20b,d", options) shouldBe mapOf("foo" to listOf("a, b", "d"))

                decode("foo=a%2C%20b,c%2C%20d", options) shouldBe
                    mapOf("foo" to listOf("a, b", "c, d"))
            }

            it("should not crash when parsing deep objects") {
                val options = DecodeOptions(depth = 500)

                var str = "foo"
                repeat(500) { str += "[p]" }
                str += "=bar"

                var result: Map<String, Any?>? = null
                shouldNotThrow<Exception> { result = decode(str, options) }

                var depth = 0
                var current: Any? = result?.get("foo")
                while (current is Map<*, *> && current.containsKey("p")) {
                    current = current["p"]
                    depth++
                }
                depth shouldBe 500
            }

            it("should handle params starting with a closing bracket") {
                val options = DecodeOptions()

                decode("]=toString", options) shouldBe mapOf("]" to "toString")

                decode("]]=toString", options) shouldBe mapOf("]]" to "toString")

                decode("]hello]=toString", options) shouldBe mapOf("]hello]" to "toString")
            }

            it("should handle params starting with a starting bracket") {
                val options = DecodeOptions()

                decode("[=toString", options) shouldBe mapOf("[" to "toString")

                decode("[[=toString", options) shouldBe mapOf("[[" to "toString")

                decode("[hello[=toString", options) shouldBe mapOf("[hello[" to "toString")
            }

            it("should add keys to objects") {
                val options = DecodeOptions()

                decode("a[b]=c&a=d", options) shouldBe mapOf("a" to mapOf("b" to "c", "d" to true))
            }

            it("should parse with custom encoding") {
                val customDecoder = Decoder { content: String?, _, _ ->
                    try {
                        java.net.URLDecoder.decode(content ?: "", "Shift_JIS")
                    } catch (_: Exception) {
                        content
                    }
                }
                val options = DecodeOptions(decoder = customDecoder)

                decode("%8c%a7=%91%e5%8d%e3%95%7b", options) shouldBe mapOf("県" to "大阪府")
            }

            it("should parse other charset") {
                val options = DecodeOptions(charset = StandardCharsets.ISO_8859_1)

                decode("%A2=%BD", options) shouldBe mapOf("¢" to "½")
            }

            it("should parse charset sentinel") {
                val urlEncodedCheckmarkInUtf8 = "%E2%9C%93"
                val urlEncodedOSlashInUtf8 = "%C3%B8"
                val urlEncodedNumCheckmark = "%26%2310003%3B"

                val optionsIso =
                    DecodeOptions(charsetSentinel = true, charset = StandardCharsets.ISO_8859_1)
                val optionsUtf8 =
                    DecodeOptions(charsetSentinel = true, charset = StandardCharsets.UTF_8)
                val optionsDefault = DecodeOptions(charsetSentinel = true)

                decode(
                    "utf8=$urlEncodedCheckmarkInUtf8&$urlEncodedOSlashInUtf8=$urlEncodedOSlashInUtf8",
                    optionsIso,
                ) shouldBe mapOf("ø" to "ø")

                decode(
                    "utf8=$urlEncodedNumCheckmark&$urlEncodedOSlashInUtf8=$urlEncodedOSlashInUtf8",
                    optionsUtf8,
                ) shouldBe mapOf("Ã¸" to "Ã¸")

                decode(
                    "a=$urlEncodedOSlashInUtf8&utf8=$urlEncodedNumCheckmark",
                    optionsUtf8,
                ) shouldBe mapOf("a" to "Ã¸")

                decode(
                    "utf8=foo&$urlEncodedOSlashInUtf8=$urlEncodedOSlashInUtf8",
                    optionsUtf8,
                ) shouldBe mapOf("ø" to "ø")

                decode(
                    "utf8=$urlEncodedCheckmarkInUtf8&$urlEncodedOSlashInUtf8=$urlEncodedOSlashInUtf8",
                    optionsDefault,
                ) shouldBe mapOf("ø" to "ø")

                decode(
                    "utf8=$urlEncodedNumCheckmark&$urlEncodedOSlashInUtf8=$urlEncodedOSlashInUtf8",
                    optionsDefault,
                ) shouldBe mapOf("Ã¸" to "Ã¸")
            }

            it("should interpret numeric entities") {
                val urlEncodedNumSmiley = "%26%239786%3B"

                val optionsIso = DecodeOptions(charset = StandardCharsets.ISO_8859_1)
                val optionsIsoInterpret =
                    DecodeOptions(
                        charset = StandardCharsets.ISO_8859_1,
                        interpretNumericEntities = true,
                    )
                val optionsUtfInterpret =
                    DecodeOptions(charset = StandardCharsets.UTF_8, interpretNumericEntities = true)

                decode("foo=$urlEncodedNumSmiley", optionsIsoInterpret) shouldBe mapOf("foo" to "☺")

                decode("foo=$urlEncodedNumSmiley", optionsIso) shouldBe mapOf("foo" to "&#9786;")

                decode("foo=$urlEncodedNumSmiley", optionsUtfInterpret) shouldBe
                    mapOf("foo" to "&#9786;")
            }

            it("should allow for decoding keys and values") {
                val keyValueDecoder = Decoder { content: String?, _, _ ->
                    // This decoder lowercases both keys and values. With DecodeKind available,
                    // you could branch on kind == DecodeKind.KEY or VALUE if different behaviors
                    // are desired.
                    content?.lowercase()
                }
                val options = DecodeOptions(decoder = keyValueDecoder)

                decode("KeY=vAlUe", options) shouldBe mapOf("key" to "value")
            }

            it("should handle proof of concept") {
                val options = DecodeOptions()

                decode(
                    "filters[name][:eq]=John&filters[age][:ge]=18&filters[age][:le]=60",
                    options,
                ) shouldBe
                    mapOf(
                        "filters" to
                            mapOf(
                                "name" to mapOf(":eq" to "John"),
                                "age" to mapOf(":ge" to "18", ":le" to "60"),
                            )
                    )
            }
        }

        /// Ported from
        // https://github.com/atek-software/qsparser/blob/main/src/test/java/ro/atek/qsparser/QueryStringBuilderTest.java
        describe("encoding") {
            it("should stringify a querystring object") {
                encode(mapOf("a" to "b")) shouldBe "a=b"
                encode(mapOf("a" to 1)) shouldBe "a=1"
                encode(mapOf("a" to 1, "b" to 2)) shouldBe "a=1&b=2"
                encode(mapOf("a" to "A_Z")) shouldBe "a=A_Z"
                encode(mapOf("a" to "€")) shouldBe "a=%E2%82%AC"
                encode(mapOf("a" to "\uE000")) shouldBe "a=%EE%80%80"
                encode(mapOf("a" to "א")) shouldBe "a=%D7%90"
                encode(mapOf("a" to "\uD801\uDC37")) shouldBe "a=%F0%90%90%B7"
            }

            it("should stringify falsy values") {
                encode(null) shouldBe ""
                encode(null, EncodeOptions(strictNullHandling = true)) shouldBe ""
                encode(false) shouldBe ""
                encode(0) shouldBe ""
                encode(emptyMap<String, Any>()) shouldBe ""
            }

            it("should stringify integers with custom encoder") {
                val encoder: ValueEncoder = { value, _, _ ->
                    val stringValue = value.toString()
                    // Only encode actual integer values, not string representations of indices
                    if (value is Int) "${stringValue}n" else stringValue
                }

                val options = EncodeOptions(encoder = encoder)
                val optionsValuesOnly =
                    EncodeOptions(
                        encoder = encoder,
                        encodeValuesOnly = true,
                        listFormat = ListFormat.BRACKETS,
                    )

                encode(3) shouldBe ""
                encode(listOf(3)) shouldBe "0=3"
                encode(listOf(3), options) shouldBe "0=3n"
                encode(mapOf("a" to 3)) shouldBe "a=3"
                encode(mapOf("a" to 3), options) shouldBe "a=3n"
                encode(
                    mapOf("a" to listOf(3)),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a[]=3"
                encode(mapOf("a" to listOf(3)), optionsValuesOnly) shouldBe "a[]=3n"
            }

            it("should add query prefix") {
                val options = EncodeOptions(addQueryPrefix = true)
                encode(mapOf("a" to "b"), options) shouldBe "?a=b"
            }

            it("should not add query prefix for empty objects") {
                val options = EncodeOptions(addQueryPrefix = true)
                encode(emptyMap<String, Any>(), options) shouldBe ""
            }

            it("should stringify nested falsy values") {
                val nested = mapOf("a" to mapOf("b" to mapOf("c" to null)))

                encode(nested) shouldBe "a%5Bb%5D%5Bc%5D="
                encode(nested, EncodeOptions(strictNullHandling = true)) shouldBe "a%5Bb%5D%5Bc%5D"
                encode(mapOf("a" to mapOf("b" to mapOf("c" to false)))) shouldBe
                    "a%5Bb%5D%5Bc%5D=false"
            }

            it("should stringify nested objects") {
                encode(mapOf("a" to mapOf("b" to "c"))) shouldBe "a%5Bb%5D=c"
                encode(mapOf("a" to mapOf("b" to mapOf("c" to mapOf("d" to "e"))))) shouldBe
                    "a%5Bb%5D%5Bc%5D%5Bd%5D=e"
            }

            it("should stringify nested objects with dots notation") {
                val options = EncodeOptions(allowDots = true)

                encode(mapOf("a" to mapOf("b" to "c")), options) shouldBe "a.b=c"
                encode(
                    mapOf("a" to mapOf("b" to mapOf("c" to mapOf("d" to "e")))),
                    options,
                ) shouldBe "a.b.c.d=e"
            }

            it("should stringify array values") {
                val data = mapOf("a" to listOf("b", "c", "d"))

                encode(data, EncodeOptions(listFormat = ListFormat.INDICES)) shouldBe
                    "a%5B0%5D=b&a%5B1%5D=c&a%5B2%5D=d"
                encode(data, EncodeOptions(listFormat = ListFormat.BRACKETS)) shouldBe
                    "a%5B%5D=b&a%5B%5D=c&a%5B%5D=d"
                encode(data, EncodeOptions(listFormat = ListFormat.COMMA)) shouldBe "a=b%2Cc%2Cd"
                encode(data) shouldBe "a%5B0%5D=b&a%5B1%5D=c&a%5B2%5D=d"
            }

            it("should omit nulls when asked") {
                val options = EncodeOptions(skipNulls = true)
                encode(mapOf("a" to "b", "c" to null), options) shouldBe "a=b"
            }

            it("should omit nested nulls when asked") {
                val options = EncodeOptions(skipNulls = true)
                encode(mapOf("a" to mapOf("b" to "c", "d" to null)), options) shouldBe "a%5Bb%5D=c"
            }

            it("should omit array indices when asked") {
                val options = EncodeOptions(listFormat = ListFormat.REPEAT)
                encode(mapOf("a" to listOf("b", "c", "d")), options) shouldBe "a=b&a=c&a=d"
            }

            it("should handle non-array items") {
                val options = EncodeOptions(encodeValuesOnly = true)
                val value = mapOf("a" to "c")

                encode(value, options) shouldBe "a=c"
                encode(value, options.copy(listFormat = ListFormat.INDICES)) shouldBe "a=c"
                encode(value, options.copy(listFormat = ListFormat.BRACKETS)) shouldBe "a=c"
                encode(value, options.copy(listFormat = ListFormat.COMMA)) shouldBe "a=c"
            }

            it("should handle array with single item") {
                val options = EncodeOptions(encodeValuesOnly = true)
                val value = mapOf("a" to listOf("c"))

                encode(value, options) shouldBe "a[0]=c"
                encode(value, options.copy(listFormat = ListFormat.INDICES)) shouldBe "a[0]=c"
                encode(value, options.copy(listFormat = ListFormat.BRACKETS)) shouldBe "a[]=c"
                encode(value, options.copy(listFormat = ListFormat.COMMA)) shouldBe "a=c"
                encode(
                    value,
                    options.copy(listFormat = ListFormat.COMMA, commaRoundTrip = true),
                ) shouldBe "a[]=c"
            }

            it("should handle array with multiple items") {
                val options = EncodeOptions(encodeValuesOnly = true)
                val value = mapOf("a" to listOf("c", "d"))

                encode(value, options) shouldBe "a[0]=c&a[1]=d"
                encode(value, options.copy(listFormat = ListFormat.INDICES)) shouldBe
                    "a[0]=c&a[1]=d"
                encode(value, options.copy(listFormat = ListFormat.BRACKETS)) shouldBe "a[]=c&a[]=d"
                encode(value, options.copy(listFormat = ListFormat.COMMA)) shouldBe "a=c,d"
            }

            it("should handle array with multiple items containing commas") {
                val value = mapOf("a" to listOf("c,d", "e"))

                encode(value, EncodeOptions(listFormat = ListFormat.COMMA)) shouldBe "a=c%2Cd%2Ce"
                encode(
                    value,
                    EncodeOptions(listFormat = ListFormat.COMMA, encodeValuesOnly = true),
                ) shouldBe "a=c%2Cd,e"
            }

            it("should stringify nested array values") {
                val options = EncodeOptions(encodeValuesOnly = true)
                val value = mapOf("a" to mapOf("b" to listOf("c", "d")))

                encode(value, options) shouldBe "a[b][0]=c&a[b][1]=d"
                encode(value, options.copy(listFormat = ListFormat.INDICES)) shouldBe
                    "a[b][0]=c&a[b][1]=d"
                encode(value, options.copy(listFormat = ListFormat.BRACKETS)) shouldBe
                    "a[b][]=c&a[b][]=d"
                encode(value, options.copy(listFormat = ListFormat.COMMA)) shouldBe "a[b]=c,d"
            }

            it("should stringify comma and empty array values") {
                val value = mapOf("a" to listOf(",", "", "c,d%"))

                // Without encoding
                encode(
                    value,
                    EncodeOptions(encode = false, listFormat = ListFormat.INDICES),
                ) shouldBe "a[0]=,&a[1]=&a[2]=c,d%"
                encode(
                    value,
                    EncodeOptions(encode = false, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a[]=,&a[]=&a[]=c,d%"
                encode(value, EncodeOptions(encode = false, listFormat = ListFormat.COMMA)) shouldBe
                    "a=,,,c,d%"
                encode(
                    value,
                    EncodeOptions(encode = false, listFormat = ListFormat.REPEAT),
                ) shouldBe "a=,&a=&a=c,d%"

                // With encoding, values only
                encode(
                    value,
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.INDICES),
                ) shouldBe "a[0]=%2C&a[1]=&a[2]=c%2Cd%25"
                encode(
                    value,
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a[]=%2C&a[]=&a[]=c%2Cd%25"
                encode(
                    value,
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.COMMA),
                ) shouldBe "a=%2C,,c%2Cd%25"
                encode(
                    value,
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.REPEAT),
                ) shouldBe "a=%2C&a=&a=c%2Cd%25"

                // With encoding, keys and values
                encode(
                    value,
                    EncodeOptions(encodeValuesOnly = false, listFormat = ListFormat.INDICES),
                ) shouldBe "a%5B0%5D=%2C&a%5B1%5D=&a%5B2%5D=c%2Cd%25"
                encode(
                    value,
                    EncodeOptions(encodeValuesOnly = false, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a%5B%5D=%2C&a%5B%5D=&a%5B%5D=c%2Cd%25"
                encode(
                    value,
                    EncodeOptions(encodeValuesOnly = false, listFormat = ListFormat.COMMA),
                ) shouldBe "a=%2C%2C%2Cc%2Cd%25"
                encode(
                    value,
                    EncodeOptions(encodeValuesOnly = false, listFormat = ListFormat.REPEAT),
                ) shouldBe "a=%2C&a=&a=c%2Cd%25"
            }

            it("should stringify comma and empty non-array values") {
                val value = mapOf("a" to ",", "b" to "", "c" to "c,d%")

                // All array formats should produce the same result for non-arrays
                encode(
                    value,
                    EncodeOptions(encode = false, listFormat = ListFormat.INDICES),
                ) shouldBe "a=,&b=&c=c,d%"
                encode(
                    value,
                    EncodeOptions(encode = false, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a=,&b=&c=c,d%"
                encode(value, EncodeOptions(encode = false, listFormat = ListFormat.COMMA)) shouldBe
                    "a=,&b=&c=c,d%"
                encode(
                    value,
                    EncodeOptions(encode = false, listFormat = ListFormat.REPEAT),
                ) shouldBe "a=,&b=&c=c,d%"

                encode(value, EncodeOptions(encodeValuesOnly = true)) shouldBe "a=%2C&b=&c=c%2Cd%25"
                encode(value, EncodeOptions(encodeValuesOnly = false)) shouldBe
                    "a=%2C&b=&c=c%2Cd%25"
            }

            it("should drop null entries when commaCompactNulls is enabled") {
                val value = mapOf("a" to mapOf("b" to listOf("one", null, "two", null, "three")))

                encode(value, EncodeOptions(encode = false, listFormat = ListFormat.COMMA)) shouldBe
                    "a[b]=one,,two,,three"

                encode(
                    value,
                    EncodeOptions(
                        encode = false,
                        listFormat = ListFormat.COMMA,
                        commaCompactNulls = true,
                    ),
                ) shouldBe "a[b]=one,two,three"
            }

            it("should omit key when commaCompactNulls strips all values") {
                val value = mapOf("a" to listOf(null, null))

                encode(value, EncodeOptions(encode = false, listFormat = ListFormat.COMMA)) shouldBe
                    "a=," // baseline behaviour keeps empty slots

                encode(
                    value,
                    EncodeOptions(
                        encode = false,
                        listFormat = ListFormat.COMMA,
                        commaCompactNulls = true,
                    ),
                ) shouldBe ""
            }

            it("should preserve round-trip marker after compacting nulls") {
                val value = mapOf("a" to listOf(null, "foo"))

                encode(
                    value,
                    EncodeOptions(
                        encode = false,
                        listFormat = ListFormat.COMMA,
                        commaRoundTrip = true,
                    ),
                ) shouldBe "a=,foo"

                encode(
                    value,
                    EncodeOptions(
                        encode = false,
                        listFormat = ListFormat.COMMA,
                        commaRoundTrip = true,
                        commaCompactNulls = true,
                    ),
                ) shouldBe "a[]=foo"
            }

            it("should stringify nested array values with dots notation") {
                val value = mapOf("a" to mapOf("b" to listOf("c", "d")))
                val options = EncodeOptions(allowDots = true, encodeValuesOnly = true)

                encode(value, options) shouldBe "a.b[0]=c&a.b[1]=d"
                encode(value, options.copy(listFormat = ListFormat.INDICES)) shouldBe
                    "a.b[0]=c&a.b[1]=d"
                encode(value, options.copy(listFormat = ListFormat.BRACKETS)) shouldBe
                    "a.b[]=c&a.b[]=d"
                encode(value, options.copy(listFormat = ListFormat.COMMA)) shouldBe "a.b=c,d"
            }

            it("should stringify objects inside arrays") {
                val value = mapOf("a" to listOf(mapOf("b" to "c")))
                val value2 = mapOf("a" to listOf(mapOf("b" to mapOf("c" to listOf(1)))))

                encode(value) shouldBe "a%5B0%5D%5Bb%5D=c"
                encode(value2) shouldBe "a%5B0%5D%5Bb%5D%5Bc%5D%5B0%5D=1"

                encode(value, EncodeOptions(listFormat = ListFormat.INDICES)) shouldBe
                    "a%5B0%5D%5Bb%5D=c"
                encode(value2, EncodeOptions(listFormat = ListFormat.INDICES)) shouldBe
                    "a%5B0%5D%5Bb%5D%5Bc%5D%5B0%5D=1"

                encode(value, EncodeOptions(listFormat = ListFormat.BRACKETS)) shouldBe
                    "a%5B%5D%5Bb%5D=c"
                encode(value2, EncodeOptions(listFormat = ListFormat.BRACKETS)) shouldBe
                    "a%5B%5D%5Bb%5D%5Bc%5D%5B%5D=1"
            }

            it("should stringify arrays with mixed objects and primitives") {
                val value = mapOf("a" to listOf(mapOf("b" to 1), 2, 3))
                val options = EncodeOptions(encodeValuesOnly = true)

                encode(value, options) shouldBe "a[0][b]=1&a[1]=2&a[2]=3"
                encode(value, options.copy(listFormat = ListFormat.INDICES)) shouldBe
                    "a[0][b]=1&a[1]=2&a[2]=3"
                encode(value, options.copy(listFormat = ListFormat.BRACKETS)) shouldBe
                    "a[][b]=1&a[]=2&a[]=3"

                // Note: COMMA format with mixed types may not produce exact Java equivalent
                // but should handle the conversion appropriately
                val commaResult = encode(value, options.copy(listFormat = ListFormat.COMMA))
                commaResult shouldContain "a="
            }
        }

        /// Ported from
        // https://github.com/atek-software/qsparser/blob/main/src/test/java/ro/atek/qsparser/UtilsTest.java
        describe("Utils.merge") {
            it("should merge with null values") {
                Utils.merge(null, listOf(42)) shouldBe listOf(null, 42)
                Utils.merge(null, true) shouldBe listOf(null, true)
            }

            it("should merge maps and arrays") {
                val dict1 = mapOf("a" to "b")
                val dict2 = mapOf("a" to "c")
                val dict3 = mapOf("a" to dict2)

                Utils.merge(dict1, dict2) shouldBe mapOf("a" to listOf("b", "c"))
                Utils.merge(dict1, dict3) shouldBe mapOf("a" to listOf("b", mapOf("a" to "c")))

                val d1 = mapOf("foo" to listOf("bar", mapOf("first" to "123")))
                val d2 = mapOf("foo" to mapOf("second" to "456"))

                val expected1 =
                    mapOf(
                        "foo" to
                            mapOf("0" to "bar", "1" to mapOf("first" to "123"), "second" to "456")
                    )
                Utils.merge(d1, d2) shouldBe expected1

                val a = mapOf("foo" to listOf("baz"))
                val b = mapOf("foo" to listOf("bar", "xyzz"))
                Utils.merge(a, b) shouldBe mapOf("foo" to listOf("baz", "bar", "xyzz"))

                val x = mapOf("foo" to "baz")
                Utils.merge(x, "bar") shouldBe mapOf("foo" to "baz", "bar" to true)
            }
        }
    })
