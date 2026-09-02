package no.imr.tools.range;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

final class FloatRangeSetTest {
   @Test
   void containsNonEmptyDisjointSortedRanges() {
      assertTrue(FloatRangeSet.containsNonEmptyDisjointSortedRanges(List.of(FloatRange.of(0, 1), FloatRange.of(3, 4))));
      assertFalse(FloatRangeSet.containsNonEmptyDisjointSortedRanges(List.of(FloatRange.of(0, 1), FloatRange.of(1, 1)))); // Not non-empty.
      assertFalse(FloatRangeSet.containsNonEmptyDisjointSortedRanges(List.of(FloatRange.of(0, 1), FloatRange.of(1, 2)))); // Not disjoint.
      assertFalse(FloatRangeSet.containsNonEmptyDisjointSortedRanges(List.of(FloatRange.of(0, 2), FloatRange.of(1, 2)))); // Not disjoint.
      assertFalse(FloatRangeSet.containsNonEmptyDisjointSortedRanges(List.of(FloatRange.of(3, 4), FloatRange.of(0, 1)))); // Not sorted.
   }

   @Test
   void equalsAndHashCode() {
      FloatRangeSet a = FloatRangeSet.of(List.of(FloatRange.of(0, 1), FloatRange.of(2, 3), FloatRange.of(3, 4)));
      FloatRangeSet b = FloatRangeSet.of(List.of(FloatRange.of(0, 1), FloatRange.of(2, 4)));
      assertEquals(a.hashCode(), b.hashCode());
      assertEquals(a, b);
   }

   @Test
   void isEmpty() {
      assertTrue(FloatRangeSet.of().isEmpty());
      assertFalse(FloatRangeSet.of(FloatRange.ALL).isEmpty());
   }

   @Test
   void nullIfEmpty() {
      assertNull(FloatRangeSet.of().nullIfEmpty());
      assertNotNull(FloatRangeSet.of(FloatRange.ALL).nullIfEmpty());
   }

   @Test
   void size() {
      assertEquals(0, FloatRangeSet.of().size());
      assertEquals(12, FloatRangeSet.of(List.of(FloatRange.of(-2, 0), FloatRange.of(10, 20))).size());
   }

   @Test
   void getBoundingRange() {
      assertEquals(FloatRange.EMPTY_RANGE, FloatRangeSet.of().getBoundingRange());
      assertEquals(FloatRange.of(0, 1), FloatRangeSet.of(FloatRange.of(0, 1)).getBoundingRange());
      assertEquals(FloatRange.of(0, 3), FloatRangeSet.of(List.of(FloatRange.of(2, 3), FloatRange.of(0, 1))).getBoundingRange());
   }

   @Test
   void add() {
      FloatRangeSet a = FloatRangeSet.of(FloatRange.of(2, 3));
      FloatRangeSet b = FloatRangeSet.of(List.of(FloatRange.of(0, 10), FloatRange.of(20, 30)));

      assertSame(a, a.add(FloatRangeSet.of()));
      assertSame(a, a.add(FloatRange.of(0, 0)));
      assertSame(a, a.add(a));
      assertSame(b, b.add(a));
      assertSame(b, a.add(b));
      assertSame(b, b.add(b));

      assertEquals(List.of(FloatRange.of(0, 1), FloatRange.of(2, 3)),
            a.add(FloatRange.of(0, 1)).getFloatRanges());

      assertEquals(List.of(FloatRange.of(0, 30)),
            b.add(FloatRange.of(10, 20)).getFloatRanges());

      assertEquals(List.of(FloatRange.of(2, 4), FloatRange.of(5, 6)),
            a.add(FloatRangeSet.of(List.of(FloatRange.of(3, 4), FloatRange.of(5, 6)))).getFloatRanges());
   }

   @Test
   void expandEachRange() {
      FloatRangeSet a = FloatRangeSet.of(List.of(FloatRange.of(0, 10), FloatRange.of(20, 30)));
      assertEquals(a, a.expandEachRange(0));
      assertEquals(FloatRangeSet.of(List.of(FloatRange.of(-1, 11), FloatRange.of(19, 31))), a.expandEachRange(1));
      assertEquals(FloatRangeSet.of(List.of(FloatRange.of(1, 9), FloatRange.of(21, 29))), a.expandEachRange(-1));
      assertEquals(FloatRangeSet.of(FloatRange.of(-5, 35)), a.expandEachRange(5));
      assertEquals(FloatRangeSet.of(), a.expandEachRange(-5));

      JUnitUtils.runWithRandom(random -> {
         FloatRangeSet rangeSet = FloatRangeSet.of(randomRanges(random));
         float delta = random.nextFloat() - 0.5f;
         FloatRangeSet expanded = FloatRangeSet.of(rangeSet.stream()
               .map(range -> range.expand(delta))
               .toList());
         assertEquals(expanded, rangeSet.expandEachRange(delta));
      });
   }

