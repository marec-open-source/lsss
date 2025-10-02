package no.imr.tools.range;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class RangeUtilsTest {
   @Test
   void getValueSet() {
      RangeMap<Integer, String> rangeMap = new ArrayRangeMap<>();
      rangeMap.put(0, 5, "a");
      rangeMap.put(6, 10, "b");
      assertEquals(Set.of(), RangeUtils.getValueSet(rangeMap, new DefaultRange<>(4, 4)));
      assertEquals(Set.of("a"), RangeUtils.getValueSet(rangeMap, new DefaultRange<>(4, 5)));
      assertEquals(Set.of("a"), RangeUtils.getValueSet(rangeMap, new DefaultRange<>(4, 6)));
      assertEquals(Set.of("a", "b"), RangeUtils.getValueSet(rangeMap, new DefaultRange<>(4, 7)));
   }

   @Test
   void getCompletelyMappedSingleValue() {
      RangeMap<Integer, String> rangeMap = new ArrayRangeMap<>();
      rangeMap.put(0, 5, "a");
      rangeMap.put(6, 10, "b");
      assertNull(RangeUtils.getCompletelyMappedSingleValue(rangeMap, new DefaultRange<>(4, 4)));
      assertEquals("a", RangeUtils.getCompletelyMappedSingleValue(rangeMap, new DefaultRange<>(4, 5)));
      assertNull(RangeUtils.getCompletelyMappedSingleValue(rangeMap, new DefaultRange<>(4, 6)));
      assertNull(RangeUtils.getCompletelyMappedSingleValue(rangeMap, new DefaultRange<>(4, 7)));
   }

   @Test
   void subList() {
      List<Integer> list = List.of(1, 2, 5, 8, 9);
      assertEquals(List.of(), RangeUtils.subList(list, new DefaultRange<>(1, 1)));
      assertEquals(List.of(), RangeUtils.subList(list, new DefaultRange<>(0, 1)));
      assertEquals(List.of(), RangeUtils.subList(list, new DefaultRange<>(6, 8)));
      assertEquals(list, RangeUtils.subList(list, new DefaultRange<>(1, 10)));
      assertEquals(List.of(2, 5, 8), RangeUtils.subList(list, new DefaultRange<>(2, 9)));
      assertEquals(List.of(5), RangeUtils.subList(list, new DefaultRange<>(3, 8)));
   }

   @Test
   void toComplement() {
      RangeSet<Integer> rangeSet = new ArrayRangeSet<>();
      assertEquals(List.of(new DefaultRange<>(1, 10)),
            RangeUtils.toComplement(rangeSet, new DefaultRange<>(1, 10)).stream().toList());
      rangeSet.add(2, 3);
      assertEquals(List.of(new DefaultRange<>(1, 2), new DefaultRange<>(3, 10)),
            RangeUtils.toComplement(rangeSet, new DefaultRange<>(1, 10)).stream().toList());
      rangeSet.add(-1, 0);
      rangeSet.add(9, 11);
      assertEquals(List.of(new DefaultRange<>(1, 2), new DefaultRange<>(3, 9)),
            RangeUtils.toComplement(rangeSet, new DefaultRange<>(1, 10)).stream().toList());
   }

   @Test
   void emptyRangeMap() {
      RangeMap<Integer, Object> m = RangeUtils.emptyRangeMap();
      assertNull(m.get(1));
      assertEquals(0, m.size());
      assertFalse(m.iterator().hasNext());
      assertThrows(UnsupportedOperationException.class, () -> {
         m.put(new DefaultRange<>(0, 5), "");
      });
   }
}
