package no.imr.tools.range;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultRangeTest {
   @Test
   void wrongOrder() {
      assertThrows(IllegalArgumentException.class, () -> {
         new DefaultRange<>(1, 0);
      });
   }

   @Test
   void equalsTest() {
      Range<Integer> range = new DefaultRange<>(1, 5);
      assertEquals(range, range);
      assertEquals(range, new DefaultRange<>(1, 5));
      assertNotEquals(range, new DefaultRange<>(1, 6));
   }

   @Test
   void hashCodeTest() {
      Range<Integer> range = new DefaultRange<>(1, 5);
      assertEquals(range.hashCode(), new DefaultRange<>(1, 5).hashCode());
   }

   @Test
   void isBeginOrEnd() {
      Range<Integer> range = new DefaultRange<>(1, 5);
      assertTrue(range.isBeginOrEnd(1));
      assertTrue(range.isBeginOrEnd(5));
      assertFalse(range.isBeginOrEnd(0));
      assertFalse(range.isBeginOrEnd(4));
   }

   @Test
   void containsValue() {
      Range<Integer> range = new DefaultRange<>(1, 5);
      assertFalse(range.contains(0));
      assertTrue(range.contains(1));
      assertTrue(range.contains(4));
      assertFalse(range.contains(5));
   }

   @Test
   void containsExcludingBegin() {
      Range<Integer> range = new DefaultRange<>(1, 5);
      assertFalse(range.containsExcludingBegin(0));
      assertFalse(range.containsExcludingBegin(1));
      assertTrue(range.containsExcludingBegin(2));
      assertTrue(range.containsExcludingBegin(4));
      assertFalse(range.containsExcludingBegin(5));
   }

   @Test
   void containsIncludingEnd() {
      Range<Integer> range = new DefaultRange<>(1, 5);
      assertFalse(range.containsIncludingEnd(0));
      assertTrue(range.containsIncludingEnd(1));
      assertTrue(range.containsIncludingEnd(5));
      assertFalse(range.containsIncludingEnd(6));
   }

   @Test
   void containsRange() {
      Range<Integer> range = new DefaultRange<>(1, 5);
      assertTrue(range.contains(range));
      assertTrue(range.contains(new DefaultRange<>(2, 2)));
      assertTrue(range.contains(new DefaultRange<>(1, 5)));
      assertTrue(range.contains(new DefaultRange<>(2, 3)));
      assertFalse(range.contains(new DefaultRange<>(1, 6)));
      assertFalse(range.contains(new DefaultRange<>(-3, 9)));
   }

   @Test
   void intersects() {
      Range<Integer> range = new DefaultRange<>(1, 5);
      assertFalse(range.intersects(new DefaultRange<>(0, 1)));
      assertTrue(range.intersects(new DefaultRange<>(0, 2)));
      assertTrue(range.intersects(new DefaultRange<>(0, 10)));
      assertFalse(range.intersects(new DefaultRange<>(5, 10)));
      assertFalse(range.intersects(new DefaultRange<>(2, 2)));
      assertFalse(new DefaultRange<>(2, 2).intersects(range));
   }

   @Test
   void union() {
      assertEquals(new DefaultRange<>(1, 6), new DefaultRange<>(1, 2).union(new DefaultRange<>(5, 6)));
      assertEquals(new DefaultRange<>(1, 6), new DefaultRange<>(5, 6).union(new DefaultRange<>(1, 2)));
      assertEquals(new DefaultRange<>(1, 6), new DefaultRange<>(1, 6).union(new DefaultRange<>(2, 3)));
      assertEquals(new DefaultRange<>(1, 6), new DefaultRange<>(2, 3).union(new DefaultRange<>(1, 6)));

      assertEquals(new DefaultRange<>(1, 6), new DefaultRange<>(0, 0).union(new DefaultRange<>(1, 6)));
      assertEquals(new DefaultRange<>(1, 6), new DefaultRange<>(1, 6).union(new DefaultRange<>(0, 0)));
   }

   @Test
   void clamp() {
      Range<Integer> range = new DefaultRange<>(1, 5);
      assertEquals(1, range.clamp(0));
      assertEquals(1, range.clamp(1));
      assertEquals(2, range.clamp(2));
      assertEquals(5, range.clamp(5));
      assertEquals(5, range.clamp(6));

      range = new DefaultRange<>(1, 1);
      assertEquals(1, range.clamp(0));
      assertEquals(1, range.clamp(1));
      assertEquals(1, range.clamp(2));
   }
}
