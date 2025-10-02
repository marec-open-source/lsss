package no.imr.tools;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class ImmutableUtilsTest {
   // List --------------------------

   @Test
   void listAdd() {
      assertEquals(ImmutableList.of(1), ImmutableUtils.add(ImmutableList.of(), 1));
      assertEquals(ImmutableList.of(1, 2), ImmutableUtils.add(ImmutableList.of(1), 2));
   }

   @Test
   void listAddAtIndex() {
      assertEquals(ImmutableList.of(1), ImmutableUtils.add(ImmutableList.of(), 0, 1));
      assertEquals(ImmutableList.of(1, 2), ImmutableUtils.add(ImmutableList.of(1), 1, 2));
      assertEquals(ImmutableList.of(2, 1), ImmutableUtils.add(ImmutableList.of(1), 0, 2));
   }

   @Test
   void listRemove() {
      assertEquals(ImmutableList.of(), ImmutableUtils.remove(ImmutableList.of(0), 0));
      assertEquals(ImmutableList.of(1), ImmutableUtils.remove(ImmutableList.of(0, 1), 0));
      assertEquals(ImmutableList.of(0), ImmutableUtils.remove(ImmutableList.of(0, 1), 1));
   }

   @Test
   void listRemoveAll() {
      assertEquals(ImmutableList.of(), ImmutableUtils.removeAll(ImmutableList.of(0), ImmutableSet.of(0)));
      assertEquals(ImmutableList.of(1), ImmutableUtils.removeAll(ImmutableList.of(0, 1), ImmutableSet.of(0)));
      assertEquals(ImmutableList.of(0, 2, 3, 6), ImmutableUtils.removeAll(ImmutableList.of(0, 1, 2, 3, 4, 5, 6), ImmutableSet.of(1, 4, 5)));
   }

   @Test
   void listSet() {
      assertEquals(ImmutableList.of(1), ImmutableUtils.set(ImmutableList.of(0), 0, 1));
      assertEquals(ImmutableList.of(1, 2), ImmutableUtils.set(ImmutableList.of(1, 1), 1, 2));
   }

   // Map ---------------------------

   @Test
   void mapPut() {
      assertEquals(ImmutableMap.of("a", 1), ImmutableUtils.put(ImmutableMap.of(), "a", 1));
      assertEquals(ImmutableMap.of("a", 1), ImmutableUtils.put(ImmutableMap.of("a", 0), "a", 1));
      assertEquals(ImmutableMap.of("a", 1, "b", 2), ImmutableUtils.put(ImmutableMap.of("b", 2), "a", 1));
   }

   @Test
   void mapPutAll() {
      assertEquals(ImmutableMap.of("a", 1), ImmutableUtils.putAll(ImmutableMap.of(), ImmutableMap.of("a", 1)));
      assertEquals(ImmutableMap.of("a", 1), ImmutableUtils.putAll(ImmutableMap.of("a", 0), ImmutableMap.of("a", 1)));
      assertEquals(ImmutableMap.of("a", 1, "b", 2), ImmutableUtils.putAll(ImmutableMap.of(), ImmutableMap.of("a", 1, "b", 2)));
      assertEquals(ImmutableMap.of("a", 1, "b", 2), ImmutableUtils.putAll(ImmutableMap.of("a", 1), ImmutableMap.of("b", 2)));
      assertEquals(ImmutableMap.of("a", 1, "b", 2), ImmutableUtils.putAll(ImmutableMap.of("a", 1, "b", 2), ImmutableMap.of()));
      assertEquals(ImmutableMap.of("a", 1, "b", 2), ImmutableUtils.putAll(ImmutableMap.of("a", 1, "b", 2), ImmutableMap.of("b", 2)));
      assertEquals(ImmutableMap.of("a", 1, "b", 2), ImmutableUtils.putAll(ImmutableMap.of("a", 1, "b", 2), ImmutableMap.of("a", 1, "b", 2)));
   }

   @Test
   void mapRemove() {
      assertEquals(ImmutableMap.of(), ImmutableUtils.remove(ImmutableMap.of("a", 1), "a"));
      assertEquals(ImmutableMap.of("b", 2), ImmutableUtils.remove(ImmutableMap.of("a", 1, "b", 2), "a"));
   }

   // Sorted map ---------------------------

   @Test
   void sortedMapPutAll() {
      assertEquals(ImmutableSortedMap.of("a", 1), ImmutableUtils.putAll(ImmutableSortedMap.of(), ImmutableMap.of("a", 1)));
      assertEquals(ImmutableSortedMap.of("a", 1), ImmutableUtils.putAll(ImmutableSortedMap.of("a", 0), ImmutableMap.of("a", 1)));
      assertEquals(ImmutableSortedMap.of("a", 1, "b", 2), ImmutableUtils.putAll(ImmutableSortedMap.of(), ImmutableMap.of("a", 1, "b", 2)));
      assertEquals(ImmutableSortedMap.of("a", 1, "b", 2), ImmutableUtils.putAll(ImmutableSortedMap.of("a", 1), ImmutableMap.of("b", 2)));
      assertEquals(ImmutableSortedMap.of("a", 1, "b", 2), ImmutableUtils.putAll(ImmutableSortedMap.of("a", 1, "b", 2), ImmutableMap.of()));
      assertEquals(ImmutableSortedMap.of("a", 1, "b", 2), ImmutableUtils.putAll(ImmutableSortedMap.of("a", 1, "b", 2), ImmutableMap.of("b", 2)));
      assertEquals(ImmutableSortedMap.of("a", 1, "b", 2), ImmutableUtils.putAll(ImmutableSortedMap.of("a", 1, "b", 2), ImmutableMap.of("a", 1, "b", 2)));
   }

   // Set ---------------------------

   @Test
   void setAdd() {
      assertEquals(ImmutableSet.of(1), ImmutableUtils.add(ImmutableSet.of(), 1));
      assertEquals(ImmutableSet.of(1), ImmutableUtils.add(ImmutableSet.of(1), 1));
   }

   @Test
   void setAddAll() {
      assertEquals(ImmutableSet.of(1, 2, 3), ImmutableUtils.addAll(ImmutableSet.of(), List.of(1, 2, 3)));
      assertEquals(ImmutableSet.of(1, 2), ImmutableUtils.addAll(ImmutableSet.of(1), List.of(1, 2, 1, 2)));
   }

   @Test
   void setRemove() {
      assertEquals(ImmutableSet.of(1), ImmutableUtils.remove(ImmutableSet.of(1, 2), 2));
      assertEquals(ImmutableSet.of(1, 2), ImmutableUtils.remove(ImmutableSet.of(1, 2), 3));
   }
}
