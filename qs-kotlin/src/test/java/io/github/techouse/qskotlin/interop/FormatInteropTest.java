package io.github.techouse.qskotlin.interop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.techouse.qskotlin.enums.Format;
import io.github.techouse.qskotlin.enums.JFormatter;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FormatInteropTest {

  @Test
  @DisplayName("RFC3986.format is identity / RFC1738.format replaces %20 with +")
  void enumFormatImplementations() {
    String input = "a%20b%20c";
    assertEquals("a%20b%20c", Format.RFC3986.format(input));
    assertEquals("a+b+c", Format.RFC1738.format(input));

    // idempotency / existing plus signs
    assertEquals("a+b+c", Format.RFC1738.format("a+b%20c"));
    // multiple %20 sequences and boundaries
    assertEquals("+1+", Format.RFC1738.format("%201%20"));

    // chaining identity then RFC1738
    assertEquals(Format.RFC1738.format(input), Format.RFC1738.format(Format.RFC3986.format(input)));
  }

  @Test
  @DisplayName("formatter(Function) adapter works")
  void functionAdapter() {
    Function<String, String> fn = s -> s.replace('X', 'Y');
    kotlin.jvm.functions.Function1<String, String> formatter = Format.formatter(fn);
    assertEquals("aYb", formatter.invoke("aXb"));
  }

  @Test
  @DisplayName("formatter(JFormatter) adapter works")
  void jFormatterAdapter() {
    JFormatter jf = v -> v + "!";
    kotlin.jvm.functions.Function1<String, String> formatter = Format.formatter(jf);
    assertEquals("hello!", formatter.invoke("hello"));
  }

  @Test
  @DisplayName("formatter(UnaryOperator) adapter works")
  void unaryOperatorAdapter() {
    UnaryOperator<String> op = s -> s + s; // duplicate
    kotlin.jvm.functions.Function1<String, String> formatter = Format.formatter(op);
    assertEquals("zZzZ", formatter.invoke("zZ"));
  }

  @Test
  @DisplayName("Distinct adapters produce independent instances")
  void distinctAdaptersIndependence() {
    kotlin.jvm.functions.Function1<String, String> f1 =
        Format.formatter((Function<String, String>) String::toUpperCase);
    kotlin.jvm.functions.Function1<String, String> f2 =
        Format.formatter((UnaryOperator<String>) String::toLowerCase);
    assertNotEquals(f1.invoke("aBc"), f2.invoke("aBc"));
    assertEquals("ABC", f1.invoke("aBc"));
    assertEquals("abc", f2.invoke("aBc"));
  }

  @Test
  @DisplayName("Adapters can be composed manually")
  void adapterComposition() {
    kotlin.jvm.functions.Function1<String, String> upper =
        Format.formatter((Function<String, String>) String::toUpperCase);
    kotlin.jvm.functions.Function1<String, String> exclaim =
        Format.formatter((JFormatter) v -> v + "!");
    String result = exclaim.invoke(upper.invoke("mix"));
    assertEquals("MIX!", result);
  }
}
