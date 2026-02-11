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
   void fraction() {
      assertEquals(0.1, DoubleRange.of(0, 10).valueToFraction(1), 1e-6);

      assertEquals(1, DoubleRange.of(0, 10).fractionToValue(0.1), 1e-6);

      DoubleRange r = DoubleRange.of(-34, 5);
      assertEquals(4.5, r.fractionToValue(r.valueToFraction(4.5)));
   }

   @Test
   void expandToMultipleOf() {
      assertEquals(DoubleRange.of(0, 2), DoubleRange.of(1, 1).expandToMultipleOf(2));
      assertEquals(DoubleRange.of(0, 2), DoubleRange.of(0, 2).expandToMultipleOf(2));
      assertEquals(DoubleRange.of(-10, 20), DoubleRange.of(-1, 11).expandToMultipleOf(10));
   }

   @Test
   void shift() {
      assertEquals(DoubleRange.EMPTY_RANGE, DoubleRange.EMPTY_RANGE.shift(1));
      assertEquals(DoubleRange.of(1, 1), DoubleRange.of(1, 1).shift(1));
      assertEquals(DoubleRange.of(3, 4), DoubleRange.of(2, 3).shift(1));
      assertEquals(DoubleRange.of(1, 2), DoubleRange.of(2, 3).shift(-1));
   }

   @Test
   void shiftToBeContainedIn() {
      assertEquals(DoubleRange.EMPTY_RANGE, DoubleRange.EMPTY_RANGE.shiftToBeContainedIn(DoubleRange.of(0, 2)));
      assertEquals(DoubleRange.EMPTY_RANGE, DoubleRange.of(0, 2).shiftToBeContainedIn(DoubleRange.EMPTY_RANGE));
      assertEquals(DoubleRange.of(4, 5), DoubleRange.of(0, 3).shiftToBeContainedIn(DoubleRange.of(4, 5)));
      assertEquals(DoubleRange.of(4, 7), DoubleRange.of(0, 3).shiftToBeContainedIn(DoubleRange.of(4, 10)));
      assertEquals(DoubleRange.of(4, 5), DoubleRange.of(8, 11).shiftToBeContainedIn(DoubleRange.of(4, 5)));
      assertEquals(DoubleRange.of(7, 10), DoubleRange.of(8, 11).shiftToBeContainedIn(DoubleRange.of(4, 10)));
   }
}
