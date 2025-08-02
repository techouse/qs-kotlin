package io.github.techouse.qskotlin.unit

import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.RegexDelimiter
import io.github.techouse.qskotlin.models.StringDelimiter
import io.github.techouse.qskotlin.queryParametersQs
import io.github.techouse.qskotlin.toStringQs
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset

class UriSpec :
    FunSpec({
        context("URI.queryParametersQs") {
            test("parses a simple string") {
                URI.create("$testUrl?0=foo").queryParametersQs(null) shouldBe mapOf(0 to "foo")

                URI.create("$testUrl?foo=c++").queryParametersQs(null) shouldBe
                    mapOf("foo" to "c  ")

                URI.create("$testUrl?a[${URLEncoder.encode(">=", "UTF-8")}]=23")
                    .queryParametersQs(null) shouldBe mapOf("a" to mapOf(">=" to "23"))

                URI.create("$testUrl?a[${URLEncoder.encode("<=>", "UTF-8")}]==23")
                    .queryParametersQs(null) shouldBe mapOf("a" to mapOf("<=>" to "=23"))

                URI.create("$testUrl?a[${URLEncoder.encode("==", "UTF-8")}]=23")
                    .queryParametersQs(null) shouldBe mapOf("a" to mapOf("==" to "23"))

                URI.create("$testUrl?foo")
                    .queryParametersQs(DecodeOptions(strictNullHandling = true)) shouldBe
                    mapOf("foo" to null)

                URI.create("$testUrl?foo").queryParametersQs(null) shouldBe mapOf("foo" to "")

                URI.create("$testUrl?foo=").queryParametersQs(null) shouldBe mapOf("foo" to "")

                URI.create("$testUrl?foo=bar").queryParametersQs(null) shouldBe
                    mapOf("foo" to "bar")

                URI.create(
                        "$testUrl?${URLEncoder.encode(" foo ", "UTF-8")}=${URLEncoder.encode(" bar = baz ", "UTF-8")}"
                    )
                    .queryParametersQs(null) shouldBe mapOf(" foo " to " bar = baz ")

                URI.create("$testUrl?foo=bar=baz").queryParametersQs(null) shouldBe
                    mapOf("foo" to "bar=baz")

                URI.create("$testUrl?foo=bar&bar=baz").queryParametersQs(null) shouldBe
                    mapOf("foo" to "bar", "bar" to "baz")

                URI.create("$testUrl?foo2=bar2&baz2=").queryParametersQs(null) shouldBe
                    mapOf("foo2" to "bar2", "baz2" to "")

                URI.create("$testUrl?foo=bar&baz")
                    .queryParametersQs(DecodeOptions(strictNullHandling = true)) shouldBe
                    mapOf("foo" to "bar", "baz" to null)

                URI.create("$testUrl?foo=bar&baz").queryParametersQs(null) shouldBe
                    mapOf("foo" to "bar", "baz" to "")

                URI.create(
                        "$testUrl?cht=p3&chd=t:60,40&chs=250x100&chl=Hello${URLEncoder.encode("|", "UTF-8")}World"
                    )
                    .queryParametersQs(null) shouldBe
                    mapOf(
                        "cht" to "p3",
                        "chd" to "t:60,40",
                        "chs" to "250x100",
                        "chl" to "Hello|World",
                    )
            }

            test("comma: false") {
                URI.create(
                        "$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=b&a${URLEncoder.encode("[]", "UTF-8")}=c"
                    )
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("b", "c"))

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[0]", "UTF-8")}=b&a${URLEncoder.encode("[1]", "UTF-8")}=c"
                    )
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("b", "c"))

                URI.create("$testUrl?a=b,c").queryParametersQs(null) shouldBe mapOf("a" to "b,c")

                URI.create("$testUrl?a=b&a=c").queryParametersQs(null) shouldBe
                    mapOf("a" to listOf("b", "c"))
            }

            test("comma: true") {
                URI.create(
                        "$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=b&a${URLEncoder.encode("[]", "UTF-8")}=c"
                    )
                    .queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("a" to listOf("b", "c"))

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[0]", "UTF-8")}=b&a${URLEncoder.encode("[1]", "UTF-8")}=c"
                    )
                    .queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("a" to listOf("b", "c"))

                URI.create("$testUrl?a=b,c").queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("a" to listOf("b", "c"))

                URI.create("$testUrl?a=b&a=c")
                    .queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("a" to listOf("b", "c"))
            }

            test("allows enabling dot notation") {
                URI.create("$testUrl?a.b=c").queryParametersQs(null) shouldBe mapOf("a.b" to "c")

                URI.create("$testUrl?a.b=c")
                    .queryParametersQs(DecodeOptions(allowDots = true)) shouldBe
                    mapOf("a" to mapOf("b" to "c"))
            }

            test("decode dot keys correctly") {
                URI.create("$testUrl?name%252Eobj.first=John&name%252Eobj.last=Doe")
                    .queryParametersQs(
                        DecodeOptions(allowDots = false, decodeDotInKeys = false)
                    ) shouldBe mapOf("name%2Eobj.first" to "John", "name%2Eobj.last" to "Doe")

                URI.create("$testUrl?name.obj.first=John&name.obj.last=Doe")
                    .queryParametersQs(
                        DecodeOptions(allowDots = true, decodeDotInKeys = false)
                    ) shouldBe
                    mapOf("name" to mapOf("obj" to mapOf("first" to "John", "last" to "Doe")))

                URI.create("$testUrl?name%252Eobj.first=John&name%252Eobj.last=Doe")
                    .queryParametersQs(
                        DecodeOptions(allowDots = true, decodeDotInKeys = false)
                    ) shouldBe mapOf("name%2Eobj" to mapOf("first" to "John", "last" to "Doe"))

                URI.create("$testUrl?name%252Eobj.first=John&name%252Eobj.last=Doe")
                    .queryParametersQs(
                        DecodeOptions(allowDots = true, decodeDotInKeys = true)
                    ) shouldBe mapOf("name.obj" to mapOf("first" to "John", "last" to "Doe"))

                URI.create(
                        "$testUrl?name%252Eobj%252Esubobject.first%252Egodly%252Ename=John&name%252Eobj%252Esubobject.last=Doe"
                    )
                    .queryParametersQs(
                        DecodeOptions(allowDots = false, decodeDotInKeys = false)
                    ) shouldBe
                    mapOf(
                        "name%2Eobj%2Esubobject.first%2Egodly%2Ename" to "John",
                        "name%2Eobj%2Esubobject.last" to "Doe",
                    )

                URI.create(
                        "$testUrl?name.obj.subobject.first.godly.name=John&name.obj.subobject.last=Doe"
                    )
                    .queryParametersQs(
                        DecodeOptions(allowDots = true, decodeDotInKeys = false)
                    ) shouldBe
                    mapOf(
                        "name" to
                            mapOf(
                                "obj" to
                                    mapOf(
                                        "subobject" to
                                            mapOf(
                                                "first" to
                                                    mapOf("godly" to mapOf("name" to "John")),
                                                "last" to "Doe",
                                            )
                                    )
                            )
                    )

                URI.create(
                        "$testUrl?name%252Eobj%252Esubobject.first%252Egodly%252Ename=John&name%252Eobj%252Esubobject.last=Doe"
                    )
                    .queryParametersQs(
                        DecodeOptions(allowDots = true, decodeDotInKeys = true)
                    ) shouldBe
                    mapOf(
                        "name.obj.subobject" to mapOf("first.godly.name" to "John", "last" to "Doe")
                    )

                URI.create("$testUrl?name%252Eobj.first=John&name%252Eobj.last=Doe")
                    .queryParametersQs(null) shouldBe
                    mapOf("name%2Eobj.first" to "John", "name%2Eobj.last" to "Doe")

                URI.create("$testUrl?name%252Eobj.first=John&name%252Eobj.last=Doe")
                    .queryParametersQs(DecodeOptions(decodeDotInKeys = false)) shouldBe
                    mapOf("name%2Eobj.first" to "John", "name%2Eobj.last" to "Doe")

                URI.create("$testUrl?name%252Eobj.first=John&name%252Eobj.last=Doe")
                    .queryParametersQs(DecodeOptions(decodeDotInKeys = true)) shouldBe
                    mapOf("name.obj" to mapOf("first" to "John", "last" to "Doe"))
            }

            test(
                "should decode dot in key of map, and allow enabling dot notation when decodeDotInKeys is set to true and allowDots is undefined"
            ) {
                URI.create(
                        "$testUrl?name%252Eobj%252Esubobject.first%252Egodly%252Ename=John&name%252Eobj%252Esubobject.last=Doe"
                    )
                    .queryParametersQs(DecodeOptions(decodeDotInKeys = true)) shouldBe
                    mapOf(
                        "name.obj.subobject" to mapOf("first.godly.name" to "John", "last" to "Doe")
                    )
            }

            test("allows empty lists in obj values") {
                URI.create("$testUrl?foo${URLEncoder.encode("[]", "UTF-8")}&bar=baz")
                    .queryParametersQs(DecodeOptions(allowEmptyLists = true)) shouldBe
                    mapOf("foo" to emptyList<String>(), "bar" to "baz")

                URI.create("$testUrl?foo${URLEncoder.encode("[]", "UTF-8")}&bar=baz")
                    .queryParametersQs(DecodeOptions(allowEmptyLists = false)) shouldBe
                    mapOf("foo" to listOf(""), "bar" to "baz")
            }

            test("parses a single nested string") {
                URI.create("$testUrl?a${URLEncoder.encode("[b]", "UTF-8")}=c")
                    .queryParametersQs(null) shouldBe mapOf("a" to mapOf("b" to "c"))
            }

            test("parses a double nested string") {
                URI.create("$testUrl?a${URLEncoder.encode("[b][c]", "UTF-8")}=d")
                    .queryParametersQs(null) shouldBe mapOf("a" to mapOf("b" to mapOf("c" to "d")))
            }

            test("defaults to a depth of 5") {
                URI.create("$testUrl?a${URLEncoder.encode("[b][c][d][e][f][g][h]", "UTF-8")}=i")
                    .queryParametersQs(null) shouldBe
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
            }

            test("only parses one level when depth = 1") {
                URI.create("$testUrl?a${URLEncoder.encode("[b][c]", "UTF-8")}=d")
                    .queryParametersQs(DecodeOptions(depth = 1)) shouldBe
                    mapOf("a" to mapOf("b" to mapOf("[c]" to "d")))

                URI.create("$testUrl?a${URLEncoder.encode("[b][c][d]", "UTF-8")}=e")
                    .queryParametersQs(DecodeOptions(depth = 1)) shouldBe
                    mapOf("a" to mapOf("b" to mapOf("[c][d]" to "e")))
            }

            test("uses original key when depth = 0") {
                URI.create(
                        "$testUrl?a${URLEncoder.encode("[0]", "UTF-8")}=b&a${URLEncoder.encode("[1]", "UTF-8")}=c"
                    )
                    .queryParametersQs(DecodeOptions(depth = 0)) shouldBe
                    mapOf("a[0]" to "b", "a[1]" to "c")

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[0][0]", "UTF-8")}=b&a${
                    URLEncoder.encode(
                        "[0][1]", "UTF-8",
                    )
                }=c&a${URLEncoder.encode("[1]", "UTF-8")}=d&e=2"
                    )
                    .queryParametersQs(DecodeOptions(depth = 0)) shouldBe
                    mapOf("a[0][0]" to "b", "a[0][1]" to "c", "a[1]" to "d", "e" to "2")
            }

            test("parses a simple list") {
                URI.create("$testUrl?a=b&a=c").queryParametersQs(null) shouldBe
                    mapOf("a" to listOf("b", "c"))
            }

            test("parses an explicit list") {
                URI.create("$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=b")
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("b"))

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=b&a${URLEncoder.encode("[]", "UTF-8")}=c"
                    )
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("b", "c"))

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=b&a${
                    URLEncoder.encode(
                        "[]", "UTF-8",
                    )
                }=c&a${URLEncoder.encode("[]", "UTF-8")}=d"
                    )
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("b", "c", "d"))
            }

            test("parses a mix of simple and explicit lists") {
                URI.create("$testUrl?a=b&a${URLEncoder.encode("[]", "UTF-8")}=c")
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("b", "c"))

                URI.create("$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=b&a=c")
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("b", "c"))

                URI.create("$testUrl?a${URLEncoder.encode("[0]", "UTF-8")}=b&a=c")
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("b", "c"))

                URI.create("$testUrl?a=b&a${URLEncoder.encode("[0]", "UTF-8")}=c")
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("b", "c"))

                URI.create("$testUrl?a${URLEncoder.encode("[1]", "UTF-8")}=b&a=c")
                    .queryParametersQs(DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to listOf("b", "c"))

                URI.create("$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=b&a=c")
                    .queryParametersQs(DecodeOptions(listLimit = 0)) shouldBe
                    mapOf("a" to listOf("b", "c"))

                URI.create("$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=b&a=c")
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("b", "c"))

                URI.create("$testUrl?a=b&a${URLEncoder.encode("[1]", "UTF-8")}=c")
                    .queryParametersQs(DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to listOf("b", "c"))

                URI.create("$testUrl?a=b&a${URLEncoder.encode("[]", "UTF-8")}=c")
                    .queryParametersQs(DecodeOptions(listLimit = 0)) shouldBe
                    mapOf("a" to listOf("b", "c"))

                URI.create("$testUrl?a=b&a${URLEncoder.encode("[]", "UTF-8")}=c")
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("b", "c"))
            }

            test("parses a nested list") {
                URI.create(
                        "$testUrl?a${URLEncoder.encode("[b][]", "UTF-8")}=c&a${URLEncoder.encode("[b][]", "UTF-8")}=d"
                    )
                    .queryParametersQs(null) shouldBe mapOf("a" to mapOf("b" to listOf("c", "d")))

                URI.create("$testUrl?a${URLEncoder.encode("[>=]", "UTF-8")}=25")
                    .queryParametersQs(null) shouldBe mapOf("a" to mapOf(">=" to "25"))
            }

            test("allows to specify list indices") {
                URI.create(
                        "$testUrl?a${URLEncoder.encode("[1]", "UTF-8")}=c&a${
                    URLEncoder.encode(
                        "[0]", "UTF-8",
                    )
                }=b&a${URLEncoder.encode("[2]", "UTF-8")}=d"
                    )
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("b", "c", "d"))

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[1]", "UTF-8")}=c&a${URLEncoder.encode("[0]", "UTF-8")}=b"
                    )
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("b", "c"))

                URI.create("$testUrl?a${URLEncoder.encode("[1]", "UTF-8")}=c")
                    .queryParametersQs(DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to listOf("c"))

                URI.create("$testUrl?a${URLEncoder.encode("[1]", "UTF-8")}=c")
                    .queryParametersQs(DecodeOptions(listLimit = 0)) shouldBe
                    mapOf("a" to mapOf(1 to "c"))

                URI.create("$testUrl?a${URLEncoder.encode("[1]", "UTF-8")}=c")
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("c"))
            }

            test("limits specific list indices to listLimit") {
                URI.create("$testUrl?a${URLEncoder.encode("[20]", "UTF-8")}=a")
                    .queryParametersQs(DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to listOf("a"))

                URI.create("$testUrl?a${URLEncoder.encode("[21]", "UTF-8")}=a")
                    .queryParametersQs(DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to mapOf(21 to "a"))

                URI.create("$testUrl?a${URLEncoder.encode("[20]", "UTF-8")}=a")
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("a"))

                URI.create("$testUrl?a${URLEncoder.encode("[21]", "UTF-8")}=a")
                    .queryParametersQs(null) shouldBe mapOf("a" to mapOf(21 to "a"))
            }

            test("supports keys that begin with a number") {
                URI.create("$testUrl?a${URLEncoder.encode("[12b]", "UTF-8")}=c")
                    .queryParametersQs(null) shouldBe mapOf("a" to mapOf("12b" to "c"))
            }

            test("supports encoded = signs") {
                URI.create("$testUrl?he%3Dllo=th%3Dere").queryParametersQs(null) shouldBe
                    mapOf("he=llo" to "th=ere")
            }

            test("is ok with url encoded strings") {
                URI.create("$testUrl?a[b%20c]=d").queryParametersQs(null) shouldBe
                    mapOf("a" to mapOf("b c" to "d"))

                URI.create("$testUrl?a${URLEncoder.encode("[b]", "UTF-8")}=c%20d")
                    .queryParametersQs(null) shouldBe mapOf("a" to mapOf("b" to "c d"))
            }

            test("allows brackets in the value") {
                URI.create("$testUrl?pets=${URLEncoder.encode("[\"tobi\"]", "UTF-8")}")
                    .queryParametersQs(null) shouldBe mapOf("pets" to "[\"tobi\"]")

                URI.create("$testUrl?operators=${URLEncoder.encode("[\">=\", \"<=\"]", "UTF-8")}")
                    .queryParametersQs(null) shouldBe mapOf("operators" to "[\">=\", \"<=\"]")
            }

            test("allows empty values") {
                URI.create(testUrl).queryParametersQs(null) shouldBe emptyMap<String, Any?>()
            }

            test("transforms lists to maps") {
                URI.create(
                        "$testUrl?foo${URLEncoder.encode("[0]", "UTF-8")}=bar&foo${
                    URLEncoder.encode(
                        "[bad]", "UTF-8",
                    )
                }=baz"
                    )
                    .queryParametersQs(null) shouldBe
                    mapOf("foo" to mapOf(0 to "bar", "bad" to "baz"))

                URI.create(
                        "$testUrl?foo${URLEncoder.encode("[bad]", "UTF-8")}=baz&foo${
                    URLEncoder.encode(
                        "[0]", "UTF-8",
                    )
                }=bar"
                    )
                    .queryParametersQs(null) shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", 0 to "bar"))

                URI.create(
                        "$testUrl?foo${URLEncoder.encode("[bad]", "UTF-8")}=baz&foo${
                    URLEncoder.encode(
                        "[]", "UTF-8",
                    )
                }=bar"
                    )
                    .queryParametersQs(null) shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", 0 to "bar"))

                URI.create(
                        "$testUrl?foo${URLEncoder.encode("[]", "UTF-8")}=bar&foo${
                    URLEncoder.encode(
                        "[bad]", "UTF-8",
                    )
                }=baz"
                    )
                    .queryParametersQs(null) shouldBe
                    mapOf("foo" to mapOf(0 to "bar", "bad" to "baz"))

                URI.create(
                        "$testUrl?foo${URLEncoder.encode("[bad]", "UTF-8")}=baz&foo${
                    URLEncoder.encode(
                        "[]", "UTF-8",
                    )
                }=bar&foo${URLEncoder.encode("[]", "UTF-8")}=foo"
                    )
                    .queryParametersQs(null) shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", 0 to "bar", 1 to "foo"))

                URI.create(
                        "$testUrl?foo${URLEncoder.encode("[0][a]", "UTF-8")}=a&foo${
                    URLEncoder.encode(
                        "[0][b]", "UTF-8",
                    )
                }=b&foo${URLEncoder.encode("[1][a]", "UTF-8")}=aa&foo${URLEncoder.encode("[1][b]", "UTF-8")}=bb"
                    )
                    .queryParametersQs(null) shouldBe
                    mapOf(
                        "foo" to
                            listOf(mapOf("a" to "a", "b" to "b"), mapOf("a" to "aa", "b" to "bb"))
                    )
            }

            test("transforms lists to maps (dot notation)") {
                URI.create("$testUrl?foo${URLEncoder.encode("[0]", "UTF-8")}.baz=bar&fool.bad=baz")
                    .queryParametersQs(DecodeOptions(allowDots = true)) shouldBe
                    mapOf("foo" to listOf(mapOf("baz" to "bar")), "fool" to mapOf("bad" to "baz"))

                URI.create(
                        "$testUrl?foo${URLEncoder.encode("[0]", "UTF-8")}.baz=bar&fool.bad.boo=baz"
                    )
                    .queryParametersQs(DecodeOptions(allowDots = true)) shouldBe
                    mapOf(
                        "foo" to listOf(mapOf("baz" to "bar")),
                        "fool" to mapOf("bad" to mapOf("boo" to "baz")),
                    )

                URI.create(
                        "$testUrl?foo${URLEncoder.encode("[0][0]", "UTF-8")}.baz=bar&fool.bad=baz"
                    )
                    .queryParametersQs(DecodeOptions(allowDots = true)) shouldBe
                    mapOf(
                        "foo" to listOf(listOf(mapOf("baz" to "bar"))),
                        "fool" to mapOf("bad" to "baz"),
                    )

                URI.create(
                        "$testUrl?foo${URLEncoder.encode("[0]", "UTF-8")}.baz${
                    URLEncoder.encode(
                        "[0]", "UTF-8",
                    )
                }=15&foo${URLEncoder.encode("[0]", "UTF-8")}.bar=2"
                    )
                    .queryParametersQs(DecodeOptions(allowDots = true)) shouldBe
                    mapOf("foo" to listOf(mapOf("baz" to listOf("15"), "bar" to "2")))

                URI.create(
                        "$testUrl?foo${URLEncoder.encode("[0]", "UTF-8")}.baz${
                    URLEncoder.encode(
                        "[0]", "UTF-8",
                    )
                }=15&foo${URLEncoder.encode("[0]", "UTF-8")}.baz${
                    URLEncoder.encode(
                        "[1]", "UTF-8",
                    )
                }=16&foo${URLEncoder.encode("[0]", "UTF-8")}.bar=2"
                    )
                    .queryParametersQs(DecodeOptions(allowDots = true)) shouldBe
                    mapOf("foo" to listOf(mapOf("baz" to listOf("15", "16"), "bar" to "2")))

                URI.create("$testUrl?foo.bad=baz&foo${URLEncoder.encode("[0]", "UTF-8")}=bar")
                    .queryParametersQs(DecodeOptions(allowDots = true)) shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", 0 to "bar"))

                URI.create("$testUrl?foo.bad=baz&foo${URLEncoder.encode("[]", "UTF-8")}=bar")
                    .queryParametersQs(DecodeOptions(allowDots = true)) shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", 0 to "bar"))

                URI.create("$testUrl?foo${URLEncoder.encode("[]", "UTF-8")}=bar&foo.bad=baz")
                    .queryParametersQs(DecodeOptions(allowDots = true)) shouldBe
                    mapOf("foo" to mapOf(0 to "bar", "bad" to "baz"))

                URI.create(
                        "$testUrl?foo.bad=baz&foo${URLEncoder.encode("[]", "UTF-8")}=bar&foo${
                    URLEncoder.encode(
                        "[]", "UTF-8",
                    )
                }=foo"
                    )
                    .queryParametersQs(DecodeOptions(allowDots = true)) shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", 0 to "bar", 1 to "foo"))

                URI.create(
                        "$testUrl?foo${URLEncoder.encode("[0]", "UTF-8")}.a=a&foo${
                    URLEncoder.encode(
                        "[0]", "UTF-8",
                    )
                }.b=b&foo${URLEncoder.encode("[1]", "UTF-8")}.a=aa&foo${URLEncoder.encode("[1]", "UTF-8")}.b=bb"
                    )
                    .queryParametersQs(DecodeOptions(allowDots = true)) shouldBe
                    mapOf(
                        "foo" to
                            listOf(mapOf("a" to "a", "b" to "b"), mapOf("a" to "aa", "b" to "bb"))
                    )
            }

            test("correctly prunes undefined values when converting a list to a map") {
                URI.create(
                        "$testUrl?a${URLEncoder.encode("[2]", "UTF-8")}=b&a${
                    URLEncoder.encode(
                        "[99999999]", "UTF-8",
                    )
                }=c"
                    )
                    .queryParametersQs(null) shouldBe mapOf("a" to mapOf(2 to "b", 99999999 to "c"))
            }

            test("supports malformed uri characters") {
                URI.create("$testUrl?${URLEncoder.encode("{%:%}", "UTF-8")}")
                    .queryParametersQs(DecodeOptions(strictNullHandling = true)) shouldBe
                    mapOf("{%:%}" to null)

                URI.create("$testUrl?${URLEncoder.encode("{%:%}", "UTF-8")}=")
                    .queryParametersQs(null) shouldBe mapOf("{%:%}" to "")

                URI.create("$testUrl?foo=${URLEncoder.encode("%:%}", "UTF-8")}")
                    .queryParametersQs(null) shouldBe mapOf("foo" to "%:%}")
            }

            test("does not produce empty keys") {
                URI.create("$testUrl?_r=1&").queryParametersQs(null) shouldBe mapOf("_r" to "1")
            }

            test("parses lists of maps") {
                URI.create("$testUrl?a${URLEncoder.encode("[][b]", "UTF-8")}=c")
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf(mapOf("b" to "c")))

                URI.create("$testUrl?a${URLEncoder.encode("[0][b]", "UTF-8")}=c")
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf(mapOf("b" to "c")))
            }

            test("allows for empty strings in lists") {
                URI.create(
                        "$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=b&a${
                    URLEncoder.encode(
                        "[]", "UTF-8",
                    )
                }=&a${URLEncoder.encode("[]", "UTF-8")}=c"
                    )
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("b", "", "c"))

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[0]", "UTF-8")}=b&a${
                    URLEncoder.encode(
                        "[1]", "UTF-8",
                    )
                }&a${URLEncoder.encode("[2]", "UTF-8")}=c&a${URLEncoder.encode("[19]", "UTF-8")}="
                    )
                    .queryParametersQs(
                        DecodeOptions(strictNullHandling = true, listLimit = 20)
                    ) shouldBe mapOf("a" to listOf("b", null, "c", ""))

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=b&a${
                    URLEncoder.encode(
                        "[]", "UTF-8",
                    )
                }&a${URLEncoder.encode("[]", "UTF-8")}=c&a${URLEncoder.encode("[]", "UTF-8")}="
                    )
                    .queryParametersQs(
                        DecodeOptions(strictNullHandling = true, listLimit = 0)
                    ) shouldBe mapOf("a" to listOf("b", null, "c", ""))

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[0]", "UTF-8")}=b&a${
                    URLEncoder.encode(
                        "[1]", "UTF-8",
                    )
                }=&a${URLEncoder.encode("[2]", "UTF-8")}=c&a${URLEncoder.encode("[19]", "UTF-8")}"
                    )
                    .queryParametersQs(
                        DecodeOptions(strictNullHandling = true, listLimit = 20)
                    ) shouldBe mapOf("a" to listOf("b", "", "c", null))

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=b&a${
                    URLEncoder.encode(
                        "[]", "UTF-8",
                    )
                }=&a${URLEncoder.encode("[]", "UTF-8")}=c&a${URLEncoder.encode("[]", "UTF-8")}"
                    )
                    .queryParametersQs(
                        DecodeOptions(strictNullHandling = true, listLimit = 0)
                    ) shouldBe mapOf("a" to listOf("b", "", "c", null))

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=&a${
                    URLEncoder.encode(
                        "[]", "UTF-8",
                    )
                }=b&a${URLEncoder.encode("[]", "UTF-8")}=c"
                    )
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("", "b", "c"))
            }

            test("compacts sparse lists") {
                URI.create(
                        "$testUrl?a${URLEncoder.encode("[10]", "UTF-8")}=1&a${URLEncoder.encode("[2]", "UTF-8")}=2"
                    )
                    .queryParametersQs(DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to listOf("2", "1"))

                URI.create("$testUrl?a${URLEncoder.encode("[1][b][2][c]", "UTF-8")}=1")
                    .queryParametersQs(DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to listOf(mapOf("b" to listOf(mapOf("c" to "1")))))

                URI.create("$testUrl?a${URLEncoder.encode("[1][2][3][c]", "UTF-8")}=1")
                    .queryParametersQs(DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to listOf(listOf(listOf(mapOf("c" to "1")))))

                URI.create("$testUrl?a${URLEncoder.encode("[1][2][3][c][1]", "UTF-8")}=1")
                    .queryParametersQs(DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to listOf(listOf(listOf(mapOf("c" to listOf("1"))))))
            }

            test("parses semi-parsed strings") {
                URI.create("$testUrl?a${URLEncoder.encode("[b]", "UTF-8")}=c")
                    .queryParametersQs(null) shouldBe mapOf("a" to mapOf("b" to "c"))

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[b]", "UTF-8")}=c&a${URLEncoder.encode("[d]", "UTF-8")}=e"
                    )
                    .queryParametersQs(null) shouldBe mapOf("a" to mapOf("b" to "c", "d" to "e"))
            }

            test("parses jquery-param strings") {
                val encoded =
                    "filter%5B0%5D%5B%5D=int1&filter%5B0%5D%5B%5D=%3D&filter%5B0%5D%5B%5D=77&filter%5B%5D=and&filter%5B2%5D%5B%5D=int2&filter%5B2%5D%5B%5D=%3D&filter%5B2%5D%5B%5D=8"
                val expected =
                    mapOf(
                        "filter" to
                            listOf(listOf("int1", "=", "77"), "and", listOf("int2", "=", "8"))
                    )
                URI.create("$testUrl?$encoded").queryParametersQs(null) shouldBe expected
            }

            test("continues parsing when no parent is found") {
                URI.create("$testUrl?${URLEncoder.encode("[]", "UTF-8")}&a=b")
                    .queryParametersQs(DecodeOptions(strictNullHandling = true)) shouldBe
                    mapOf(0 to null, "a" to "b")

                URI.create("$testUrl?${URLEncoder.encode("[foo]", "UTF-8")}=bar")
                    .queryParametersQs(null) shouldBe mapOf("foo" to "bar")
            }

            test("does not error when parsing a very long list") {
                val str = buildString {
                    append("a${URLEncoder.encode("[]", "UTF-8")}=a")
                    while (toString().toByteArray().size < 128 * 1024) {
                        append("&")
                        append(this)
                    }
                }

                shouldNotThrow<Exception> { URI.create("$testUrl?$str").queryParametersQs(null) }
            }

            test("parses a string with an alternative string delimiter") {
                URI.create("$testUrl?a=b;c=d")
                    .queryParametersQs(DecodeOptions(delimiter = StringDelimiter(";"))) shouldBe
                    mapOf("a" to "b", "c" to "d")
            }

            test("parses a string with an alternative RegExp delimiter") {
                URI.create("$testUrl?a=b;${URLEncoder.encode(" ", "UTF-8")}c=d")
                    .queryParametersQs(
                        DecodeOptions(delimiter = RegexDelimiter("[;,][%20|+]*"))
                    ) shouldBe mapOf("a" to "b", "c" to "d")
            }

            test("allows overriding parameter limit") {
                URI.create("$testUrl?a=b&c=d")
                    .queryParametersQs(DecodeOptions(parameterLimit = 1)) shouldBe mapOf("a" to "b")
            }

            test("allows setting the parameter limit to Int.MAX_VALUE") {
                URI.create("$testUrl?a=b&c=d")
                    .queryParametersQs(DecodeOptions(parameterLimit = Int.MAX_VALUE)) shouldBe
                    mapOf("a" to "b", "c" to "d")
            }

            test("allows overriding list limit") {
                URI.create("$testUrl?a${URLEncoder.encode("[0]", "UTF-8")}=b")
                    .queryParametersQs(DecodeOptions(listLimit = -1)) shouldBe
                    mapOf("a" to mapOf(0 to "b"))

                URI.create("$testUrl?a${URLEncoder.encode("[0]", "UTF-8")}=b")
                    .queryParametersQs(DecodeOptions(listLimit = 0)) shouldBe
                    mapOf("a" to listOf("b"))

                URI.create("$testUrl?a${URLEncoder.encode("[-1]", "UTF-8")}=b")
                    .queryParametersQs(DecodeOptions(listLimit = -1)) shouldBe
                    mapOf("a" to mapOf("-1" to "b"))

                URI.create("$testUrl?a${URLEncoder.encode("[-1]", "UTF-8")}=b")
                    .queryParametersQs(DecodeOptions(listLimit = 0)) shouldBe
                    mapOf("a" to mapOf("-1" to "b"))

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[0]", "UTF-8")}=b&a${URLEncoder.encode("[1]", "UTF-8")}=c"
                    )
                    .queryParametersQs(DecodeOptions(listLimit = -1)) shouldBe
                    mapOf("a" to mapOf(0 to "b", 1 to "c"))

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[0]", "UTF-8")}=b&a${URLEncoder.encode("[1]", "UTF-8")}=c"
                    )
                    .queryParametersQs(DecodeOptions(listLimit = 0)) shouldBe
                    mapOf("a" to mapOf(0 to "b", 1 to "c"))
            }

            test("allows disabling list parsing") {
                URI.create(
                        "$testUrl?a${URLEncoder.encode("[0]", "UTF-8")}=b&a${URLEncoder.encode("[1]", "UTF-8")}=c"
                    )
                    .queryParametersQs(DecodeOptions(parseLists = false)) shouldBe
                    mapOf("a" to mapOf(0 to "b", 1 to "c"))

                URI.create("$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=b")
                    .queryParametersQs(DecodeOptions(parseLists = false)) shouldBe
                    mapOf("a" to mapOf(0 to "b"))
            }

            test("allows for query string prefix") {
                URI.create("$testUrl??foo=bar")
                    .queryParametersQs(DecodeOptions(ignoreQueryPrefix = true)) shouldBe
                    mapOf("foo" to "bar")

                URI.create("$testUrl?foo=bar")
                    .queryParametersQs(DecodeOptions(ignoreQueryPrefix = true)) shouldBe
                    mapOf("foo" to "bar")

                URI.create("$testUrl??foo=bar")
                    .queryParametersQs(DecodeOptions(ignoreQueryPrefix = false)) shouldBe
                    mapOf("?foo" to "bar")
            }

            test("parses string with comma as list divider") {
                URI.create("$testUrl?foo=bar,tee")
                    .queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to listOf("bar", "tee"))

                URI.create("$testUrl?foo${URLEncoder.encode("[bar]", "UTF-8")}=coffee,tee")
                    .queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to mapOf("bar" to listOf("coffee", "tee")))

                URI.create("$testUrl?foo=").queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to "")

                URI.create("$testUrl?foo").queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to "")

                URI.create("$testUrl?foo")
                    .queryParametersQs(
                        DecodeOptions(comma = true, strictNullHandling = true)
                    ) shouldBe mapOf("foo" to null)

                URI.create("$testUrl?a${URLEncoder.encode("[0]", "UTF-8")}=c")
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("c"))

                URI.create("$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=c")
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("c"))

                URI.create("$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=c")
                    .queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("a" to listOf("c"))

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[0]", "UTF-8")}=c&a${URLEncoder.encode("[1]", "UTF-8")}=d"
                    )
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("c", "d"))

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=c&a${URLEncoder.encode("[]", "UTF-8")}=d"
                    )
                    .queryParametersQs(null) shouldBe mapOf("a" to listOf("c", "d"))

                URI.create("$testUrl?a=c,d").queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("a" to listOf("c", "d"))
            }

            test(
                "use number decoder, parses string that has one number with comma option enabled"
            ) {
                val decoder: (str: String?, charset: Charset?) -> Any? = { str, _ ->
                    str?.toIntOrNull() ?: str?.toDoubleOrNull() ?: str
                }

                URI.create("$testUrl?foo=1")
                    .queryParametersQs(DecodeOptions(comma = true, decoder = decoder)) shouldBe
                    mapOf("foo" to 1)

                URI.create("$testUrl?foo=0")
                    .queryParametersQs(DecodeOptions(comma = true, decoder = decoder)) shouldBe
                    mapOf("foo" to 0)
            }

            test(
                "parses brackets holds list of lists when having two parts of strings with comma as list divider"
            ) {
                URI.create(
                        "$testUrl?foo${URLEncoder.encode("[]", "UTF-8")}=1,2,3&foo${
                    URLEncoder.encode(
                        "[]", "UTF-8",
                    )
                }=4,5,6"
                    )
                    .queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to listOf(listOf("1", "2", "3"), listOf("4", "5", "6")))

                URI.create(
                        "$testUrl?foo${URLEncoder.encode("[]", "UTF-8")}=1,2,3&foo${URLEncoder.encode("[]", "UTF-8")}="
                    )
                    .queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to listOf(listOf("1", "2", "3"), ""))

                URI.create(
                        "$testUrl?foo${URLEncoder.encode("[]", "UTF-8")}=1,2,3&foo${URLEncoder.encode("[]", "UTF-8")}=,"
                    )
                    .queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to listOf(listOf("1", "2", "3"), listOf("", "")))

                URI.create(
                        "$testUrl?foo${URLEncoder.encode("[]", "UTF-8")}=1,2,3&foo${URLEncoder.encode("[]", "UTF-8")}=a"
                    )
                    .queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to listOf(listOf("1", "2", "3"), "a"))
            }

            test(
                "parses comma delimited list while having percent-encoded comma treated as normal text"
            ) {
                URI.create("$testUrl?foo=a%2Cb")
                    .queryParametersQs(DecodeOptions(comma = true)) shouldBe mapOf("foo" to "a,b")

                URI.create("$testUrl?foo=a%2C%20b,d")
                    .queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to listOf("a, b", "d"))

                URI.create("$testUrl?foo=a%2C%20b,c%2C%20d")
                    .queryParametersQs(DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to listOf("a, b", "c, d"))
            }

            test("params starting with a closing bracket") {
                URI.create("$testUrl?${URLEncoder.encode("]", "UTF-8")}=toString")
                    .queryParametersQs(null) shouldBe mapOf("]" to "toString")

                URI.create("$testUrl?${URLEncoder.encode("]]", "UTF-8")}=toString")
                    .queryParametersQs(null) shouldBe mapOf("]]" to "toString")

                URI.create("$testUrl?${URLEncoder.encode("]hello]", "UTF-8")}=toString")
                    .queryParametersQs(null) shouldBe mapOf("]hello]" to "toString")
            }

            test("params starting with a starting bracket") {
                URI.create("$testUrl?${URLEncoder.encode("[", "UTF-8")}=toString")
                    .queryParametersQs(null) shouldBe mapOf("[" to "toString")

                URI.create("$testUrl?${URLEncoder.encode("[[", "UTF-8")}=toString")
                    .queryParametersQs(null) shouldBe mapOf("[[" to "toString")

                URI.create("$testUrl?${URLEncoder.encode("[hello[", "UTF-8")}=toString")
                    .queryParametersQs(null) shouldBe mapOf("[hello[" to "toString")
            }

            test("add keys to maps") {
                URI.create("$testUrl?a${URLEncoder.encode("[b]", "UTF-8")}=c")
                    .queryParametersQs(null) shouldBe mapOf("a" to mapOf("b" to "c"))
            }

            test("can return null maps") {
                val expected = mutableMapOf<String, Any?>()
                expected["a"] =
                    mutableMapOf<String, Any?>().apply {
                        put("b", "c")
                        put("hasOwnProperty", "d")
                    }

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[b]", "UTF-8")}=c&a${
                    URLEncoder.encode(
                        "[hasOwnProperty]", "UTF-8",
                    )
                }=d"
                    )
                    .queryParametersQs(null) shouldBe expected

                val expectedList = mutableMapOf<Any, Any?>()
                expectedList["a"] =
                    mutableMapOf<Any, Any?>().apply {
                        put(0, "b")
                        put("c", "d")
                    }

                URI.create(
                        "$testUrl?a${URLEncoder.encode("[]", "UTF-8")}=b&a${URLEncoder.encode("[c]", "UTF-8")}=d"
                    )
                    .queryParametersQs(null) shouldBe expectedList
            }

            test("can parse with custom encoding") {
                val expected = mapOf("県" to "大阪府")

                val decoder: (String?, Charset?) -> String? = { str, _ ->
                    if (str == null) {
                        null
                    } else {
                        val regex = Regex("%([0-9A-F]{2})", RegexOption.IGNORE_CASE)
                        val result = mutableListOf<Byte>()
                        var remaining = str

                        while (!remaining.isNullOrEmpty()) {
                            val match = regex.find(remaining)
                            if (match != null) {
                                val hexValue = match.groupValues[1]
                                result.add(hexValue.toInt(16).toByte())
                                remaining = remaining.substring(match.range.last + 1)
                            } else {
                                break
                            }
                        }

                        // Note: Shift_JIS decoding would need a proper implementation
                        // This is a simplified version for demonstration
                        String(result.toByteArray(), Charset.forName("Shift_JIS"))
                    }
                }

                URI.create("$testUrl?%8c%a7=%91%e5%8d%e3%95%7b")
                    .queryParametersQs(DecodeOptions(decoder = decoder)) shouldBe expected
            }

            test("parses an iso-8859-1 string if asked to") {
                val expected = mapOf("¢" to "½")

                URI.create("$testUrl?%A2=%BD")
                    .queryParametersQs(
                        DecodeOptions(charset = Charset.forName("ISO-8859-1"))
                    ) shouldBe expected
            }

            context("charset") {
                test("throws an exception when given an unknown charset") {
                    shouldThrow<Exception> {
                        URI.create("$testUrl?a=b")
                            .queryParametersQs(
                                DecodeOptions(charset = Charset.forName("Unknown-Charset"))
                            )
                    }
                }

                val urlEncodedCheckmarkInUtf8 = "%E2%9C%93"
                val urlEncodedOSlashInUtf8 = "%C3%B8"
                val urlEncodedNumCheckmark = "%26%2310003%3B"
                val urlEncodedNumSmiley = "%26%239786%3B"

                test(
                    "prefers an utf-8 charset specified by the utf8 sentinel to a default charset of iso-8859-1"
                ) {
                    URI.create(
                            "$testUrl?utf8=$urlEncodedCheckmarkInUtf8&$urlEncodedOSlashInUtf8=$urlEncodedOSlashInUtf8"
                        )
                        .queryParametersQs(
                            DecodeOptions(
                                charsetSentinel = true,
                                charset = Charset.forName("ISO-8859-1"),
                            )
                        ) shouldBe mapOf("ø" to "ø")
                }

                test(
                    "prefers an iso-8859-1 charset specified by the utf8 sentinel to a default charset of utf-8"
                ) {
                    URI.create(
                            "$testUrl?utf8=$urlEncodedNumCheckmark&$urlEncodedOSlashInUtf8=$urlEncodedOSlashInUtf8"
                        )
                        .queryParametersQs(
                            DecodeOptions(
                                charsetSentinel = true,
                                charset = Charset.forName("UTF-8"),
                            )
                        ) shouldBe mapOf("Ã¸" to "Ã¸")
                }

                test(
                    "does not require the utf8 sentinel to be defined before the parameters whose decoding it affects"
                ) {
                    URI.create("$testUrl?a=$urlEncodedOSlashInUtf8&utf8=$urlEncodedNumCheckmark")
                        .queryParametersQs(
                            DecodeOptions(
                                charsetSentinel = true,
                                charset = Charset.forName("UTF-8"),
                            )
                        ) shouldBe mapOf("a" to "Ã¸")
                }

                test("should ignore an utf8 sentinel with an unknown value") {
                    URI.create("$testUrl?utf8=foo&$urlEncodedOSlashInUtf8=$urlEncodedOSlashInUtf8")
                        .queryParametersQs(
                            DecodeOptions(
                                charsetSentinel = true,
                                charset = Charset.forName("UTF-8"),
                            )
                        ) shouldBe mapOf("ø" to "ø")
                }

                test("uses the utf8 sentinel to switch to utf-8 when no default charset is given") {
                    URI.create(
                            "$testUrl?utf8=$urlEncodedCheckmarkInUtf8&$urlEncodedOSlashInUtf8=$urlEncodedOSlashInUtf8"
                        )
                        .queryParametersQs(DecodeOptions(charsetSentinel = true)) shouldBe
                        mapOf("ø" to "ø")
                }

                test(
                    "uses the utf8 sentinel to switch to iso-8859-1 when no default charset is given"
                ) {
                    URI.create(
                            "$testUrl?utf8=$urlEncodedNumCheckmark&$urlEncodedOSlashInUtf8=$urlEncodedOSlashInUtf8"
                        )
                        .queryParametersQs(DecodeOptions(charsetSentinel = true)) shouldBe
                        mapOf("Ã¸" to "Ã¸")
                }

                test("interprets numeric entities in iso-8859-1 when interpretNumericEntities") {
                    URI.create("$testUrl?foo=$urlEncodedNumSmiley")
                        .queryParametersQs(
                            DecodeOptions(
                                charset = Charset.forName("ISO-8859-1"),
                                interpretNumericEntities = true,
                            )
                        ) shouldBe mapOf("foo" to "☺")
                }

                test(
                    "handles a custom decoder returning null, in the iso-8859-1 charset, when interpretNumericEntities"
                ) {
                    val decoder: (String?, Charset?) -> String? = { str, charset ->
                        if (!str.isNullOrEmpty()) {
                            // Simulate Utils.decode functionality
                            URLDecoder.decode(str, charset?.name() ?: "UTF-8")
                        } else {
                            null
                        }
                    }

                    URI.create("$testUrl?foo=&bar=$urlEncodedNumSmiley")
                        .queryParametersQs(
                            DecodeOptions(
                                charset = Charset.forName("ISO-8859-1"),
                                decoder = decoder,
                                interpretNumericEntities = true,
                            )
                        ) shouldBe mapOf("foo" to null, "bar" to "☺")
                }

                test(
                    "does not interpret numeric entities in iso-8859-1 when interpretNumericEntities is absent"
                ) {
                    URI.create("$testUrl?foo=$urlEncodedNumSmiley")
                        .queryParametersQs(
                            DecodeOptions(charset = Charset.forName("ISO-8859-1"))
                        ) shouldBe mapOf("foo" to "&#9786;")
                }

                test(
                    "does not interpret numeric entities when the charset is utf-8, even when interpretNumericEntities"
                ) {
                    URI.create("$testUrl?foo=$urlEncodedNumSmiley")
                        .queryParametersQs(
                            DecodeOptions(
                                charset = Charset.forName("UTF-8"),
                                interpretNumericEntities = true,
                            )
                        ) shouldBe mapOf("foo" to "&#9786;")
                }

                test("does not interpret %uXXXX syntax in iso-8859-1 mode") {
                    URI.create(
                            "$testUrl?${URLEncoder.encode("%u263A", "UTF-8")}=${URLEncoder.encode("%u263A", "UTF-8")}"
                        )
                        .queryParametersQs(
                            DecodeOptions(charset = Charset.forName("ISO-8859-1"))
                        ) shouldBe mapOf("%u263A" to "%u263A")
                }
            }
        }

        context("URI.toStringQs") {
            test("encodes a query string object") {
                // Test empty URI first
                URI("https://$authority$path").toStringQs() shouldBe "https://$authority$path"

                // Test cases with parameters
                val testCases =
                    listOf(
                        mapOf("a" to "b") to "$testUrl?a=b",
                        mapOf("a" to "1") to "$testUrl?a=1",
                        mapOf("a" to "1", "b" to "2") to "$testUrl?a=1&b=2",
                        mapOf("a" to "A_Z") to "$testUrl?a=A_Z",
                        mapOf("a" to "€") to "$testUrl?a=%E2%82%AC",
                        mapOf("a" to "\uE000") to "$testUrl?a=%EE%80%80",
                        mapOf("a" to "א") to "$testUrl?a=%D7%90",
                        mapOf("a" to "\uD801\uDC37") to "$testUrl?a=%F0%90%90%B7",
                        mapOf("a" to "b", "c" to "d") to "$testUrl?a=b&c=d",
                        mapOf("a" to "b", "c" to "d", "e" to "f") to "$testUrl?a=b&c=d&e=f",
                        mapOf("a" to "b", "c" to "d", "e" to "f", "g" to "h") to
                            "$testUrl?a=b&c=d&e=f&g=h",
                        mapOf("a" to listOf("b", "c", "d"), "e" to "f") to
                            "$testUrl?a=b&a=c&a=d&e=f",
                    )

                testCases.forEach { (params, expected) ->
                    createTestUri(params).toStringQs() shouldBe expected
                }
            }

            test("empty map yields no query string") {
                createTestUri(emptyMap()).toStringQs() shouldBe
                    URI("https://$authority$path").toString()
            }

            test("single key with empty string value") {
                createTestUri(mapOf("a" to "")).toStringQs() shouldBe "$testUrl?a="
            }

            test("null value is not skipped") {
                createTestUri(mapOf("a" to null, "b" to "2")).toStringQs() shouldBe
                    "$testUrl?a=&b=2"
            }

            test("keys with special characters are encoded") {
                createTestUri(mapOf("a b" to "c d")).toStringQs() shouldBe "$testUrl?a%20b=c%20d"
                createTestUri(mapOf("ä" to "ö")).toStringQs() shouldBe "$testUrl?%C3%A4=%C3%B6"
            }

            test("values containing reserved characters") {
                createTestUri(mapOf("q" to "foo@bar.com")).toStringQs() shouldBe
                    "$testUrl?q=foo%40bar.com"
                createTestUri(mapOf("path" to "/home")).toStringQs() shouldBe
                    "$testUrl?path=%2Fhome"
            }

            test("plus sign and space in value") {
                createTestUri(mapOf("v" to "a+b c")).toStringQs() shouldBe "$testUrl?v=a%2Bb%20c"
            }

            test("list values including numbers and empty strings") {
                createTestUri(mapOf("x" to listOf("1", "", "3"))).toStringQs() shouldBe
                    "$testUrl?x=1&x=&x=3"
            }

            test("multiple keys maintain insertion order") {
                createTestUri(linkedMapOf("first" to "1", "second" to "2", "third" to "3"))
                    .toStringQs() shouldBe "$testUrl?first=1&second=2&third=3"
            }

            test("handles URIs with port numbers") {
                val uriWithPort = URI.create("https://example.com:8080/path?a=b&c=d")
                uriWithPort.toStringQs() shouldBe "https://example.com:8080/path?a=b&c=d"

                val uriWithCustomPort = URI.create("http://localhost:3000/api")
                uriWithCustomPort.toStringQs() shouldBe "http://localhost:3000/api"

                val uriWithPortAndQuery =
                    URI.create("https://test.example.com:9090/endpoint?key=value")
                uriWithPortAndQuery.toStringQs() shouldBe
                    "https://test.example.com:9090/endpoint?key=value"

                // Test with non-standard port
                val uriWithNonStandardPort =
                    URI.create("ftp://files.example.com:2121/download?file=test.zip")
                uriWithNonStandardPort.toStringQs() shouldBe
                    "ftp://files.example.com:2121/download?file=test.zip"
            }
        }
    })

private val authority = "test.local"
private val path = "/example"
private val testUrl = "https://$authority$path"

// Helper function to create test URIs with query parameters
private fun createTestUri(params: Map<String, Any?>): URI {
    val baseUri = URI.create("https://$authority$path")
    if (params.isEmpty()) return baseUri

    // Build unencoded query string exactly as the tests expect
    val queryParts = mutableListOf<String>()
    params.forEach { (key, value) ->
        when (value) {
            is List<*> -> value.forEach { item -> queryParts.add("$key=${item ?: ""}") }

            null -> queryParts.add("$key=")
            else -> queryParts.add("$key=$value")
        }
    }

    val queryString = queryParts.joinToString("&")

    // Use URI constructor that accepts raw query string without validation
    return URI(baseUri.scheme, baseUri.authority, baseUri.path, queryString, baseUri.fragment)
}
