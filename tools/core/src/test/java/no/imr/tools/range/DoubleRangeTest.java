package no.imr.tools.range;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DoubleRangeTest {
   @Test
   void of() {
      assertEquals(DoubleRange.EMPTY_RANGE, DoubleRange.of(2, 0));
   }

   @Test
   void equalsTest() {
      assertEquals(DoubleRange.EMPTY_RANGE, DoubleRange.of(0, 0));
      DoubleRange range = DoubleRange.of(1, 1);
      assertEquals(range, DoubleRange.of(1, 1));
      assertNotEquals(range, DoubleRange.of(1, 2));
      assertNotEquals(range, DoubleRange.of(0, 1));
   }

   @Test
   void hashCodeTest() {
      assertEquals(DoubleRange.of(0, 0).hashCode(), DoubleRange.EMPTY_RANGE.hashCode());
      assertEquals(DoubleRange.of(1, 1).hashCode(), DoubleRange.of(1, 1).hashCode());
   }

   @Test
   void containsValue() {
      assertFalse(DoubleRange.EMPTY_RANGE.contains(0));
      assertTrue(DoubleRange.of(0, 1).contains(0));
      assertTrue(DoubleRange.of(0, 1).contains(0.5f));
      assertTrue(DoubleRange.of(0, 1).contains(Math.nextDown(1)));
      assertFalse(DoubleRange.of(0, 1).contains(1));
   }

   @Test
   void containsRange() {
      assertTrue(DoubleRange.EMPTY_RANGE.contains(DoubleRange.of(0, 0)));
      assertTrue(DoubleRange.EMPTY_RANGE.contains(DoubleRange.of(1, 1)));
      assertFalse(DoubleRange.EMPTY_RANGE.contains(DoubleRange.of(0, 1)));
      assertTrue(DoubleRange.of(0, 1).contains(DoubleRange.of(0, 1)));
      assertTrue(DoubleRange.of(0, 1).contains(DoubleRange.EMPTY_RANGE));
      assertFalse(DoubleRange.of(0, 1).contains(DoubleRange.of(-1, 1)));
      assertFalse(DoubleRange.of(0, 1).contains(DoubleRange.of(0, 2)));
   }

   @Test
   void intersects() {
      assertFalse(DoubleRange.of(1, 1).intersects(DoubleRange.EMPTY_RANGE));
      assertFalse(DoubleRange.of(0, 0).intersects(DoubleRange.EMPTY_RANGE));
      assertFalse(DoubleRange.of(0, 1).intersects(DoubleRange.EMPTY_RANGE));
      assertFalse(DoubleRange.of(0, 1).intersects(DoubleRange.of(1, 2)));
      assertTrue(DoubleRange.of(0, 1).intersects(DoubleRange.of(0.9, 2)));
      assertFalse(DoubleRange.of(0, 2).intersects(DoubleRange.of(1, 1)));
      assertFalse(DoubleRange.of(1, 1).intersects(DoubleRange.of(0, 2)));
   }

   @Test
   void clamp() {
      assertEquals(DoubleRange.of(0, 1), DoubleRange.of(0, 1).clamp(DoubleRange.of(-1, 2)));
      assertEquals(DoubleRange.of(0.5, 1), DoubleRange.of(0, 1).clamp(DoubleRange.of(0.5, 2)));
      assertEquals(DoubleRange.of(0, 0.5), DoubleRange.of(0, 1).clamp(DoubleRange.of(-1, 0.5)));
   }

   @Test
   void shift() {
      assertEquals(DoubleRange.of(1, 2), DoubleRange.of(0, 1).shift(1));
      assertEquals(DoubleRange.of(-2, -1), DoubleRange.of(0, 1).shift(-2));
   }
}
