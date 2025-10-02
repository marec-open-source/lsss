package no.imr.tools.range;

import no.imr.tools.test.JUnitUtils;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

final class RangeMapTest {
   @Test
   void copy() {
      RangeMap<Integer, String> orig = new ArrayRangeMap<>();
      orig.put(0, 1, "a");

      RangeMap<Integer, String> copy = orig.copy();
      copy.put(0, 1, "b");

      assertEquals("a", orig.get(0));
      assertEquals("b", copy.get(0));
   }

   @Test
   void putAll() {
      RangeMap<Integer, String> m = new ArrayRangeMap<>();
      m.put(0, 1, "a");
      m.put(5, 6, "b");

      RangeMap<Integer, String> m2 = new ArrayRangeMap<>();
      m2.putAll(m);
      assertEquals(List.of(
                  new RangeMap.Entry<>(new DefaultRange<>(0, 1), "a"),
                  new RangeMap.Entry<>(new DefaultRange<>(5, 6), "b")),
            m2.stream().toList());
   }

   @Test
   void random() {
      JUnitUtils.runWithRandom(random -> {
         RangeMap<Integer, Integer> rangeMap = new ArrayRangeMap<>();
         Map<Integer, @Nullable Integer> map = new HashMap<>();
         for (int i = 0; i < 20; i++) {
            int k1 = random.nextInt(10);
            int k2 = k1 + random.nextInt(10);
            Integer value = random.nextInt(3) == 0 ? null : random.nextInt(5);

            rangeMap.put(k1, k2, value);

            for (int k = k1; k < k2; k++) {
               map.put(k, value);
            }

            for (int k = 0; k < 20; k++) {
               assertEquals(map.get(k), rangeMap.get(k));
            }
         }
      });
   }

   @Test
   void containsKey() {
      RangeMap<Integer, String> m = new ArrayRangeMap<>();
      m.put(5, 8, "a1");
      m.put(8, 10, "a2");
      m.put(15, 18, "b1");
      m.put(18, 20, "b2");

      assertFalse(m.containsKey(4));
      assertTrue(m.containsKey(5));
      assertTrue(m.containsKey(9));
      assertFalse(m.containsKey(10));
      assertTrue(m.containsKey(15));

      Range<Integer> emptyRange = new DefaultRange<>(0, 0);

      assertFalse(m.containsAnyKey(emptyRange));
      assertFalse(m.containsAnyKey(new DefaultRange<>(0, 5)));
      assertTrue(m.containsAnyKey(new DefaultRange<>(0, 6)));
      assertTrue(m.containsAnyKey(new DefaultRange<>(9, 12)));
      assertFalse(m.containsAnyKey(new DefaultRange<>(10, 12)));

      assertTrue(m.containsAllKeys(emptyRange));
      assertFalse(m.containsAllKeys(new DefaultRange<>(0, 10)));
      assertTrue(m.containsAllKeys(new DefaultRange<>(6, 10)));
      assertFalse(m.containsAllKeys(new DefaultRange<>(6, 20)));
      assertTrue(m.containsAllKeys(new DefaultRange<>(16, 20)));
      assertFalse(m.containsAllKeys(new DefaultRange<>(16, 21)));

      assertTrue(m.containsNoKeys(emptyRange));
      assertTrue(m.containsNoKeys(new DefaultRange<>(0, 5)));
      assertTrue(m.containsNoKeys(new DefaultRange<>(10, 15)));
      assertTrue(m.containsNoKeys(new DefaultRange<>(20, 25)));
      assertFalse(m.containsNoKeys(new DefaultRange<>(0, 6)));
      assertFalse(m.containsNoKeys(new DefaultRange<>(14, 16)));
      assertFalse(m.containsNoKeys(new DefaultRange<>(0, 99)));
   }

   @Test
   void test1() {
      doTest1(new ArrayRangeMap<>());
      doTest1(new CopyOnWriteRangeMap<>());
   }

   private static void doTest1(RangeMap<Integer, String> m) {
      m.put(-1, 5, "a");
      assertNull(m.get(-3));
      assertEquals("a", m.get(-1));
      assertEquals("a", m.get(1));
      assertNull(m.get(5));

      m.put(1, 3, "b");
      assertNull(m.get(-2));
      assertEquals("a", m.get(-1));
      assertEquals("a", m.get(0));
      assertEquals("b", m.get(1));
      assertEquals("b", m.get(2));
      assertEquals("a", m.get(3));
      assertEquals("a", m.get(4));
      assertNull(m.get(5));

      m.clear();
      assertNull(m.get(0));
      assertNull(m.get(1));

      m.put(0, 5, "a");
      m.put(6, 10, "c");
      m.put(3, 8, "b");
      for (int i = 0; i < 3; i++) {
         assertEquals("a", m.get(i), Integer.toString(i));
      }
      for (int i = 3; i < 8; i++) {
         assertEquals("b", m.get(i), Integer.toString(i));
      }
      for (int i = 8; i < 10; i++) {
         assertEquals("c", m.get(i), Integer.toString(i));
      }
   }

   @Test
   void test2() {
      doTest2(new ArrayRangeMap<>());
      doTest2(new CopyOnWriteRangeMap<>());
   }

   private static void doTest2(RangeMap<String, Double> m) {
      m.put("d", "k", 3.4);
      m.put("q", "s", 5.6);
      assertNull(m.get("c"));
      assertEquals(3.4, m.get("d"));
      assertEquals(3.4, m.get("j"));
      assertNull(m.get("k"));
      assertNull(m.get("l"));
      assertNull(m.get("p"));
      assertEquals(5.6, m.get("q"));
      assertEquals(5.6, m.get("r"));
      assertNull(m.get("s"));
   }

