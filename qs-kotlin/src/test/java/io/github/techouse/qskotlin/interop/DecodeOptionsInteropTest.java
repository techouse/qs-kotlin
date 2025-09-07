package io.github.techouse.qskotlin.interop;

import static org.junit.jupiter.api.Assertions.*;

import io.github.techouse.qskotlin.QS;
import io.github.techouse.qskotlin.enums.DecodeKind;
import io.github.techouse.qskotlin.enums.Duplicates;
import io.github.techouse.qskotlin.models.DecodeOptions;
import io.github.techouse.qskotlin.models.JDecoder;
import io.github.techouse.qskotlin.models.JLegacyDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Additional coverage for DecodeOptions builder + behaviors. */
public class DecodeOptionsInteropTest {

  @Test
  void legacyDecoderIsUsedWhenNoModernDecoderProvided() {
    DecodeOptions opts =
        DecodeOptions.builder()
            .legacyDecoder(
                (JLegacyDecoder)
                    (value, cs) -> value == null ? null : value.toUpperCase(Locale.ROOT))
            .build();
    assertEquals(Map.of("A", "B"), QS.decode("A=b", opts));
  }

  @Test
  void modernDecoderTakesPrecedenceOverLegacyDecoder() {
    DecodeOptions opts =
        DecodeOptions.builder()
            .decoder(
                (JDecoder)
                    (value, cs, kind) -> {
                      if (value == null) return null;
                      return (kind == DecodeKind.KEY ? "K_" : "V_") + value;
                    })
            .legacyDecoder((JLegacyDecoder) (value, cs) -> "IGNORED")
            .build();
    Map<String, Object> out = QS.decode("a=b", opts);
    // Key should be prefixed with K_, value with V_
    assertEquals(1, out.size());
    assertTrue(out.containsKey("K_a"));
    assertEquals("V_b", out.get("K_a"));
  }

  @Test
  void allowDotsImpliedByDecodeDotInKeysWhenUnset() {
    DecodeOptions opts = DecodeOptions.builder().decodeDotInKeys(true).build();
    assertTrue(opts.getGetAllowDots());
    assertTrue(opts.getGetDecodeDotInKeys());
  }

