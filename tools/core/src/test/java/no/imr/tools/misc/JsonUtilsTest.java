package no.imr.tools.misc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class JsonUtilsTest {
   @Test
   void compactJsonWithoutNewline() throws JsonProcessingException {
      Object object = Map.of("a", "b");
      String json = JsonUtils.JSON_MAPPER.writeValueAsString(object);
      assertFalse(json.contains("\r"), json);
      assertFalse(json.contains("\n"), json);
   }

   @Test
   void prettyJsonWithoutCarriageReturn() throws JsonProcessingException {
      Object object = Map.of("a", "b");
      String json = JsonUtils.PRETTY_PRINTER.writeValueAsString(object);
      assertFalse(json.contains("\r"), json);
      assertTrue(json.contains("\n"), json);
   }

   @Test
   void parseSet() throws IOException {
      assertEquals(Set.of(1, 2, 3), JsonUtils.parseSet("[1, 2, 2, 3]", Integer.class));
      assertThrows(InvalidNullException.class, () -> JsonUtils.parseSet("[1, 2, 2, 3, null]", Object.class));
   }

   @Test
   void parseMap() throws IOException {
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
