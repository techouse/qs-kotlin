package io.github.techouse.qskotlin.interop;

import static io.github.techouse.qskotlin.fixtures.data.E2EFixtures.EndToEndTestCases;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.techouse.qskotlin.QS;
import io.github.techouse.qskotlin.enums.ListFormat;
import io.github.techouse.qskotlin.models.DecodeOptions;
import io.github.techouse.qskotlin.models.EncodeOptions;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ExtensionsInteropTest {

  @Test
  void string_toQueryMap_decodes_all_fixtures() {
    for (var tc : EndToEndTestCases) {
      Map<String, Object> expected = tc.getData();

      Map<String, Object> actual = QS.toQueryMap(tc.getEncoded());

      assertEquals(expected, actual, "decode mismatch for: " + tc.getEncoded());
    }
  }

  @Test
  void map_toQueryString_encodes_all_fixtures() {
    for (var tc : EndToEndTestCases) {
      Map<String, Object> input = tc.getData();

      EncodeOptions opts =
          EncodeOptions.builder()
              .listFormat(ListFormat.INDICES)
              .encode(false) // mirror Kotlin: EncodeOptions(encode = false)
              .delimiter("&") // deterministic delimiter
              .build();

      String qs = QS.toQueryString(input, opts);
      assertEquals(tc.getEncoded(), qs, "encode mismatch for: " + input);
    }
  }

  @Test
  void map_toQueryString_uses_default_overload() {
    assertEquals("a=b", QS.toQueryString(Map.of("a", "b")));
  }

  @Test
  void uri_decodeQsQuery_uses_raw_query() {
    URI uri =
        URI.create(
            "https://example.com/search?filter%5Bname%5D=John%20Doe&tag=a&tag=b&escaped=x%26y&pct=%2525#results");

    assertEquals(
        Map.of(
            "filter",
            Map.of("name", "John Doe"),
            "tag",
            List.of("a", "b"),
            "escaped",
            "x&y",
            "pct",
            "%25"),
        QS.decodeQsQuery(uri));
  }

  @Test
  void uri_decodeQsQuery_accepts_options() {
    DecodeOptions options =
        DecodeOptions.builder().comma(true).delimiter(";").strictNullHandling(true).build();
    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("flag", null);
    expected.put("tags", List.of("kotlin", "java"));

    assertEquals(expected, QS.decodeQsQuery(URI.create("search?flag;tags=kotlin,java"), options));
  }

  @SuppressWarnings("unused")
  private static Map<String, Object> string_toQueryMap_null_literal_stays_unambiguous() {
    return QS.toQueryMap(null);
  }
}
