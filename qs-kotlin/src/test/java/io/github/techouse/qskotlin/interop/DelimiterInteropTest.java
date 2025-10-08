package io.github.techouse.qskotlin.interop;

import static org.junit.jupiter.api.Assertions.*;

import io.github.techouse.qskotlin.QS;
import io.github.techouse.qskotlin.models.DecodeOptions;
import io.github.techouse.qskotlin.models.Delimiter;
import io.github.techouse.qskotlin.models.EncodeOptions;
import io.github.techouse.qskotlin.models.RegexDelimiter;
import io.github.techouse.qskotlin.models.StringDelimiter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import kotlin.text.RegexOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DelimiterInteropTest {

  @Test
  @DisplayName("Companion factories and constants produce expected delimiter instances")
  void companionFactories() {
    StringDelimiter amp = Delimiter.AMPERSAND;
    StringDelimiter comma = Delimiter.COMMA;
    StringDelimiter semi = Delimiter.SEMICOLON;
    assertEquals("&", amp.getValue());
    assertEquals(",", comma.getValue());
    assertEquals(";", semi.getValue());

    StringDelimiter viaFactory = Delimiter.string("&");
    assertEquals(amp, viaFactory);
    assertNotSame(amp, viaFactory); // data class equality, but different instance

    RegexDelimiter regex = Delimiter.regex("[;,]");
    assertEquals("[;,]", regex.getPattern());
  }

  @Test
  @DisplayName("StringDelimiter splitting edge cases")
  void stringDelimiterSplit() {
    StringDelimiter d = new StringDelimiter("&");
    assertEquals(List.of("a", "b", "c"), d.split("a&b&c"));
    assertEquals(List.of("abc"), d.split("abc")); // no delimiter
    assertEquals(List.of("", "b"), d.split("&b")); // leading
    assertEquals(List.of("a", ""), d.split("a&")); // trailing (Kotlin keeps trailing empty)
    assertEquals(List.of("a", "", "b"), d.split("a&&b")); // adjacent
  }

  @Test
  @DisplayName("StringDelimiter equality and hashCode based on value")
  void stringDelimiterEquality() {
    StringDelimiter a1 = new StringDelimiter(",");
    StringDelimiter a2 = new StringDelimiter(",");
    StringDelimiter b = new StringDelimiter(";");
    assertEquals(a1, a2);
    assertEquals(a1.hashCode(), a2.hashCode());
    assertNotEquals(a1, b);
  }

  @Test
  @DisplayName("RegexDelimiter equality, hashCode, toString, split")
  void regexDelimiterBehavior() {
    RegexDelimiter r1 = new RegexDelimiter("[;,]");
    RegexDelimiter r2 = new RegexDelimiter("[;,]");
    RegexDelimiter rFlags = new RegexDelimiter(Pattern.compile("[;,]", Pattern.CASE_INSENSITIVE));
    assertEquals(r1, r2);
    assertEquals(r1.hashCode(), r2.hashCode());
    assertNotEquals(r1, rFlags); // flags differ
    assertNotEquals(r1.hashCode(), rFlags.hashCode());
    // Additional branch coverage
    assertEquals(r1, r1); // identity branch
    assertNotEquals("not a delimiter", r1); // other !is RegexDelimiter branch
    RegexDelimiter rPatternDiff = new RegexDelimiter(";");
    assertNotEquals(r1, rPatternDiff); // pattern differs

    assertTrue(r1.toString().contains("RegexDelimiter("));
    assertTrue(r1.toString().contains("[;,]"));

    assertEquals(List.of("a", "b", "c"), r1.split("a,b;c"));
  }

  @Test
  @DisplayName("EncodeOptions uses custom delimiters (string + constant)")
  void encodeOptionsDelimiter() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("a", 1);
    m.put("b", 2);

    // Using constant COMMA
    String commaEncoded =
        QS.encode(m, EncodeOptions.builder().delimiter(Delimiter.COMMA).encode(false).build());
    assertEquals("a=1,b=2", commaEncoded);

    // Using builder string -> semicolon
    String semiEncoded = QS.encode(m, EncodeOptions.builder().delimiter(";").encode(false).build());
    assertEquals("a=1;b=2", semiEncoded);
  }

  @Test
  @DisplayName("DecodeOptions uses string and regex delimiters")
  void decodeOptionsDelimiter() {
    // String delimiter ';'
    Map<String, Object> semi = QS.decode("a=1;b=2", DecodeOptions.builder().delimiter(";").build());
    assertEquals(Map.of("a", "1", "b", "2"), semi);

    // Regex delimiter split on both ; and ,
    Map<String, Object> mixed =
        QS.decode(
            "a=1;b=2,c=3", DecodeOptions.builder().delimiter(Delimiter.regex("[;,]")).build());
    assertEquals(Map.of("a", "1", "b", "2", "c", "3"), mixed);
  }

  @Test
  @DisplayName("Delimiter splitting integration through QS.encode and decode (ampersand default)")
  void integrationDefaultAmpersand() {
    Map<String, Object> data = Map.of("x", "1", "y", "2");
    String encoded = QS.encode(data); // default should use '&'
    assertTrue(encoded.equals("x=1&y=2") || encoded.equals("y=2&x=1"));
    Map<String, Object> decoded = QS.decode(encoded);
    assertEquals(data, decoded);
  }

  @Test
  @DisplayName("Additional regex factory overload coverage and advanced splitting")
  void regexFactoryOverloadsAndSplit() {
    // Pattern overload
    Pattern whitespaceMultiline = Pattern.compile("\\s+", Pattern.MULTILINE);
    RegexDelimiter viaPattern = Delimiter.regex(whitespaceMultiline);
    assertEquals(whitespaceMultiline.pattern(), viaPattern.getPattern());
    assertEquals(whitespaceMultiline.flags(), viaPattern.getFlags());

    // (pattern, flags) overload
    RegexDelimiter ciUnicode =
        Delimiter.regex("abc", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    assertEquals("abc", ciUnicode.getPattern());
    int flags = ciUnicode.getFlags();
    assertTrue((flags & Pattern.CASE_INSENSITIVE) != 0);
    assertTrue((flags & Pattern.UNICODE_CASE) != 0);

    // Distinguish hashCodes by different pattern and flags
    RegexDelimiter ciOnly = Delimiter.regex("abc", Pattern.CASE_INSENSITIVE);
    assertNotEquals(ciUnicode, ciOnly);
    assertNotEquals(ciUnicode.hashCode(), ciOnly.hashCode());

    // Splitting with whitespace regex
    List<String> parts = viaPattern.split("a  b\tc\n d");
    assertEquals(List.of("a", "b", "c", "d"), parts);

    // Splitting empty string returns single empty element (Pattern.split behavior)
    assertEquals(List.of(""), viaPattern.split(""));

    // toString contains flags integer
    String ts = ciUnicode.toString();
    assertTrue(ts.contains("abc"));
    assertTrue(ts.contains(String.valueOf(ciUnicode.getFlags())));
  }

  @Test
  @DisplayName("regex(pattern, Set<RegexOption>) factory works for Java callers")
  void regexFactoryWithKotlinOptions() {
    RegexDelimiter fromOptions = Delimiter.regex("[,&]", java.util.Set.of(RegexOption.IGNORE_CASE));
    assertTrue((fromOptions.getFlags() & Pattern.CASE_INSENSITIVE) != 0);
  }
}
