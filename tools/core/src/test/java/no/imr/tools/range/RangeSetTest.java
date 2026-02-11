package no.imr.tools.range;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class RangeSetTest {
   @Test
   void containsAll() {
      RangeSet<Integer> s = new ArrayRangeSet<>();

      assertTrue(s.containsAll(r(3, 3)));
      assertFalse(s.containsAll(r(3, 7)));

      s.add(r(1, 9));
      assertTrue(s.containsAll(r(3, 7)));

      s.remove(r(5, 9));
      assertFalse(s.containsAll(r(3, 7)));

      s.add(r(5, 9));
      assertTrue(s.containsAll(r(3, 7)));
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

   @Test
   void addAll() {
      RangeSet<Integer> a = new ArrayRangeSet<>();
      a.add(0, 10);
      a.add(20, 30);

      RangeSet<Integer> b = new ArrayRangeSet<>();
      b.add(10, 11);
      b.add(25, 26);
      b.add(35, 36);

      a.addAll(b);
      assertEquals(List.of(r(0, 11), r(20, 30), r(35, 36)), a.stream().toList());
   }

   @Test
   void removeAll() {
      RangeSet<Integer> a = new ArrayRangeSet<>();
      a.add(0, 10);
      a.add(20, 30);

      RangeSet<Integer> b = new ArrayRangeSet<>();
      b.add(0, 5);
      b.add(25, 26);
      b.add(35, 36);

      a.removeAll(b);
      assertEquals(List.of(r(5, 10), r(20, 25), r(26, 30)), a.stream().toList());
   }

   private static <T extends Comparable<? super T>> Range<T> r(T begin, T end) {
      return new DefaultRange<>(begin, end);
   }
}
