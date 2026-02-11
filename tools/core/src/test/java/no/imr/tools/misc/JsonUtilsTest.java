package no.imr.tools.misc;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.InvalidNullException;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class JsonUtilsTest {
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
      assertThrows(InvalidNullException.class, () -> JsonUtils.parseSet("[1, 2, 2, 3, null]", Object.class));
   }

   @Test
   void parseMap() {
      assertEquals(Map.of("a", 1, "b", true), JsonUtils.parseMap("{ \"a\": 1, \"b\": true }"));
      assertThrows(InvalidNullException.class, () -> JsonUtils.parseMap("{ \"a\": 1, \"b\": null }"));
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
