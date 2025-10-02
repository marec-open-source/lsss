package no.imr.tools;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class CyclicListTest {
   @Test
   void get() {
      CyclicList<Integer> list = new CyclicList<>(List.of(10, 11, 12, 13, 14));
      assertEquals(13, list.get(-7));
      assertEquals(13, list.get(-2));
      assertEquals(14, list.get(-1));
      assertEquals(14, list.get(4));
      assertEquals(10, list.get(5));
      assertEquals(10, list.get(10));
   }

   @Test
   void subList() {
      CyclicList<Integer> list = new CyclicList<>(List.of(10, 11, 12, 13, 14));
      assertEquals(List.of(), list.subList(0, 0));
      assertEquals(List.of(), list.subList(4, 4));
      assertEquals(List.of(), list.subList(5, 5));
      assertEquals(list, list.subList(0, 5));
      assertEquals(list, list.subList(-5, 0));
      assertEquals(list, list.subList(5, 10));
      assertEquals(list, list.subList(50, -10));
      assertEquals(List.of(14, 10), list.subList(-1, 1));
      assertEquals(List.of(13, 14, 10, 11), list.subList(3, 7));
      assertEquals(List.of(13, 14, 10, 11), list.subList(3, 2));
      assertEquals(List.of(13, 14, 10, 11, 12), list.subList(3, 8));
   }
}
