package io.github.techouse.qskotlin.interop;

import io.github.techouse.qskotlin.QS;
import io.github.techouse.qskotlin.enums.Duplicates;
import io.github.techouse.qskotlin.enums.Format;
import io.github.techouse.qskotlin.enums.ListFormat;
import io.github.techouse.qskotlin.models.*;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ExampleInteropTest {

    // ===== Decoding: Maps =====

    @Test
    void decodesSimpleQueryString() {
        Map<String, Object> out = QS.decode("a=c");
        assertEquals(Map.of("a", "c"), out);
    }

    @Test
    void encodesSimpleMap() {
        assertEquals("a=c", QS.encode(Map.of("a", "c")));
    }

    @Test
    void decodeIgnoreQueryPrefix() {
        Map<String, Object> out = QS.decode("?a=b&c=d", new DecodeOptions(null, null, null, null, false, false, 20, StandardCharsets.UTF_8, false, false, new StringDelimiter("&"), 5, 1000, Duplicates.COMBINE, true, false, true, false, false, false));
        assertEquals(Map.of("a", "b", "c", "d"), out);
    }

    @Test
    void decodeCustomDelimiter() {
        // String delimiter
        Map<String, Object> out1 = QS.decode("a=b;c=d",
                // full-args ctor is verbose from Java; this shows the delimiter objects compile & are usable.
                new DecodeOptions(null, null, null, null, false, false, 20, StandardCharsets.UTF_8, false, false, new StringDelimiter(";"), 5, 1000, Duplicates.COMBINE, false, false, true, false, false, false));
        assertEquals(Map.of("a", "b", "c", "d"), out1);

        // Regex delimiter
        Map<String, Object> out2 = QS.decode("a=b;c=d", new DecodeOptions(null, null, null, null, false, false, 20, StandardCharsets.UTF_8, false, false, new RegexDelimiter("[;,]"), 5, 1000, Duplicates.COMBINE, false, false, true, false, false, false));
        assertEquals(Map.of("a", "b", "c", "d"), out2);
    }

    @Test
    void decodeNestedBrackets() {
        // allows nested maps via brackets
        Map<String, Object> out = QS.decode("foo[bar]=baz");
        assertEquals(Map.of("foo", Map.of("bar", "baz")), out);
    }

    @Test
    void decodeListBrackets() {
        // lists via [] notation
        Map<String, Object> out = QS.decode("a[]=b&a[]=c");
        assertEquals(List.of("b", "c"), out.get("a"));
    }

    // ===== Encoding =====

    @Test
    void encodeAddQueryPrefixAndListFormat() {
        // Add query prefix (uses the Java-friendly ctor)
        // Use LinkedHashMap to guarantee insertion order for deterministic output
        Map<String, Object> ordered = new LinkedHashMap<>();
        ordered.put("a", "b");
        ordered.put("c", "d");
        assertEquals("?a=b&c=d", QS.encode(ordered, new EncodeOptions(null, null, ListFormat.INDICES, null, null, true, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), true, false, true, Format.RFC3986, null, false, false, null, null)));

        // List format brackets (will be percent-encoded by default)
        assertEquals("a%5B%5D=b&a%5B%5D=c", QS.encode(Map.of("a", List.of("b", "c")), new EncodeOptions(null, null, ListFormat.BRACKETS, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), true, false, false, // encode keys too → brackets get percent-encoded
                Format.RFC3986, null, false, false, null, null)));
    }

    @Test
    void encodeWithCustomValueEncoder() {
        // Replace "č" with "c" while encoding AND encode keys (encodeValuesOnly=false)
        kotlin.jvm.functions.Function3<Object, Charset, Format, String> enc = (value, cs, fmt) -> {
            String s = Objects.toString(value, "");
            if (Objects.equals(s, "č")) s = "c"; // custom transform
            try {
                Charset charset = (cs != null) ? cs : StandardCharsets.UTF_8;
                return URLEncoder.encode(s, charset); // ensure keys/values are percent-encoded
            } catch (Exception e) {
                return s; // fallback
            }
        };

        EncodeOptions opts = new EncodeOptions(enc, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), true, false, false, // encode keys too
                Format.RFC3986, null, false, false, null, null);

        assertEquals("a%5Bb%5D=c", QS.encode(Map.of("a", Map.of("b", "č")), opts));
    }

    @Test
    void encodeWithCustomDateSerializer() {
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(7), ZoneOffset.UTC);
        EncodeOptions opts = EncodeOptions.withDateSerializer(dt -> Long.toString(dt.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()));
        assertEquals("a=7", QS.encode(Map.of("a", date), opts));
    }

    @Test
    void encodeWithSorter() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("a", "c");
        input.put("z", "y");
        input.put("b", "f");

        EncodeOptions opts = EncodeOptions.withSorter(Comparator.comparing(o -> o == null ? "" : o.toString()));
        assertEquals("a=c&b=f&z=y", QS.encode(input, opts));
    }

    @Test
    void encodeDisableEncodingShowsRawBrackets() {
        assertEquals("a[b]=c", QS.encode(Map.of("a", Map.of("b", "c")), new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), false, // encode=false
                false, false, Format.RFC3986, null, false, false, null, null)));
    }

    @Test
    void encodeValuesOnly() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("a", "b");
        m.put("c", List.of("d", "e=f"));
        m.put("f", List.of(List.of("g"), List.of("h")));
        EncodeOptions opts = new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), true, false, true, // encodeValuesOnly
                Format.RFC3986, null, false, false, null, null);
        assertEquals("a=b&c[0]=d&c[1]=e%3Df&f[0][0]=g&f[1][0]=h", QS.encode(m, opts));
    }

    @Test
    void encodeListIndicesDefaultAndNoIndices() {
        // indices default (encode=false to see brackets)
        assertEquals("a[0]=b&a[1]=c&a[2]=d", QS.encode(Map.of("a", List.of("b", "c", "d")), new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), false, // encode=false
                false, false, Format.RFC3986, null, false, false, null, null)));
        // no indices
        assertEquals("a=b&a=c&a=d", QS.encode(Map.of("a", List.of("b", "c", "d")), new EncodeOptions(null, null, ListFormat.REPEAT, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), false, // encode=false
                false, false, Format.RFC3986, null, false, false, null, null)));
    }

    @Test
    void encodeListFormatsAll() {
        // encode=false to avoid percent-encoding
        assertEquals("a[0]=b&a[1]=c", QS.encode(Map.of("a", List.of("b", "c")), new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), false, false, false, Format.RFC3986, null, false, false, null, null)));
        assertEquals("a[]=b&a[]=c", QS.encode(Map.of("a", List.of("b", "c")), new EncodeOptions(null, null, ListFormat.BRACKETS, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), false, false, false, Format.RFC3986, null, false, false, null, null)));
        assertEquals("a=b&a=c", QS.encode(Map.of("a", List.of("b", "c")), new EncodeOptions(null, null, ListFormat.REPEAT, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), false, false, false, Format.RFC3986, null, false, false, null, null)));
        assertEquals("a=b,c", QS.encode(Map.of("a", List.of("b", "c")), new EncodeOptions(null, null, ListFormat.COMMA, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), false, false, false, Format.RFC3986, null, false, false, null, null)));
    }

    @Test
    void encodeNestedMapsBracketAndDotNotation() {
        // Use LinkedHashMap at all levels to guarantee insertion order (c then e)
        LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
        inner.put("c", "d");
        inner.put("e", "f");
        LinkedHashMap<String, Object> mid = new LinkedHashMap<>();
        mid.put("b", inner);
        LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
        nested.put("a", mid);
        assertEquals("a[b][c]=d&a[b][e]=f", QS.encode(nested, new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), false, false, false, Format.RFC3986, null, false, false, null, null)));
        // dot notation
        assertEquals("a.b.c=d&a.b.e=f", QS.encode(nested, new EncodeOptions(null, null, ListFormat.INDICES, null, true, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), false, false, false, Format.RFC3986, null, false, false, null, null)));
    }

    @Test
    void encodeDotInKeys() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("first", "John");
        inner.put("last", "Doe");
        Map<String, Object> outer = new LinkedHashMap<>();
        outer.put("name.obj", inner);
        assertEquals("name%252Eobj.first=John&name%252Eobj.last=Doe", QS.encode(outer, new EncodeOptions(null, null, ListFormat.INDICES, null, true, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), true, true, false, Format.RFC3986, null, false, false, null, null)));
    }

    @Test
    void encodeAllowEmptyLists() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("foo", List.of());
        m.put("bar", "baz");
        assertEquals("foo[]&bar=baz", QS.encode(m, new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, true, StandardCharsets.UTF_8, false, new StringDelimiter("&"), false, false, false, Format.RFC3986, null, false, false, null, null)));
    }

    @Test
    void encodeEmptyCollectionsAndUndefined() {
        assertEquals("", QS.encode(Map.of("a", List.of())));
        assertEquals("", QS.encode(Map.of("a", Map.of())));
        assertEquals("", QS.encode(Map.of("a", List.of(Map.of()))));
        assertEquals("", QS.encode(Map.of("a", Map.of("b", List.of()))));
        assertEquals("", QS.encode(Map.of("a", Map.of("b", Map.of()))));
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("a", null);
        m.put("b", Undefined.Companion.invoke());
        assertEquals("a=", QS.encode(m));
    }

    @Test
    void encodeDelimiterOverride() {
        Map<String, Object> ordered = new LinkedHashMap<>();
        ordered.put("a", "b");
        ordered.put("c", "d");
        assertEquals("a=b;c=d", QS.encode(ordered, new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter(";"), true, false, false, Format.RFC3986, null, false, false, null, null)));
    }

    @Test
    void encodeDefaultDateSerialization() {
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(7), ZoneOffset.UTC);
        assertEquals("a=1970-01-01T00:00:00.007", QS.encode(Map.of("a", date), new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), false, false, false, Format.RFC3986, null, false, false, null, null)));
    }

    @Test
    void encodeFunctionAndIterableFilters() {
        // Function filter
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("a", "b");
        input.put("c", "d");
        Map<String, Object> innerE = new LinkedHashMap<>();
        innerE.put("f", Instant.ofEpochMilli(123));
        innerE.put("g", List.of(2));
        input.put("e", innerE);
        FunctionFilter fn = FunctionFilter.from((k, v) -> switch (k) {
            case "b" -> Undefined.Companion.invoke();
            case "e[f]" -> {
                assertNotNull(v);
                yield ((Instant) v).toEpochMilli();
            }
            case "e[g][0]" -> {
                assertNotNull(v);
                yield ((Number) v).intValue() * 2;
            }
            default -> v;
        });
        EncodeOptions optsFn = new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), false, false, false, Format.RFC3986, fn, false, false, null, null);
        assertEquals("a=b&c=d&e[f]=123&e[g][0]=4", QS.encode(input, optsFn));

        // Iterable filter
        EncodeOptions optsIter1 = new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), false, false, false, Format.RFC3986, new IterableFilter(List.of("a", "e")), false, false, null, null);
        assertEquals("a=b&e=f", QS.encode(Map.of("a", "b", "c", "d", "e", "f"), optsIter1));

        EncodeOptions optsIter2 = new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), false, false, false, Format.RFC3986, new IterableFilter(List.of("a", 0, 2)), false, false, null, null);
        assertEquals("a[0]=b&a[2]=d", QS.encode(Map.of("a", List.of("b", "c", "d"), "e", "f"), optsIter2));
    }

    // ===== Decoding: additional =====

    @Test
    void decodeNestedDepthDefaultAndOverride() {
        // default depth keeps trailing as one segment
        Map<String, Object> out = QS.decode("a[b][c][d][e][f][g][h][i]=j");
        assertEquals(Map.of("a", Map.of("b", Map.of("c", Map.of("d", Map.of("e", Map.of("f", Map.of("[g][h][i]", "j"))))))), out);
        // override depth=1
        Map<String, Object> out2 = QS.decode("a[b][c][d][e][f][g][h][i]=j", new DecodeOptions(null, null, null, null, false, false, 20, StandardCharsets.UTF_8, false, false, new StringDelimiter("&"), 1, 1000, Duplicates.COMBINE, false, false, true, false, false, false));
        assertEquals(Map.of("a", Map.of("b", Map.of("[c][d][e][f][g][h][i]", "j"))), out2);
    }

    @Test
    void decodeParameterLimit() {
        Map<String, Object> out = QS.decode("a=b&c=d", new DecodeOptions(null, null, null, null, false, false, 20, StandardCharsets.UTF_8, false, false, new StringDelimiter("&"), 5, 1, Duplicates.COMBINE, false, false, true, false, false, false));
        assertEquals(Map.of("a", "b"), out);
    }

    @Test
    void decodeAllowDotsAndDecodeDotInKeys() {
        assertEquals(Map.of("a", Map.of("b", "c")), QS.decode("a.b=c", new DecodeOptions(true, null, null, null, false, false, 20, StandardCharsets.UTF_8, false, false, new StringDelimiter("&"), 5, 1000, Duplicates.COMBINE, false, false, true, false, false, false)));
        assertEquals(Map.of("name.obj", Map.of("first", "John", "last", "Doe")), QS.decode("name%252Eobj.first=John&name%252Eobj.last=Doe", new DecodeOptions(null, null, null, true, false, false, 20, StandardCharsets.UTF_8, false, false, new StringDelimiter("&"), 5, 1000, Duplicates.COMBINE, false, false, true, false, false, false)));
    }

    @Test
    void decodeAllowEmptyListsAndDuplicates() {
        assertEquals(Map.of("foo", List.of(), "bar", "baz"), QS.decode("foo[]&bar=baz", new DecodeOptions(null, null, null, null, true, false, 20, StandardCharsets.UTF_8, false, false, new StringDelimiter("&"), 5, 1000, Duplicates.COMBINE, false, false, true, false, false, false)));
        assertEquals(Map.of("foo", List.of("bar", "baz")), QS.decode("foo=bar&foo=baz"));
        assertEquals(Map.of("foo", "bar"), QS.decode("foo=bar&foo=baz", new DecodeOptions(null, null, null, null, false, false, 20, StandardCharsets.UTF_8, false, false, new StringDelimiter("&"), 5, 1000, Duplicates.FIRST, false, false, true, false, false, false)));
        assertEquals(Map.of("foo", "baz"), QS.decode("foo=bar&foo=baz", new DecodeOptions(null, null, null, null, false, false, 20, StandardCharsets.UTF_8, false, false, new StringDelimiter("&"), 5, 1000, Duplicates.LAST, false, false, true, false, false, false)));
    }

    @Test
    void decodeCharsetsAndNumericEntities() {
        assertEquals(Map.of("a", "§"), QS.decode("a=%A7", new DecodeOptions(null, null, null, null, false, false, 20, StandardCharsets.ISO_8859_1, false, false, new StringDelimiter("&"), 5, 1000, Duplicates.COMBINE, false, false, true, false, false, false)));
        assertEquals(Map.of("a", "ø"), QS.decode("utf8=%E2%9C%93&a=%C3%B8", new DecodeOptions(null, null, null, null, false, false, 20, StandardCharsets.ISO_8859_1, true, false, new StringDelimiter("&"), 5, 1000, Duplicates.COMBINE, false, false, true, false, false, false)));
        assertEquals(Map.of("a", "ø"), QS.decode("utf8=%26%2310003%3B&a=%F8", new DecodeOptions(null, null, null, null, false, false, 20, StandardCharsets.UTF_8, true, false, new StringDelimiter("&"), 5, 1000, Duplicates.COMBINE, false, false, true, false, false, false)));
        assertEquals(Map.of("a", "☺"), QS.decode("a=%26%239786%3B", new DecodeOptions(null, null, null, null, false, false, 20, StandardCharsets.ISO_8859_1, false, false, new StringDelimiter("&"), 5, 1000, Duplicates.COMBINE, false, true, true, false, false, false)));
    }

    @Test
    void decodeListsBehavior() {
        assertEquals(Map.of("a", List.of("b", "c")), QS.decode("a[]=b&a[]=c"));
        assertEquals(Map.of("a", List.of("b", "c")), QS.decode("a[1]=c&a[0]=b"));
        assertEquals(Map.of("a", List.of("b", "c")), QS.decode("a[1]=b&a[15]=c"));
        assertEquals(Map.of("a", List.of("", "b")), QS.decode("a[]=&a[]=b"));
        assertEquals(Map.of("a", List.of("b", "", "c")), QS.decode("a[0]=b&a[1]=&a[2]=c"));
        assertEquals(Map.of("a", Map.of("100", "b")), QS.decode("a[100]=b"));
        assertEquals(Map.of("a", Map.of("1", "b")), QS.decode("a[1]=b", new DecodeOptions(null, null, null, null, false, false, 0, StandardCharsets.UTF_8, false, false, new StringDelimiter("&"), 5, 1000, Duplicates.COMBINE, false, false, true, false, false, false)));
        assertEquals(Map.of("a", Map.of("0", "b")), QS.decode("a[]=b", new DecodeOptions(null, null, null, null, false, false, 20, StandardCharsets.UTF_8, false, false, new StringDelimiter("&"), 5, 1000, Duplicates.COMBINE, false, false, false, false, false, false)));
        assertEquals(Map.of("a", Map.of("0", "b", "b", "c")), QS.decode("a[0]=b&a[b]=c"));
        assertEquals(Map.of("a", List.of(Map.of("b", "c"))), QS.decode("a[][b]=c"));
        assertEquals(Map.of("a", List.of("b", "c")), QS.decode("a=b,c", new DecodeOptions(null, null, null, null, false, false, 20, StandardCharsets.UTF_8, false, true, new StringDelimiter("&"), 5, 1000, Duplicates.COMBINE, false, false, true, false, false, false)));
    }

    @Test
    void decodePrimitivesAsStrings() {
        assertEquals(Map.of("a", "15", "b", "true", "c", "null"), QS.decode("a=15&b=true&c=null"));
    }

    // ===== Null handling =====

    @Test
    void nullsDefaultStrictAndSkip() {
        // default encode null like empty string
        assertEquals("a=&b=", QS.encode(new LinkedHashMap<String, Object>() {{
            put("a", null);
            put("b", "");
        }}));
        // decode without '=' yields empty strings
        assertEquals(Map.of("a", "", "b", ""), QS.decode("a&b="));
        // strictNullHandling encode
        LinkedHashMap<String, Object> m1 = new LinkedHashMap<>();
        m1.put("a", null);
        m1.put("b", "");
        assertEquals("a&b=", QS.encode(m1, new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), true, false, false, Format.RFC3986, null, false, true, null, null)));
        // strictNullHandling decode
        LinkedHashMap<String, Object> expectedNullMap = new LinkedHashMap<>();
        expectedNullMap.put("a", null);
        expectedNullMap.put("b", "");
        assertEquals(expectedNullMap, QS.decode("a&b=", new DecodeOptions(null, null, null, null, false, false, 20, StandardCharsets.UTF_8, false, false, new StringDelimiter("&"), 5, 1000, Duplicates.COMBINE, false, false, true, true, true, false)));
        // skipNulls
        LinkedHashMap<String, Object> m2 = new LinkedHashMap<>();
        m2.put("a", "b");
        m2.put("c", null);
        assertEquals("a=b", QS.encode(m2, new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), true, false, false, Format.RFC3986, null, true, false, null, null)));
    }

    // ===== Charset (encode) =====

    @Test
    void encodeCharsetLatin1AndSentinelAndNumericEntities() {
        assertEquals("%E6=%E6", QS.encode(Map.of("æ", "æ"), new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.ISO_8859_1, false, new StringDelimiter("&"), true, false, false, Format.RFC3986, null, false, false, null, null)));
        assertEquals("a=%26%239786%3B", QS.encode(Map.of("a", "☺"), new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.ISO_8859_1, false, new StringDelimiter("&"), true, false, false, Format.RFC3986, null, false, false, null, null)));
        assertEquals("utf8=%E2%9C%93&a=%E2%98%BA", QS.encode(Map.of("a", "☺"), new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, true, new StringDelimiter("&"), true, false, false, Format.RFC3986, null, false, false, null, null)));
        assertEquals("utf8=%26%2310003%3B&a=%E6", QS.encode(Map.of("a", "æ"), new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.ISO_8859_1, true, new StringDelimiter("&"), true, false, false, Format.RFC3986, null, false, false, null, null)));
    }

    @Test
    void decodeCustomDecoderMock() {
        DecodeOptions opts = DecodeOptions.withDecoder((value, charset, kind) -> {
            if (Objects.equals(value, "%61")) return "a";
            if (Objects.equals(value, "%68%65%6c%6c%6f")) return "hello";
            return value;
        });
        assertEquals(Map.of("a", "hello"), QS.decode("%61=%68%65%6c%6c%6f", opts));
    }

    // ===== RFC spaces =====

    @Test
    void rfc3986And1738Spaces() {
        assertEquals("a=b%20c", QS.encode(Map.of("a", "b c")));
        assertEquals("a=b%20c", QS.encode(Map.of("a", "b c"), new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), true, false, false, Format.RFC3986, null, false, false, null, null)));
        assertEquals("a=b+c", QS.encode(Map.of("a", "b c"), new EncodeOptions(null, null, ListFormat.INDICES, null, null, false, false, StandardCharsets.UTF_8, false, new StringDelimiter("&"), true, false, false, Format.RFC1738, null, false, false, null, null)));
    }

    @Test
    void constructFiltersFromJava() {
        // IterableFilter (varargs & collection)
        IterableFilter keys = IterableFilter.of("a", "e");
        IterableFilter fromColl = IterableFilter.from(Arrays.asList("a", "e"));
        assertNotNull(keys);
        assertNotNull(fromColl);

        // FunctionFilter
        FunctionFilter fn = FunctionFilter.from((key, value) -> {
            if ("b".equals(key)) return Undefined.Companion.invoke();
            return value;
        });
        assertNotNull(fn);
    }
}
