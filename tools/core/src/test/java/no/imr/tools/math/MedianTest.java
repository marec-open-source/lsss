package no.imr.tools.math;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MedianTest {
   @Test
   void quickSelect() {
      assertEquals(1, Median.quickSelect(new int[]{-1, 9, 7, 5, 0, 1, -100}));
      assertEquals(1, Median.quickSelect(new float[]{-1, 9, 7, 5, 0, 1, -100}));
      assertEquals(1, Median.quickSelect(new double[]{-1, 9, 7, 5, 0, 1, -100}));
   }

   @Test
   void of3() {
      assertEquals(2, Median.of(1, 2, 3));
      assertEquals(2, Median.of(1, 3, 2));
      assertEquals(2, Median.of(2, 1, 3));
      assertEquals(2, Median.of(2, 3, 1));
      assertEquals(2, Median.of(3, 1, 2));
      assertEquals(2, Median.of(3, 2, 1));
   }

   @Test
   void of9() {
      JUnitUtils.runWithRandom(random -> {
         float[] a = JUnitUtils.createRandomFloatArray(random, 9);
         float ofMedian = Median.of(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8]);
         float quickMedian = Median.quickSelect(a); // last since it modifies array
         assertEquals(quickMedian, ofMedian);
      });
   }
}