   @Test
   void testSize() {
      doTestSize(new ArrayRangeMap<>());
      doTestSize(new CopyOnWriteRangeMap<>());
   }

   private static void doTestSize(RangeMap<Integer, Boolean> m) {
      assertEquals(0, m.size());
      m.put(0, 1, true);
      assertEquals(1, m.size());
      m.put(4, 5, true);
      assertEquals(2, m.size());
      m.put(1, 4, true);
      assertEquals(1, m.size());
      m.put(5, 8, false);
      assertEquals(2, m.size());
      m.put(5, 8, true);
      assertEquals(1, m.size());
   }

   @Test
   void testIterator() {
      doTestIterator(new ArrayRangeMap<>());
      doTestIterator(new CopyOnWriteRangeMap<>());
   }

   private static void doTestIterator(RangeMap<Integer, Boolean> m) {
      m.put(-1, 3, true);
      m.put(3, 4, true);
      m.put(4, 5, false);
      assertEquals(2, m.size());

      Iterator<RangeMap.Entry<Integer, Boolean>> iterator = m.iterator();

      assertTrue(iterator.hasNext());
      RangeMap.Entry<Integer, Boolean> entry = iterator.next();
      assertTrue(entry.value());
      assertEquals(new DefaultRange<>(-1, 4), entry.range());

      assertTrue(iterator.hasNext());
      entry = iterator.next();
      assertFalse(entry.value());
      assertEquals(new DefaultRange<>(4, 5), entry.range());

      assertFalse(iterator.hasNext());
   }

   @Test
   void testIntervalRemoval() {
      ArrayRangeMap<Integer, Boolean> m = new ArrayRangeMap<>();
      m.put(0, 1, true);
      m.put(2, 3, true);
      m.put(4, 5, true);
      assertEquals(6, m.entryCount());
      m.remove(2, 3);
      assertEquals(4, m.entryCount());

      m.clear();

      m.put(0, 1, true);
      m.put(2, 3, true);
      assertEquals(4, m.entryCount());
      m.remove(2, 3);
      assertEquals(2, m.entryCount());
      m.remove(0, 1);
      assertEquals(0, m.entryCount());
   }

   @Test
   void testNoSuchElementException() {
      RangeMap<Integer, Boolean> m = new ArrayRangeMap<>();
      assertThrows(NoSuchElementException.class, () -> {
         m.iterator().next();
      });
   }

   @Test
   void stream() {
      RangeMap<Integer, String> m = new ArrayRangeMap<>();
      m.put(10, 15, "a");
      m.put(20, 30, "b");
      m.put(30, 35, "c");
      assertEquals(List.of(
                  new RangeMap.Entry<>(new DefaultRange<>(10, 15), "a"),
                  new RangeMap.Entry<>(new DefaultRange<>(20, 30), "b"),
                  new RangeMap.Entry<>(new DefaultRange<>(30, 35), "c")),
            m.stream().toList());
   }

   @Test
   void streamRange() {
      RangeMap<Integer, String> m = new ArrayRangeMap<>();

      assertEquals(List.of(), m.stream(new DefaultRange<>(0, 90)).toList());

      m.put(10, 15, "a");
      m.put(20, 25, "b");
      m.put(30, 40, "c");
      m.put(40, 45, "d");
      m.put(50, 55, "e");

      assertEquals(List.of(
                  new RangeMap.Entry<>(new DefaultRange<>(22, 25), "b"),
                  new RangeMap.Entry<>(new DefaultRange<>(30, 40), "c"),
                  new RangeMap.Entry<>(new DefaultRange<>(40, 41), "d")),
            m.stream(new DefaultRange<>(22, 41)).toList());

      assertEquals(List.of(), m.stream(new DefaultRange<>(0, 9)).toList());
      assertEquals(List.of(), m.stream(new DefaultRange<>(0, 10)).toList());
      assertEquals(List.of(), m.stream(new DefaultRange<>(25, 30)).toList());
      assertEquals(List.of(), m.stream(new DefaultRange<>(26, 29)).toList());
      assertEquals(List.of(), m.stream(new DefaultRange<>(55, 90)).toList());
      assertEquals(List.of(), m.stream(new DefaultRange<>(56, 90)).toList());

      List<RangeMap.Entry<Integer, String>> a = List.of(new RangeMap.Entry<>(new DefaultRange<>(10, 15), "a"));
      assertEquals(a, m.stream(new DefaultRange<>(0, 15)).toList());
      assertEquals(a, m.stream(new DefaultRange<>(10, 20)).toList());

      List<RangeMap.Entry<Integer, String>> b = List.of(new RangeMap.Entry<>(new DefaultRange<>(20, 25), "b"));
      assertEquals(b, m.stream(new DefaultRange<>(20, 25)).toList());
      assertEquals(b, m.stream(new DefaultRange<>(19, 25)).toList());
      assertEquals(b, m.stream(new DefaultRange<>(19, 26)).toList());
      assertEquals(b, m.stream(new DefaultRange<>(15, 30)).toList());

      List<RangeMap.Entry<Integer, String>> e = List.of(new RangeMap.Entry<>(new DefaultRange<>(50, 55), "e"));
      assertEquals(e, m.stream(new DefaultRange<>(50, 55)).toList());
      assertEquals(e, m.stream(new DefaultRange<>(45, 90)).toList());
   }
}
