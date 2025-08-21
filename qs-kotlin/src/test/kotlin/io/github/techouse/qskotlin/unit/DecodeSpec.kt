package io.github.techouse.qskotlin.unit

import io.github.techouse.qskotlin.decode
import io.github.techouse.qskotlin.encode
import io.github.techouse.qskotlin.enums.Duplicates
import io.github.techouse.qskotlin.fixtures.data.EmptyTestCases
import io.github.techouse.qskotlin.internal.Utils
import io.github.techouse.qskotlin.models.DecodeOptions
import io.github.techouse.qskotlin.models.Decoder
import io.github.techouse.qskotlin.models.RegexDelimiter
import io.github.techouse.qskotlin.models.StringDelimiter
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.seconds

class DecodeSpec :
    DescribeSpec({
        describe("decode") {
            it("throws IllegalArgumentException when parameter limit is not positive") {
                shouldThrow<IllegalArgumentException> {
                    decode("a=b&c=d", DecodeOptions(parameterLimit = 0))
                }
            }

            it("Nested list handling in parseObject method") {
                // This test targets nested list handling in the parseObject method
                // We need to create a scenario where value is a List and parentKey exists in the
                // list

                // First, create a list with a nested list at index 0
                val list = listOf(listOf("nested"))

                // Convert to a query string
                val queryString = encode(mapOf("a" to list))

                // Now decode it back, which should exercise the code path we're targeting
                val result = decode(queryString)

                // Verify the result
                result shouldBe mapOf("a" to listOf(listOf("nested")))

                // Try another approach with a more complex structure
                // This creates a query string like 'a[0][0]=value'
                val result2 = decode("a[0][0]=value", DecodeOptions(depth = 5))

                // This should create a nested list structure
                result2 shouldBe mapOf("a" to listOf(listOf("value")))

                // Try a more complex approach that should trigger the specific code path
                // First, create a query string that will create a list with a specific index
                val queryString3 = "a[0][]=first&a[0][]=second"

                // Now decode it, which should create a list with a nested list
                val result3 = decode(queryString3)

                // Verify the result
                result3 shouldBe mapOf("a" to listOf(listOf("first", "second")))

                // Now try to add to the existing list
                val queryString4 = "a[0][2]=third"

                // Decode it with the existing result as the input
                val result4 = decode(queryString4)

                // Verify the result
                result4 shouldBe mapOf("a" to listOf(listOf("third")))
            }

            it("throws IllegalArgumentException if the input is not a String or a Map") {
                shouldThrow<IllegalArgumentException> { decode(123) }
            }

            it("parses a simple string") {
                decode("0=foo") shouldBe mapOf("0" to "foo")
                decode("foo=c++") shouldBe mapOf("foo" to "c  ")
                decode("a[>=]=23") shouldBe mapOf("a" to mapOf(">=" to "23"))
                decode("a[<=>]==23") shouldBe mapOf("a" to mapOf("<=>" to "=23"))
                decode("a[==]=23") shouldBe mapOf("a" to mapOf("==" to "23"))
                decode("foo", DecodeOptions(strictNullHandling = true)) shouldBe
                    mapOf("foo" to null)
                decode("foo") shouldBe mapOf("foo" to "")
                decode("foo=") shouldBe mapOf("foo" to "")
                decode("foo=bar") shouldBe mapOf("foo" to "bar")
                decode(" foo = bar = baz ") shouldBe mapOf(" foo " to " bar = baz ")
                decode("foo=bar=baz") shouldBe mapOf("foo" to "bar=baz")
                decode("foo=bar&bar=baz") shouldBe mapOf("foo" to "bar", "bar" to "baz")
                decode("foo2=bar2&baz2=") shouldBe mapOf("foo2" to "bar2", "baz2" to "")
                decode("foo=bar&baz", DecodeOptions(strictNullHandling = true)) shouldBe
                    mapOf("foo" to "bar", "baz" to null)
                decode("foo=bar&baz") shouldBe mapOf("foo" to "bar", "baz" to "")
                decode("cht=p3&chd=t:60,40&chs=250x100&chl=Hello|World") shouldBe
                    mapOf(
                        "cht" to "p3",
                        "chd" to "t:60,40",
                        "chs" to "250x100",
                        "chl" to "Hello|World",
                    )
            }

            it("comma: false") {
                decode("a[]=b&a[]=c") shouldBe mapOf("a" to listOf("b", "c"))
                decode("a[0]=b&a[1]=c") shouldBe mapOf("a" to listOf("b", "c"))
                decode("a=b,c") shouldBe mapOf("a" to "b,c")
                decode("a=b&a=c") shouldBe mapOf("a" to listOf("b", "c"))
            }

            it("comma: true") {
                decode("a[]=b&a[]=c", DecodeOptions(comma = true)) shouldBe
                    mapOf("a" to listOf("b", "c"))
                decode("a[0]=b&a[1]=c", DecodeOptions(comma = true)) shouldBe
                    mapOf("a" to listOf("b", "c"))
                decode("a=b,c", DecodeOptions(comma = true)) shouldBe mapOf("a" to listOf("b", "c"))
                decode("a=b&a=c", DecodeOptions(comma = true)) shouldBe
                    mapOf("a" to listOf("b", "c"))
            }

            it("comma: true with list limit exceeded throws error") {
                shouldThrow<IndexOutOfBoundsException> {
                        decode(
                            "a=b,c,d,e,f",
                            DecodeOptions(comma = true, throwOnLimitExceeded = true, listLimit = 3),
                        )
                    }
                    .message shouldBe "List limit exceeded. Only 3 elements allowed in a list."
            }

            it("allows enabling dot notation") {
                decode("a.b=c") shouldBe mapOf("a.b" to "c")
                decode("a.b=c", DecodeOptions(allowDots = true)) shouldBe
                    mapOf("a" to mapOf("b" to "c"))
            }

            it("decode dot keys correctly") {
                decode(
                    "name%252Eobj.first=John&name%252Eobj.last=Doe",
                    DecodeOptions(allowDots = false, decodeDotInKeys = false),
                ) shouldBe mapOf("name%2Eobj.first" to "John", "name%2Eobj.last" to "Doe")
                decode(
                    "name.obj.first=John&name.obj.last=Doe",
                    DecodeOptions(allowDots = true, decodeDotInKeys = false),
                ) shouldBe
                    mapOf("name" to mapOf("obj" to mapOf("first" to "John", "last" to "Doe")))
                decode(
                    "name%252Eobj.first=John&name%252Eobj.last=Doe",
                    DecodeOptions(allowDots = true, decodeDotInKeys = false),
                ) shouldBe mapOf("name%2Eobj" to mapOf("first" to "John", "last" to "Doe"))
                decode(
                    "name%252Eobj.first=John&name%252Eobj.last=Doe",
                    DecodeOptions(allowDots = true, decodeDotInKeys = true),
                ) shouldBe mapOf("name.obj" to mapOf("first" to "John", "last" to "Doe"))
                decode(
                    "name%252Eobj%252Esubobject.first%252Egodly%252Ename=John&name%252Eobj%252Esubobject.last=Doe",
                    DecodeOptions(allowDots = false, decodeDotInKeys = false),
                ) shouldBe
                    mapOf(
                        "name%2Eobj%2Esubobject.first%2Egodly%2Ename" to "John",
                        "name%2Eobj%2Esubobject.last" to "Doe",
                    )
                decode(
                    "name.obj.subobject.first.godly.name=John&name.obj.subobject.last=Doe",
                    DecodeOptions(allowDots = true, decodeDotInKeys = false),
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
                decode(
                    "name%252Eobj%252Esubobject.first%252Egodly%252Ename=John&name%252Eobj%252Esubobject.last=Doe",
                    DecodeOptions(allowDots = true, decodeDotInKeys = true),
                ) shouldBe
                    mapOf(
                        "name.obj.subobject" to mapOf("first.godly.name" to "John", "last" to "Doe")
                    )
                decode("name%252Eobj.first=John&name%252Eobj.last=Doe") shouldBe
                    mapOf("name%2Eobj.first" to "John", "name%2Eobj.last" to "Doe")
                decode(
                    "name%252Eobj.first=John&name%252Eobj.last=Doe",
                    DecodeOptions(decodeDotInKeys = false),
                ) shouldBe mapOf("name%2Eobj.first" to "John", "name%2Eobj.last" to "Doe")
                decode(
                    "name%252Eobj.first=John&name%252Eobj.last=Doe",
                    DecodeOptions(decodeDotInKeys = true),
                ) shouldBe mapOf("name.obj" to mapOf("first" to "John", "last" to "Doe"))
            }

            it(
                "should decode dot in key of map, and allow enabling dot notation when decodeDotInKeys is set to true and allowDots is undefined"
            ) {
                decode(
                    "name%252Eobj%252Esubobject.first%252Egodly%252Ename=John&name%252Eobj%252Esubobject.last=Doe",
                    DecodeOptions(decodeDotInKeys = true),
                ) shouldBe
                    mapOf(
                        "name.obj.subobject" to mapOf("first.godly.name" to "John", "last" to "Doe")
                    )
            }

            it("allows empty lists in obj values") {
                decode("foo[]&bar=baz", DecodeOptions(allowEmptyLists = true)) shouldBe
                    mapOf("foo" to emptyList<String>(), "bar" to "baz")
                decode("foo[]&bar=baz", DecodeOptions(allowEmptyLists = false)) shouldBe
                    mapOf("foo" to listOf(""), "bar" to "baz")
            }

            it("allowEmptyLists + strictNullHandling") {
                decode(
                    "testEmptyList[]",
                    DecodeOptions(strictNullHandling = true, allowEmptyLists = true),
                ) shouldBe mapOf("testEmptyList" to emptyList<String>())
            }

            it("parses a single nested string") {
                decode("a[b]=c") shouldBe mapOf("a" to mapOf("b" to "c"))
            }

            it("parses a double nested string") {
                decode("a[b][c]=d") shouldBe mapOf("a" to mapOf("b" to mapOf("c" to "d")))
            }

            it("defaults to a depth of 5") {
                decode("a[b][c][d][e][f][g][h]=i") shouldBe
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

            it("only parses one level when depth = 1") {
                decode("a[b][c]=d", DecodeOptions(depth = 1)) shouldBe
                    mapOf("a" to mapOf("b" to mapOf("[c]" to "d")))
                decode("a[b][c][d]=e", DecodeOptions(depth = 1)) shouldBe
                    mapOf("a" to mapOf("b" to mapOf("[c][d]" to "e")))
            }

            it("uses original key when depth = 0") {
                decode("a[0]=b&a[1]=c", DecodeOptions(depth = 0)) shouldBe
                    mapOf("a[0]" to "b", "a[1]" to "c")
                decode("a[0][0]=b&a[0][1]=c&a[1]=d&e=2", DecodeOptions(depth = 0)) shouldBe
                    mapOf("a[0][0]" to "b", "a[0][1]" to "c", "a[1]" to "d", "e" to "2")
            }

            it("parses a simple list") { decode("a=b&a=c") shouldBe mapOf("a" to listOf("b", "c")) }

            it("parses an explicit list") {
                decode("a[]=b") shouldBe mapOf("a" to listOf("b"))
                decode("a[]=b&a[]=c") shouldBe mapOf("a" to listOf("b", "c"))
                decode("a[]=b&a[]=c&a[]=d") shouldBe mapOf("a" to listOf("b", "c", "d"))
            }

            it("parses a mix of simple and explicit lists") {
                decode("a=b&a[]=c") shouldBe mapOf("a" to listOf("b", "c"))
                decode("a[]=b&a=c") shouldBe mapOf("a" to listOf("b", "c"))
                decode("a[0]=b&a=c") shouldBe mapOf("a" to listOf("b", "c"))
                decode("a=b&a[0]=c") shouldBe mapOf("a" to listOf("b", "c"))

                decode("a[1]=b&a=c", DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to listOf("b", "c"))
                decode("a[]=b&a=c", DecodeOptions(listLimit = 0)) shouldBe
                    mapOf("a" to listOf("b", "c"))
                decode("a[]=b&a=c") shouldBe mapOf("a" to listOf("b", "c"))

                decode("a=b&a[1]=c", DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to listOf("b", "c"))
                decode("a=b&a[]=c", DecodeOptions(listLimit = 0)) shouldBe
                    mapOf("a" to listOf("b", "c"))
                decode("a=b&a[]=c") shouldBe mapOf("a" to listOf("b", "c"))
            }

            it("parses a nested list") {
                decode("a[b][]=c&a[b][]=d") shouldBe mapOf("a" to mapOf("b" to listOf("c", "d")))
                decode("a[>=]=25") shouldBe mapOf("a" to mapOf(">=" to "25"))
            }

            it("decodes nested lists with parentKey not null") {
                decode("a[0][]=b") shouldBe mapOf("a" to listOf(listOf("b")))
            }

            it("allows to specify list indices") {
                decode("a[1]=c&a[0]=b&a[2]=d") shouldBe mapOf("a" to listOf("b", "c", "d"))
                decode("a[1]=c&a[0]=b") shouldBe mapOf("a" to listOf("b", "c"))
                decode("a[1]=c", DecodeOptions(listLimit = 20)) shouldBe mapOf("a" to listOf("c"))
                decode("a[1]=c", DecodeOptions(listLimit = 0)) shouldBe
                    mapOf("a" to mapOf("1" to "c"))
                decode("a[1]=c") shouldBe mapOf("a" to listOf("c"))
                decode("a[0]=b&a[2]=c", DecodeOptions(parseLists = false)) shouldBe
                    mapOf("a" to mapOf("0" to "b", "2" to "c"))
                decode("a[0]=b&a[2]=c", DecodeOptions(parseLists = true)) shouldBe
                    mapOf("a" to listOf("b", "c"))
                decode("a[1]=b&a[15]=c", DecodeOptions(parseLists = false)) shouldBe
                    mapOf("a" to mapOf("1" to "b", "15" to "c"))
                decode("a[1]=b&a[15]=c", DecodeOptions(parseLists = true)) shouldBe
                    mapOf("a" to listOf("b", "c"))
            }

            it("limits specific list indices to listLimit") {
                decode("a[20]=a", DecodeOptions(listLimit = 20)) shouldBe mapOf("a" to listOf("a"))
                decode("a[21]=a", DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to mapOf("21" to "a"))

                decode("a[20]=a") shouldBe mapOf("a" to listOf("a"))
                decode("a[21]=a") shouldBe mapOf("a" to mapOf("21" to "a"))
            }

            it("supports keys that begin with a number") {
                decode("a[12b]=c") shouldBe mapOf("a" to mapOf("12b" to "c"))
            }

            it("supports encoded = signs") {
                decode("he%3Dllo=th%3Dere") shouldBe mapOf("he=llo" to "th=ere")
            }

            it("is ok with url encoded strings") {
                decode("a[b%20c]=d") shouldBe mapOf("a" to mapOf("b c" to "d"))
                decode("a[b]=c%20d") shouldBe mapOf("a" to mapOf("b" to "c d"))
            }

            it("allows brackets in the value") {
                decode("pets=[\"tobi\"]") shouldBe mapOf("pets" to "[\"tobi\"]")
                decode("operators=[\">=\", \"<=\"]") shouldBe
                    mapOf("operators" to "[\">=\", \"<=\"]")
            }

            it("allows empty values") {
                decode("") shouldBe emptyMap()
                decode(null) shouldBe emptyMap()
            }

            it("transforms lists to maps") {
                decode("foo[0]=bar&foo[bad]=baz") shouldBe
                    mapOf("foo" to mapOf("0" to "bar", "bad" to "baz"))
                decode("foo[bad]=baz&foo[0]=bar") shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", "0" to "bar"))
                decode("foo[bad]=baz&foo[]=bar") shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", "0" to "bar"))
                decode("foo[]=bar&foo[bad]=baz") shouldBe
                    mapOf("foo" to mapOf("0" to "bar", "bad" to "baz"))
                decode("foo[bad]=baz&foo[]=bar&foo[]=foo") shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", "0" to "bar", "1" to "foo"))
                decode("foo[0][a]=a&foo[0][b]=b&foo[1][a]=aa&foo[1][b]=bb") shouldBe
                    mapOf(
                        "foo" to
                            listOf(mapOf("a" to "a", "b" to "b"), mapOf("a" to "aa", "b" to "bb"))
                    )
            }

            it("transforms lists to maps (dot notation)") {
                decode("foo[0].baz=bar&fool.bad=baz", DecodeOptions(allowDots = true)) shouldBe
                    mapOf("foo" to listOf(mapOf("baz" to "bar")), "fool" to mapOf("bad" to "baz"))
                decode("foo[0].baz=bar&fool.bad.boo=baz", DecodeOptions(allowDots = true)) shouldBe
                    mapOf(
                        "foo" to listOf(mapOf("baz" to "bar")),
                        "fool" to mapOf("bad" to mapOf("boo" to "baz")),
                    )
                decode("foo[0][0].baz=bar&fool.bad=baz", DecodeOptions(allowDots = true)) shouldBe
                    mapOf(
                        "foo" to listOf(listOf(mapOf("baz" to "bar"))),
                        "fool" to mapOf("bad" to "baz"),
                    )
                decode("foo[0].baz[0]=15&foo[0].bar=2", DecodeOptions(allowDots = true)) shouldBe
                    mapOf("foo" to listOf(mapOf("baz" to listOf("15"), "bar" to "2")))
                decode(
                    "foo[0].baz[0]=15&foo[0].baz[1]=16&foo[0].bar=2",
                    DecodeOptions(allowDots = true),
                ) shouldBe mapOf("foo" to listOf(mapOf("baz" to listOf("15", "16"), "bar" to "2")))
                decode("foo.bad=baz&foo[0]=bar", DecodeOptions(allowDots = true)) shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", "0" to "bar"))
                decode("foo.bad=baz&foo[]=bar", DecodeOptions(allowDots = true)) shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", "0" to "bar"))
                decode("foo[]=bar&foo.bad=baz", DecodeOptions(allowDots = true)) shouldBe
                    mapOf("foo" to mapOf("0" to "bar", "bad" to "baz"))
                decode("foo.bad=baz&foo[]=bar&foo[]=foo", DecodeOptions(allowDots = true)) shouldBe
                    mapOf("foo" to mapOf("bad" to "baz", "0" to "bar", "1" to "foo"))
                decode(
                    "foo[0].a=a&foo[0].b=b&foo[1].a=aa&foo[1].b=bb",
                    DecodeOptions(allowDots = true),
                ) shouldBe
                    mapOf(
                        "foo" to
                            listOf(mapOf("a" to "a", "b" to "b"), mapOf("a" to "aa", "b" to "bb"))
                    )
            }

            it("correctly prunes undefined values when converting a list to a map") {
                decode("a[2]=b&a[99999999]=c") shouldBe
                    mapOf("a" to mapOf("2" to "b", "99999999" to "c"))
            }

            it("supports malformed uri characters") {
                decode("{%:%}", DecodeOptions(strictNullHandling = true)) shouldBe
                    mapOf("{%:%}" to null)
                decode("{%:%}=") shouldBe mapOf("{%:%}" to "")
                decode("foo=%:%}") shouldBe mapOf("foo" to "%:%}")
            }

            it("does not produce empty keys") { decode("_r=1&") shouldBe mapOf("_r" to "1") }

            it("parses lists of maps") {
                decode("a[][b]=c") shouldBe mapOf("a" to listOf(mapOf("b" to "c")))
                decode("a[0][b]=c") shouldBe mapOf("a" to listOf(mapOf("b" to "c")))
            }

            it("allows for empty strings in lists") {
                decode("a[]=b&a[]=&a[]=c") shouldBe mapOf("a" to listOf("b", "", "c"))

                decode(
                    "a[0]=b&a[1]&a[2]=c&a[19]=",
                    DecodeOptions(strictNullHandling = true, listLimit = 20),
                ) shouldBe mapOf("a" to listOf("b", null, "c", ""))

                decode(
                    "a[]=b&a[]&a[]=c&a[]=",
                    DecodeOptions(strictNullHandling = true, listLimit = 0),
                ) shouldBe mapOf("a" to listOf("b", null, "c", ""))

                decode(
                    "a[0]=b&a[1]=&a[2]=c&a[19]",
                    DecodeOptions(strictNullHandling = true, listLimit = 20),
                ) shouldBe mapOf("a" to listOf("b", "", "c", null))

                decode(
                    "a[]=b&a[]=&a[]=c&a[]",
                    DecodeOptions(strictNullHandling = true, listLimit = 0),
                ) shouldBe mapOf("a" to listOf("b", "", "c", null))

                decode("a[]=&a[]=b&a[]=c") shouldBe mapOf("a" to listOf("", "b", "c"))
            }

            it("compacts sparse lists") {
                decode("a[10]=1&a[2]=2", DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to listOf("2", "1"))
                decode("a[1][b][2][c]=1", DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to listOf(mapOf("b" to listOf(mapOf("c" to "1")))))
                decode("a[1][2][3][c]=1", DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to listOf(listOf(listOf(mapOf("c" to "1")))))
                decode("a[1][2][3][c][1]=1", DecodeOptions(listLimit = 20)) shouldBe
                    mapOf("a" to listOf(listOf(listOf(mapOf("c" to listOf("1"))))))
            }

            it("parses semi-parsed strings") {
                decode("a[b]=c") shouldBe mapOf("a" to mapOf("b" to "c"))
                decode("a[b]=c&a[d]=e") shouldBe mapOf("a" to mapOf("b" to "c", "d" to "e"))
            }

            it("parses buffers correctly") {
                val b = "test".toByteArray()
                decode(mapOf("a" to b)) shouldBe mapOf("a" to b)
            }

            it("parses jquery-param strings") {
                val encoded =
                    "filter%5B0%5D%5B%5D=int1&filter%5B0%5D%5B%5D=%3D&filter%5B0%5D%5B%5D=77&filter%5B%5D=and&filter%5B2%5D%5B%5D=int2&filter%5B2%5D%5B%5D=%3D&filter%5B2%5D%5B%5D=8"
                val expected =
                    mapOf(
                        "filter" to
                            listOf(listOf("int1", "=", "77"), "and", listOf("int2", "=", "8"))
                    )
                decode(encoded) shouldBe expected
            }

            it("continues parsing when no parent is found") {
                decode("[]=&a=b") shouldBe mapOf("0" to "", "a" to "b")
                decode("[]&a=b", DecodeOptions(strictNullHandling = true)) shouldBe
                    mapOf("0" to null, "a" to "b")
                decode("[foo]=bar") shouldBe mapOf("foo" to "bar")
            }

            it("does not error when parsing a very long list") {
                val str = StringBuilder("a[]=a")
                while (str.toString().toByteArray().size < 128 * 1024) {
                    str.append("&")
                    str.append(str)
                }

                shouldNotThrow<Exception> { decode(str.toString()) }
            }

            it("parses a string with an alternative string delimiter") {
                decode("a=b;c=d", DecodeOptions(delimiter = StringDelimiter(";"))) shouldBe
                    mapOf("a" to "b", "c" to "d")
            }

            it("parses a string with an alternative RegExp delimiter") {
                decode("a=b; c=d", DecodeOptions(delimiter = RegexDelimiter("[;,] *"))) shouldBe
                    mapOf("a" to "b", "c" to "d")
            }

            it("allows overriding parameter limit") {
                decode("a=b&c=d", DecodeOptions(parameterLimit = 1)) shouldBe mapOf("a" to "b")
            }

            it("allows setting the parameter limit to Int.MAX_VALUE") {
                decode("a=b&c=d", DecodeOptions(parameterLimit = Int.MAX_VALUE)) shouldBe
                    mapOf("a" to "b", "c" to "d")
            }

            it("allows overriding list limit") {
                decode("a[0]=b", DecodeOptions(listLimit = -1)) shouldBe
                    mapOf("a" to mapOf("0" to "b"))
                decode("a[0]=b", DecodeOptions(listLimit = 0)) shouldBe mapOf("a" to listOf("b"))

                decode("a[-1]=b", DecodeOptions(listLimit = -1)) shouldBe
                    mapOf("a" to mapOf("-1" to "b"))
                decode("a[-1]=b", DecodeOptions(listLimit = 0)) shouldBe
                    mapOf("a" to mapOf("-1" to "b"))

                decode("a[0]=b&a[1]=c", DecodeOptions(listLimit = -1)) shouldBe
                    mapOf("a" to mapOf("0" to "b", "1" to "c"))
                decode("a[0]=b&a[1]=c", DecodeOptions(listLimit = 0)) shouldBe
                    mapOf("a" to mapOf("0" to "b", "1" to "c"))
            }

            it("allows disabling list parsing") {
                decode("a[0]=b&a[1]=c", DecodeOptions(parseLists = false)) shouldBe
                    mapOf("a" to mapOf("0" to "b", "1" to "c"))
                decode("a[]=b", DecodeOptions(parseLists = false)) shouldBe
                    mapOf("a" to mapOf("0" to "b"))
            }

            it("allows for query string prefix") {
                decode("?foo=bar", DecodeOptions(ignoreQueryPrefix = true)) shouldBe
                    mapOf("foo" to "bar")
                decode("foo=bar", DecodeOptions(ignoreQueryPrefix = true)) shouldBe
                    mapOf("foo" to "bar")
                decode("?foo=bar", DecodeOptions(ignoreQueryPrefix = false)) shouldBe
                    mapOf("?foo" to "bar")
            }

            it("parses a map") {
                val input = mapOf("user[name]" to mapOf("pop[bob]" to 3), "user[email]" to null)

                val expected =
                    mapOf("user" to mapOf("name" to mapOf("pop[bob]" to 3), "email" to null))

                decode(input) shouldBe expected
            }

            it("parses string with comma as list divider") {
                decode("foo=bar,tee", DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to listOf("bar", "tee"))
                decode("foo[bar]=coffee,tee", DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to mapOf("bar" to listOf("coffee", "tee")))
                decode("foo=", DecodeOptions(comma = true)) shouldBe mapOf("foo" to "")
                decode("foo", DecodeOptions(comma = true)) shouldBe mapOf("foo" to "")
                decode("foo", DecodeOptions(comma = true, strictNullHandling = true)) shouldBe
                    mapOf("foo" to null)

                decode("a[0]=c") shouldBe mapOf("a" to listOf("c"))
                decode("a[]=c") shouldBe mapOf("a" to listOf("c"))
                decode("a[]=c", DecodeOptions(comma = true)) shouldBe mapOf("a" to listOf("c"))

                decode("a[0]=c&a[1]=d") shouldBe mapOf("a" to listOf("c", "d"))
                decode("a[]=c&a[]=d") shouldBe mapOf("a" to listOf("c", "d"))
                decode("a=c,d", DecodeOptions(comma = true)) shouldBe mapOf("a" to listOf("c", "d"))
            }

            it("parses values with comma as list divider") {
                decode(mapOf("foo" to "bar,tee"), DecodeOptions(comma = false)) shouldBe
                    mapOf("foo" to "bar,tee")
                decode(mapOf("foo" to "bar,tee"), DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to listOf("bar", "tee"))
            }

            it("use number decoder, parses string that has one number with comma option enabled") {
                val decoder = Decoder { str, charset, _ ->
                    str?.toIntOrNull() ?: Utils.decode(str, charset)
                }

                decode("foo=1", DecodeOptions(comma = true, decoder = decoder)) shouldBe
                    mapOf("foo" to 1)
                decode("foo=0", DecodeOptions(comma = true, decoder = decoder)) shouldBe
                    mapOf("foo" to 0)
                // ensure keys are not coerced to numbers
                decode("1=foo", DecodeOptions(decoder = decoder)) shouldBe mapOf("1" to "foo")
            }

            it(
                "parses brackets holds list of lists when having two parts of strings with comma as list divider"
            ) {
                decode("foo[]=1,2,3&foo[]=4,5,6", DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to listOf(listOf("1", "2", "3"), listOf("4", "5", "6")))
                decode("foo[]=1,2,3&foo[]=", DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to listOf(listOf("1", "2", "3"), ""))
                decode("foo[]=1,2,3&foo[]=,", DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to listOf(listOf("1", "2", "3"), listOf("", "")))
                decode("foo[]=1,2,3&foo[]=a", DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to listOf(listOf("1", "2", "3"), "a"))
            }

            it(
                "parses comma delimited list while having percent-encoded comma treated as normal text"
            ) {
                decode("foo=a%2Cb", DecodeOptions(comma = true)) shouldBe mapOf("foo" to "a,b")
                decode("foo=a%2C%20b,d", DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to listOf("a, b", "d"))
                decode("foo=a%2C%20b,c%2C%20d", DecodeOptions(comma = true)) shouldBe
                    mapOf("foo" to listOf("a, b", "c, d"))
            }

            it("parses a map in dot notation") {
                decode(
                    mapOf("user.name" to mapOf("pop[bob]" to 3), "user.email." to null),
                    DecodeOptions(allowDots = true),
                ) shouldBe mapOf("user" to mapOf("name" to mapOf("pop[bob]" to 3), "email" to null))
            }

            it("parses a map and not child values") {
                decode(
                    mapOf(
                        "user[name]" to mapOf("pop[bob]" to mapOf("test" to 3)),
                        "user[email]" to null,
                    )
                ) shouldBe
                    mapOf(
                        "user" to
                            mapOf(
                                "name" to mapOf("pop[bob]" to mapOf("test" to 3)),
                                "email" to null,
                            )
                    )
            }

            it("does not crash when parsing circular references") {
                val a = mutableMapOf<String, Any?>()
                a["b"] = a

                lateinit var parsed: Map<String, Any?>

                shouldNotThrow<Exception> {
                    parsed = decode(mapOf("foo[bar]" to "baz", "foo[baz]" to a))
                }

                parsed.containsKey("foo") shouldBe true
                (parsed["foo"] as Map<*, *>).containsKey("bar") shouldBe true
                (parsed["foo"] as Map<*, *>).containsKey("baz") shouldBe true
                (parsed["foo"] as Map<*, *>)["bar"] shouldBe "baz"
                (parsed["foo"] as Map<*, *>)["baz"] shouldBe a
            }

            it("does not crash or time out when parsing deep maps").config(timeout = 5.seconds) {
                val depth = 5000

                val str = StringBuilder("foo")
                repeat(depth) { str.append("[p]") }
                str.append("=bar")

                lateinit var parsed: Map<String, Any?>

                shouldNotThrow<Exception> {
                    parsed = decode(str.toString(), DecodeOptions(depth = depth))
                }

                parsed.containsKey("foo") shouldBe true

                var actualDepth = 0
                var ref: Any? = parsed["foo"]
                while (ref != null && ref is Map<*, *> && ref.containsKey("p")) {
                    ref = ref["p"]
                    actualDepth++
                }

                actualDepth shouldBe depth
            }

            it("parses null maps correctly") {
                val a = mapOf("b" to "c")
                decode(a) shouldBe mapOf("b" to "c")
                decode(mapOf("a" to a)) shouldBe mapOf("a" to a)
            }

            it("parses dates correctly") {
                val now = java.time.LocalDateTime.now()
                decode(mapOf("a" to now)) shouldBe mapOf("a" to now)
            }

            it("parses regular expressions correctly") {
                val re = Regex("^test$")
                decode(mapOf("a" to re)) shouldBe mapOf("a" to re)
            }

            it("params starting with a closing bracket") {
                decode("]=toString") shouldBe mapOf("]" to "toString")
                decode("]]=toString") shouldBe mapOf("]]" to "toString")
                decode("]hello]=toString") shouldBe mapOf("]hello]" to "toString")
            }

            it("params starting with a starting bracket") {
                decode("[=toString") shouldBe mapOf("[" to "toString")
                decode("[[=toString") shouldBe mapOf("[[" to "toString")
                decode("[hello[=toString") shouldBe mapOf("[hello[" to "toString")
            }

            it("add keys to maps") { decode("a[b]=c") shouldBe mapOf("a" to mapOf("b" to "c")) }

            @Suppress("UNCHECKED_CAST")
            it("can return null maps") {
                val expected = mutableMapOf<String, Any?>()
                expected["a"] = mutableMapOf<String, Any?>()
                (expected["a"] as MutableMap<String, Any?>)["b"] = "c"
                (expected["a"] as MutableMap<String, Any?>)["hasOwnProperty"] = "d"
                decode("a[b]=c&a[hasOwnProperty]=d") shouldBe expected

                decode(null) shouldBe emptyMap<String, Any?>()

                val expectedList = mutableMapOf<Any, Any?>()
                expectedList["a"] = mutableMapOf<Any, Any?>()
                (expectedList["a"] as MutableMap<Any, Any?>)["0"] = "b"
                (expectedList["a"] as MutableMap<Any, Any?>)["c"] = "d"
                decode("a[]=b&a[c]=d") shouldBe expectedList
            }

            it("can parse with custom encoding") {
                val expected = mapOf("県" to "大阪府")

                val decode = Decoder { str, _, _ ->
                    str?.replace("%8c%a7", "県")?.replace("%91%e5%8d%e3%95%7b", "大阪府")
                }

                decode("%8c%a7=%91%e5%8d%e3%95%7b", DecodeOptions(decoder = decode)) shouldBe
                    expected
            }

            it("parses an iso-8859-1 string if asked to") {
                val expected = mapOf("¢" to "½")

                decode("%A2=%BD", DecodeOptions(charset = StandardCharsets.ISO_8859_1)) shouldBe
                    expected
            }

            describe("charset") {
                it("throws an exception when given an unknown charset") {
                    @Suppress("CHARSET")
                    shouldThrow<Exception> {
                        decode("a=b", DecodeOptions(charset = Charset.forName("foo")))
                    }
                }

                val urlEncodedCheckmarkInUtf8 = "%E2%9C%93"
                val urlEncodedOSlashInUtf8 = "%C3%B8"
                val urlEncodedNumCheckmark = "%26%2310003%3B"
                val urlEncodedNumSmiley = "%26%239786%3B"

                it(
                    "prefers an utf-8 charset specified by the utf8 sentinel to a default charset of iso-8859-1"
                ) {
                    decode(
                        "utf8=$urlEncodedCheckmarkInUtf8&$urlEncodedOSlashInUtf8=$urlEncodedOSlashInUtf8",
                        DecodeOptions(charsetSentinel = true, charset = StandardCharsets.ISO_8859_1),
                    ) shouldBe mapOf("ø" to "ø")
                }

                it(
                    "prefers an iso-8859-1 charset specified by the utf8 sentinel to a default charset of utf-8"
                ) {
                    decode(
                        "utf8=$urlEncodedNumCheckmark&$urlEncodedOSlashInUtf8=$urlEncodedOSlashInUtf8",
                        DecodeOptions(charsetSentinel = true, charset = StandardCharsets.UTF_8),
                    ) shouldBe mapOf("Ã¸" to "Ã¸")
                }

                it(
                    "does not require the utf8 sentinel to be defined before the parameters whose decoding it affects"
                ) {
                    decode(
                        "a=$urlEncodedOSlashInUtf8&utf8=$urlEncodedNumCheckmark",
                        DecodeOptions(charsetSentinel = true, charset = StandardCharsets.UTF_8),
                    ) shouldBe mapOf("a" to "Ã¸")
                }

                it("should ignore an utf8 sentinel with an unknown value") {
                    decode(
                        "utf8=foo&$urlEncodedOSlashInUtf8=$urlEncodedOSlashInUtf8",
                        DecodeOptions(charsetSentinel = true, charset = StandardCharsets.UTF_8),
                    ) shouldBe mapOf("ø" to "ø")
                }

                it("uses the utf8 sentinel to switch to utf-8 when no default charset is given") {
                    decode(
                        "utf8=$urlEncodedCheckmarkInUtf8&$urlEncodedOSlashInUtf8=$urlEncodedOSlashInUtf8",
                        DecodeOptions(charsetSentinel = true),
                    ) shouldBe mapOf("ø" to "ø")
                }

                it(
                    "uses the utf8 sentinel to switch to iso-8859-1 when no default charset is given"
                ) {
                    decode(
                        "utf8=$urlEncodedNumCheckmark&$urlEncodedOSlashInUtf8=$urlEncodedOSlashInUtf8",
                        DecodeOptions(charsetSentinel = true),
                    ) shouldBe mapOf("Ã¸" to "Ã¸")
                }

                it("interprets numeric entities in iso-8859-1 when `interpretNumericEntities`") {
                    decode(
                        "foo=$urlEncodedNumSmiley",
                        DecodeOptions(
                            charset = StandardCharsets.ISO_8859_1,
                            interpretNumericEntities = true,
                        ),
                    ) shouldBe mapOf("foo" to "☺")
                }

                it(
                    "handles a custom decoder returning `null`, in the `iso-8859-1` charset, when `interpretNumericEntities`"
                ) {
                    val decoder = Decoder { str, charset, _ ->
                        if (!str.isNullOrEmpty()) Utils.decode(str, charset) else null
                    }

                    decode(
                        "foo=&bar=$urlEncodedNumSmiley",
                        DecodeOptions(
                            charset = StandardCharsets.ISO_8859_1,
                            decoder = decoder,
                            interpretNumericEntities = true,
                        ),
                    ) shouldBe mapOf("foo" to null, "bar" to "☺")
                }

                it(
                    "does not interpret numeric entities in iso-8859-1 when `interpretNumericEntities` is absent"
                ) {
                    decode(
                        "foo=$urlEncodedNumSmiley",
                        DecodeOptions(charset = StandardCharsets.ISO_8859_1),
                    ) shouldBe mapOf("foo" to "&#9786;")
                }

                it(
                    "`interpretNumericEntities` with comma:true and iso-8859-1 charset does not crash"
                ) {
                    decode(
                        "b&a[]=1,$urlEncodedNumSmiley",
                        DecodeOptions(
                            comma = true,
                            charset = StandardCharsets.ISO_8859_1,
                            interpretNumericEntities = true,
                        ),
                    ) shouldBe mapOf("b" to "", "a" to listOf("1,☺"))
                }

                it(
                    "does not interpret numeric entities when the charset is utf-8, even when `interpretNumericEntities`"
                ) {
                    decode(
                        "foo=$urlEncodedNumSmiley",
                        DecodeOptions(
                            charset = StandardCharsets.UTF_8,
                            interpretNumericEntities = true,
                        ),
                    ) shouldBe mapOf("foo" to "&#9786;")
                }

                it("does not interpret %uXXXX syntax in iso-8859-1 mode") {
                    decode(
                        "%u263A=%u263A",
                        DecodeOptions(charset = StandardCharsets.ISO_8859_1),
                    ) shouldBe mapOf("%u263A" to "%u263A")
                }
            }
        }

        describe("parses empty keys") {
            for (element in EmptyTestCases) {
                it("skips empty string key with ${element["input"]}") {
                    decode(element["input"] as String) shouldBe element["noEmptyKeys"]
                }
            }
        }

        describe("`duplicates` option") {
            it("duplicates: default, combine") {
                decode("foo=bar&foo=baz") shouldBe mapOf("foo" to listOf("bar", "baz"))
            }

            it("duplicates: combine") {
                decode("foo=bar&foo=baz", DecodeOptions(duplicates = Duplicates.COMBINE)) shouldBe
                    mapOf("foo" to listOf("bar", "baz"))
            }

            it("duplicates: first") {
                decode("foo=bar&foo=baz", DecodeOptions(duplicates = Duplicates.FIRST)) shouldBe
                    mapOf("foo" to "bar")
            }

            it("duplicates: last") {
                decode("foo=bar&foo=baz", DecodeOptions(duplicates = Duplicates.LAST)) shouldBe
                    mapOf("foo" to "baz")
            }
        }

        describe("strictDepth option - throw cases") {
            it("throws an exception for multiple nested objects with strictDepth: true") {
                shouldThrow<IndexOutOfBoundsException> {
                    decode(
                        "a[b][c][d][e][f][g][h][i]=j",
                        DecodeOptions(depth = 1, strictDepth = true),
                    )
                }
            }

            it("throws an exception for multiple nested lists with strictDepth: true") {
                shouldThrow<IndexOutOfBoundsException> {
                    decode("a[0][1][2][3][4]=b", DecodeOptions(depth = 3, strictDepth = true))
                }
            }

            it("throws an exception for nested maps and lists with strictDepth: true") {
                shouldThrow<IndexOutOfBoundsException> {
                    decode("a[b][c][0][d][e]=f", DecodeOptions(depth = 3, strictDepth = true))
                }
            }

            it("throws an exception for different types of values with strictDepth: true") {
                shouldThrow<IndexOutOfBoundsException> {
                    decode(
                        "a[b][c][d][e]=true&a[b][c][d][f]=42",
                        DecodeOptions(depth = 3, strictDepth = true),
                    )
                }
            }
        }

        describe("strictDepth option - non-throw cases") {
            it("when depth is 0 and strictDepth true, do not throw") {
                shouldNotThrow<Exception> {
                    decode(
                        "a[b][c][d][e]=true&a[b][c][d][f]=42",
                        DecodeOptions(depth = 0, strictDepth = true),
                    )
                }
            }

            it("parses successfully when depth is within the limit with strictDepth: true") {
                decode("a[b]=c", DecodeOptions(depth = 1, strictDepth = true)) shouldBe
                    mapOf("a" to mapOf("b" to "c"))
            }

            it("does not throw an exception when depth exceeds the limit with strictDepth: false") {
                decode("a[b][c][d][e][f][g][h][i]=j", DecodeOptions(depth = 1)) shouldBe
                    mapOf("a" to mapOf("b" to mapOf("[c][d][e][f][g][h][i]" to "j")))
            }

            it("parses successfully when depth is within the limit with strictDepth: false") {
                decode("a[b]=c", DecodeOptions(depth = 1)) shouldBe mapOf("a" to mapOf("b" to "c"))
            }

            it("does not throw when depth is exactly at the limit with strictDepth: true") {
                decode("a[b][c]=d", DecodeOptions(depth = 2, strictDepth = true)) shouldBe
                    mapOf("a" to mapOf("b" to mapOf("c" to "d")))
            }
        }

        describe("parameter limit") {
            it("does not throw error when within parameter limit") {
                decode(
                    "a=1&b=2&c=3",
                    DecodeOptions(parameterLimit = 5, throwOnLimitExceeded = true),
                ) shouldBe mapOf("a" to "1", "b" to "2", "c" to "3")
            }

            it("throws error when parameter limit exceeded") {
                shouldThrow<IndexOutOfBoundsException> {
                    decode(
                        "a=1&b=2&c=3&d=4&e=5&f=6",
                        DecodeOptions(parameterLimit = 3, throwOnLimitExceeded = true),
                    )
                }
            }

            it("silently truncates when throwOnLimitExceeded is not given") {
                decode("a=1&b=2&c=3&d=4&e=5", DecodeOptions(parameterLimit = 3)) shouldBe
                    mapOf("a" to "1", "b" to "2", "c" to "3")
            }

            it("silently truncates when parameter limit exceeded without error") {
                decode(
                    "a=1&b=2&c=3&d=4&e=5",
                    DecodeOptions(parameterLimit = 3, throwOnLimitExceeded = false),
                ) shouldBe mapOf("a" to "1", "b" to "2", "c" to "3")
            }

            it("allows unlimited parameters when parameterLimit set to Infinity") {
                decode(
                    "a=1&b=2&c=3&d=4&e=5&f=6",
                    DecodeOptions(parameterLimit = Int.MAX_VALUE),
                ) shouldBe
                    mapOf("a" to "1", "b" to "2", "c" to "3", "d" to "4", "e" to "5", "f" to "6")
            }
        }

        describe("list limit tests") {
            it("does not throw error when list is within limit") {
                decode(
                    "a[]=1&a[]=2&a[]=3",
                    DecodeOptions(listLimit = 5, throwOnLimitExceeded = true),
                ) shouldBe mapOf("a" to listOf("1", "2", "3"))
            }

            it("throws error when list limit exceeded") {
                shouldThrow<IndexOutOfBoundsException> {
                    decode(
                        "a[]=1&a[]=2&a[]=3&a[]=4",
                        DecodeOptions(listLimit = 3, throwOnLimitExceeded = true),
                    )
                }
            }

            it("converts list to map if length is greater than limit") {
                decode(
                    "a[1]=1&a[2]=2&a[3]=3&a[4]=4&a[5]=5&a[6]=6",
                    DecodeOptions(listLimit = 5),
                ) shouldBe
                    mapOf(
                        "a" to
                            mapOf(
                                "1" to "1",
                                "2" to "2",
                                "3" to "3",
                                "4" to "4",
                                "5" to "5",
                                "6" to "6",
                            )
                    )
            }

            it("handles list limit of zero correctly") {
                decode("a[]=1&a[]=2", DecodeOptions(listLimit = 0)) shouldBe
                    mapOf("a" to listOf("1", "2"))
            }

            it("handles negative list limit correctly") {
                shouldThrow<IndexOutOfBoundsException> {
                    decode(
                        "a[]=1&a[]=2",
                        DecodeOptions(listLimit = -1, throwOnLimitExceeded = true),
                    )
                }
            }

            it("applies list limit to nested lists") {
                shouldThrow<IndexOutOfBoundsException> {
                    decode(
                        "a[0][]=1&a[0][]=2&a[0][]=3&a[0][]=4",
                        DecodeOptions(listLimit = 3, throwOnLimitExceeded = true),
                    )
                }
            }
        }

        describe("encoded dot behavior in keys (%2E / %2e)") {
            it(
                "allowDots=false, decodeDotInKeys=false: encoded dots decode to literal '.'; no dot-splitting"
            ) {
                decode(
                    "a%2Eb=c",
                    DecodeOptions(allowDots = false, decodeDotInKeys = false),
                ) shouldBe mapOf("a.b" to "c")
                decode(
                    "a%2eb=c",
                    DecodeOptions(allowDots = false, decodeDotInKeys = false),
                ) shouldBe mapOf("a.b" to "c")
            }

            it(
                "allowDots=true, decodeDotInKeys=false: encoded dots are preserved inside segments; plain dots split"
            ) {
                // Plain dot splits
                decode("a.b=c", DecodeOptions(allowDots = true, decodeDotInKeys = false)) shouldBe
                    mapOf("a" to mapOf("b" to "c"))
                // Encoded dot stays encoded inside segment (no extra split)
                decode(
                    "name%252Eobj.first=John",
                    DecodeOptions(allowDots = true, decodeDotInKeys = false),
                ) shouldBe mapOf("name%2Eobj" to mapOf("first" to "John"))
                // Lowercase variant inside first segment
                decode(
                    "a%2eb.c=d",
                    DecodeOptions(allowDots = true, decodeDotInKeys = false),
                ) shouldBe mapOf("a%2eb" to mapOf("c" to "d"))
            }

            it(
                "allowDots=true, decodeDotInKeys=true: encoded dots become literal '.' inside a segment (no extra split)"
            ) {
                decode(
                    "name%252Eobj.first=John",
                    DecodeOptions(allowDots = true, decodeDotInKeys = true),
                ) shouldBe mapOf("name.obj" to mapOf("first" to "John"))
                // Double-encoded single segment becomes a literal dot after post-split mapping
                decode(
                    "a%252Eb=c",
                    DecodeOptions(allowDots = true, decodeDotInKeys = true),
                ) shouldBe mapOf("a.b" to "c")
                // Lowercase mapping as well
                decode("a[%2e]=x", DecodeOptions(allowDots = true, decodeDotInKeys = true)) shouldBe
                    mapOf("a" to mapOf("." to "x"))
            }

            it("bracket segment: %2E mapped based on decodeDotInKeys; case-insensitive") {
                // When disabled, keep %2E literal (no conversion)
                decode(
                    "a[%2E]=x",
                    DecodeOptions(allowDots = false, decodeDotInKeys = false),
                ) shouldBe mapOf("a" to mapOf("%2E" to "x"))
                decode(
                    "a[%2e]=x",
                    DecodeOptions(allowDots = true, decodeDotInKeys = false),
                ) shouldBe mapOf("a" to mapOf("%2e" to "x"))
                // When enabled, convert to '.' regardless of case
                decode("a[%2E]=x", DecodeOptions(allowDots = true, decodeDotInKeys = true)) shouldBe
                    mapOf("a" to mapOf("." to "x"))
                shouldThrow<IllegalArgumentException> {
                        decode("a[%2e]=x", DecodeOptions(allowDots = false, decodeDotInKeys = true))
                    }
                    .message shouldBe "decodeDotInKeys requires allowDots to be true"
            }

            it("bare-key (no '='): behavior matches key decoding path") {
                // allowDots=false → %2E decodes to '.'; no splitting because allowDots=false
                decode(
                    "a%2Eb",
                    DecodeOptions(
                        allowDots = false,
                        decodeDotInKeys = false,
                        strictNullHandling = true,
                    ),
                ) shouldBe mapOf("a.b" to null)
                // allowDots=true & decodeDotInKeys=false → keep %2E inside key segment
                decode("a%2Eb", DecodeOptions(allowDots = true, decodeDotInKeys = false)) shouldBe
                    mapOf("a%2Eb" to "")
            }

            it("depth=0 with allowDots=true: do not split key") {
                decode("a.b=c", DecodeOptions(allowDots = true, depth = 0)) shouldBe
                    mapOf("a.b" to "c")
            }
        }
    })
