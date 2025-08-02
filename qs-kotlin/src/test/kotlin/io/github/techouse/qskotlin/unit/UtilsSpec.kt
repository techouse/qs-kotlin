package io.github.techouse.qskotlin.unit

import io.github.techouse.qskotlin.Utils
import io.github.techouse.qskotlin.enums.Format
import io.github.techouse.qskotlin.fixtures.DummyEnum
import io.github.techouse.qskotlin.models.Undefined
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class UtilsSpec :
    FunSpec({
        context("Utils.encode") {
            test("encodes various values correctly") {
                // Basic encoding
                Utils.encode("foo+bar") shouldBe "foo%2Bbar"

                // Exceptions (characters that should not be encoded)
                Utils.encode("foo-bar") shouldBe "foo-bar"
                Utils.encode("foo_bar") shouldBe "foo_bar"
                Utils.encode("foo~bar") shouldBe "foo~bar"
                Utils.encode("foo.bar") shouldBe "foo.bar"

                // Space encoding
                Utils.encode("foo bar") shouldBe "foo%20bar"

                // Parentheses
                Utils.encode("foo(bar)") shouldBe "foo%28bar%29"
                Utils.encode("foo(bar)", format = Format.RFC1738) shouldBe "foo(bar)"

                // Enum encoding
                Utils.encode(DummyEnum.LOREM) shouldBe "LOREM"

                // Values that should not be encoded (return empty string)
                // Iterable
                Utils.encode(listOf(1, 2)) shouldBe ""
                // Map
                Utils.encode(mapOf("a" to "b")) shouldBe ""
                // Undefined
                Utils.encode(Undefined()) shouldBe ""
            }

            test("encode huge string") {
                val hugeString = "a".repeat(1_000_000)
                Utils.encode(hugeString) shouldBe hugeString
            }

            test("encodes utf8") {
                Utils.encode("foo+bar", StandardCharsets.UTF_8) shouldBe "foo%2Bbar"
                // exceptions
                Utils.encode("foo-bar", StandardCharsets.UTF_8) shouldBe "foo-bar"
                Utils.encode("foo_bar", StandardCharsets.UTF_8) shouldBe "foo_bar"
                Utils.encode("foo~bar", StandardCharsets.UTF_8) shouldBe "foo~bar"
                Utils.encode("foo.bar", StandardCharsets.UTF_8) shouldBe "foo.bar"
                // space
                Utils.encode("foo bar", StandardCharsets.UTF_8) shouldBe "foo%20bar"
                // parentheses
                Utils.encode("foo(bar)", StandardCharsets.UTF_8) shouldBe "foo%28bar%29"
                Utils.encode("foo(bar)", StandardCharsets.UTF_8, Format.RFC1738) shouldBe "foo(bar)"
            }

            test("encodes latin1") {
                Utils.encode("foo+bar", StandardCharsets.ISO_8859_1) shouldBe "foo+bar"
                // exceptions
                Utils.encode("foo-bar", StandardCharsets.ISO_8859_1) shouldBe "foo-bar"
                Utils.encode("foo_bar", StandardCharsets.ISO_8859_1) shouldBe "foo_bar"
                Utils.encode("foo~bar", StandardCharsets.ISO_8859_1) shouldBe "foo%7Ebar"
                Utils.encode("foo.bar", StandardCharsets.ISO_8859_1) shouldBe "foo.bar"
                // space
                Utils.encode("foo bar", StandardCharsets.ISO_8859_1) shouldBe "foo%20bar"
                // parentheses
                Utils.encode("foo(bar)", StandardCharsets.ISO_8859_1) shouldBe "foo%28bar%29"
                Utils.encode("foo(bar)", StandardCharsets.ISO_8859_1, Format.RFC1738) shouldBe
                    "foo(bar)"
            }

            test("encodes empty string") { Utils.encode("") shouldBe "" }

            test("encodes parentheses with default format") {
                Utils.encode("(abc)") shouldBe "%28abc%29"
            }

            test("encodes unicode with ISO-8859-1 charset") {
                Utils.encode("abc 123 💩", StandardCharsets.ISO_8859_1) shouldBe
                    "abc%20123%20%26%2355357%3B%26%2356489%3B"
            }

            test("encodes unicode with UTF-8 charset") {
                Utils.encode("abc 123 💩") shouldBe "abc%20123%20%F0%9F%92%A9"
            }

            test("encodes long strings efficiently") {
                val longString = " ".repeat(1500)
                val expectedString = "%20".repeat(1500)
                Utils.encode(longString) shouldBe expectedString
            }

            test("encodes parentheses") {
                Utils.encode("()") shouldBe "%28%29"
                Utils.encode("()", format = Format.RFC1738) shouldBe "()"
            }

            test("encodes multi-byte unicode characters") {
                Utils.encode("Āက豈") shouldBe "%C4%80%E1%80%80%EF%A4%80"
            }

            test("encodes surrogate pairs") {
                Utils.encode("\uD83D\uDCA9") shouldBe "%F0%9F%92%A9"
                Utils.encode("💩") shouldBe "%F0%9F%92%A9"
            }

            test("encodes emoji with ISO-8859-1 charset") {
                Utils.encode("💩", StandardCharsets.ISO_8859_1) shouldBe
                    "%26%2355357%3B%26%2356489%3B"
            }

            test("encodes null values") { Utils.encode(null) shouldBe "" }

            test("encodes byte arrays") { Utils.encode("test".toByteArray()) shouldBe "test" }

            test("returns empty string for unsupported types") {
                Utils.encode(listOf(1, 2, 3)) shouldBe ""
                Utils.encode(mapOf("a" to "b")) shouldBe ""
                Utils.encode(Undefined()) shouldBe ""
            }

            test("handles special characters") {
                Utils.encode("~._-") shouldBe "~._-"
                Utils.encode("!@#\$%^&*()") shouldBe "%21%40%23%24%25%5E%26%2A%28%29"
            }

            test("latin1 encodes characters as numeric entities when not representable") {
                val out = Utils.encode("☺", StandardCharsets.ISO_8859_1, Format.RFC3986)
                out shouldBe "a=%26%239786%3B".removePrefix("a=") // value-only expectation
            }

            test("RFC1738 leaves parentheses unescaped") {
                val out = Utils.encode("()", StandardCharsets.UTF_8, Format.RFC1738)
                out shouldBe "()"
            }

            test("encodes surrogate pairs (emoji) correctly") {
                Utils.encode("😀") shouldBe "%F0%9F%98%80"
            }

            test("encodes ByteArray and ByteBuffer") {
                Utils.encode("ä".toByteArray(StandardCharsets.UTF_8)) shouldBe "%C3%A4"
                Utils.encode(ByteBuffer.wrap("hi".toByteArray())) shouldBe "hi"
            }
        }

        context("Utils.decode") {
            test("decodes URL encoded strings") { Utils.decode("foo%2Bbar") shouldBe "foo+bar" }

            test("handles exceptions (characters that don't need decoding)") {
                Utils.decode("foo-bar") shouldBe "foo-bar"
                Utils.decode("foo_bar") shouldBe "foo_bar"
                Utils.decode("foo~bar") shouldBe "foo~bar"
                Utils.decode("foo.bar") shouldBe "foo.bar"
            }

            test("decodes spaces") { Utils.decode("foo%20bar") shouldBe "foo bar" }

            test("decodes parentheses") { Utils.decode("foo%28bar%29") shouldBe "foo(bar)" }

            test("decodes utf8") {
                Utils.decode("foo%2Bbar", StandardCharsets.UTF_8) shouldBe "foo+bar"
                // exceptions
                Utils.decode("foo-bar", StandardCharsets.UTF_8) shouldBe "foo-bar"
                Utils.decode("foo_bar", StandardCharsets.UTF_8) shouldBe "foo_bar"
                Utils.decode("foo~bar", StandardCharsets.UTF_8) shouldBe "foo~bar"
                Utils.decode("foo.bar", StandardCharsets.UTF_8) shouldBe "foo.bar"
                // space
                Utils.decode("foo%20bar", StandardCharsets.UTF_8) shouldBe "foo bar"
                // parentheses
                Utils.decode("foo%28bar%29", StandardCharsets.UTF_8) shouldBe "foo(bar)"
            }

            test("decode latin1") {
                Utils.decode("foo+bar", StandardCharsets.ISO_8859_1) shouldBe "foo bar"
                // exceptions
                Utils.decode("foo-bar", StandardCharsets.ISO_8859_1) shouldBe "foo-bar"
                Utils.decode("foo_bar", StandardCharsets.ISO_8859_1) shouldBe "foo_bar"
                Utils.decode("foo%7Ebar", StandardCharsets.ISO_8859_1) shouldBe "foo~bar"
                Utils.decode("foo.bar", StandardCharsets.ISO_8859_1) shouldBe "foo.bar"
                // space
                Utils.decode("foo%20bar", StandardCharsets.ISO_8859_1) shouldBe "foo bar"
                // parentheses
                Utils.decode("foo%28bar%29", StandardCharsets.ISO_8859_1) shouldBe "foo(bar)"
            }

            test("decodes URL-encoded strings") {
                Utils.decode("a+b") shouldBe "a b"
                Utils.decode("name%2Eobj") shouldBe "name.obj"
                Utils.decode("name%2Eobj%2Efoo", StandardCharsets.ISO_8859_1) shouldBe
                    "name.obj.foo"
            }
        }

        @Suppress("DEPRECATION")
        context("Utils.escape") {
            test("handles basic alphanumerics (remain unchanged)") {
                Utils.escape(
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@*_+-./"
                ) shouldBe "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@*_+-./"

                Utils.escape("abc123") shouldBe "abc123"
            }

            test("handles accented characters (Latin-1 range uses %XX)") {
                Utils.escape("äöü") shouldBe "%E4%F6%FC"
            }

            test("handles non-ASCII that falls outside Latin-1 uses %uXXXX") {
                Utils.escape("ć") shouldBe "%u0107"
            }

            test("handles characters that are defined as safe") {
                Utils.escape("@*_+-./") shouldBe "@*_+-./"
            }

            test("handles parentheses (in RFC3986 they are encoded)") {
                Utils.escape("(") shouldBe "%28"
                Utils.escape(")") shouldBe "%29"
            }

            test("handles space character") { Utils.escape(" ") shouldBe "%20" }

            test("handles tilde as safe") { Utils.escape("~") shouldBe "%7E" }

            test("handles unsafe punctuation") {
                Utils.escape("!") shouldBe "%21"
                Utils.escape(",") shouldBe "%2C"
            }

            test("handles mixed safe and unsafe characters") {
                Utils.escape("hello world!") shouldBe "hello%20world%21"
            }

            test("handles multiple spaces") { Utils.escape("a b c") shouldBe "a%20b%20c" }

            test("handles string with various punctuation") {
                Utils.escape("Hello, World!") shouldBe "Hello%2C%20World%21"
            }

            test("handles null character") { Utils.escape("\u0000") shouldBe "%00" }

            test("handles emoji") { Utils.escape("😀") shouldBe "%uD83D%uDE00" }

            test("handles RFC1738 format where parentheses are safe") {
                Utils.escape("(", Format.RFC1738) shouldBe "("
                Utils.escape(")", Format.RFC1738) shouldBe ")"
            }

            test("handles mixed test with RFC1738") {
                Utils.escape("(hello)!", Format.RFC1738) shouldBe "(hello)%21"
            }

            test("escape huge string") {
                val hugeString = "äöü".repeat(1000000)
                Utils.escape(hugeString) shouldBe "%E4%F6%FC".repeat(1000000)
            }
        }

        @Suppress("DEPRECATION")
        context("Utils.unescape") {
            test("No escapes.") { Utils.unescape("abc123") shouldBe "abc123" }

            test("Hex escapes with uppercase hex digits.") {
                Utils.unescape("%E4%F6%FC") shouldBe "äöü"
            }

            test("Hex escapes with lowercase hex digits.") {
                Utils.unescape("%e4%f6%fc") shouldBe "äöü"
            }

            test("Unicode escape.") { Utils.unescape("%u0107") shouldBe "ć" }

            test("Unicode escape with lowercase digits.") { Utils.unescape("%u0061") shouldBe "a" }

            test("Characters that do not need escaping.") {
                Utils.unescape("@*_+-./") shouldBe "@*_+-./"
            }

            test("Hex escapes for punctuation.") {
                Utils.unescape("%28") shouldBe "("
                Utils.unescape("%29") shouldBe ")"
                Utils.unescape("%20") shouldBe " "
                Utils.unescape("%7E") shouldBe "~"
            }

            test("A long string with only safe characters.") {
                Utils.unescape(
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@*_+-./"
                ) shouldBe "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@*_+-./"
            }

            test("A mix of Unicode and hex escapes.") {
                Utils.unescape("%u0041%20%42") shouldBe "A B"
            }

            test("A mix of literal text and hex escapes.") {
                Utils.unescape("hello%20world") shouldBe "hello world"
            }

            test(
                "A literal percent sign that is not followed by a valid escape remains unchanged."
            ) {
                Utils.unescape("100% sure") shouldBe "100% sure"
            }

            test("Mixed Unicode and hex escapes.") { Utils.unescape("%u0041%65") shouldBe "Ae" }

            test("Escaped percent signs that do not form a valid escape remain unchanged.") {
                Utils.unescape("50%% off") shouldBe "50%% off"
            }

            test("Consecutive escapes producing multiple spaces.") {
                Utils.unescape("%20%u0020") shouldBe "  "
            }

            test("An invalid escape sequence should remain unchanged.") {
                Utils.unescape("abc%g") shouldBe "abc%g"
            }

            test("An invalid Unicode escape sequence should remain unchanged.") {
                // The input "%uZZZZ" is 6 characters long so it passes the length check.
                // However, "ZZZZ" is not valid hex so toInt will throw a NumberFormatException.
                // In that case, the catch block writes the literal '%' and increments i by 1.
                // The remainder of the string is then processed normally.
                // For input "%uZZZZ", the processing is:
                // - At i = 0, encounter '%', then since i+1 is 'u' and there are 6 characters, try
                // block is entered.
                // - "ZZZZ".toInt(16) fails, so the catch writes '%' and i becomes 1.
                // - Then the rest of the string ("uZZZZ") is appended as literal.
                // The expected result is "%uZZZZ".
                Utils.unescape("%uZZZZ") shouldBe "%uZZZZ"

                // Input "%u12" has only 4 characters.
                // For a valid %u escape we need 6 characters.
                // Thus, the branch "Not enough characters for a valid %u escape" is triggered,
                // which writes the literal '%' and increments i.
                // The remainder of the string ("u12") is then appended as literal.
                // Expected output is "%u12".
                Utils.unescape("%u12") shouldBe "%u12"

                // When "%" is the last character of the string (with no following characters),
                // the code writes it as literal.
                // For example, "abc%" should remain "abc%".
                Utils.unescape("abc%") shouldBe "abc%"
            }

            test("huge string") {
                val hugeString = "%E4%F6%FC".repeat(1000000)
                Utils.unescape(hugeString) shouldBe "äöü".repeat(1000000)
            }

            test("leaves trailing '%' literal when incomplete escape") {
                Utils.unescape("%") shouldBe "%"
            }
            test("leaves incomplete %uXXXX literal") { Utils.unescape("%u12") shouldBe "%u12" }
            test("handles bad hex after %") { Utils.unescape("%GZ") shouldBe "%GZ" }
        }

        context("Utils.merge") {
            test("merges SplayTreeMap with List") {
                Utils.merge(mapOf(0 to "a"), listOf(Undefined(), "b")) shouldBe
                    mapOf(0 to "a", 1 to "b")
            }

            test("merges two objects with the same key and different values") {
                Utils.merge(
                    mapOf("foo" to listOf(mapOf("a" to "a", "b" to "b"), mapOf("a" to "aa"))),
                    mapOf("foo" to listOf(Undefined(), mapOf("b" to "bb"))),
                ) shouldBe
                    mapOf(
                        "foo" to
                            listOf(mapOf("a" to "a", "b" to "b"), mapOf("a" to "aa", "b" to "bb"))
                    )
            }

            test("merges two objects with the same key and different list values") {
                Utils.merge(
                    mapOf("foo" to listOf(mapOf("baz" to listOf("15")))),
                    mapOf("foo" to listOf(mapOf("baz" to listOf(Undefined(), "16")))),
                ) shouldBe mapOf("foo" to listOf(mapOf("baz" to listOf("15", "16"))))
            }

            test("merges two objects with the same key and different values into a list") {
                Utils.merge(
                    mapOf("foo" to listOf(mapOf("a" to "b"))),
                    mapOf("foo" to listOf(mapOf("c" to "d"))),
                ) shouldBe mapOf("foo" to listOf(mapOf("a" to "b", "c" to "d")))
            }

            test("merges true into null") { Utils.merge(null, true) shouldBe listOf(null, true) }

            test("merges null into a list") {
                val result = Utils.merge(null, listOf(42))
                result shouldBe listOf(null, 42)
                result.shouldBeInstanceOf<List<Int?>>()
            }

            test("merges null into a set") {
                val result = Utils.merge(null, setOf("foo"))
                result shouldBe listOf(null, "foo")
                result.shouldBeInstanceOf<List<String?>>()
            }

            test("merges String into set") {
                val result = Utils.merge(setOf("foo"), "bar")
                result shouldBe setOf("foo", "bar")
                result.shouldBeInstanceOf<Set<String>>()
            }

            test("merges two objects with the same key") {
                val result = Utils.merge(mapOf("a" to "b"), mapOf("a" to "c"))
                result shouldBe mapOf("a" to listOf("b", "c"))

                @Suppress("UNCHECKED_CAST") val map = result as Map<String, Any>
                map.shouldContainKey("a")
                map["a"].shouldBeInstanceOf<List<String>>()
            }

            test("merges a standalone and an object into a list") {
                Utils.merge(mapOf("foo" to "bar"), mapOf("foo" to mapOf("first" to "123"))) shouldBe
                    mapOf("foo" to listOf("bar", mapOf("first" to "123")))
            }

            test("merges a standalone and two objects into a list") {
                Utils.merge(
                    mapOf("foo" to listOf("bar", mapOf("first" to "123"))),
                    mapOf("foo" to mapOf("second" to "456")),
                ) shouldBe
                    mapOf(
                        "foo" to mapOf(0 to "bar", 1 to mapOf("first" to "123"), "second" to "456")
                    )
            }

            test("merges an object sandwiched by two standalones into a list") {
                Utils.merge(
                    mapOf("foo" to listOf("bar", mapOf("first" to "123", "second" to "456"))),
                    mapOf("foo" to "baz"),
                ) shouldBe
                    mapOf("foo" to listOf("bar", mapOf("first" to "123", "second" to "456"), "baz"))
            }

            test("merges two lists into a list") {
                val result1 = Utils.merge(listOf("foo"), listOf("bar", "xyzzy"))
                result1 shouldBe listOf("foo", "bar", "xyzzy")
                result1.shouldBeInstanceOf<List<String>>()

                val result2 =
                    Utils.merge(
                        mapOf("foo" to listOf("baz")),
                        mapOf("foo" to listOf("bar", "xyzzy")),
                    )
                result2 shouldBe mapOf("foo" to listOf("baz", "bar", "xyzzy"))

                @Suppress("UNCHECKED_CAST") val map = result2 as Map<String, Any>
                map.shouldContainKey("foo")
                map["foo"].shouldBeInstanceOf<List<String>>()
            }

            test("merges two sets into a list") {
                val result1 = Utils.merge(setOf("foo"), setOf("bar", "xyzzy"))
                result1 shouldBe setOf("foo", "bar", "xyzzy")
                result1.shouldBeInstanceOf<Set<String>>()

                val result2 =
                    Utils.merge(mapOf("foo" to setOf("baz")), mapOf("foo" to setOf("bar", "xyzzy")))
                result2 shouldBe mapOf("foo" to setOf("baz", "bar", "xyzzy"))

                @Suppress("UNCHECKED_CAST") val map = result2 as Map<String, Any>
                map.shouldContainKey("foo")
                map["foo"].shouldBeInstanceOf<Set<String>>()
            }

            test("merges a set into a list") {
                val result =
                    Utils.merge(mapOf("foo" to listOf("baz")), mapOf("foo" to setOf("bar")))
                result shouldBe mapOf("foo" to listOf("baz", "bar"))

                @Suppress("UNCHECKED_CAST") val map = result as Map<String, Any>
                map.shouldContainKey("foo")
                map["foo"].shouldBeInstanceOf<List<String>>()
            }

            test("merges a list into a set") {
                val result =
                    Utils.merge(mapOf("foo" to setOf("baz")), mapOf("foo" to listOf("bar")))
                result shouldBe mapOf("foo" to setOf("baz", "bar"))

                @Suppress("UNCHECKED_CAST") val map = result as Map<String, Any>
                map.shouldContainKey("foo")
                map["foo"].shouldBeInstanceOf<Set<String>>()
            }

            test("merges a set into a list with multiple elements") {
                val result =
                    Utils.merge(
                        mapOf("foo" to listOf("baz")),
                        mapOf("foo" to setOf("bar", "xyzzy")),
                    )
                result shouldBe mapOf("foo" to listOf("baz", "bar", "xyzzy"))

                @Suppress("UNCHECKED_CAST") val map = result as Map<String, Any>
                map.shouldContainKey("foo")
                map["foo"].shouldBeInstanceOf<List<String>>()
            }

            test("merges an object into a list") {
                val result =
                    Utils.merge(
                        mapOf("foo" to listOf("bar")),
                        mapOf("foo" to mapOf("baz" to "xyzzy")),
                    )
                result shouldBe mapOf("foo" to mapOf(0 to "bar", "baz" to "xyzzy"))

                @Suppress("UNCHECKED_CAST") val map = result as Map<String, Any>
                map.shouldContainKey("foo")
                map["foo"].shouldBeInstanceOf<Map<String, String>>()
            }

            test("merges a list into an object") {
                val result =
                    Utils.merge(
                        mapOf("foo" to mapOf("bar" to "baz")),
                        mapOf("foo" to listOf("xyzzy")),
                    )
                result shouldBe mapOf("foo" to mapOf("bar" to "baz", 0 to "xyzzy"))

                @Suppress("UNCHECKED_CAST") val map = result as Map<String, Any>
                map.shouldContainKey("foo")
                map["foo"].shouldBeInstanceOf<Map<String, String>>()
            }

            test("merge set with undefined with another set") {
                val undefined = Undefined()

                val result1 =
                    Utils.merge(
                        mapOf("foo" to setOf("bar")),
                        mapOf("foo" to setOf(undefined, "baz")),
                    )
                result1 shouldBe mapOf("foo" to setOf("bar", "baz"))

                @Suppress("UNCHECKED_CAST") val map1 = result1 as Map<String, Any>
                map1.shouldContainKey("foo")
                map1["foo"].shouldBeInstanceOf<Set<String>>()

                val result2 =
                    Utils.merge(
                        mapOf("foo" to setOf(undefined, "bar")),
                        mapOf("foo" to setOf("baz")),
                    )
                result2 shouldBe mapOf("foo" to setOf("bar", "baz"))

                @Suppress("UNCHECKED_CAST") val map2 = result2 as Map<String, Any>
                map2.shouldContainKey("foo")
                map2["foo"].shouldBeInstanceOf<Set<String>>()
            }

            test("merge set of Maps with another set of Maps") {
                val result1 =
                    Utils.merge(setOf(mapOf("bar" to "baz")), setOf(mapOf("baz" to "xyzzy")))
                result1 shouldBe setOf(mapOf("bar" to "baz", "baz" to "xyzzy"))
                result1.shouldBeInstanceOf<Set<Map<String, String>>>()

                val result2 =
                    Utils.merge(
                        mapOf("foo" to setOf(mapOf("bar" to "baz"))),
                        mapOf("foo" to setOf(mapOf("baz" to "xyzzy"))),
                    )
                result2 shouldBe mapOf("foo" to setOf(mapOf("bar" to "baz", "baz" to "xyzzy")))

                @Suppress("UNCHECKED_CAST") val map = result2 as Map<String, Any>
                map.shouldContainKey("foo")
                map["foo"].shouldBeInstanceOf<Set<Map<String, String>>>()
            }
        }

        context("Utils.combine") {
            test("both lists") {
                val a = listOf(1)
                val b = listOf(2)
                val combined = Utils.combine<Int>(a, b)

                a shouldBe listOf(1)
                b shouldBe listOf(2)
                a shouldNotBeSameInstanceAs combined
                b shouldNotBeSameInstanceAs combined
                combined shouldBe listOf(1, 2)
            }

            test("one list, one non-list") {
                val aN = 1
                val a = listOf(aN)
                val bN = 2
                val b = listOf(bN)

                val combinedAnB = Utils.combine<Int>(aN, b)
                b shouldBe listOf(bN)
                combinedAnB shouldBe listOf(1, 2)

                val combinedABn = Utils.combine<Int>(a, bN)
                a shouldBe listOf(aN)
                combinedABn shouldBe listOf(1, 2)
            }

            test("neither is a list") {
                val a = 1
                val b = 2
                val combined = Utils.combine<Int>(a, b)

                combined shouldBe listOf(1, 2)
            }

            test("combine list and scalar preserves order") {
                Utils.combine<String>(listOf("a"), "b") shouldBe listOf("a", "b")
                Utils.combine<Int>(1, listOf(2, 3)) shouldBe listOf(1, 2, 3)
            }
        }

        context("Utils.interpretNumericEntities") {
            test("returns input unchanged when there are no entities") {
                Utils.interpretNumericEntities("hello world") shouldBe "hello world"
                Utils.interpretNumericEntities("100% sure") shouldBe "100% sure"
            }

            test("decodes a single decimal entity") {
                Utils.interpretNumericEntities("A = &#65;") shouldBe "A = A"
                Utils.interpretNumericEntities("&#48;&#49;&#50;") shouldBe "012"
            }

            test("decodes multiple entities in a sentence") {
                val input = "Hello &#87;&#111;&#114;&#108;&#100;!"
                val expected = "Hello World!"
                Utils.interpretNumericEntities(input) shouldBe expected
            }

            test("decodes surrogate pair represented as two decimal entities (emoji)") {
                // U+1F4A9 (💩) as surrogate halves: 55357 (0xD83D), 56489 (0xDCA9)
                Utils.interpretNumericEntities("&#55357;&#56489;") shouldBe "💩"
            }

            test("entities can appear at string boundaries") {
                Utils.interpretNumericEntities("&#65;BC") shouldBe "ABC"
                Utils.interpretNumericEntities("ABC&#33;") shouldBe "ABC!"
                Utils.interpretNumericEntities("&#65;") shouldBe "A"
            }

            test("mixes literals and entities") {
                // '=' is 61
                Utils.interpretNumericEntities("x&#61;y") shouldBe "x=y"
                Utils.interpretNumericEntities("x=&#61;y") shouldBe "x==y"
            }

            test("malformed or unsupported patterns remain unchanged") {
                // No digits
                Utils.interpretNumericEntities("&#;") shouldBe "&#;"
                // Missing terminating semicolon
                Utils.interpretNumericEntities("&#12") shouldBe "&#12"
                // Hex form not supported by this decoder
                Utils.interpretNumericEntities("&#x41;") shouldBe "&#x41;"
                // Space inside
                Utils.interpretNumericEntities("&# 12;") shouldBe "&# 12;"
                // Negative / non-digit after '#'
                Utils.interpretNumericEntities("&#-12;") shouldBe "&#-12;"
                // Mixed garbage
                Utils.interpretNumericEntities("&#+;") shouldBe "&#+;"
            }

            test("out-of-range code points remain unchanged") {
                // Max valid is 0x10FFFF (1114111). One above should be left as literal.
                Utils.interpretNumericEntities("&#1114112;") shouldBe "&#1114112;"
            }
        }

        context("Utils.apply") {
            test("apply on scalar and list") {
                Utils.apply<Int>(3) { it * 2 } shouldBe 6
                Utils.apply<Int>(listOf(1, 2)) { it + 1 } shouldBe listOf(2, 3)
            }
        }

        context("Utils.isNonNullishPrimitive and isEmpty") {
            test("treats URI as primitive, honors skipNulls for empty string") {
                Utils.isNonNullishPrimitive(URI("https://example.com")) shouldBe true
                Utils.isNonNullishPrimitive("", skipNulls = true) shouldBe false
            }
        }

        context("Utils.isEmpty") {
            test("empty collections and maps") {
                Utils.isEmpty(emptyMap<String, Any?>()) shouldBe true
            }
        }
    })
