package io.github.techouse.qskotlin.interop;

import static org.junit.jupiter.api.Assertions.*;

import io.github.techouse.qskotlin.models.FunctionFilter;
import io.github.techouse.qskotlin.models.IterableFilter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class FilterInteropTest {

  // Helper to get a list out of an Iterable for assertions
  private static List<Object> toList(Iterable<?> iterable) {
    List<Object> list = new ArrayList<>();
    for (Object o : iterable) {
      list.add(o);
    }
    return list;
  }

  @Nested
  @DisplayName("FunctionFilter Java interop")
  class FunctionFilterTests {
    @Test
    void constructor_with_BiFunction_invokes_lambda_and_returns_value() {
      AtomicReference<String> keyRef = new AtomicReference<>();
      AtomicReference<Object> valueRef = new AtomicReference<>();

      FunctionFilter filter =
          new FunctionFilter(
              (BiFunction<String, Object, Object>)
                  (k, v) -> {
                    keyRef.set(k);
                    valueRef.set(v);
                    return v == null ? "<null>" : v.toString().toUpperCase(Locale.ROOT);
                  });

      Object result = filter.getFunction().invoke("myKey", "abc");
      assertEquals("ABC", result);
      assertEquals("myKey", keyRef.get());
      assertEquals("abc", valueRef.get());
      // cover null branch
      Object nullResult = filter.getFunction().invoke("nullKey", null);
      assertEquals("<null>", nullResult);
    }

    @Test
    void kotlin_function2_constructor_invokes_lambda() {
      AtomicReference<String> keyRef = new AtomicReference<>();
      AtomicReference<Object> valueRef = new AtomicReference<>();

      FunctionFilter filter =
          new FunctionFilter(
              (kotlin.jvm.functions.Function2<String, Object, Object>)
                  (k, v) -> {
                    keyRef.set(k);
                    valueRef.set(v);
                    return v == null ? null : (v.toString() + "!");
                  });

      Object result = filter.getFunction().invoke("anotherKey", "hey");
      assertEquals("hey!", result);
      assertEquals("anotherKey", keyRef.get());
      assertEquals("hey", valueRef.get());
      // cover null branch
      Object nullResult = filter.getFunction().invoke("nullKey2", null);
      assertNull(nullResult);
    }

    @Test
    void static_factory_from_wraps_BiFunction() {
      BiFunction<String, Object, Object> biFn = (k, v) -> k + ":" + v;
      FunctionFilter filter = FunctionFilter.from(biFn);
      Object result = filter.getFunction().invoke("k", 123);
      assertEquals("k:123", result);
    }

    @Test
    void constructor_handles_null_values() {
      FunctionFilter filter =
          new FunctionFilter((BiFunction<String, Object, Object>) (k, v) -> v == null ? 42 : v);
      Object result = filter.getFunction().invoke("ignored", null);
      assertEquals(42, result);
    }
  }

  @Nested
  @DisplayName("IterableFilter Java interop")
  class IterableFilterTests {
    @Test
    void array_constructor_wraps_array_as_iterable() {
      Object[] arr = new Object[] {"a", 1, null};
      IterableFilter filter = new IterableFilter(arr);
      assertIterableEquals(Arrays.asList("a", 1, null), toList(filter.getIterable()));
    }

    @Test
    void collection_constructor_wraps_collection() {
      Collection<String> coll = new LinkedList<>();
      coll.add("x");
      coll.add("y");
      IterableFilter filter = new IterableFilter(coll);
      assertIterableEquals(Arrays.asList("x", "y"), toList(filter.getIterable()));
    }

    @Test
    void static_factory_varargs_of() {
      IterableFilter filter = IterableFilter.of("p", "q", "r");
      assertIterableEquals(Arrays.asList("p", "q", "r"), toList(filter.getIterable()));
    }

    @Test
    void static_factory_from_collection() {
      List<Integer> nums = Arrays.asList(10, 20, 30);
      IterableFilter filter = IterableFilter.from(nums);
      assertIterableEquals(nums, toList(filter.getIterable()));
    }

    @Test
    void empty_inputs_supported() {
      IterableFilter fromEmptyArray = new IterableFilter(new Object[] {});
      assertFalse(fromEmptyArray.getIterable().iterator().hasNext());

      IterableFilter fromEmptyCollection = IterableFilter.from(Collections.emptyList());
      assertFalse(fromEmptyCollection.getIterable().iterator().hasNext());

      IterableFilter fromVarargs = IterableFilter.of();
      assertFalse(fromVarargs.getIterable().iterator().hasNext());
    }
  }
}
