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
        Map<String, Object> out = QS.decode(
                "?a=b&c=d",
                new DecodeOptions(
                        /* allowDots */ null,
                        /* decoder */ null,
                        /* legacyDecoder */ null,
                        /* decodeDotInKeys */ null,
                        /* allowEmptyLists */ false,
                        /* allowSparseLists */ false,
                        /* listLimit */ 20,
                        /* charset */ StandardCharsets.UTF_8,
                        /* charsetSentinel */ false,
                        /* comma */ false,
                        /* delimiter */ new StringDelimiter("&"),
                        /* depth */ 5,
                        /* parameterLimit */ 1000,
                        /* duplicates */ Duplicates.COMBINE,
                        /* ignoreQueryPrefix */ true,
                        /* interpretNumericEntities */ false,
                        /* parseLists */ true,
                        /* strictDepth */ false,
                        /* strictNullHandling */ false,
                        /* throwOnLimitExceeded */ false
                ));
        assertEquals(Map.of("a", "b", "c", "d"), out);
    }

    @Test
    void decodeCustomDelimiter() {
        // String delimiter
        Map<String, Object> out1 = QS.decode("a=b;c=d",
                // full-args ctor is verbose from Java; this shows the delimiter objects compile & are usable.
                new DecodeOptions(
                        null, null, null, null,
                        false, false, 20,
                        StandardCharsets.UTF_8,
                        false, false,
                        new StringDelimiter(";"),
                        5, 1000, Duplicates.COMBINE,
                        false, false, true, false, false, false));
        assertEquals(Map.of("a", "b", "c", "d"), out1);

        // Regex delimiter
        Map<String, Object> out2 = QS.decode("a=b;c=d",
                new DecodeOptions(
                        null, null, null, null,
                        false, false, 20,
                        StandardCharsets.UTF_8,
                        false, false,
                        new RegexDelimiter("[;,]"),
                        5, 1000, Duplicates.COMBINE,
                        false, false, true, false, false, false));
        assertEquals(Map.of("a", "b", "c", "d"), out2);
    }

    @Test
    void decodeNestedBrackets() {
        Map<String, Object> out = QS.decode("foo[bar]=baz");
        assertEquals(Map.of("foo", Map.of("bar", "baz")), out);
    }

    @Test
    void decodeListBrackets() {
        Map<String, Object> out = QS.decode("a[]=b&a[]=c");
        assertEquals(List.of("b", "c"), out.get("a"));
    }

    @Test
    void encodeAddQueryPrefixAndListFormat() {
        // Add query prefix (uses the Java-friendly ctor)
        // Use LinkedHashMap to guarantee insertion order for deterministic output
        Map<String, Object> ordered = new LinkedHashMap<>();
        ordered.put("a", "b");
        ordered.put("c", "d");
        assertEquals("?a=b&c=d", QS.encode(ordered,
                new EncodeOptions(
                        /* encoder */ null,
                        /* dateSerializer */ null,
                        /* listFormat */ ListFormat.INDICES,
                        /* indices */ null,
                        /* allowDots */ null,
                        /* addQueryPrefix */ true,
                        /* allowEmptyLists */ false,
                        /* charset */ StandardCharsets.UTF_8,
                        /* charsetSentinel */ false,
                        /* delimiter */ "&",
                        /* encode */ true,
                        /* encodeDotInKeys */ false,
                        /* encodeValuesOnly */ true,
                        /* format */ Format.RFC3986,
                        /* filter */ null,
                        /* skipNulls */ false,
                        /* strictNullHandling */ false,
                        /* commaRoundTrip */ null,
                        /* sort */ null
                )));

        // List format brackets (will be percent-encoded by default)
        assertEquals("a%5B%5D=b&a%5B%5D=c",
                QS.encode(Map.of("a", List.of("b", "c")),
                        new EncodeOptions(
                                /* encoder */ null,
                                /* dateSerializer */ null,
                                /* listFormat */ ListFormat.BRACKETS,
                                /* indices */ null,
                                /* allowDots */ null,
                                /* addQueryPrefix */ false,
                                /* allowEmptyLists */ false,
                                /* charset */ StandardCharsets.UTF_8,
                                /* charsetSentinel */ false,
                                /* delimiter */ "&",
                                /* encode */ true,
                                /* encodeDotInKeys */ false,
                                /* encodeValuesOnly */ false, // encode keys too → brackets get percent-encoded
                                /* format */ Format.RFC3986,
                                /* filter */ null,
                                /* skipNulls */ false,
                                /* strictNullHandling */ false,
                                /* commaRoundTrip */ null,
                                /* sort */ null
                        )));
    }

    @Test
    void encodeWithCustomValueEncoder() {
        // Replace "č" with "c" while encoding AND encode keys (encodeValuesOnly=false)
        kotlin.jvm.functions.Function3<Object, Charset, Format, String> enc =
                (value, cs, fmt) -> {
                    String s = Objects.toString(value, "");
                    if (Objects.equals(s, "č")) s = "c"; // custom transform
                    try {
                        Charset charset = (cs != null) ? cs : StandardCharsets.UTF_8;
                        return URLEncoder.encode(s, charset); // ensure keys/values are percent-encoded
                    } catch (Exception e) {
                        return s; // fallback
                    }
                };

        EncodeOptions opts = new EncodeOptions(
                /* encoder */ enc,
                /* dateSerializer */ null,
                /* listFormat */ ListFormat.INDICES,
                /* indices */ null,
                /* allowDots */ null,
                /* addQueryPrefix */ false,
                /* allowEmptyLists */ false,
                /* charset */ StandardCharsets.UTF_8,
                /* charsetSentinel */ false,
                /* delimiter */ "&",
                /* encode */ true,
                /* encodeDotInKeys */ false,
                /* encodeValuesOnly */ false, // encode keys too
                /* format */ Format.RFC3986,
                /* filter */ null,
                /* skipNulls */ false,
                /* strictNullHandling */ false,
                /* commaRoundTrip */ null,
                /* sort */ null
        );

        assertEquals("a%5Bb%5D=c",
                QS.encode(Map.of("a", Map.of("b", "č")), opts));
    }

    @Test
    void encodeWithCustomDateSerializer() {
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(7), ZoneOffset.UTC);
        EncodeOptions opts = EncodeOptions.withDateSerializer(dt ->
                Long.toString(dt.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()));
        assertEquals("a=7", QS.encode(Map.of("a", date), opts));
    }

    @Test
    void encodeWithSorter() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("a", "c");
        input.put("z", "y");
        input.put("b", "f");

        EncodeOptions opts = EncodeOptions.withSorter(
                Comparator.comparing(o -> o == null ? "" : o.toString())
        );
        assertEquals("a=c&b=f&z=y", QS.encode(input, opts));
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
