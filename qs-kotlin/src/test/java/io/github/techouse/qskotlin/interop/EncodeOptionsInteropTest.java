package io.github.techouse.qskotlin.interop;

import static org.junit.jupiter.api.Assertions.*;

import io.github.techouse.qskotlin.QS;
import io.github.techouse.qskotlin.enums.Format;
import io.github.techouse.qskotlin.enums.ListFormat;
import io.github.techouse.qskotlin.models.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation") // indices() usage for fallback coverage
public class EncodeOptionsInteropTest {

  @Test
  @DisplayName("listFormat fallback via indices and default")
  void listFormatFallback() {
    EncodeOptions idxTrue = EncodeOptions.builder().indices(true).build();
    assertEquals(ListFormat.INDICES, idxTrue.getGetListFormat());
    EncodeOptions idxFalse = EncodeOptions.builder().indices(false).build();
    assertEquals(ListFormat.REPEAT, idxFalse.getGetListFormat());
    EncodeOptions deflt = EncodeOptions.builder().build();
    assertEquals(ListFormat.INDICES, deflt.getGetListFormat());
  }

  @Test
  @DisplayName("allowDots derived from encodeDotInKeys when unset")
  void allowDotsDerived() {
    EncodeOptions opts = EncodeOptions.builder().encodeDotInKeys(true).build();
    assertTrue(opts.getGetAllowDots());
  }

