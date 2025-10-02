package no.imr.tools.range;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RangeSetTest {
   @Test
   void containsAll() {
      RangeSet<Integer> s = new ArrayRangeSet<>();

      assertTrue(s.containsAll(new DefaultRange<>(3, 3)));
      assertFalse(s.containsAll(new DefaultRange<>(3, 7)));

      s.add(new DefaultRange<>(1, 9));
      assertTrue(s.containsAll(new DefaultRange<>(3, 7)));

      s.remove(new DefaultRange<>(5, 9));
      assertFalse(s.containsAll(new DefaultRange<>(3, 7)));

      s.add(new DefaultRange<>(5, 9));
      assertTrue(s.containsAll(new DefaultRange<>(3, 7)));
   }

   @Test
   void size() {
      RangeSet<Integer> s = new ArrayRangeSet<>();
      assertEquals(0, s.size());
      s.add(1, 2);
      assertEquals(1, s.size());
      s.add(2, 3);
      assertEquals(1, s.size());
      s.add(4, 5);
      assertEquals(2, s.size());
      s.add(3, 4);
      assertEquals(1, s.size());
   }

   @Test
   void isEmpty() {
      RangeSet<Integer> s = new ArrayRangeSet<>();
      assertTrue(s.isEmpty());
      s.add(1, 2);
      assertFalse(s.isEmpty());
      s.clear();
      assertTrue(s.isEmpty());
   }
}