   @Test
   void fillGaps() {
      FloatRangeSet a = FloatRangeSet.of(List.of(FloatRange.of(0, 1), FloatRange.of(2, 3), FloatRange.of(5, 6)));
      assertSame(a, a.fillGaps(0.999f));
      assertEquals(List.of(FloatRange.of(0, 3), FloatRange.of(5, 6)), a.fillGaps(1).getFloatRanges());
      assertEquals(List.of(FloatRange.of(0, 6)), a.fillGaps(2).getFloatRanges());
   }

   @Test
   void xor() {
      FloatRangeSet a = FloatRangeSet.of(FloatRange.of(0, 10));
      FloatRangeSet b = FloatRangeSet.of(FloatRange.of(5, 15));
      FloatRangeSet c = FloatRangeSet.of(List.of(FloatRange.of(0, 5), FloatRange.of(10, 15)));
      assertEquals(c, a.xor(b));
      assertEquals(FloatRangeSet.of(), c.xor(c));
   }

   @Test
   void subtract() {
      FloatRangeSet a = FloatRangeSet.of(FloatRange.of(1, 10));
      FloatRangeSet b = FloatRangeSet.of(List.of(FloatRange.of(0, 2), FloatRange.of(5, 6), FloatRange.of(8, 8)));

      assertSame(FloatRangeSet.of(), FloatRangeSet.of().subtract(a));
      assertSame(a, a.subtract(FloatRangeSet.of()));
      assertSame(a, a.subtract(FloatRange.of(0, 1)));
      assertSame(a, a.subtract(FloatRangeSet.of(List.of(FloatRange.of(0, 1), FloatRange.of(11, 12)))));
      assertSame(b, b.subtract(FloatRangeSet.of(List.of(FloatRange.of(2, 4), FloatRange.of(9, 10)))));

      assertEquals(List.of(FloatRange.of(2, 5), FloatRange.of(6, 10)), a.subtract(b).getFloatRanges());
      assertEquals(List.of(FloatRange.of(0, 1)), b.subtract(a).getFloatRanges());

      assertEquals(FloatRangeSet.of(), b.subtract(b));
   }

   @Test
   void complement() {
      testComplement(
            FloatRangeSet.of(),
            FloatRangeSet.of(FloatRange.ALL));
      testComplement(
            FloatRangeSet.of(FloatRange.of(Float.NEGATIVE_INFINITY, 0)),
            FloatRangeSet.of(FloatRange.of(0, Float.POSITIVE_INFINITY)));
      testComplement(
            FloatRangeSet.of(List.of(FloatRange.of(1, 10), FloatRange.of(20, 30))),
            FloatRangeSet.of(List.of(FloatRange.of(Float.NEGATIVE_INFINITY, 1), FloatRange.of(10, 20), FloatRange.of(30, Float.POSITIVE_INFINITY))));
   }

   private static void testComplement(FloatRangeSet a, FloatRangeSet ac) {
      assertEquals(ac, a.complement());
      assertEquals(a, ac.complement());
      assertEquals(FloatRangeSet.of(FloatRange.ALL), a.add(ac));
      assertEquals(FloatRangeSet.of(FloatRange.ALL), ac.add(a));
      assertEquals(a, a.subtract(ac));
      assertEquals(ac, ac.subtract(a));
   }

   @Test
   void intersection() {
      FloatRangeSet a = FloatRangeSet.of(FloatRange.of(1, 10));
      FloatRangeSet b = FloatRangeSet.of(FloatRange.of(10, 20));
      assertEquals(a, a.intersection(a));
      assertEquals(List.of(), a.intersection(b).getFloatRanges());
      assertEquals(List.of(), b.intersection(a).getFloatRanges());

      FloatRangeSet c = FloatRangeSet.of(List.of(FloatRange.of(1, 5), FloatRange.of(11, 15), FloatRange.of(20, 21)));
      assertEquals(List.of(FloatRange.of(11, 13)), c.intersection(FloatRange.of(7, 13)).getFloatRanges());

      FloatRangeSet d = FloatRangeSet.of(List.of(FloatRange.of(3, 5), FloatRange.of(8, 13)));
      assertEquals(List.of(FloatRange.of(3, 5), FloatRange.of(11, 13)), c.intersection(d).getFloatRanges());
   }

