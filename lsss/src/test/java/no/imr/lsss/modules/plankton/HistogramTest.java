package no.imr.lsss.modules.plankton;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class HistogramTest {
   @Test
   void testCombinedDividerCount() {
      assertEquals(1, Histogram.combinedDividerCount(new float[]{0}, new float[]{0}));
      assertEquals(2, Histogram.combinedDividerCount(new float[]{0}, new float[]{1}));
      assertEquals(2, Histogram.combinedDividerCount(new float[]{0, 1}, new float[]{1}));
      assertEquals(3, Histogram.combinedDividerCount(new float[]{0, 1}, new float[]{1, 2}));
      assertEquals(8, Histogram.combinedDividerCount(new float[]{0, 1, 6}, new float[]{0.5f, 1.5f, 3, 4, 5}));
   }

   @Test
   void testAccumulate1() {
      Histogram a = new Histogram(new float[]{0, 1, 2}, new float[]{2, 1});
      Histogram b = new Histogram(new float[]{1, 2}, new float[]{3});
      a.accumulate(b, 1);
      assertArrayEquals(new float[]{0, 1, 2}, a.getDividers());
      assertArrayEquals(new float[]{1, 2}, a.getValues());

      a.accumulate(new float[]{0, 1}, new float[]{3}, 2);
      assertArrayEquals(new float[]{0, 1, 2}, a.getDividers());
      assertArrayEquals(new float[]{2, 1}, a.getValues());
   }

   @Test
   void testAccumulate2() {
      Histogram a = new Histogram(new float[]{0, 1, 2, 3}, new float[]{1, 0, 2});
      Histogram b = new Histogram(new float[]{0.5f, 1, 2.5f}, new float[]{13, 18});
      a.accumulate(b, 1);
      assertArrayEquals(new float[]{0, 0.5f, 1, 2, 2.5f, 3}, a.getDividers());
      assertArrayEquals(new float[]{0.25f, 6.75f, 6f, 3.5f, 0.5f}, a.getValues());

      a = new Histogram(new float[]{0, 1, 2, 3}, new float[]{1, 0, 2});
      b.accumulate(a, 1);
      assertArrayEquals(new float[]{0, 0.5f, 1, 2, 2.5f, 3}, b.getDividers());
      assertArrayEquals(new float[]{0.25f, 6.75f, 6f, 3.5f, 0.5f}, b.getValues());
   }

   @Test
   void testToFixedBinHistogram() {
      Histogram a = new Histogram(new float[]{1, 3.5f, 8}, new float[]{5, 18});
      Histogram b = a.toFixedBinHistogram(1);
      assertArrayEquals(new float[]{1, 2, 3, 4, 5, 6, 7, 8}, b.getDividers());
      assertArrayEquals(new float[]{2, 2, 3, 4, 4, 4, 4}, b.getValues());
   }
}
