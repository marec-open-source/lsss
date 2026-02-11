package no.imr.tools.range;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class RangeUtilsTest {
   @Test
   void replaceValues() {
      RangeMap<Integer, String> rangeMap = new ArrayRangeMap<>();
      rangeMap.put(0, 1, "x");
      rangeMap.put(1, 2, "r1");
      rangeMap.put(2, 3, "r2");
      rangeMap.put(3, 4, "x");
      rangeMap.put(5, 6, "y");
      RangeUtils.replaceValues(rangeMap, r(1, 3), "x", s -> s.startsWith("r"));
      assertEquals(List.of(e(0, 4, "x"), e(5, 6, "y")), rangeMap.stream().toList());
   }

   @Test
   void getValueSet() {
      RangeMap<Integer, String> rangeMap = new ArrayRangeMap<>();
      rangeMap.put(0, 5, "a");
      rangeMap.put(6, 10, "b");
      assertEquals(Set.of(), RangeUtils.getValueSet(rangeMap, r(4, 4)));
      assertEquals(Set.of("a"), RangeUtils.getValueSet(rangeMap, r(4, 5)));
      assertEquals(Set.of("a"), RangeUtils.getValueSet(rangeMap, r(4, 6)));
      assertEquals(Set.of("a", "b"), RangeUtils.getValueSet(rangeMap, r(4, 7)));
   }

   @Test
   void getCompletelyMappedSingleValue() {
      RangeMap<Integer, String> rangeMap = new ArrayRangeMap<>();
      rangeMap.put(0, 5, "a");
      rangeMap.put(6, 10, "b");
      assertNull(RangeUtils.getCompletelyMappedSingleValue(rangeMap, r(4, 4)));
      assertEquals("a", RangeUtils.getCompletelyMappedSingleValue(rangeMap, r(4, 5)));
      assertNull(RangeUtils.getCompletelyMappedSingleValue(rangeMap, r(4, 6)));
      assertNull(RangeUtils.getCompletelyMappedSingleValue(rangeMap, r(4, 7)));
   }

   @Test
   void subList() {
      List<Integer> list = List.of(1, 2, 5, 8, 9);
      assertEquals(List.of(), RangeUtils.subList(list, r(1, 1)));
      assertEquals(List.of(), RangeUtils.subList(list, r(0, 1)));
      assertEquals(List.of(), RangeUtils.subList(list, r(6, 8)));
      assertEquals(list, RangeUtils.subList(list, r(1, 10)));
      assertEquals(List.of(2, 5, 8), RangeUtils.subList(list, r(2, 9)));
      assertEquals(List.of(5), RangeUtils.subList(list, r(3, 8)));
   }

   @Test
   void toComplement() {
      RangeSet<Integer> rangeSet = new ArrayRangeSet<>();
      assertEquals(List.of(r(1, 10)),
            RangeUtils.toComplement(rangeSet, r(1, 10)).stream().toList());
      rangeSet.add(2, 3);
      assertEquals(List.of(r(1, 2), r(3, 10)),
            RangeUtils.toComplement(rangeSet, r(1, 10)).stream().toList());
      rangeSet.add(-1, 0);
      rangeSet.add(9, 11);
      assertEquals(List.of(r(1, 2), r(3, 9)),
            RangeUtils.toComplement(rangeSet, r(1, 10)).stream().toList());
   }

   @Test
   void emptyRangeMap() {
      RangeMap<Integer, Object> m = RangeUtils.emptyRangeMap();
      assertThrows(UnsupportedOperationException.class, () -> m.put(r(0, 1), ""));
      assertThrows(UnsupportedOperationException.class, () -> m.remove(r(0, 1)));
      assertNull(m.get(1));
      assertThrows(UnsupportedOperationException.class, m::clear);
      assertTrue(m.isEmpty());
      assertEquals(0, m.size());
      assertFalse(m.iterator().hasNext());
      assertEquals(List.of(), m.stream().toList());
      assertEquals(List.of(), m.stream(r(0, 1)).toList());
   }

   @Test
   void emptyRangeSet() {
      RangeSet<Integer> m = RangeUtils.emptyRangeSet();
      assertThrows(UnsupportedOperationException.class, () -> m.add(r(0, 1)));
      assertThrows(UnsupportedOperationException.class, () -> m.remove(r(0, 1)));
      assertFalse(m.contains(1));
      assertThrows(UnsupportedOperationException.class, m::clear);
      assertTrue(m.isEmpty());
      assertEquals(0, m.size());
      assertFalse(m.iterator().hasNext());
      assertEquals(List.of(), m.stream().toList());
      assertEquals(List.of(), m.stream(r(0, 1)).toList());
   }

   private static <K extends Comparable<? super K>, V> RangeMap.Entry<K, V> e(K begin, K end, V value) {
      return new RangeMap.Entry<>(r(begin, end), value);
   }

   private static <T extends Comparable<? super T>> Range<T> r(T begin, T end) {
      return new DefaultRange<>(begin, end);
   }
}