   @Test
   void intersects() {
      FloatRangeSet a = FloatRangeSet.of(FloatRange.of(5, 10));
      assertTrue(a.intersects(a));
      assertTrue(a.intersects(FloatRange.of(5, 12)));
      assertFalse(a.intersects(FloatRange.of(11, 12)));

      FloatRangeSet b = FloatRangeSet.of(List.of(FloatRange.of(1, 5), FloatRange.of(8, 9), FloatRange.of(20, 21)));
      assertTrue(b.intersects(b));
      assertTrue(b.intersects(FloatRange.of(8, 12)));
      assertFalse(b.intersects(FloatRange.of(9, 12)));
      assertTrue(b.intersects(a));
      assertTrue(a.intersects(b));

      JUnitUtils.runWithRandom(random -> {
         FloatRangeSet set1 = FloatRangeSet.of(randomRanges(random));
         FloatRangeSet set2 = FloatRangeSet.of(randomRanges(random));
         assertEquals(set1.stream().anyMatch(set2::intersects), set1.intersects(set2));
      });
   }

   @Test
   void contains() {
      FloatRangeSet a = FloatRangeSet.of(FloatRange.of(5, 10));
      assertTrue(a.contains(6));
      assertFalse(a.contains(10));

      assertTrue(a.contains(FloatRange.of(9, 10)));
      assertFalse(a.contains(FloatRange.of(9, 11)));

      assertTrue(a.contains(FloatRangeSet.of(List.of(FloatRange.of(5, 7), FloatRange.of(8, 10)))));
      assertFalse(a.contains(FloatRangeSet.of(List.of(FloatRange.of(5, 7), FloatRange.of(8, 11)))));

      FloatRangeSet b = FloatRangeSet.of(List.of(FloatRange.of(1, 5), FloatRange.of(8, 9), FloatRange.of(20, 22)));
      assertTrue(b.contains(2));
      assertTrue(b.contains(8));
      assertTrue(b.contains(21));
      assertFalse(b.contains(6));
      assertFalse(b.contains(22));

      assertTrue(b.contains(FloatRange.of(2, 3)));
      assertFalse(b.contains(FloatRange.of(2, 9)));

      assertTrue(b.contains(FloatRangeSet.of(List.of(FloatRange.of(2, 3), FloatRange.of(21, 22)))));
      assertFalse(b.contains(FloatRangeSet.of(List.of(FloatRange.of(2, 3), FloatRange.of(19, 21)))));
   }

   @Test
   void clamp() {
      assertEquals(Float.NaN, FloatRangeSet.of().clamp(0));

      FloatRangeSet a = FloatRangeSet.of(List.of(FloatRange.of(5, 10), FloatRange.of(15, 20)));
      assertEquals(5, a.clamp(0));
      assertEquals(9, a.clamp(9));
      assertEquals(10, a.clamp(10));
      assertEquals(10, a.clamp(11));
      assertEquals(10, a.clamp(12));
      assertEquals(15, a.clamp(13));
      assertEquals(20, a.clamp(21));
   }

   @Test
   void random() {
      JUnitUtils.runWithRandom(FloatRangeSetTest::run);
   }

   private static void run(Random random) {
      FloatRangeSet a = FloatRangeSet.of();
      RangeSet<Float> b = new ArrayRangeSet<>();
      for (int i = 0; i < 100; i++) {
         List<FloatRange> ranges = randomRanges(random);
         FloatRangeSet rangeSet = FloatRangeSet.of(ranges);
         RangeSet<Float> c = new ArrayRangeSet<>();
         ranges.forEach(range -> c.add(range.min(), range.max()));
         assertEquals(c.stream().map(FloatRange::of).toList(), rangeSet.getFloatRanges());
         assertEquals(a.contains(rangeSet), c.stream().allMatch(b::containsAll));
         assertEquals(a.intersects(rangeSet), c.stream().anyMatch(b::containsAny));

         switch (random.nextInt(4)) {
            case 0 -> {
               a = a.add(rangeSet);
               b.addAll(c);
            }
            case 1 -> {
               a = a.subtract(rangeSet);
               b.removeAll(c);
            }
            case 2 -> {
               a = a.complement();
               List<Range<Float>> tmp = b.stream().toList();
               b.add(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
               tmp.forEach(b::remove);
            }
            case 3 -> {
               a = a.intersection(rangeSet);
               rangeSet.complement().getFloatRanges().forEach(range -> b.remove(range.min(), range.max()));
            }
            default -> throw new AssertionError();
         }
         assertEquals(b.stream().map(FloatRange::of).toList(), a.getFloatRanges());
      }
   }

   private static List<FloatRange> randomRanges(Random random) {
      return IntStream.range(0, random.nextInt(11))
            .mapToObj(j -> FloatRange.of(j + random.nextFloat(), j + random.nextFloat()))
            .toList();
   }
}
