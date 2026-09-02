package no.imr.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MaxTest {
   @Test
   void test() {
      assertEquals(2, Max.of(1, 2));
      assertEquals(2L, Max.of(1L, 2L));
      assertEquals(2f, Max.of(1f, 2f));
      assertEquals(2.0, Max.of(1.0, 2.0));

      assertEquals(3, Max.of(1, 2, 3));
      assertEquals(3, Max.of(3, 1, 2));
      assertEquals(3, Max.of(2, 3, 1));

      assertEquals(3L, Max.of(1L, 2L, 3L));
      assertEquals(3L, Max.of(3L, 1L, 2L));
      assertEquals(3L, Max.of(2L, 3L, 1L));

      assertEquals(3f, Max.of(1f, 2f, 3f));
      assertEquals(3f, Max.of(3f, 1f, 2f));
      assertEquals(3f, Max.of(2f, 3f, 1f));
      assertEquals(Float.NaN, Max.of(2f, 3f, Float.NaN));

      assertEquals(3.0, Max.of(1.0, 2.0, 3.0));
      assertEquals(3.0, Max.of(3.0, 1.0, 2.0));
      assertEquals(3.0, Max.of(2.0, 3.0, 1.0));
      assertEquals(Double.NaN, Max.of(2.0, 3.0, Double.NaN));

      assertEquals(3, Max.of(new byte[]{1, 2, 3, 4}, 1, 3));

      assertEquals(4, Max.of(1, 2, 3, 4));
      assertEquals(3, Max.of(new int[]{1, 2, 3, 4}, 1, 3));
      assertEquals(Float.NaN, Max.of(new float[]{1f, Float.NaN, 3f, 4f}, 1, 3));

      assertEquals(4f, Max.of(1f, 2f, 3f, 4f));
      assertEquals(3f, Max.of(new float[]{1f, 2f, 3f, 4f}, 1, 3));

      assertEquals("b", Max.of("a", "b"));
      assertEquals("b", Max.of("b", "a"));
   }
}