  @Test
  @DisplayName("Custom encoder and date serializer override defaults")
  void customEncoderAndDateSerializer() {
    AtomicInteger encCalls = new AtomicInteger();
    EncodeOptions opts =
        EncodeOptions.builder()
            .encoder(
                (JValueEncoder)
                    (value, cs, fmt) -> {
                      encCalls.incrementAndGet();
                      return value == null ? "" : ("X" + value.toString().toUpperCase(Locale.ROOT));
                    })
            .dateSerializer(
                (JDateSerializer) dt -> Long.toString(dt.toInstant(ZoneOffset.UTC).toEpochMilli()))
            .build();
    LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(42), ZoneOffset.UTC);
    String out = QS.encode(Map.of("a", "abc", "d", date), opts);
    assertTrue(out.contains("XA=XABC"), out);
    assertTrue(out.contains("XD=X42"), out);
    assertTrue(encCalls.get() >= 4); // keys + values
  }

  @Test
  @DisplayName("encode=false leaves keys unencoded but still processes lists")
  void encodeFalse() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("a b", List.of("c d"));
    String out =
        QS.encode(m, EncodeOptions.builder().encode(false).listFormat(ListFormat.REPEAT).build());
    // Space in key should remain literal, value also
    assertEquals("a b=c d", out);
  }

  @Test
  @DisplayName("encodeValuesOnly + encodeDotInKeys encodes dots and only values")
  void encodeValuesOnlyAndDotKeys() {
    Map<String, Object> m = Map.of("a.b", "c d");
    String out =
        QS.encode(m, EncodeOptions.builder().encodeDotInKeys(true).encodeValuesOnly(true).build());
    // Key left as a.b (early primitive path), value encoded
    assertEquals("a.b=c%20d", out);
  }

  @Test
  @DisplayName("skipNulls omits null keys; strictNullHandling formats bare nulls")
  void skipNullsAndStrictNullHandling() {
    LinkedHashMap<String, Object> m = new LinkedHashMap<>();
    m.put("a", null);
    m.put("b", null);
    m.put("c", "");
    String outStrict = QS.encode(m, EncodeOptions.builder().strictNullHandling(true).build());
    assertTrue(outStrict.equals("a&b&c=") || outStrict.equals("b&a&c="));
    String outSkip = QS.encode(m, EncodeOptions.builder().skipNulls(true).build());
    assertTrue(outSkip.equals("c=") || outSkip.isEmpty());
  }

  @Test
  @DisplayName("allowEmptyLists encodes empty list as key[]")
  void allowEmptyLists() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("foo", List.of());
    String out = QS.encode(m, EncodeOptions.builder().allowEmptyLists(true).encode(false).build());
    assertEquals("foo[]", out);
  }

  @Test
  @DisplayName("commaRoundTrip single-element list adds [] suffix")
  void commaRoundTripSingleElement() {
    Map<String, Object> m = Map.of("a", List.of("x"));
    String out =
        QS.encode(
            m,
            EncodeOptions.builder()
                .listFormat(ListFormat.COMMA)
                .commaRoundTrip(true)
                .encode(false)
                .build());
    assertEquals("a[]=x", out);
  }

  @Test
  @DisplayName("ListFormat.COMMA with two elements produces comma-joined")
  void listFormatCommaMultiple() {
    Map<String, Object> m = Map.of("a", List.of("x", "y"));
    String out =
        QS.encode(m, EncodeOptions.builder().listFormat(ListFormat.COMMA).encode(false).build());
    assertEquals("a=x,y", out);
  }

  @Test
  @DisplayName("charsetSentinel for UTF-8 and ISO-8859-1")
  void charsetSentinel() {
    Map<String, Object> m = Map.of("a", "b");
    String utf8 =
        QS.encode(
            m,
            EncodeOptions.builder().charsetSentinel(true).charset(StandardCharsets.UTF_8).build());
    assertTrue(utf8.startsWith("utf8=%E2%9C%93&"));
    String latin1 =
        QS.encode(
            m,
            EncodeOptions.builder()
                .charsetSentinel(true)
                .charset(StandardCharsets.ISO_8859_1)
                .build());
    assertTrue(latin1.startsWith("utf8=%26%2310003%3B&"));
  }

  @Test
  @DisplayName("custom delimiter (string and constant)")
  void customDelimiter() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("a", 1);
    m.put("b", 2);
    String semi = QS.encode(m, EncodeOptions.builder().delimiter(";").encode(false).build());
    assertEquals("a=1;b=2", semi);
    String comma =
        QS.encode(m, EncodeOptions.builder().delimiter(Delimiter.COMMA).encode(false).build());
    assertEquals("a=1,b=2", comma);
  }

  @Test
  @DisplayName("sort comparator reorders keys")
  void sortComparator() {
    Map<String, Object> m = new HashMap<>();
    m.put("c", 3);
    m.put("a", 1);
    m.put("b", 2);
    EncodeOptions opts =
        EncodeOptions.builder().sort(Comparator.comparing(Object::toString).reversed()).build();
    String out = QS.encode(m, opts);
    // Expect descending order: c,b,a (Map iteration may vary but sort enforces)
    assertEquals("c=3&b=2&a=1", out);
  }

  @Test
  @DisplayName("filter FunctionFilter removes and transforms keys")
  void functionFilter() {
    LinkedHashMap<String, Object> m = new LinkedHashMap<>();
    m.put("a", 1);
    m.put("b", 2);
    m.put("c", 3);
    FunctionFilter fn =
        FunctionFilter.from(
            (key, value) -> {
              if ("b".equals(key)) return Undefined.Companion.invoke();
              if ("c".equals(key)) return 42; // transform
              return value;
            });
    String out = QS.encode(m, EncodeOptions.builder().filter(fn).build());
    // b removed, c transformed
    assertTrue(out.contains("a=1"));
    assertTrue(out.contains("c=42"));
    assertFalse(out.contains("b="));
  }

  @Test
  @DisplayName("filter IterableFilter restricts keys")
  void iterableFilter() {
    Map<String, Object> m = Map.of("a", 1, "b", 2, "c", 3);
    String out =
        QS.encode(m, EncodeOptions.builder().filter(new IterableFilter(List.of("c", "a"))).build());
    // Only c and a, preserving iterable order
    assertEquals("c=3&a=1", out);
  }

  @Test
  @DisplayName("RFC1738 format encodes spaces as +")
  void rfc1738Spaces() {
    String out =
        QS.encode(Map.of("a", "b c"), EncodeOptions.builder().format(Format.RFC1738).build());
    assertEquals("a=b+c", out);
  }

  @Test
  @DisplayName("strictNullHandling + allowEmptyLists combined")
  void strictNullAndEmptyLists() {
    LinkedHashMap<String, Object> m = new LinkedHashMap<>();
    m.put("x", null);
    m.put("y", List.of());
    String out =
        QS.encode(
            m, EncodeOptions.builder().strictNullHandling(true).allowEmptyLists(true).build());
    // x bare, y as y[]
    assertTrue(out.equals("x&y[]") || out.equals("y[]&x"));
  }

  @Test
  @DisplayName("builder full chain does not throw")
  void builderFullChain() {
    EncodeOptions opts =
        EncodeOptions.builder()
            .encoder((JValueEncoder) (v, cs, f) -> Objects.toString(v, ""))
            .dateSerializer((JDateSerializer) dt -> "D")
            .listFormat(ListFormat.BRACKETS)
            .indices(null)
            .allowDots(true)
            .addQueryPrefix(true)
            .allowEmptyLists(true)
            .charset(StandardCharsets.UTF_8)
            .charsetSentinel(false)
            .delimiter(Delimiter.SEMICOLON)
            .encode(true)
            .encodeDotInKeys(false)
            .encodeValuesOnly(false)
            .format(Format.RFC3986)
            .filter(null)
            .skipNulls(false)
            .strictNullHandling(false)
            .commaRoundTrip(null)
            .sort(Comparator.comparing(o -> o == null ? "" : o.toString()))
            .build();
    assertNotNull(opts);
    assertEquals(ListFormat.BRACKETS, opts.getGetListFormat());
    String out = QS.encode(Map.of("a", List.of("b")), opts);
    assertTrue(out.startsWith("?"));
  }
}
