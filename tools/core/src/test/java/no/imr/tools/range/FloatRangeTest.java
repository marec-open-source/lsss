package no.imr.tools.range;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class FloatRangeTest {
   @Test
   void of() {
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.of(2, 0));
   }

   @Test
   void ofUnsorted() {
      assertEquals(FloatRange.of(0, 2), FloatRange.ofUnsorted(2, 0));
   }

   @Test
   void ofCenterAndSize() {
      assertEquals(FloatRange.of(0, 2), FloatRange.ofCenterAndSize(1, 2));
   }

   @Test
   void ofCenterAndRadius() {
      assertEquals(FloatRange.of(0, 2), FloatRange.ofCenterAndRadius(1, 1));
   }

   @Test
   void equalsTest() {
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.of(0, 0));
      FloatRange range = FloatRange.of(1, 1);
      assertEquals(range, FloatRange.of(1, 1));
      assertNotEquals(range, FloatRange.of(1, 2));
      assertNotEquals(range, FloatRange.of(0, 1));
   }

   @Test
   void hashCodeTest() {
      assertEquals(FloatRange.of(0, 0).hashCode(), FloatRange.EMPTY_RANGE.hashCode());
      assertEquals(FloatRange.of(1, 1).hashCode(), FloatRange.of(1, 1).hashCode());
   }

   @Test
   void containsValue() {
      assertFalse(FloatRange.EMPTY_RANGE.contains(0));
      assertTrue(FloatRange.of(0, 1).contains(0));
      assertTrue(FloatRange.of(0, 1).contains(0.5f));
      assertTrue(FloatRange.of(0, 1).contains(Math.nextDown(1)));
      assertFalse(FloatRange.of(0, 1).contains(1));
   }

   @Test
   void containsExcludingBegin() {
      assertFalse(FloatRange.EMPTY_RANGE.containsExcludingBegin(0));
      assertFalse(FloatRange.of(0, 1).containsExcludingBegin(0));
      assertTrue(FloatRange.of(0, 1).containsExcludingBegin(Math.nextUp(0)));
      assertTrue(FloatRange.of(0, 1).containsExcludingBegin(0.5f));
      assertTrue(FloatRange.of(0, 1).containsExcludingBegin(Math.nextDown(1)));
      assertFalse(FloatRange.of(0, 1).containsExcludingBegin(1));
   }

   @Test
   void containsIncludingEnd() {
      assertTrue(FloatRange.EMPTY_RANGE.containsIncludingEnd(0));
      assertTrue(FloatRange.of(0, 1).containsIncludingEnd(0));
      assertTrue(FloatRange.of(0, 1).containsIncludingEnd(0.5f));
      assertTrue(FloatRange.of(0, 1).containsIncludingEnd(1));
   }

   @Test
   void containsRange() {
      assertTrue(FloatRange.EMPTY_RANGE.contains(FloatRange.of(0, 0)));
      assertTrue(FloatRange.EMPTY_RANGE.contains(FloatRange.of(1, 1)));
      assertFalse(FloatRange.EMPTY_RANGE.contains(FloatRange.of(0, 1)));
      assertTrue(FloatRange.of(0, 1).contains(FloatRange.of(0, 1)));
      assertTrue(FloatRange.of(0, 1).contains(FloatRange.EMPTY_RANGE));
      assertFalse(FloatRange.of(0, 1).contains(FloatRange.of(-1, 1)));
      assertFalse(FloatRange.of(0, 1).contains(FloatRange.of(0, 2)));
   }

   @Test
   void intersects() {
      assertFalse(FloatRange.of(1, 1).intersects(FloatRange.EMPTY_RANGE));
      assertFalse(FloatRange.of(0, 0).intersects(FloatRange.EMPTY_RANGE));
      assertFalse(FloatRange.of(0, 1).intersects(FloatRange.EMPTY_RANGE));
      assertFalse(FloatRange.of(0, 1).intersects(FloatRange.of(1, 2)));
      assertTrue(FloatRange.of(0, 1).intersects(FloatRange.of(0.9f, 2)));
      assertFalse(FloatRange.of(0, 2).intersects(FloatRange.of(1, 1)));
      assertFalse(FloatRange.of(1, 1).intersects(FloatRange.of(0, 2)));
   }

   @Test
   void intersection() {
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE.intersection(FloatRange.EMPTY_RANGE));
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.of(1, 1).intersection(FloatRange.EMPTY_RANGE));
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE.intersection(FloatRange.of(1, 1)));
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE.intersection(FloatRange.of(0, 1)));
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.of(0, 1).intersection(FloatRange.EMPTY_RANGE));
      assertEquals(FloatRange.of(1, 1), FloatRange.of(0, 1).intersection(FloatRange.of(1, 2)));
      assertEquals(FloatRange.of(1, 2), FloatRange.of(0, 2).intersection(FloatRange.of(1, 3)));
   }

   @Test
   void union() {
      assertEquals(FloatRange.of(2, 3), FloatRange.EMPTY_RANGE.union(FloatRange.of(2, 3)));
      assertEquals(FloatRange.of(0, 3), FloatRange.of(0, 1).union(FloatRange.of(2, 3)));
   }

   @Test
   void distanceTo() {
      FloatRange r = FloatRange.of(2, 3);
      assertEquals(2, r.distanceTo(0));
      assertEquals(0, r.distanceTo(2));
      assertEquals(0, r.distanceTo(2.5f));
      assertEquals(0, r.distanceTo(3));
      assertEquals(1, r.distanceTo(4));

      assertEquals(0, FloatRange.EMPTY_RANGE.distanceTo(0));
      assertEquals(4, FloatRange.EMPTY_RANGE.distanceTo(4));
   }

   @Test
   void fraction() {
      FloatRange r = FloatRange.of(0, 10);
      assertEquals(-0.1f, r.valueToFraction(-1));
      assertEquals(0, r.valueToFraction(0));
      assertEquals(0.1f, r.valueToFraction(1));
      assertEquals(1, r.valueToFraction(10));
      assertEquals(2, r.valueToFraction(20));

      assertEquals(-1, r.fractionToValue(-0.1f));
      assertEquals(0, r.fractionToValue(0));
      assertEquals(1, r.fractionToValue(0.1f));
      assertEquals(10, r.fractionToValue(1));
      assertEquals(20, r.fractionToValue(2));

      r = FloatRange.of(1, 1);
      assertEquals(Float.NaN, r.valueToFraction(0));
      assertEquals(0, r.valueToFraction(1));
      assertEquals(Float.NaN, r.valueToFraction(2));
      assertEquals(1, r.fractionToValue(-1));
      assertEquals(1, r.fractionToValue(0));
      assertEquals(1, r.fractionToValue(1));
      assertEquals(1, r.fractionToValue(2));

      r = FloatRange.of(-34, 5);
      assertEquals(4.5, r.fractionToValue(r.valueToFraction(4.5f)));
   }

   @Test
   void expandToNonDegenerated() {
      assertEquals(FloatRange.of(1, 2), FloatRange.of(1, 2).expandToNonDegenerated());

      FloatRange r = FloatRange.of(1, 1).expandToNonDegenerated();
      assertTrue(r.contains(1));
      assertFalse(r.contains(Math.nextDown(1)));
      assertFalse(r.contains(Math.nextUp(1)));

      assertTrue(FloatRange.of(-0, -0).expandToNonDegenerated().contains(0));
      assertTrue(FloatRange.of(0, 0).expandToNonDegenerated().contains(-0));
      assertTrue(FloatRange.of(-1, -1).expandToNonDegenerated().contains(-1));
   }

   @Test
   void expandToIncludeMax() {
      assertTrue(FloatRange.of(1, 1).expandToIncludeMax().contains(1));
      assertTrue(FloatRange.of(1, 2).expandToIncludeMax().contains(2));
   }

   @Test
   void expandToMultipleOf() {
      assertEquals(FloatRange.of(0, 2), FloatRange.of(1, 1).expandToMultipleOf(2));
      assertEquals(FloatRange.of(0, 2), FloatRange.of(0, 2).expandToMultipleOf(2));
      assertEquals(FloatRange.of(-10, 20), FloatRange.of(-1, 11).expandToMultipleOf(10));
   }

   @Test
   void shrinkToMultipleOf() {
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE.shrinkToMultipleOf(2));
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.of(1, 1).shrinkToMultipleOf(2));
      assertEquals(FloatRange.of(2, 4), FloatRange.of(1, 5).shrinkToMultipleOf(2));
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.of(2, 7).shrinkToMultipleOf(10));
   }

   @Test
   void roundToMultipleOf() {
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE.roundToMultipleOf(2));
      assertEquals(FloatRange.of(2, 2), FloatRange.of(1, 1).roundToMultipleOf(2));
      assertEquals(FloatRange.of(2, 4), FloatRange.of(2, 3).roundToMultipleOf(2));
      assertEquals(FloatRange.of(0, 10), FloatRange.of(2, 7).roundToMultipleOf(10));
   }

   @Test
   void shift() {
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE.shift(1));
      assertEquals(FloatRange.of(1, 1), FloatRange.of(1, 1).shift(1));
      assertEquals(FloatRange.of(3, 4), FloatRange.of(2, 3).shift(1));
      assertEquals(FloatRange.of(1, 2), FloatRange.of(2, 3).shift(-1));
   }

   @Test
   void shiftToBeContainedIn() {
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE.shiftToBeContainedIn(FloatRange.of(0, 2)));
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.of(0, 2).shiftToBeContainedIn(FloatRange.EMPTY_RANGE));
      assertEquals(FloatRange.of(4, 5), FloatRange.of(0, 3).shiftToBeContainedIn(FloatRange.of(4, 5)));
      assertEquals(FloatRange.of(4, 7), FloatRange.of(0, 3).shiftToBeContainedIn(FloatRange.of(4, 10)));
      assertEquals(FloatRange.of(4, 5), FloatRange.of(8, 11).shiftToBeContainedIn(FloatRange.of(4, 5)));
      assertEquals(FloatRange.of(7, 10), FloatRange.of(8, 11).shiftToBeContainedIn(FloatRange.of(4, 10)));
   }

   @Test
   void add() {
      assertEquals(FloatRange.of(2, 2), FloatRange.EMPTY_RANGE.add(2));
      assertEquals(FloatRange.of(3, 3), FloatRange.of(1, 1).add(2));
      assertEquals(FloatRange.of(3, 4), FloatRange.of(1, 2).add(2));
   }

   @Test
   void multiply() {
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE.multiply(2));
      assertEquals(FloatRange.of(2, 2), FloatRange.of(1, 1).multiply(2));
      assertEquals(FloatRange.of(2, 4), FloatRange.of(1, 2).multiply(2));
   }

   @Test
   void shrink() {
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE.shrink(1));
      assertEquals(FloatRange.of(1, 9), FloatRange.of(0, 10).shrink(1));
   }

   @Test
   void shrinkByFraction() {
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE.shrinkByFraction(0.1f));
      assertEquals(FloatRange.of(1, 9), FloatRange.of(0, 10).shrinkByFraction(0.1f));
   }

   @Test
   void zoom() {
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE.zoom(0.8f));
      assertEquals(FloatRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE.zoom(2));
      assertEquals(FloatRange.of(1, 9), FloatRange.of(0, 10).zoom(0.8f));
      assertEquals(FloatRange.of(-5, 15), FloatRange.of(0, 10).zoom(2));
   }

   @Test
   void zoomByReference() {
      assertEquals(FloatRange.of(-10, -10), FloatRange.EMPTY_RANGE.zoom(2, 10));
      assertEquals(FloatRange.of(-10, 10), FloatRange.of(0, 10).zoom(2, 10));
   }
}
