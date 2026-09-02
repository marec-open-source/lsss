package no.imr.tools.misc;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class JsonUtilsTest {
   @Test
   void readValue() {
      assertEquals(new TestPojo(1, "b", null, new TestNestedPojo(1, null)), JsonUtils.JSON_MAPPER.readValue("""
            {"a":1, "b":"b", "d":{"x": 1}}
            """, TestPojo.class));
      assertEquals(new TestPojo(1, "b", null, new TestNestedPojo(1, null)), JsonUtils.JSON_MAPPER.readValue("""
            {"a":1, "b":"b", "c":null, "d":{"x":1, "y":null}}
            """, TestPojo.class));
   }

   @Test
   void compactJsonWithoutNewline() {
      Object object = Map.of("a", "b");
      String json = JsonUtils.JSON_MAPPER.writeValueAsString(object);
      assertFalse(json.contains("\r"), json);
      assertFalse(json.contains("\n"), json);
   }

   @Test
   void prettyJsonWithoutCarriageReturn() {
      Object object = Map.of("a", "b");
      String json = JsonUtils.PRETTY_PRINTER.writeValueAsString(object);
      assertFalse(json.contains("\r"), json);
      assertTrue(json.contains("\n"), json);
   }

   @Test
   void parseSet() {
      assertEquals(Set.of(1, 2, 3), JsonUtils.parseSet("[1, 2, 2, 3]", Integer.class));
      assertEquals(Set.of(1, 2, 3), JsonUtils.parseSet("[1, 2, 2, 3, null]", Object.class));
   }

   @Test
   void parseMap() {
      assertEquals(Map.of("a", 1, "b", true), JsonUtils.parseMap("{ \"a\": 1, \"b\": true }"));
      assertEquals(Map.of("a", 1), JsonUtils.parseMap("{ \"a\": 1, \"b\": null }"));
   }

   @Test
   void convertToMap() {
      TestPojo value = new TestPojo(5, "abc", null, new TestNestedPojo(8, null));
      assertEquals(Map.of("a", 5, "b", "abc", "d", Map.of("x", 8)), JsonUtils.convertToMap(value));
   }

   public record TestPojo(int a, String b, @Nullable Object c, TestNestedPojo d) {
   }

   public record TestNestedPojo(int x, @Nullable Object y) {
   }
}
