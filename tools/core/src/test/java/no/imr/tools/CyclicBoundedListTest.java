package no.imr.tools;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class CyclicBoundedListTest {
   @Test
   void add() {
      CyclicBoundedList<Integer> list = new CyclicBoundedList<>(3);
      list.add(1);
      assertEquals(List.of(1), list);
      assertEquals(0, list.getOrigin());

      list.add(2);
      assertEquals(List.of(1, 2), list);
      assertEquals(0, list.getOrigin());

      list.add(3);
      assertEquals(List.of(1, 2, 3), list);
      assertEquals(0, list.getOrigin());

      list.add(4);
      assertEquals(List.of(2, 3, 4), list);
      assertEquals(1, list.getOrigin());

      list.addAll(List.of(5, 6));
      assertEquals(List.of(4, 5, 6), list);
      assertEquals(0, list.getOrigin());
   }

   @Test
   void addFirst() {
      CyclicBoundedList<Integer> list = new CyclicBoundedList<>(3);

      list.add(1);
      list.addFirst(2);
      assertEquals(List.of(2, 1), list);
      assertEquals(2, list.getOrigin());

      list.addFirst(3);
      assertEquals(List.of(3, 2, 1), list);
      assertEquals(1, list.getOrigin());

      list.addFirst(4);
      assertEquals(List.of(4, 3, 2), list);
      assertEquals(0, list.getOrigin());
   }

   @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
   @Test
   void get() {
      CyclicBoundedList<String> list = new CyclicBoundedList<>(3);
      assertEquals(0, list.size());
      assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));

      list.add("a");
      assertEquals(1, list.size());
      assertEquals("a", list.get(0));
      assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));

      list.add("b");
      list.add("c");
      list.add("d");
      assertEquals(3, list.size());
      assertEquals("b", list.get(0));
      assertEquals("c", list.get(1));
      assertEquals("d", list.get(2));
      assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
   }

   @Test
   void clear() {
      CyclicBoundedList<String> list = new CyclicBoundedList<>(4);
      list.add("a");
      list.addFirst("b");
      assertEquals(2, list.size());
      assertEquals(3, list.getOrigin());
      list.clear();
      //noinspection ConstantValue
      assertEquals(0, list.size());
      assertEquals(0, list.getOrigin());
   }
}