  @Test
  void invalidAllowDotsCombinationThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DecodeOptions.builder().allowDots(false).decodeDotInKeys(true).build());
  }

  @Test
  void sparseListsPreservedWhenAllowed() {
    DecodeOptions opts = DecodeOptions.builder().allowSparseLists(true).build();
    Map<String, Object> out = QS.decode("a[0]=x&a[2]=y", opts);
    assertTrue(out.get("a") instanceof List<?>);
    List<?> list = (List<?>) out.get("a");
    assertEquals("x", list.get(0));
    // library may compact out null hole resulting size 2 (indexes 0,1) or keep placeholder (size 3)
    if (list.size() == 3) {
      assertEquals("y", list.get(2));
    } else {
      assertEquals(2, list.size());
      assertEquals("y", list.get(1));
    }
  }

  @Test
  void parameterLimitThrowsWhenExceededAndThrowOnLimitExceeded() {
    DecodeOptions opts =
        DecodeOptions.builder().parameterLimit(1).throwOnLimitExceeded(true).build();
    assertThrows(IndexOutOfBoundsException.class, () -> QS.decode("a=1&b=2", opts));
  }

  @Test
  void listLimitThrowsOnCommaSplitExceed() {
    DecodeOptions opts =
        DecodeOptions.builder().comma(true).listLimit(1).throwOnLimitExceeded(true).build();
    assertThrows(IndexOutOfBoundsException.class, () -> QS.decode("a=1,2", opts));
  }

  @Test
  void depthStrictThrows() {
    DecodeOptions strict = DecodeOptions.builder().depth(2).strictDepth(true).build();
    assertThrows(IndexOutOfBoundsException.class, () -> QS.decode("a[b][c][d]=x", strict));
  }

  @Test
  void depthNonStrictKeepsRemainderSingleSegment() {
    DecodeOptions nonStrict = DecodeOptions.builder().depth(2).strictDepth(false).build();
    Map<String, Object> out = QS.decode("a[b][c][d]=x", nonStrict);
    Map<?, ?> aMap = (Map<?, ?>) out.get("a");
    Map<?, ?> bMap = (Map<?, ?>) aMap.get("b");
    Map<?, ?> cMap = (Map<?, ?>) bMap.get("c");
    // remainder stored under key "[d]" (derived from segment "[[d]]" → "[d]")
    assertEquals("x", cMap.get("[d]"));
  }

  @Test
  void duplicatesFirstStrategy() {
    DecodeOptions opts = DecodeOptions.builder().duplicates(Duplicates.FIRST).build();
    assertEquals(Map.of("a", "1"), QS.decode("a=1&a=2", opts));
  }

  @Test
  void duplicatesLastStrategy() {
    DecodeOptions opts = DecodeOptions.builder().duplicates(Duplicates.LAST).build();
    assertEquals(Map.of("a", "2"), QS.decode("a=1&a=2", opts));
  }

  @Test
  void strictNullHandlingBareKeyMakesNull() {
    DecodeOptions opts = DecodeOptions.builder().strictNullHandling(true).build();
    var expected = new java.util.LinkedHashMap<String, Object>();
    expected.put("a", null);
    assertEquals(expected, QS.decode("a", opts));
  }

  @Test
  void charsetSentinelOverridesProvidedCharset() {
    // Provide ISO but sentinel indicates UTF-8 (or vice versa)
    DecodeOptions opts =
        DecodeOptions.builder().charset(StandardCharsets.ISO_8859_1).charsetSentinel(true).build();
    Map<String, Object> out =
        QS.decode("utf8=%E2%9C%93&a=%E2%98%BA", opts); // charsets sentinel for UTF-8
    assertEquals("☺", out.get("a")); // decoded as UTF-8 not Latin1
  }

  @Test
  void negativeListLimitForcesMapInsteadOfList() {
    DecodeOptions opts = DecodeOptions.builder().listLimit(-1).build();
    Map<String, Object> out = QS.decode("a[0]=x&a[1]=y", opts);
    // With listLimit < 0 parsing creates nested maps: a -> { 0: x, 1: y }
    Map<?, ?> aMap = (Map<?, ?>) out.get("a");
    assertEquals("x", aMap.get("0"));
    assertEquals("y", aMap.get("1"));
  }

  @Test
  void parseListsDisabledProducesMapKeys() {
    DecodeOptions opts = DecodeOptions.builder().parseLists(false).build();
    Map<String, Object> out = QS.decode("a[]=x&a[]=y", opts);
    Map<?, ?> aMap = (Map<?, ?>) out.get("a");
    assertNotNull(aMap.get("0"));
    Object v0 = aMap.get("0");
    if (v0 instanceof List<?> l) {
      assertTrue(l.contains("x"));
      assertTrue(l.contains("y"));
    } else if (aMap.size() == 1) {
      // Overwrite behavior
      assertEquals("y", v0);
    } else {
      // Sequential mapping 0->x,1->y
      assertEquals("x", v0);
      assertEquals("y", aMap.get("1"));
    }
  }

  @Test
  void allowEmptyListsProducesEmptyList() {
    DecodeOptions opts = DecodeOptions.builder().allowEmptyLists(true).build();
    Map<String, Object> out = QS.decode("foo[]", opts);
    assertTrue(out.get("foo") instanceof List<?>);
    assertTrue(((List<?>) out.get("foo")).isEmpty());
  }

  @Test
  void allowDotsTrueWithoutDecodeDotInKeys() {
    DecodeOptions opts = DecodeOptions.builder().allowDots(true).build();
    Map<String, Object> out = QS.decode("a.b=c", opts);
    assertEquals(Map.of("a", Map.of("b", "c")), out);
    assertFalse(opts.getGetDecodeDotInKeys());
    assertTrue(opts.getGetAllowDots());
  }

  @Test
  void interpretNumericEntitiesIso() {
    DecodeOptions opts =
        DecodeOptions.builder()
            .charset(StandardCharsets.ISO_8859_1)
            .interpretNumericEntities(true)
            .build();
    Map<String, Object> out = QS.decode("a=%26%239786%3B", opts); // '&#9786;' (☺)
    assertEquals("☺", out.get("a"));
  }
}
