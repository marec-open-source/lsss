package no.imr.tools.range;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class IntRangeTest {
   @Test
   void wrongOrder() {
      assertThrows(IllegalArgumentException.class, () -> {
         new IntRange(1, 0);
      });
   }

   @Test
   void equalsTest() {
      IntRange range = new IntRange(1, 5);
      assertEquals(range, new IntRange(1, 5));
      assertNotEquals(range, new IntRange(1, 6));
   }

   @Test
   void hashCodeTest() {
      IntRange range = new IntRange(1, 5);
      assertEquals(range.hashCode(), new IntRange(1, 5).hashCode());
   }

   @Test
   void containsExcludingBegin() {
      IntRange range = new IntRange(1, 5);
      assertFalse(range.containsExcludingBegin(0));
      assertFalse(range.containsExcludingBegin(1));
      assertTrue(range.containsExcludingBegin(2));
      assertTrue(range.containsExcludingBegin(4));
      assertFalse(range.containsExcludingBegin(5));
   }

   @Test
   void clamp() {
      IntRange range = new IntRange(1, 5);
      assertEquals(1, range.clamp(0));
      assertEquals(1, range.clamp(1));
      assertEquals(2, range.clamp(2));
      assertEquals(5, range.clamp(5));
      assertEquals(5, range.clamp(6));

      range = new IntRange(1, 1);
      assertEquals(1, range.clamp(0));
      assertEquals(1, range.clamp(1));
      assertEquals(1, range.clamp(2));
   }
}
