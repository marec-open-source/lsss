package no.imr.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MinTest {
   @Test
   void test() {
      assertEquals(1, Min.of(1, 2));
      assertEquals(1L, Min.of(1L, 2L));
      assertEquals(1f, Min.of(1f, 2f));
      assertEquals(1.0, Min.of(1.0, 2.0));

      assertEquals(1, Min.of(1, 2, 3));
      assertEquals(1, Min.of(3, 1, 2));
      assertEquals(1, Min.of(2, 3, 1));

      assertEquals(1L, Min.of(1L, 2L, 3L));
      assertEquals(1L, Min.of(3L, 1L, 2L));
      assertEquals(1L, Min.of(2L, 3L, 1L));

      assertEquals(1f, Min.of(1f, 2f, 3f));
      assertEquals(1f, Min.of(3f, 1f, 2f));
      assertEquals(1f, Min.of(2f, 3f, 1f));

      assertEquals(1.0, Min.of(1.0, 2.0, 3.0));
      assertEquals(1.0, Min.of(3.0, 1.0, 2.0));
      assertEquals(1.0, Min.of(2.0, 3.0, 1.0));

      assertEquals(1, Min.of(1, 2, 3, 4));
      assertEquals(2, Min.of(new int[]{1, 2, 3, 4}, 1, 3));

      assertEquals(1f, Min.of(1f, 2f, 3f, 4f));
      assertEquals(2f, Min.of(new float[]{1f, 2f, 3f, 4f}, 1, 3));

      assertEquals("a", Min.of("a", "b"));
      assertEquals("a", Min.of("b", "a"));
   }
}
