package io.github.techouse.qskotlin.interop;

import io.github.techouse.qskotlin.enums.JListFormatGenerator;
import io.github.techouse.qskotlin.enums.ListFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class ListFormatInteropTest {

    @Test
    @DisplayName("Enum generate() basic forms")
    void enumGenerateBasic() {
        assertEquals("foo[]", ListFormat.BRACKETS.generate("foo", null));
        assertEquals("foo", ListFormat.COMMA.generate("foo", null));
        assertEquals("foo", ListFormat.REPEAT.generate("foo", null));
        assertEquals("foo[0]", ListFormat.INDICES.generate("foo", "0"));
    }

    @Test
    @DisplayName("INDICES.generate requires non-null key")
    void indicesNullKeyThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ListFormat.INDICES.generate("foo", null));
        assertTrue(ex.getMessage().contains("requires a non-null key"));
    }

    @Test
    @DisplayName("generator(JListFormatGenerator) adapts correctly")
    void adapterJListFormatGenerator() {
        @SuppressWarnings("unchecked")
        kotlin.jvm.functions.Function2<String, String, String> fn = ListFormat.generator((JListFormatGenerator) (p, k) -> p + (k == null ? "[]" : "{" + k + "}"));
        assertEquals("x[]", fn.invoke("x", null));
        assertEquals("x{3}", fn.invoke("x", "3"));
    }

    @Test
    @DisplayName("generator(BiFunction) adapts correctly")
    void adapterBiFunction() {
        BiFunction<String, String, String> bi = (p, k) -> p + ":" + (k == null ? "_" : k);
        @SuppressWarnings("unchecked")
        kotlin.jvm.functions.Function2<String, String, String> fn = ListFormat.generator(bi);
        assertEquals("a:_", fn.invoke("a", null));
        assertEquals("a:7", fn.invoke("a", "7"));
    }

    @Test
    @DisplayName("generator(Function) (key-ignoring) adapts correctly")
    void adapterFunctionIgnoringKey() {
        Function<String, String> f = String::toUpperCase;
        @SuppressWarnings("unchecked")
        kotlin.jvm.functions.Function2<String, String, String> fn = ListFormat.generator(f);
        assertEquals("FOO", fn.invoke("foo", null));
        assertEquals("FOO", fn.invoke("foo", "99")); // key ignored
    }

    @Test
    @DisplayName("Enum generators ignore key where specified")
    void ignoreKeyWhereApplicable() {
        assertEquals(ListFormat.COMMA.generate("p", null), ListFormat.COMMA.generate("p", "x"));
        assertEquals(ListFormat.REPEAT.generate("r", null), ListFormat.REPEAT.generate("r", "1"));
        // BRACKETS ignores key entirely
        assertEquals("q[]", ListFormat.BRACKETS.generate("q", null));
        assertEquals("q[]", ListFormat.BRACKETS.generate("q", "7"));
    }
}
