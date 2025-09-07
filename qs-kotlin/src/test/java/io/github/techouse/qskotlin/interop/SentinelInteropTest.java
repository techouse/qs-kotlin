package io.github.techouse.qskotlin.interop;

import static org.junit.jupiter.api.Assertions.*;

import io.github.techouse.qskotlin.enums.Sentinel;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SentinelInteropTest {

  @Test
  @DisplayName("Raw sentinel values and encoded forms are correct")
  void rawAndEncoded() {
    assertEquals("&#10003;", Sentinel.ISO.getValue());
    assertEquals("utf8=%26%2310003%3B", Sentinel.ISO.asQueryParam());
    assertEquals("✓", Sentinel.CHARSET.getValue());
    assertEquals("utf8=%E2%9C%93", Sentinel.CHARSET.asQueryParam());
  }

  @Test
  @DisplayName("toString matches encoded and asQueryParam")
  void toStringConsistency() {
    for (Sentinel s : Sentinel.getEntries()) {
      assertEquals(s.asQueryParam(), s.toString());
    }
  }

  @Test
  @DisplayName("toEntry returns immutable utf8=value pair")
  void toEntryImmutable() {
    for (Sentinel s : Sentinel.getEntries()) {
      var entry = s.toEntry();
      assertEquals(Sentinel.PARAM_NAME, entry.getKey());
      assertEquals(s.getValue(), entry.getValue());
      assertThrows(UnsupportedOperationException.class, () -> entry.setValue("X"));
    }
  }

  @Test
  @DisplayName("Encoded forms are unique")
  void encodedUniqueness() {
    Set<String> seen = new HashSet<>();
    for (Sentinel s : Sentinel.getEntries()) {
      assertTrue(seen.add(s.asQueryParam()), "Duplicate encoded sentinel: " + s);
    }
  }

  @Test
  @DisplayName("PARAM_NAME constant is utf8")
  void paramNameConstant() {
    assertEquals("utf8", Sentinel.PARAM_NAME);
  }
}
