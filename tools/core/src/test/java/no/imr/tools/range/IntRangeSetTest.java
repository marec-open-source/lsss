package no.imr.tools.range;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

final class IntRangeSetTest {
   @Test
   void containsTest() {
      IntRangeSet intRangeSet = new IntRangeSet();
      intRangeSet.add(5, 8);
      assertFalse(intRangeSet.contains(4));
      assertTrue(intRangeSet.contains(5));
      assertTrue(intRangeSet.contains(7));
      assertFalse(intRangeSet.contains(8));
   }

   @Test
   void addTest() {
      IntRangeSet intRangeSet = new IntRangeSet();
      intRangeSet.add(5, 8);
      assertArrayEquals(new int[]{5, 8}, intRangeSet.getIndexes());
      intRangeSet.add(1, 2);
      assertArrayEquals(new int[]{1, 2, 5, 8}, intRangeSet.getIndexes());
      intRangeSet.add(6, 9);
      assertArrayEquals(new int[]{1, 2, 5, 9}, intRangeSet.getIndexes());
      intRangeSet.add(9, 11);
      assertArrayEquals(new int[]{1, 2, 5, 11}, intRangeSet.getIndexes());
      intRangeSet.add(2, 5);
      assertArrayEquals(new int[]{1, 11}, intRangeSet.getIndexes());
   }

   @Test
   void removeTest() {
      IntRangeSet intRangeSet = new IntRangeSet();
      intRangeSet.add(-1, 10);
      assertArrayEquals(new int[]{-1, 10}, intRangeSet.getIndexes());
      intRangeSet.remove(1, 2);
      assertArrayEquals(new int[]{-1, 1, 2, 10}, intRangeSet.getIndexes());
      intRangeSet.remove(2, 4);
      assertArrayEquals(new int[]{-1, 1, 4, 10}, intRangeSet.getIndexes());
      intRangeSet.remove(10, 11);
      assertArrayEquals(new int[]{-1, 1, 4, 10}, intRangeSet.getIndexes());
      intRangeSet.remove(-1, 3);
      assertArrayEquals(new int[]{4, 10}, intRangeSet.getIndexes());
      intRangeSet.remove(-1, 13);
      assertArrayEquals(new int[]{}, intRangeSet.getIndexes());
   }

   @Test
   void randomTest() {
      JUnitUtils.runWithRandom(IntRangeSetTest::runTest);
   }

   private static void runTest(Random random) {
      int maxIndex = 1000;
      int maxIter = 100;

      IntRangeSet intRangeSet = new IntRangeSet();
      RangeSet<Integer> rangeSet = new ArrayRangeSet<>();

      for (int iter = 0; iter < maxIter; iter++) {
         int beginIndex = random.nextInt(maxIndex);
         int endIndex = beginIndex + random.nextInt(maxIndex - beginIndex);

         if (random.nextBoolean()) {
            intRangeSet.add(beginIndex, endIndex);
            rangeSet.add(beginIndex, endIndex);
         } else {
            intRangeSet.remove(beginIndex, endIndex);
            rangeSet.remove(beginIndex, endIndex);
         }

         test(intRangeSet, rangeSet);
      }
   }

   private static void test(IntRangeSet intRangeSet, RangeSet<Integer> rangeSet) {
      int i = 0;
      for (Range<Integer> range : rangeSet) {
         int beginIndex = intRangeSet.getIndexes()[i++];
         int endIndex = intRangeSet.getIndexes()[i++];
         assertEquals(range.begin(), beginIndex);
         assertEquals(range.end(), endIndex);
      }
   }

   @Test
   void intersectsTest() {
      IntRangeSet mask1 = new IntRangeSet();
      IntRangeSet mask2 = new IntRangeSet();
      assertFalse(mask1.intersects(mask2));
      assertFalse(mask2.intersects(mask1));

      mask1.add(0, 10);
      assertFalse(mask1.intersects(mask2));
      assertFalse(mask2.intersects(mask1));

      mask2.add(10, 20);
      assertFalse(mask1.intersects(mask2));
      assertFalse(mask2.intersects(mask1));

      mask1.add(20, 20);
      assertFalse(mask1.intersects(mask2));
      assertFalse(mask2.intersects(mask1));

      mask1.add(19, 20);
      assertTrue(mask1.intersects(mask2));
      assertTrue(mask2.intersects(mask1));
   }

   @Test
   void clampingTest() {
      IntRangeSet mask1 = new IntRangeSet();

      mask1.add(5, 20);
      assertTrue(mask1.contains(9));
      assertTrue(mask1.contains(10));

      mask1.clampRange(0, 10);
      assertTrue(mask1.contains(9));
      assertFalse(mask1.contains(10));
   }

   @Test
   void streamTest() {
      assertEquals(List.of(), new IntRangeSet().stream().toList());
      assertEquals(List.of(new IntRange(1, 3)), new IntRangeSet(1, 3).stream().toList());
      assertEquals(List.of(new IntRange(1, 3), new IntRange(5, 6)),
            new IntRangeSet(List.of(1, 3, 5, 6), 9).stream().toList());
      assertEquals(List.of(new IntRange(1, 3), new IntRange(5, 6), new IntRange(7, 9)),
            new IntRangeSet(List.of(1, 3, 5, 6, 7), 9).stream().toList());
   }

   @Test
   void iteratorTest() {
      IntRangeSet mask = new IntRangeSet();
      mask.add(1, 3);
      mask.add(5, 6);
      mask.add(7, 9);
      Iterator<IntRange> iterator = mask.iterator();
      assertTrue(iterator.hasNext());
      assertEquals(new IntRange(1, 3), iterator.next());
      assertTrue(iterator.hasNext());
      assertEquals(new IntRange(5, 6), iterator.next());
      assertTrue(iterator.hasNext());
      assertEquals(new IntRange(7, 9), iterator.next());
      assertFalse(iterator.hasNext());
   }
}
