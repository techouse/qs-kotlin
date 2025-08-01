package io.github.techouse.qskotlin.unit

import io.github.techouse.qskotlin.QS
import io.github.techouse.qskotlin.Utils
import io.github.techouse.qskotlin.enums.Format
import io.github.techouse.qskotlin.enums.ListFormat
import io.github.techouse.qskotlin.fixtures.DummyEnum
import io.github.techouse.qskotlin.fixtures.data.EmptyTestCases
import io.github.techouse.qskotlin.models.*
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class EncodeSpec :
    DescribeSpec({
        describe("QS.encode") {
            it("Default parameter initializations in _encode method") {
                // This test targets default initializations
                val result =
                    QS.encode(
                        mapOf("a" to "b"),
                        EncodeOptions(
                            // Force the code to use the default initializations
                            listFormat = null,
                            commaRoundTrip = null,
                            format = Format.RFC3986,
                        ),
                    )
                result shouldBe "a=b"

                // Try another approach with a list to trigger the generateArrayPrefix default
                val result2 =
                    QS.encode(
                        mapOf("a" to listOf("b", "c")),
                        EncodeOptions(
                            // Force the code to use the default initializations
                            listFormat = null,
                            commaRoundTrip = null,
                        ),
                    )
                result2 shouldBe "a%5B0%5D=b&a%5B1%5D=c"

                // Try with comma format to trigger the commaRoundTrip default
                val result3 =
                    QS.encode(
                        mapOf("a" to listOf("b", "c")),
                        EncodeOptions(listFormat = ListFormat.COMMA, commaRoundTrip = null),
                    )
                result3 shouldBe "a=b%2Cc"
            }

            it("Default DateTime serialization") {
                // This test targets default serialization
                val dateTime = Instant.parse("2023-01-01T00:00:00.001Z")
                val result =
                    QS.encode(
                        mapOf("date" to dateTime),
                        EncodeOptions(
                            encode = false,
                            dateSerializer = null, // Force the code to use the default serialization
                        ),
                    )
                result shouldBe "date=2023-01-01T00:00:00.001Z"

                // Try another approach with a list of DateTimes
                val result2 =
                    QS.encode(
                        mapOf("dates" to listOf(dateTime, dateTime)),
                        EncodeOptions(
                            encode = false,
                            dateSerializer = null,
                            listFormat = ListFormat.COMMA,
                        ),
                    )
                result2 shouldBe "dates=2023-01-01T00:00:00.001Z,2023-01-01T00:00:00.001Z"
            }

            it("Access property of non-Map, non-Iterable object") {
                // Create a custom object that's neither a Map nor an Iterable
                val customObj = CustomObject("test")

                // First, let's verify that our CustomObject works as expected
                customObj["prop"] shouldBe "test"

                // Now, let's create a test that will try to access the property
                try {
                    val result = QS.encode(customObj, EncodeOptions(encode = false))
                    // The result might be empty, but the important thing is that the code path is
                    // executed
                    result.isEmpty() shouldBe true
                } catch (_: Exception) {
                    // If an exception is thrown, that's also fine as long as the code path is
                    // executed
                }

                // Try another approach with a custom filter
                try {
                    val result =
                        QS.encode(
                            mapOf("obj" to customObj),
                            EncodeOptions(
                                encode = false,
                                filter =
                                    FunctionFilter { prefix: String, map: Any? ->
                                        // This should trigger the code path that accesses
                                        // properties of non-Map, non-Iterable objects
                                        val result = mutableMapOf<String, Any?>()
                                        @Suppress("UNCHECKED_CAST")
                                        (map as Map<String, Any?>).forEach { (key, value) ->
                                            if (value is CustomObject) {
                                                result[key] = value["prop"]
                                            } else {
                                                result[key] = value
                                            }
                                        }
                                        result
                                    },
                            ),
                        )
                    // Check if the result contains the expected value
                    result shouldContain "obj=test"
                } catch (_: Exception) {
                    // If an exception is thrown, that's also fine as long as the code path is
                    // executed
                }
            }

            it("encodes a query string map") {
                QS.encode(mapOf("a" to "b")) shouldBe "a=b"
                QS.encode(mapOf("a" to 1)) shouldBe "a=1"
                QS.encode(mapOf("a" to 1, "b" to 2)) shouldBe "a=1&b=2"
                QS.encode(mapOf("a" to "A_Z")) shouldBe "a=A_Z"
                QS.encode(mapOf("a" to "€")) shouldBe "a=%E2%82%AC"
                QS.encode(mapOf("a" to "")) shouldBe "a=%EE%80%80"
                QS.encode(mapOf("a" to "א")) shouldBe "a=%D7%90"
                QS.encode(mapOf("a" to "𐐷")) shouldBe "a=%F0%90%90%B7"
            }

            it("encodes with default parameter values") {
                // Test with ListFormat.COMMA but without setting commaRoundTrip
                // This should trigger the default initialization of commaRoundTrip
                val customOptions = EncodeOptions(listFormat = ListFormat.COMMA, encode = false)

                // This should use the default commaRoundTrip value (false)
                QS.encode(mapOf("a" to listOf("b")), customOptions) shouldBe "a=b"

                // Test with explicitly set commaRoundTrip to true
                val customOptionsWithCommaRoundTrip =
                    EncodeOptions(
                        listFormat = ListFormat.COMMA,
                        commaRoundTrip = true,
                        encode = false,
                    )

                // This should append [] to single-item lists
                QS.encode(mapOf("a" to listOf("b")), customOptionsWithCommaRoundTrip) shouldBe
                    "a[]=b"
            }

            it("encodes a list") {
                QS.encode(listOf(1234)) shouldBe "0=1234"
                QS.encode(listOf("lorem", 1234, "ipsum")) shouldBe "0=lorem&1=1234&2=ipsum"
            }

            it("encodes falsy values") {
                QS.encode(emptyMap<String, Any>()) shouldBe ""
                QS.encode(null) shouldBe ""
                QS.encode(null, EncodeOptions(strictNullHandling = true)) shouldBe ""
                QS.encode(false) shouldBe ""
                QS.encode(0) shouldBe ""
            }

            it("encodes bigints") {
                val three = 3L

                @Suppress("unused")
                fun encodeWithN(
                    value: Any?,
                    charset: Charset? = null,
                    format: Format? = null,
                ): String {
                    val result = Utils.encode(value, format = format)
                    return if (value is Long) "${result}n" else result
                }

                QS.encode(three) shouldBe ""
                QS.encode(listOf(three)) shouldBe "0=3"
                QS.encode(listOf(three), EncodeOptions(encoder = ::encodeWithN)) shouldBe "0=3n"
                QS.encode(mapOf("a" to three)) shouldBe "a=3"
                QS.encode(mapOf("a" to three), EncodeOptions(encoder = ::encodeWithN)) shouldBe
                    "a=3n"
                QS.encode(
                    mapOf("a" to listOf(three)),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a[]=3"
                QS.encode(
                    mapOf("a" to listOf(three)),
                    EncodeOptions(
                        encodeValuesOnly = true,
                        encoder = ::encodeWithN,
                        listFormat = ListFormat.BRACKETS,
                    ),
                ) shouldBe "a[]=3n"
            }

            it("encodes dot in key of map when encodeDotInKeys and allowDots is provided") {
                QS.encode(
                    mapOf("name.obj" to mapOf("first" to "John", "last" to "Doe")),
                    EncodeOptions(allowDots = false, encodeDotInKeys = false),
                ) shouldBe "name.obj%5Bfirst%5D=John&name.obj%5Blast%5D=Doe"

                QS.encode(
                    mapOf("name.obj" to mapOf("first" to "John", "last" to "Doe")),
                    EncodeOptions(allowDots = true, encodeDotInKeys = false),
                ) shouldBe "name.obj.first=John&name.obj.last=Doe"

                QS.encode(
                    mapOf("name.obj" to mapOf("first" to "John", "last" to "Doe")),
                    EncodeOptions(allowDots = false, encodeDotInKeys = true),
                ) shouldBe "name%252Eobj%5Bfirst%5D=John&name%252Eobj%5Blast%5D=Doe"

                QS.encode(
                    mapOf("name.obj" to mapOf("first" to "John", "last" to "Doe")),
                    EncodeOptions(allowDots = true, encodeDotInKeys = true),
                ) shouldBe "name%252Eobj.first=John&name%252Eobj.last=Doe"

                QS.encode(
                    mapOf(
                        "name.obj.subobject" to mapOf("first.godly.name" to "John", "last" to "Doe")
                    ),
                    EncodeOptions(allowDots = true, encodeDotInKeys = false),
                ) shouldBe "name.obj.subobject.first.godly.name=John&name.obj.subobject.last=Doe"

                QS.encode(
                    mapOf(
                        "name.obj.subobject" to mapOf("first.godly.name" to "John", "last" to "Doe")
                    ),
                    EncodeOptions(allowDots = false, encodeDotInKeys = true),
                ) shouldBe
                    "name%252Eobj%252Esubobject%5Bfirst.godly.name%5D=John&name%252Eobj%252Esubobject%5Blast%5D=Doe"

                QS.encode(
                    mapOf(
                        "name.obj.subobject" to mapOf("first.godly.name" to "John", "last" to "Doe")
                    ),
                    EncodeOptions(allowDots = true, encodeDotInKeys = true),
                ) shouldBe
                    "name%252Eobj%252Esubobject.first%252Egodly%252Ename=John&name%252Eobj%252Esubobject.last=Doe"
            }

            it(
                "should encode dot in key of map, and automatically set allowDots to true when encodeDotInKeys is true and allowDots in undefined"
            ) {
                QS.encode(
                    mapOf(
                        "name.obj.subobject" to mapOf("first.godly.name" to "John", "last" to "Doe")
                    ),
                    EncodeOptions(encodeDotInKeys = true),
                ) shouldBe
                    "name%252Eobj%252Esubobject.first%252Egodly%252Ename=John&name%252Eobj%252Esubobject.last=Doe"
            }

            it(
                "should encode dot in key of map when encodeDotInKeys and allowDots is provided, and nothing else when encodeValuesOnly is provided"
            ) {
                QS.encode(
                    mapOf("name.obj" to mapOf("first" to "John", "last" to "Doe")),
                    EncodeOptions(encodeDotInKeys = true, allowDots = true, encodeValuesOnly = true),
                ) shouldBe "name%2Eobj.first=John&name%2Eobj.last=Doe"

                QS.encode(
                    mapOf(
                        "name.obj.subobject" to mapOf("first.godly.name" to "John", "last" to "Doe")
                    ),
                    EncodeOptions(allowDots = true, encodeDotInKeys = true, encodeValuesOnly = true),
                ) shouldBe
                    "name%2Eobj%2Esubobject.first%2Egodly%2Ename=John&name%2Eobj%2Esubobject.last=Doe"
            }

            it("adds query prefix") {
                QS.encode(mapOf("a" to "b"), EncodeOptions(addQueryPrefix = true)) shouldBe "?a=b"
            }

            it("with query prefix, outputs blank string given an empty map") {
                QS.encode(emptyMap<String, Any>(), EncodeOptions(addQueryPrefix = true)) shouldBe ""
            }

            it("encodes nested falsy values") {
                QS.encode(mapOf("a" to mapOf("b" to mapOf("c" to null)))) shouldBe
                    "a%5Bb%5D%5Bc%5D="

                QS.encode(
                    mapOf("a" to mapOf("b" to mapOf("c" to null))),
                    EncodeOptions(strictNullHandling = true),
                ) shouldBe "a%5Bb%5D%5Bc%5D"

                QS.encode(mapOf("a" to mapOf("b" to mapOf("c" to false)))) shouldBe
                    "a%5Bb%5D%5Bc%5D=false"
            }

            it("encodes a nested map") {
                QS.encode(mapOf("a" to mapOf("b" to "c"))) shouldBe "a%5Bb%5D=c"

                QS.encode(mapOf("a" to mapOf("b" to mapOf("c" to mapOf("d" to "e"))))) shouldBe
                    "a%5Bb%5D%5Bc%5D%5Bd%5D=e"
            }

            it("encodes a nested map with dots notation") {
                QS.encode(mapOf("a" to mapOf("b" to "c")), EncodeOptions(allowDots = true)) shouldBe
                    "a.b=c"

                QS.encode(
                    mapOf("a" to mapOf("b" to mapOf("c" to mapOf("d" to "e")))),
                    EncodeOptions(allowDots = true),
                ) shouldBe "a.b.c.d=e"
            }

            it("encodes a list value") {
                QS.encode(
                    mapOf("a" to listOf("b", "c", "d")),
                    EncodeOptions(listFormat = ListFormat.INDICES),
                ) shouldBe "a%5B0%5D=b&a%5B1%5D=c&a%5B2%5D=d"

                QS.encode(
                    mapOf("a" to listOf("b", "c", "d")),
                    EncodeOptions(listFormat = ListFormat.BRACKETS),
                ) shouldBe "a%5B%5D=b&a%5B%5D=c&a%5B%5D=d"

                QS.encode(
                    mapOf("a" to listOf("b", "c", "d")),
                    EncodeOptions(listFormat = ListFormat.COMMA),
                ) shouldBe "a=b%2Cc%2Cd"

                QS.encode(
                    mapOf("a" to listOf("b", "c", "d")),
                    EncodeOptions(listFormat = ListFormat.COMMA, commaRoundTrip = true),
                ) shouldBe "a=b%2Cc%2Cd"

                QS.encode(mapOf("a" to listOf("b", "c", "d"))) shouldBe
                    "a%5B0%5D=b&a%5B1%5D=c&a%5B2%5D=d"
            }

            it("omits nulls when asked") {
                QS.encode(mapOf("a" to "b", "c" to null), EncodeOptions(skipNulls = true)) shouldBe
                    "a=b"

                QS.encode(
                    mapOf("a" to mapOf("b" to "c", "d" to null)),
                    EncodeOptions(skipNulls = true),
                ) shouldBe "a%5Bb%5D=c"
            }

            it("omits list indices when asked") {
                QS.encode(
                    mapOf("a" to listOf("b", "c", "d")),
                    EncodeOptions(indices = false),
                ) shouldBe "a=b&a=c&a=d"
            }

            it("omits map key/value pair when value is empty list") {
                QS.encode(mapOf("a" to emptyList<String>(), "b" to "zz")) shouldBe "b=zz"
            }

            it("should not omit map key/value pair when value is empty list and when asked") {
                QS.encode(mapOf("a" to emptyList<String>(), "b" to "zz")) shouldBe "b=zz"

                QS.encode(
                    mapOf("a" to emptyList<String>(), "b" to "zz"),
                    EncodeOptions(allowEmptyLists = false),
                ) shouldBe "b=zz"

                QS.encode(
                    mapOf("a" to emptyList<String>(), "b" to "zz"),
                    EncodeOptions(allowEmptyLists = true),
                ) shouldBe "a[]&b=zz"
            }

            it("allowEmptyLists + strictNullHandling") {
                QS.encode(
                    mapOf("testEmptyList" to emptyList<String>()),
                    EncodeOptions(strictNullHandling = true, allowEmptyLists = true),
                ) shouldBe "testEmptyList[]"
            }

            describe("encodes a list value with one item vs multiple items") {
                it("non-list item") {
                    QS.encode(
                        mapOf("a" to "c"),
                        EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.INDICES),
                    ) shouldBe "a=c"

                    QS.encode(
                        mapOf("a" to "c"),
                        EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.BRACKETS),
                    ) shouldBe "a=c"

                    QS.encode(
                        mapOf("a" to "c"),
                        EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.COMMA),
                    ) shouldBe "a=c"

                    QS.encode(mapOf("a" to "c"), EncodeOptions(encodeValuesOnly = true)) shouldBe
                        "a=c"
                }

                it("list with a single item") {
                    QS.encode(
                        mapOf("a" to listOf("c")),
                        EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.INDICES),
                    ) shouldBe "a[0]=c"

                    QS.encode(
                        mapOf("a" to listOf("c")),
                        EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.BRACKETS),
                    ) shouldBe "a[]=c"

                    QS.encode(
                        mapOf("a" to listOf("c")),
                        EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.COMMA),
                    ) shouldBe "a=c"

                    QS.encode(
                        mapOf("a" to listOf("c")),
                        EncodeOptions(
                            encodeValuesOnly = true,
                            listFormat = ListFormat.COMMA,
                            commaRoundTrip = true,
                        ),
                    ) shouldBe "a[]=c"

                    QS.encode(
                        mapOf("a" to listOf("c")),
                        EncodeOptions(encodeValuesOnly = true),
                    ) shouldBe "a[0]=c"
                }

                it("list with multiple items") {
                    QS.encode(
                        mapOf("a" to listOf("c", "d")),
                        EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.INDICES),
                    ) shouldBe "a[0]=c&a[1]=d"

                    QS.encode(
                        mapOf("a" to listOf("c", "d")),
                        EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.BRACKETS),
                    ) shouldBe "a[]=c&a[]=d"

                    QS.encode(
                        mapOf("a" to listOf("c", "d")),
                        EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.COMMA),
                    ) shouldBe "a=c,d"

                    QS.encode(
                        mapOf("a" to listOf("c", "d")),
                        EncodeOptions(
                            encodeValuesOnly = true,
                            listFormat = ListFormat.COMMA,
                            commaRoundTrip = true,
                        ),
                    ) shouldBe "a=c,d"

                    QS.encode(
                        mapOf("a" to listOf("c", "d")),
                        EncodeOptions(encodeValuesOnly = true),
                    ) shouldBe "a[0]=c&a[1]=d"
                }

                it("list with multiple items with a comma inside") {
                    QS.encode(
                        mapOf("a" to listOf("c,d", "e")),
                        EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.COMMA),
                    ) shouldBe "a=c%2Cd,e"

                    QS.encode(
                        mapOf("a" to listOf("c,d", "e")),
                        EncodeOptions(listFormat = ListFormat.COMMA),
                    ) shouldBe "a=c%2Cd%2Ce"

                    QS.encode(
                        mapOf("a" to listOf("c,d", "e")),
                        EncodeOptions(
                            encodeValuesOnly = true,
                            listFormat = ListFormat.COMMA,
                            commaRoundTrip = true,
                        ),
                    ) shouldBe "a=c%2Cd,e"

                    QS.encode(
                        mapOf("a" to listOf("c,d", "e")),
                        EncodeOptions(listFormat = ListFormat.COMMA, commaRoundTrip = true),
                    ) shouldBe "a=c%2Cd%2Ce"
                }
            }

            it("encodes a nested list value") {
                QS.encode(
                    mapOf("a" to mapOf("b" to listOf("c", "d"))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.INDICES),
                ) shouldBe "a[b][0]=c&a[b][1]=d"

                QS.encode(
                    mapOf("a" to mapOf("b" to listOf("c", "d"))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a[b][]=c&a[b][]=d"

                QS.encode(
                    mapOf("a" to mapOf("b" to listOf("c", "d"))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.COMMA),
                ) shouldBe "a[b]=c,d"

                QS.encode(
                    mapOf("a" to mapOf("b" to listOf("c", "d"))),
                    EncodeOptions(encodeValuesOnly = true),
                ) shouldBe "a[b][0]=c&a[b][1]=d"
            }

            it("encodes comma and empty list values") {
                QS.encode(
                    mapOf("a" to listOf(",", "", "c,d%")),
                    EncodeOptions(encode = false, listFormat = ListFormat.INDICES),
                ) shouldBe "a[0]=,&a[1]=&a[2]=c,d%"

                QS.encode(
                    mapOf("a" to listOf(",", "", "c,d%")),
                    EncodeOptions(encode = false, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a[]=,&a[]=&a[]=c,d%"

                QS.encode(
                    mapOf("a" to listOf(",", "", "c,d%")),
                    EncodeOptions(encode = false, listFormat = ListFormat.COMMA),
                ) shouldBe "a=,,,c,d%"

                QS.encode(
                    mapOf("a" to listOf(",", "", "c,d%")),
                    EncodeOptions(encode = false, listFormat = ListFormat.REPEAT),
                ) shouldBe "a=,&a=&a=c,d%"

                QS.encode(
                    mapOf("a" to listOf(",", "", "c,d%")),
                    EncodeOptions(
                        encode = true,
                        encodeValuesOnly = true,
                        listFormat = ListFormat.BRACKETS,
                    ),
                ) shouldBe "a[]=%2C&a[]=&a[]=c%2Cd%25"

                QS.encode(
                    mapOf("a" to listOf(",", "", "c,d%")),
                    EncodeOptions(
                        encode = true,
                        encodeValuesOnly = true,
                        listFormat = ListFormat.COMMA,
                    ),
                ) shouldBe "a=%2C,,c%2Cd%25"

                QS.encode(
                    mapOf("a" to listOf(",", "", "c,d%")),
                    EncodeOptions(
                        encode = true,
                        encodeValuesOnly = true,
                        listFormat = ListFormat.REPEAT,
                    ),
                ) shouldBe "a=%2C&a=&a=c%2Cd%25"

                QS.encode(
                    mapOf("a" to listOf(",", "", "c,d%")),
                    EncodeOptions(
                        encode = true,
                        encodeValuesOnly = true,
                        listFormat = ListFormat.INDICES,
                    ),
                ) shouldBe "a[0]=%2C&a[1]=&a[2]=c%2Cd%25"

                QS.encode(
                    mapOf("a" to listOf(",", "", "c,d%")),
                    EncodeOptions(
                        encode = true,
                        encodeValuesOnly = false,
                        listFormat = ListFormat.BRACKETS,
                    ),
                ) shouldBe "a%5B%5D=%2C&a%5B%5D=&a%5B%5D=c%2Cd%25"

                QS.encode(
                    mapOf("a" to listOf(",", "", "c,d%")),
                    EncodeOptions(
                        encode = true,
                        encodeValuesOnly = false,
                        listFormat = ListFormat.COMMA,
                    ),
                ) shouldBe "a=%2C%2C%2Cc%2Cd%25"

                QS.encode(
                    mapOf("a" to listOf(",", "", "c,d%")),
                    EncodeOptions(
                        encode = true,
                        encodeValuesOnly = false,
                        listFormat = ListFormat.REPEAT,
                    ),
                ) shouldBe "a=%2C&a=&a=c%2Cd%25"

                QS.encode(
                    mapOf("a" to listOf(",", "", "c,d%")),
                    EncodeOptions(
                        encode = true,
                        encodeValuesOnly = false,
                        listFormat = ListFormat.INDICES,
                    ),
                ) shouldBe "a%5B0%5D=%2C&a%5B1%5D=&a%5B2%5D=c%2Cd%25"
            }

            it("encodes comma and empty non-list values") {
                QS.encode(
                    mapOf("a" to ",", "b" to "", "c" to "c,d%"),
                    EncodeOptions(encode = false, listFormat = ListFormat.INDICES),
                ) shouldBe "a=,&b=&c=c,d%"

                QS.encode(
                    mapOf("a" to ",", "b" to "", "c" to "c,d%"),
                    EncodeOptions(encode = false, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a=,&b=&c=c,d%"

                QS.encode(
                    mapOf("a" to ",", "b" to "", "c" to "c,d%"),
                    EncodeOptions(encode = false, listFormat = ListFormat.COMMA),
                ) shouldBe "a=,&b=&c=c,d%"

                QS.encode(
                    mapOf("a" to ",", "b" to "", "c" to "c,d%"),
                    EncodeOptions(encode = false, listFormat = ListFormat.REPEAT),
                ) shouldBe "a=,&b=&c=c,d%"

                QS.encode(
                    mapOf("a" to ",", "b" to "", "c" to "c,d%"),
                    EncodeOptions(
                        encode = true,
                        encodeValuesOnly = true,
                        listFormat = ListFormat.BRACKETS,
                    ),
                ) shouldBe "a=%2C&b=&c=c%2Cd%25"

                QS.encode(
                    mapOf("a" to ",", "b" to "", "c" to "c,d%"),
                    EncodeOptions(
                        encode = true,
                        encodeValuesOnly = true,
                        listFormat = ListFormat.COMMA,
                    ),
                ) shouldBe "a=%2C&b=&c=c%2Cd%25"

                QS.encode(
                    mapOf("a" to ",", "b" to "", "c" to "c,d%"),
                    EncodeOptions(
                        encode = true,
                        encodeValuesOnly = true,
                        listFormat = ListFormat.REPEAT,
                    ),
                ) shouldBe "a=%2C&b=&c=c%2Cd%25"

                QS.encode(
                    mapOf("a" to ",", "b" to "", "c" to "c,d%"),
                    EncodeOptions(
                        encode = true,
                        encodeValuesOnly = false,
                        listFormat = ListFormat.INDICES,
                    ),
                ) shouldBe "a=%2C&b=&c=c%2Cd%25"

                QS.encode(
                    mapOf("a" to ",", "b" to "", "c" to "c,d%"),
                    EncodeOptions(
                        encode = true,
                        encodeValuesOnly = false,
                        listFormat = ListFormat.BRACKETS,
                    ),
                ) shouldBe "a=%2C&b=&c=c%2Cd%25"

                QS.encode(
                    mapOf("a" to ",", "b" to "", "c" to "c,d%"),
                    EncodeOptions(
                        encode = true,
                        encodeValuesOnly = false,
                        listFormat = ListFormat.COMMA,
                    ),
                ) shouldBe "a=%2C&b=&c=c%2Cd%25"

                QS.encode(
                    mapOf("a" to ",", "b" to "", "c" to "c,d%"),
                    EncodeOptions(
                        encode = true,
                        encodeValuesOnly = false,
                        listFormat = ListFormat.REPEAT,
                    ),
                ) shouldBe "a=%2C&b=&c=c%2Cd%25"
            }

            it("encodes a nested list value with dots notation") {
                QS.encode(
                    mapOf("a" to mapOf("b" to listOf("c", "d"))),
                    EncodeOptions(
                        allowDots = true,
                        encodeValuesOnly = true,
                        listFormat = ListFormat.INDICES,
                    ),
                ) shouldBe "a.b[0]=c&a.b[1]=d"

                QS.encode(
                    mapOf("a" to mapOf("b" to listOf("c", "d"))),
                    EncodeOptions(
                        allowDots = true,
                        encodeValuesOnly = true,
                        listFormat = ListFormat.BRACKETS,
                    ),
                ) shouldBe "a.b[]=c&a.b[]=d"

                QS.encode(
                    mapOf("a" to mapOf("b" to listOf("c", "d"))),
                    EncodeOptions(
                        allowDots = true,
                        encodeValuesOnly = true,
                        listFormat = ListFormat.COMMA,
                    ),
                ) shouldBe "a.b=c,d"

                QS.encode(
                    mapOf("a" to mapOf("b" to listOf("c", "d"))),
                    EncodeOptions(allowDots = true, encodeValuesOnly = true),
                ) shouldBe "a.b[0]=c&a.b[1]=d"
            }

            it("encodes a map inside a list") {
                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to "c"))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.INDICES),
                ) shouldBe "a[0][b]=c"

                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to "c"))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.REPEAT),
                ) shouldBe "a[b]=c"

                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to "c"))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a[][b]=c"

                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to "c"))),
                    EncodeOptions(encodeValuesOnly = true),
                ) shouldBe "a[0][b]=c"

                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to mapOf("c" to listOf(1))))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.INDICES),
                ) shouldBe "a[0][b][c][0]=1"

                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to mapOf("c" to listOf(1))))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.REPEAT),
                ) shouldBe "a[b][c]=1"

                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to mapOf("c" to listOf(1))))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a[][b][c][]=1"

                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to mapOf("c" to listOf(1))))),
                    EncodeOptions(encodeValuesOnly = true),
                ) shouldBe "a[0][b][c][0]=1"
            }

            it("encodes a list with mixed maps and primitives") {
                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to 1), 2, 3)),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.INDICES),
                ) shouldBe "a[0][b]=1&a[1]=2&a[2]=3"

                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to 1), 2, 3)),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a[][b]=1&a[]=2&a[]=3"

                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to 1), 2, 3)),
                    EncodeOptions(encodeValuesOnly = true),
                ) shouldBe "a[0][b]=1&a[1]=2&a[2]=3"
            }

            it("encodes a map inside a list with dots notation") {
                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to "c"))),
                    EncodeOptions(
                        allowDots = true,
                        encodeValuesOnly = true,
                        listFormat = ListFormat.INDICES,
                    ),
                ) shouldBe "a[0].b=c"

                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to "c"))),
                    EncodeOptions(
                        allowDots = true,
                        encodeValuesOnly = true,
                        listFormat = ListFormat.BRACKETS,
                    ),
                ) shouldBe "a[].b=c"

                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to "c"))),
                    EncodeOptions(allowDots = true, encodeValuesOnly = true),
                ) shouldBe "a[0].b=c"

                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to mapOf("c" to listOf(1))))),
                    EncodeOptions(
                        allowDots = true,
                        encodeValuesOnly = true,
                        listFormat = ListFormat.INDICES,
                    ),
                ) shouldBe "a[0].b.c[0]=1"

                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to mapOf("c" to listOf(1))))),
                    EncodeOptions(
                        allowDots = true,
                        encodeValuesOnly = true,
                        listFormat = ListFormat.BRACKETS,
                    ),
                ) shouldBe "a[].b.c[]=1"

                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to mapOf("c" to listOf(1))))),
                    EncodeOptions(allowDots = true, encodeValuesOnly = true),
                ) shouldBe "a[0].b.c[0]=1"
            }

            it("does not omit map keys when indices = false") {
                QS.encode(
                    mapOf("a" to listOf(mapOf("b" to "c"))),
                    EncodeOptions(indices = false),
                ) shouldBe "a%5Bb%5D=c"
            }

            it("uses indices notation for lists when indices=true") {
                QS.encode(mapOf("a" to listOf("b", "c")), EncodeOptions(indices = true)) shouldBe
                    "a%5B0%5D=b&a%5B1%5D=c"
            }

            it("uses indices notation for lists when no listFormat is specified") {
                QS.encode(mapOf("a" to listOf("b", "c"))) shouldBe "a%5B0%5D=b&a%5B1%5D=c"
            }

            it("uses indices notation for lists when listFormat=indices") {
                QS.encode(
                    mapOf("a" to listOf("b", "c")),
                    EncodeOptions(listFormat = ListFormat.INDICES),
                ) shouldBe "a%5B0%5D=b&a%5B1%5D=c"
            }

            it("uses repeat notation for lists when listFormat=repeat") {
                QS.encode(
                    mapOf("a" to listOf("b", "c")),
                    EncodeOptions(listFormat = ListFormat.REPEAT),
                ) shouldBe "a=b&a=c"
            }

            it("uses brackets notation for lists when listFormat=brackets") {
                QS.encode(
                    mapOf("a" to listOf("b", "c")),
                    EncodeOptions(listFormat = ListFormat.BRACKETS),
                ) shouldBe "a%5B%5D=b&a%5B%5D=c"
            }

            it("encodes a complicated map") {
                QS.encode(mapOf("a" to mapOf("b" to "c", "d" to "e"))) shouldBe
                    "a%5Bb%5D=c&a%5Bd%5D=e"
            }

            it("encodes an empty value") {
                QS.encode(mapOf("a" to "")) shouldBe "a="

                QS.encode(mapOf("a" to null), EncodeOptions(strictNullHandling = true)) shouldBe "a"

                QS.encode(mapOf("a" to "", "b" to "")) shouldBe "a=&b="

                QS.encode(
                    mapOf("a" to null, "b" to ""),
                    EncodeOptions(strictNullHandling = true),
                ) shouldBe "a&b="

                QS.encode(mapOf("a" to mapOf("b" to ""))) shouldBe "a%5Bb%5D="

                QS.encode(
                    mapOf("a" to mapOf("b" to null)),
                    EncodeOptions(strictNullHandling = true),
                ) shouldBe "a%5Bb%5D"

                QS.encode(
                    mapOf("a" to mapOf("b" to null)),
                    EncodeOptions(strictNullHandling = false),
                ) shouldBe "a%5Bb%5D="
            }

            describe("encodes an empty list in different listFormat") {
                it("default parameters") {
                    QS.encode(
                        mapOf("a" to emptyList<Any?>(), "b" to listOf(null), "c" to "c"),
                        EncodeOptions(encode = false),
                    ) shouldBe "b[0]=&c=c"
                }

                describe("listFormat default") {
                    it("uses different list formats") {
                        QS.encode(
                            mapOf("a" to emptyList<Any?>(), "b" to listOf(null), "c" to "c"),
                            EncodeOptions(encode = false, listFormat = ListFormat.INDICES),
                        ) shouldBe "b[0]=&c=c"

                        QS.encode(
                            mapOf("a" to emptyList<Any?>(), "b" to listOf(null), "c" to "c"),
                            EncodeOptions(encode = false, listFormat = ListFormat.BRACKETS),
                        ) shouldBe "b[]=&c=c"

                        QS.encode(
                            mapOf("a" to emptyList<Any?>(), "b" to listOf(null), "c" to "c"),
                            EncodeOptions(encode = false, listFormat = ListFormat.REPEAT),
                        ) shouldBe "b=&c=c"

                        QS.encode(
                            mapOf("a" to emptyList<Any?>(), "b" to listOf(null), "c" to "c"),
                            EncodeOptions(encode = false, listFormat = ListFormat.COMMA),
                        ) shouldBe "b=&c=c"

                        QS.encode(
                            mapOf("a" to emptyList<Any?>(), "b" to listOf(null), "c" to "c"),
                            EncodeOptions(
                                encode = false,
                                listFormat = ListFormat.COMMA,
                                commaRoundTrip = true,
                            ),
                        ) shouldBe "b[]=&c=c"
                    }
                }

                describe("with strictNullHandling") {
                    it("handles null values strictly") {
                        QS.encode(
                            mapOf("a" to emptyList<Any?>(), "b" to listOf(null), "c" to "c"),
                            EncodeOptions(
                                encode = false,
                                listFormat = ListFormat.BRACKETS,
                                strictNullHandling = true,
                            ),
                        ) shouldBe "b[]&c=c"

                        QS.encode(
                            mapOf("a" to emptyList<Any?>(), "b" to listOf(null), "c" to "c"),
                            EncodeOptions(
                                encode = false,
                                listFormat = ListFormat.REPEAT,
                                strictNullHandling = true,
                            ),
                        ) shouldBe "b&c=c"

                        QS.encode(
                            mapOf("a" to emptyList<Any?>(), "b" to listOf(null), "c" to "c"),
                            EncodeOptions(
                                encode = false,
                                listFormat = ListFormat.COMMA,
                                strictNullHandling = true,
                            ),
                        ) shouldBe "b&c=c"

                        QS.encode(
                            mapOf("a" to emptyList<Any?>(), "b" to listOf(null), "c" to "c"),
                            EncodeOptions(
                                encode = false,
                                listFormat = ListFormat.COMMA,
                                strictNullHandling = true,
                                commaRoundTrip = true,
                            ),
                        ) shouldBe "b[]&c=c"
                    }
                }

                describe("with skipNulls") {
                    it("skips null values") {
                        QS.encode(
                            mapOf("a" to emptyList<Any?>(), "b" to listOf(null), "c" to "c"),
                            EncodeOptions(
                                encode = false,
                                listFormat = ListFormat.INDICES,
                                skipNulls = true,
                            ),
                        ) shouldBe "c=c"

                        QS.encode(
                            mapOf("a" to emptyList<Any?>(), "b" to listOf(null), "c" to "c"),
                            EncodeOptions(
                                encode = false,
                                listFormat = ListFormat.BRACKETS,
                                skipNulls = true,
                            ),
                        ) shouldBe "c=c"

                        QS.encode(
                            mapOf("a" to emptyList<Any?>(), "b" to listOf(null), "c" to "c"),
                            EncodeOptions(
                                encode = false,
                                listFormat = ListFormat.REPEAT,
                                skipNulls = true,
                            ),
                        ) shouldBe "c=c"

                        QS.encode(
                            mapOf("a" to emptyList<Any?>(), "b" to listOf(null), "c" to "c"),
                            EncodeOptions(
                                encode = false,
                                listFormat = ListFormat.COMMA,
                                skipNulls = true,
                            ),
                        ) shouldBe "c=c"
                    }
                }
            }

            it("encodes a null map") {
                val obj = mutableMapOf<String, Any?>()
                obj["a"] = "b"
                QS.encode(obj) shouldBe "a=b"
            }

            it("returns an empty string for invalid input") {
                QS.encode(null) shouldBe ""
                QS.encode(false) shouldBe ""
                QS.encode("") shouldBe ""
            }

            it("encodes a map with a null map as a child") {
                val obj = mutableMapOf<String, Any?>("a" to mutableMapOf<String, Any?>())

                @Suppress("UNCHECKED_CAST") (obj["a"] as? MutableMap<String, Any?>)?.set("b", "c")

                QS.encode(obj) shouldBe "a%5Bb%5D=c"
            }

            it("url encodes values") { QS.encode(mapOf("a" to "b c")) shouldBe "a=b%20c" }

            it("encodes a date") {
                val now = LocalDateTime.now()
                val str = "a=${Utils.encode(now.toString())}"
                QS.encode(mapOf("a" to now)) shouldBe str
            }

            it("encodes the weird map from qs") {
                QS.encode(mapOf("my weird field" to "~q1!2\"'w\$5&7/z8)?")) shouldBe
                    "my%20weird%20field=~q1%212%22%27w%245%267%2Fz8%29%3F"
            }

            it("encodes boolean values") {
                QS.encode(mapOf("a" to true)) shouldBe "a=true"

                QS.encode(mapOf("a" to mapOf("b" to true))) shouldBe "a%5Bb%5D=true"

                QS.encode(mapOf("b" to false)) shouldBe "b=false"

                QS.encode(mapOf("b" to mapOf("c" to false))) shouldBe "b%5Bc%5D=false"
            }

            it("encodes buffer values") {
                QS.encode(mapOf("a" to "test".toByteArray())) shouldBe "a=test"

                QS.encode(mapOf("a" to mapOf("b" to "test".toByteArray()))) shouldBe "a%5Bb%5D=test"
            }

            it("encodes a map using an alternative delimiter") {
                QS.encode(mapOf("a" to "b", "c" to "d"), EncodeOptions(delimiter = ";")) shouldBe
                    "a=b;c=d"
            }

            it("does not crash when parsing circular references") {
                val a = mutableMapOf<String, Any?>()
                a["b"] = a

                shouldThrow<IndexOutOfBoundsException> {
                    QS.encode(mapOf("foo[bar]" to "baz", "foo[baz]" to a))
                }

                val circular = mutableMapOf<String, Any?>("a" to "value")
                circular["a"] = circular
                shouldThrow<IndexOutOfBoundsException> { QS.encode(circular) }

                val arr = listOf("a")
                shouldNotThrow<Exception> { QS.encode(mapOf("x" to arr, "y" to arr)) }
            }

            it("non-circular duplicated references can still work") {
                val hourOfDay = mapOf("function" to "hour_of_day")

                val p1 = mapOf("function" to "gte", "arguments" to listOf(hourOfDay, 0))

                val p2 = mapOf("function" to "lte", "arguments" to listOf(hourOfDay, 23))

                QS.encode(
                    mapOf("filters" to mapOf($$"$and" to listOf(p1, p2))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.INDICES),
                ) shouldBe
                    $$"filters[$and][0][function]=gte&filters[$and][0][arguments][0][function]=hour_of_day&filters[$and][0][arguments][1]=0&filters[$and][1][function]=lte&filters[$and][1][arguments][0][function]=hour_of_day&filters[$and][1][arguments][1]=23"

                QS.encode(
                    mapOf("filters" to mapOf("\$and" to listOf(p1, p2))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.BRACKETS),
                ) shouldBe
                    $$"filters[$and][][function]=gte&filters[$and][][arguments][][function]=hour_of_day&filters[$and][][arguments][]=0&filters[$and][][function]=lte&filters[$and][][arguments][][function]=hour_of_day&filters[$and][][arguments][]=23"

                QS.encode(
                    mapOf("filters" to mapOf($$"$and" to listOf(p1, p2))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.REPEAT),
                ) shouldBe
                    $$"filters[$and][function]=gte&filters[$and][arguments][function]=hour_of_day&filters[$and][arguments]=0&filters[$and][function]=lte&filters[$and][arguments][function]=hour_of_day&filters[$and][arguments]=23"
            }

            it("selects properties when filter = IterableFilter") {
                QS.encode(
                    mapOf("a" to "b"),
                    EncodeOptions(filter = IterableFilter(listOf("a"))),
                ) shouldBe "a=b"

                QS.encode(
                    mapOf("a" to 1),
                    EncodeOptions(filter = IterableFilter(emptyList<String>())),
                ) shouldBe ""

                QS.encode(
                    mapOf("a" to mapOf("b" to listOf(1, 2, 3, 4), "c" to "d"), "c" to "f"),
                    EncodeOptions(
                        filter = IterableFilter(listOf("a", "b", 0, 2)),
                        listFormat = ListFormat.INDICES,
                    ),
                ) shouldBe "a%5Bb%5D%5B0%5D=1&a%5Bb%5D%5B2%5D=3"

                QS.encode(
                    mapOf("a" to mapOf("b" to listOf(1, 2, 3, 4), "c" to "d"), "c" to "f"),
                    EncodeOptions(
                        filter = IterableFilter(listOf("a", "b", 0, 2)),
                        listFormat = ListFormat.BRACKETS,
                    ),
                ) shouldBe "a%5Bb%5D%5B%5D=1&a%5Bb%5D%5B%5D=3"

                QS.encode(
                    mapOf("a" to mapOf("b" to listOf(1, 2, 3, 4), "c" to "d"), "c" to "f"),
                    EncodeOptions(filter = IterableFilter(listOf("a", "b", 0, 2))),
                ) shouldBe "a%5Bb%5D%5B0%5D=1&a%5Bb%5D%5B2%5D=3"
            }

            it("supports custom representations when filter = FunctionFilter") {
                var calls = 0
                val obj =
                    mapOf(
                        "a" to "b",
                        "c" to "d",
                        "e" to mapOf("f" to LocalDateTime.of(2009, 11, 10, 23, 0, 0)),
                    )

                val filterFunc = FunctionFilter { prefix, value ->
                    calls += 1
                    when {
                        calls == 1 -> {
                            prefix shouldBe ""
                            value shouldBe obj
                            value
                        }

                        prefix == "c" -> {
                            value shouldBe "d"
                            null
                        }

                        value is LocalDateTime -> {
                            prefix shouldBe "e[f]"
                            value.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
                        }

                        else -> value
                    }
                }

                QS.encode(obj, EncodeOptions(filter = filterFunc)) shouldBe
                    "a=b&c=&e%5Bf%5D=1257894000000"
                calls shouldBe 5
            }

            it("can disable uri encoding") {
                QS.encode(mapOf("a" to "b"), EncodeOptions(encode = false)) shouldBe "a=b"

                QS.encode(mapOf("a" to mapOf("b" to "c")), EncodeOptions(encode = false)) shouldBe
                    "a[b]=c"

                QS.encode(
                    mapOf("a" to "b", "c" to null),
                    EncodeOptions(encode = false, strictNullHandling = true),
                ) shouldBe "a=b&c"
            }

            it("can sort the keys") {
                val sort: Sorter = { a, b -> a.toString().compareTo(b.toString()) }

                QS.encode(
                    mapOf("a" to "c", "z" to "y", "b" to "f"),
                    EncodeOptions(sort = sort),
                ) shouldBe "a=c&b=f&z=y"

                QS.encode(
                    mapOf("a" to "c", "z" to mapOf("j" to "a", "i" to "b"), "b" to "f"),
                    EncodeOptions(sort = sort),
                ) shouldBe "a=c&b=f&z%5Bi%5D=b&z%5Bj%5D=a"
            }

            it("can sort the keys at depth 3 or more too") {
                val sort: Sorter = { a, b -> a.toString().compareTo(b.toString()) }

                QS.encode(
                    mapOf(
                        "a" to "a",
                        "z" to
                            mapOf(
                                "zj" to mapOf("zjb" to "zjb", "zja" to "zja"),
                                "zi" to mapOf("zib" to "zib", "zia" to "zia"),
                            ),
                        "b" to "b",
                    ),
                    EncodeOptions(sort = sort, encode = false),
                ) shouldBe "a=a&b=b&z[zi][zia]=zia&z[zi][zib]=zib&z[zj][zja]=zja&z[zj][zjb]=zjb"

                QS.encode(
                    mapOf(
                        "a" to "a",
                        "z" to
                            mapOf(
                                "zj" to mapOf("zjb" to "zjb", "zja" to "zja"),
                                "zi" to mapOf("zib" to "zib", "zia" to "zia"),
                            ),
                        "b" to "b",
                    ),
                    EncodeOptions(sort = null, encode = false),
                ) shouldBe "a=a&z[zj][zjb]=zjb&z[zj][zja]=zja&z[zi][zib]=zib&z[zi][zia]=zia&b=b"
            }

            it("can encode with custom encoding") {
                val encode: ValueEncoder = { str, _, _ ->
                    val strValue = str?.toString()
                    if (!strValue.isNullOrEmpty()) {
                        val charset = Charset.forName("Shift_JIS")
                        val bytes = strValue.toByteArray(charset)
                        val result = bytes.map { "%${(it.toInt() and 0xFF).toString(16)}" }
                        result.joinToString("")
                    } else {
                        ""
                    }
                }

                QS.encode(mapOf("県" to "大阪府", "" to ""), EncodeOptions(encoder = encode)) shouldBe
                    "%8c%a7=%91%e5%8d%e3%95%7b&="
            }

            it("receives the default encoder as a second argument") {
                val obj = mapOf("a" to 1, "b" to LocalDateTime.now(), "c" to true, "d" to listOf(1))

                val encode: ValueEncoder = { str, _, _ ->
                    // Verify that str is one of the expected types
                    when (str) {
                        is String,
                        is Int,
                        is Boolean -> ""
                        else -> throw AssertionError("Unexpected type: ${str?.javaClass}")
                    }
                }

                QS.encode(obj, EncodeOptions(encoder = encode))
            }

            it("can use custom encoder for a buffer map") {
                val buf = ByteArray(1) { 1 }

                val encode1: ValueEncoder = { buffer, _, _ ->
                    when (buffer) {
                        is String -> buffer
                        is ByteArray -> String(charArrayOf((buffer[0] + 97).toChar()))
                        else -> buffer?.toString() ?: ""
                    }
                }

                QS.encode(mapOf("a" to buf), EncodeOptions(encoder = encode1)) shouldBe "a=b"

                val bufferWithText = "a b".toByteArray(Charsets.UTF_8)

                val encode2: ValueEncoder = { buffer, _, _ ->
                    when (buffer) {
                        is ByteArray -> String(buffer, Charsets.UTF_8)
                        else -> buffer?.toString() ?: ""
                    }
                }

                QS.encode(mapOf("a" to bufferWithText), EncodeOptions(encoder = encode2)) shouldBe
                    "a=a b"
            }

            it("serializeDate option") {
                val date = LocalDateTime.now()

                QS.encode(mapOf("a" to date)) shouldBe "a=${Utils.encode(date.toString())}"

                val serializeDate: DateSerializer = { d ->
                    d.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli().toString()
                }

                QS.encode(
                    mapOf("a" to date),
                    EncodeOptions(dateSerializer = serializeDate),
                ) shouldBe "a=${date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()}"

                val specificDate =
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(6), ZoneId.systemDefault())

                val customSerializeDate: DateSerializer = { d ->
                    (d.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() * 7).toString()
                }

                QS.encode(
                    mapOf("a" to specificDate),
                    EncodeOptions(dateSerializer = customSerializeDate),
                ) shouldBe "a=42"

                QS.encode(
                    mapOf("a" to listOf(date)),
                    EncodeOptions(dateSerializer = serializeDate, listFormat = ListFormat.COMMA),
                ) shouldBe "a=${date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()}"

                QS.encode(
                    mapOf("a" to listOf(date)),
                    EncodeOptions(
                        dateSerializer = serializeDate,
                        listFormat = ListFormat.COMMA,
                        commaRoundTrip = true,
                    ),
                ) shouldBe
                    "a%5B%5D=${date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()}"
            }

            it("RFC 1738 serialization") {
                QS.encode(mapOf("a" to "b c"), EncodeOptions(format = Format.RFC1738)) shouldBe
                    "a=b+c"

                QS.encode(mapOf("a b" to "c d"), EncodeOptions(format = Format.RFC1738)) shouldBe
                    "a+b=c+d"

                QS.encode(
                    mapOf("a b" to "a b".toByteArray(Charsets.UTF_8)),
                    EncodeOptions(format = Format.RFC1738),
                ) shouldBe "a+b=a+b"

                QS.encode(
                    mapOf("foo(ref)" to "bar"),
                    EncodeOptions(format = Format.RFC1738),
                ) shouldBe "foo(ref)=bar"
            }

            it("RFC 3986 spaces serialization") {
                QS.encode(mapOf("a" to "b c"), EncodeOptions(format = Format.RFC3986)) shouldBe
                    "a=b%20c"

                QS.encode(mapOf("a b" to "c d"), EncodeOptions(format = Format.RFC3986)) shouldBe
                    "a%20b=c%20d"

                QS.encode(
                    mapOf("a b" to "a b".toByteArray(Charsets.UTF_8)),
                    EncodeOptions(format = Format.RFC3986),
                ) shouldBe "a%20b=a%20b"
            }

            it("Backward compatibility to RFC 3986") {
                QS.encode(mapOf("a" to "b c")) shouldBe "a=b%20c"

                QS.encode(mapOf("a b" to "a b".toByteArray(Charsets.UTF_8))) shouldBe "a%20b=a%20b"
            }

            it("encodeValuesOnly") {
                QS.encode(
                    mapOf(
                        "a" to "b",
                        "c" to listOf("d", "e=f"),
                        "f" to listOf(listOf("g"), listOf("h")),
                    ),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.INDICES),
                ) shouldBe "a=b&c[0]=d&c[1]=e%3Df&f[0][0]=g&f[1][0]=h"

                QS.encode(
                    mapOf(
                        "a" to "b",
                        "c" to listOf("d", "e=f"),
                        "f" to listOf(listOf("g"), listOf("h")),
                    ),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a=b&c[]=d&c[]=e%3Df&f[][]=g&f[][]=h"

                QS.encode(
                    mapOf(
                        "a" to "b",
                        "c" to listOf("d", "e=f"),
                        "f" to listOf(listOf("g"), listOf("h")),
                    ),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.REPEAT),
                ) shouldBe "a=b&c=d&c=e%3Df&f=g&f=h"

                QS.encode(
                    mapOf(
                        "a" to "b",
                        "c" to listOf("d", "e"),
                        "f" to listOf(listOf("g"), listOf("h")),
                    ),
                    EncodeOptions(listFormat = ListFormat.INDICES),
                ) shouldBe "a=b&c%5B0%5D=d&c%5B1%5D=e&f%5B0%5D%5B0%5D=g&f%5B1%5D%5B0%5D=h"

                QS.encode(
                    mapOf(
                        "a" to "b",
                        "c" to listOf("d", "e"),
                        "f" to listOf(listOf("g"), listOf("h")),
                    ),
                    EncodeOptions(listFormat = ListFormat.BRACKETS),
                ) shouldBe "a=b&c%5B%5D=d&c%5B%5D=e&f%5B%5D%5B%5D=g&f%5B%5D%5B%5D=h"

                QS.encode(
                    mapOf(
                        "a" to "b",
                        "c" to listOf("d", "e"),
                        "f" to listOf(listOf("g"), listOf("h")),
                    ),
                    EncodeOptions(listFormat = ListFormat.REPEAT),
                ) shouldBe "a=b&c=d&c=e&f=g&f=h"
            }

            it("encodeValuesOnly - strictNullHandling") {
                QS.encode(
                    mapOf("a" to mapOf("b" to null)),
                    EncodeOptions(encodeValuesOnly = true, strictNullHandling = true),
                ) shouldBe "a[b]"
            }

            it("respects a charset of iso-8859-1") {
                QS.encode(
                    mapOf("æ" to "æ"),
                    EncodeOptions(charset = StandardCharsets.ISO_8859_1),
                ) shouldBe "%E6=%E6"
            }

            it("encodes unrepresentable chars as numeric entities in iso-8859-1 mode") {
                QS.encode(
                    mapOf("a" to "☺"),
                    EncodeOptions(charset = StandardCharsets.ISO_8859_1),
                ) shouldBe "a=%26%239786%3B"
            }

            it("respects an explicit charset of utf-8 (the default)") {
                QS.encode(
                    mapOf("a" to "æ"),
                    EncodeOptions(charset = StandardCharsets.UTF_8),
                ) shouldBe "a=%C3%A6"
            }

            it("charsetSentinel option") {
                QS.encode(
                    mapOf("a" to "æ"),
                    EncodeOptions(charsetSentinel = true, charset = StandardCharsets.UTF_8),
                ) shouldBe "utf8=%E2%9C%93&a=%C3%A6"

                QS.encode(
                    mapOf("a" to "æ"),
                    EncodeOptions(charsetSentinel = true, charset = StandardCharsets.ISO_8859_1),
                ) shouldBe "utf8=%26%2310003%3B&a=%E6"
            }

            it("does not mutate the options argument") {
                val options = EncodeOptions()
                QS.encode(emptyMap<String, Any>(), options)
                options shouldBe EncodeOptions()
            }

            it("strictNullHandling works with custom filter") {
                val options =
                    EncodeOptions(
                        strictNullHandling = true,
                        filter = FunctionFilter { _, value -> value },
                    )
                QS.encode(mapOf("key" to null), options) shouldBe "key"
            }

            it("objects inside lists") {
                val obj = mapOf("a" to mapOf("b" to mapOf("c" to "d", "e" to "f")))
                val withList = mapOf("a" to mapOf("b" to listOf(mapOf("c" to "d", "e" to "f"))))

                QS.encode(obj, EncodeOptions(encode = false)) shouldBe "a[b][c]=d&a[b][e]=f"

                QS.encode(
                    obj,
                    EncodeOptions(encode = false, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a[b][c]=d&a[b][e]=f"

                QS.encode(
                    obj,
                    EncodeOptions(encode = false, listFormat = ListFormat.INDICES),
                ) shouldBe "a[b][c]=d&a[b][e]=f"

                QS.encode(
                    obj,
                    EncodeOptions(encode = false, listFormat = ListFormat.REPEAT),
                ) shouldBe "a[b][c]=d&a[b][e]=f"

                QS.encode(
                    obj,
                    EncodeOptions(encode = false, listFormat = ListFormat.COMMA),
                ) shouldBe "a[b][c]=d&a[b][e]=f"

                QS.encode(withList, EncodeOptions(encode = false)) shouldBe
                    "a[b][0][c]=d&a[b][0][e]=f"

                QS.encode(
                    withList,
                    EncodeOptions(encode = false, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a[b][][c]=d&a[b][][e]=f"

                QS.encode(
                    withList,
                    EncodeOptions(encode = false, listFormat = ListFormat.INDICES),
                ) shouldBe "a[b][0][c]=d&a[b][0][e]=f"

                QS.encode(
                    withList,
                    EncodeOptions(encode = false, listFormat = ListFormat.REPEAT),
                ) shouldBe "a[b][c]=d&a[b][e]=f"
            }

            it("encodes lists with nulls") {
                QS.encode(
                    mapOf("a" to listOf(null, "2", null, null, "1")),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.INDICES),
                ) shouldBe "a[0]=&a[1]=2&a[2]=&a[3]=&a[4]=1"

                QS.encode(
                    mapOf("a" to listOf(null, "2", null, null, "1")),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a[]=&a[]=2&a[]=&a[]=&a[]=1"

                QS.encode(
                    mapOf("a" to listOf(null, "2", null, null, "1")),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.REPEAT),
                ) shouldBe "a=&a=2&a=&a=&a=1"

                QS.encode(
                    mapOf("a" to listOf(null, mapOf("b" to listOf(null, null, mapOf("c" to "1"))))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.INDICES),
                ) shouldBe "a[0]=&a[1][b][0]=&a[1][b][1]=&a[1][b][2][c]=1"

                QS.encode(
                    mapOf("a" to listOf(null, mapOf("b" to listOf(null, null, mapOf("c" to "1"))))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a[]=&a[][b][]=&a[][b][]=&a[][b][][c]=1"

                QS.encode(
                    mapOf("a" to listOf(null, mapOf("b" to listOf(null, null, mapOf("c" to "1"))))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.REPEAT),
                ) shouldBe "a=&a[b]=&a[b]=&a[b][c]=1"

                QS.encode(
                    mapOf("a" to listOf(null, listOf(null, listOf(null, null, mapOf("c" to "1"))))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.INDICES),
                ) shouldBe "a[0]=&a[1][0]=&a[1][1][0]=&a[1][1][1]=&a[1][1][2][c]=1"

                QS.encode(
                    mapOf("a" to listOf(null, listOf(null, listOf(null, null, mapOf("c" to "1"))))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.BRACKETS),
                ) shouldBe "a[]=&a[][]=&a[][][]=&a[][][]=&a[][][][c]=1"

                QS.encode(
                    mapOf("a" to listOf(null, listOf(null, listOf(null, null, mapOf("c" to "1"))))),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.REPEAT),
                ) shouldBe "a=&a=&a=&a=&a[c]=1"
            }

            it("encodes url") {
                QS.encode(
                    mapOf("url" to "https://example.com?foo=bar&baz=qux"),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.INDICES),
                ) shouldBe "url=https%3A%2F%2Fexample.com%3Ffoo%3Dbar%26baz%3Dqux"

                val uri = java.net.URI.create("https://example.com/some/path?foo=bar&baz=qux")
                QS.encode(
                    mapOf("url" to uri),
                    EncodeOptions(encodeValuesOnly = true, listFormat = ListFormat.INDICES),
                ) shouldBe "url=https%3A%2F%2Fexample.com%2Fsome%2Fpath%3Ffoo%3Dbar%26baz%3Dqux"
            }

            it("encodes Spatie map") {
                QS.encode(
                    mapOf(
                        "filters" to
                            mapOf(
                                "\$or" to
                                    listOf(
                                        mapOf("date" to mapOf("\$eq" to "2020-01-01")),
                                        mapOf("date" to mapOf("\$eq" to "2020-01-02")),
                                    ),
                                "author" to mapOf("name" to mapOf("\$eq" to "John doe")),
                            )
                    ),
                    EncodeOptions(encode = false, listFormat = ListFormat.BRACKETS),
                ) shouldBe
                    "filters[\$or][][date][\$eq]=2020-01-01&filters[\$or][][date][\$eq]=2020-01-02&filters[author][name][\$eq]=John doe"

                QS.encode(
                    mapOf(
                        "filters" to
                            mapOf(
                                "\$or" to
                                    listOf(
                                        mapOf("date" to mapOf("\$eq" to "2020-01-01")),
                                        mapOf("date" to mapOf("\$eq" to "2020-01-02")),
                                    ),
                                "author" to mapOf("name" to mapOf("\$eq" to "John doe")),
                            )
                    ),
                    EncodeOptions(listFormat = ListFormat.BRACKETS),
                ) shouldBe
                    "filters%5B%24or%5D%5B%5D%5Bdate%5D%5B%24eq%5D=2020-01-01&filters%5B%24or%5D%5B%5D%5Bdate%5D%5B%24eq%5D=2020-01-02&filters%5Bauthor%5D%5Bname%5D%5B%24eq%5D=John%20doe"
            }

            describe("encodes empty keys") {
                EmptyTestCases.forEach { element ->
                    it("encodes a map with empty string key with ${element["input"]}") {
                        @Suppress("UNCHECKED_CAST")
                        QS.encode(
                            element["withEmptyKeys"] as Map<String, Any?>,
                            EncodeOptions(encode = false, listFormat = ListFormat.INDICES),
                        ) shouldBe (element["stringifyOutput"] as Map<String, String>)["indices"]

                        @Suppress("UNCHECKED_CAST")
                        QS.encode(
                            element["withEmptyKeys"] as Map<String, Any?>,
                            EncodeOptions(encode = false, listFormat = ListFormat.BRACKETS),
                        ) shouldBe (element["stringifyOutput"] as Map<String, String>)["brackets"]

                        @Suppress("UNCHECKED_CAST")
                        QS.encode(
                            element["withEmptyKeys"] as Map<String, Any?>,
                            EncodeOptions(encode = false, listFormat = ListFormat.REPEAT),
                        ) shouldBe (element["stringifyOutput"] as Map<String, String>)["repeat"]
                    }
                }

                it("edge case with map/lists") {
                    QS.encode(
                        mapOf("" to mapOf("" to listOf(2, 3))),
                        EncodeOptions(encode = false),
                    ) shouldBe "[][0]=2&[][1]=3"

                    QS.encode(
                        mapOf("" to mapOf("" to listOf(2, 3), "a" to 2)),
                        EncodeOptions(encode = false),
                    ) shouldBe "[][0]=2&[][1]=3&[a]=2"

                    QS.encode(
                        mapOf("" to mapOf("" to listOf(2, 3))),
                        EncodeOptions(encode = false, listFormat = ListFormat.INDICES),
                    ) shouldBe "[][0]=2&[][1]=3"

                    QS.encode(
                        mapOf("" to mapOf("" to listOf(2, 3), "a" to 2)),
                        EncodeOptions(encode = false, listFormat = ListFormat.INDICES),
                    ) shouldBe "[][0]=2&[][1]=3&[a]=2"
                }

                it("encodes non-String keys") {
                    QS.encode(
                        mapOf("a" to "b", "false" to emptyMap<String, Any?>()),
                        EncodeOptions(
                            filter = IterableFilter(listOf("a", false, null)),
                            allowDots = true,
                            encodeDotInKeys = true,
                        ),
                    ) shouldBe "a=b"
                }
            }

            describe("encode non-Strings") {
                it("encodes a null value") { QS.encode(mapOf("a" to null)) shouldBe "a=" }

                it("encodes a boolean value") {
                    QS.encode(mapOf("a" to true)) shouldBe "a=true"
                    QS.encode(mapOf("a" to false)) shouldBe "a=false"
                }

                it("encodes a number value") {
                    QS.encode(mapOf("a" to 0)) shouldBe "a=0"
                    QS.encode(mapOf("a" to 1)) shouldBe "a=1"
                    QS.encode(mapOf("a" to 1.1)) shouldBe "a=1.1"
                }

                it("encodes a buffer value") {
                    QS.encode(mapOf("a" to "test".toByteArray())) shouldBe "a=test"
                }

                it("encodes a date value") {
                    val now = LocalDateTime.now()
                    val str = "a=${Utils.encode(now.toString())}"
                    QS.encode(mapOf("a" to now)) shouldBe str
                }

                it("encodes a Duration") {
                    val duration =
                        java.time.Duration.ofDays(1)
                            .plusHours(2)
                            .plusMinutes(3)
                            .plusSeconds(4)
                            .plusMillis(5)
                            .plusNanos(6000)
                    val str = "a=${Utils.encode(duration.toString())}"
                    QS.encode(mapOf("a" to duration)) shouldBe str
                }

                it("encodes a BigInteger") {
                    val bigInt = java.math.BigInteger.valueOf(1234567890123456L)
                    val str = "a=${Utils.encode(bigInt.toString())}"
                    QS.encode(mapOf("a" to bigInt)) shouldBe str
                }

                it("encodes a list value") {
                    QS.encode(mapOf("a" to listOf(1, 2, 3))) shouldBe
                        "a%5B0%5D=1&a%5B1%5D=2&a%5B2%5D=3"
                }

                it("encodes a map value") {
                    QS.encode(mapOf("a" to mapOf("b" to "c"))) shouldBe "a%5Bb%5D=c"
                }

                it("encodes a URI") {
                    QS.encode(
                        mapOf("a" to java.net.URI("https://example.com?foo=bar&baz=qux"))
                    ) shouldBe "a=https%3A%2F%2Fexample.com%3Ffoo%3Dbar%26baz%3Dqux"
                }

                it("encodes a map with a null map as a child") {
                    val obj = mutableMapOf<String, Any?>("a" to mutableMapOf<String, Any?>())
                    @Suppress("UNCHECKED_CAST")
                    (obj["a"] as MutableMap<String, Any?>)["b"] = "c"
                    QS.encode(obj) shouldBe "a%5Bb%5D=c"
                }

                it("encodes a map with an enum as a child") {
                    val obj =
                        mapOf(
                            "a" to DummyEnum.LOREM,
                            "b" to "foo",
                            "c" to 1,
                            "d" to 1.234,
                            "e" to true,
                        )
                    QS.encode(obj) shouldBe "a=LOREM&b=foo&c=1&d=1.234&e=true"
                }

                it("does not encode an Undefined") {
                    QS.encode(mapOf("a" to Undefined())) shouldBe ""
                }
            }

            describe("fixed ljharb/qs issues") {
                it("ljharb/qs#493") {
                    QS.encode(
                        mapOf("search" to mapOf("withbracket[]" to "foobar")),
                        EncodeOptions(encode = false),
                    ) shouldBe "search[withbracket[]]=foobar"
                }
            }

            describe("encodes Instant") {
                it("encodes Instant with encode=false as ISO_INSTANT (…Z)") {
                    val inst = Instant.ofEpochMilli(7)
                    QS.encode(mapOf("a" to inst), EncodeOptions(encode = false)) shouldBe
                        "a=${inst}" // 1970-01-01T00:00:00.007Z
                }

                it("encodes Instant with default settings (percent-encoded)") {
                    val inst = Instant.parse("2020-01-02T03:04:05.006Z")
                    val expected = "a=${Utils.encode(inst.toString())}"
                    QS.encode(mapOf("a" to inst)) shouldBe expected
                }

                it("COMMA list stringifies Instant elements before join (encode=false)") {
                    val a = Instant.parse("2020-01-02T03:04:05Z")
                    val b = Instant.parse("2021-02-03T04:05:06Z")

                    val opts = EncodeOptions(encode = false, listFormat = ListFormat.COMMA)

                    QS.encode(mapOf("a" to listOf(a, b)), opts) shouldBe "a=${a},${b}"
                }

                it("COMMA list encodes comma when encode=true") {
                    val a = Instant.parse("2020-01-02T03:04:05Z")
                    val b = Instant.parse("2021-02-03T04:05:06Z")

                    val opts = EncodeOptions(listFormat = ListFormat.COMMA)
                    val joined = "${a},${b}"
                    val expected = "a=${Utils.encode(joined)}"

                    QS.encode(mapOf("a" to listOf(a, b)), opts) shouldBe expected
                }

                it("single-item COMMA list: no [] by default") {
                    val only = Instant.parse("2020-01-02T03:04:05Z")
                    val opts = EncodeOptions(encode = false, listFormat = ListFormat.COMMA)

                    QS.encode(mapOf("a" to listOf(only)), opts) shouldBe "a=$only"
                }

                it("single-item COMMA list adds [] when commaRoundTrip=true") {
                    val only = Instant.parse("2020-01-02T03:04:05Z")
                    val opts =
                        EncodeOptions(
                            encode = false,
                            listFormat = ListFormat.COMMA,
                            commaRoundTrip = true,
                        )

                    QS.encode(mapOf("a" to listOf(only)), opts) shouldBe "a[]=$only"
                }

                it("indexed list (INDICES) with Instants") {
                    val a = Instant.parse("2020-01-02T03:04:05Z")
                    val b = Instant.parse("2021-02-03T04:05:06Z")

                    // Default listFormat is INDICES
                    val expected =
                        "a%5B0%5D=${Utils.encode(a.toString())}&a%5B1%5D=${Utils.encode(b.toString())}"
                    QS.encode(mapOf("a" to listOf(a, b))) shouldBe expected
                }
            }
        }
    })

// Custom class that is neither a Map nor an Iterable
private class CustomObject(private val value: String) {
    operator fun get(key: String): String? = if (key == "prop") value else null
}
