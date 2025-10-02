package no.imr.korona.computation.plankton;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SizeHistogramRedistributionTest {
   @Test
   void testRedistribute() {
      SizeHistogram histogram;
      SizeHistogramRedistribution sizeHistogramRedistribution = new SizeHistogramRedistribution();

      histogram = new SizeHistogram(new double[]{0, 1 /**/, 1, 2});
      JUnitUtils.set(histogram.getAbundances(), 0, 0);
      assertTrue(sizeHistogramRedistribution.redistribute(histogram));
      assertArrayEquals(new double[]{}, histogram.getDividers());

      histogram = new SizeHistogram(new double[]{0, 1 /**/, 1, 2});
      JUnitUtils.set(histogram.getAbundances(), 1, 0);
      assertTrue(sizeHistogramRedistribution.redistribute(histogram));
      assertArrayEquals(new double[]{0, 0.5 /**/, 0.5, 1}, histogram.getDividers());

      histogram = new SizeHistogram(new double[]{0, 3 /**/, 3, 6 /**/, 6, 9});
      JUnitUtils.set(histogram.getAbundances(), 1, 0, 0);
      assertTrue(sizeHistogramRedistribution.redistribute(histogram));
      assertArrayEquals(new double[]{0, 1 /**/, 1, 2 /**/, 2, 3}, histogram.getDividers());

      histogram = new SizeHistogram(new double[]{0, 2 /**/, 2, 4 /* gap */, 6, 8 /**/, 8, 10});
      JUnitUtils.set(histogram.getAbundances(), 0, 1, 1, 0);
      assertTrue(sizeHistogramRedistribution.redistribute(histogram));
      assertArrayEquals(new double[]{2, 3 /**/, 3, 4 /* gap */, 6, 7 /**/, 7, 8}, histogram.getDividers());

      histogram = new SizeHistogram(new double[]{0, 2 /* gap */, 3, 4 /* gap */, 6, 7});
      JUnitUtils.set(histogram.getAbundances(), 1, 0, 1);
      assertTrue(sizeHistogramRedistribution.redistribute(histogram));
      assertArrayEquals(new double[]{0, 1 /**/, 1, 2 /* gap */, 6, 7}, histogram.getDividers());

      histogram = new SizeHistogram(new double[]{0, 1, 1, 2, 2, 2.5  /* gap */, 3, 3 + 1E-4 /* gap */, 6, 7, 7, 8, 8, 9});
      JUnitUtils.set(histogram.getAbundances(), 1, 1, 1, 1, 0, 1, 1);
      assertFalse(sizeHistogramRedistribution.redistribute(histogram, 1));
      assertArrayEquals(new double[]{0, 1, 1, 2, 2, 2.5  /* gap */, 3, 3 + 1E-4 /* gap */, 6, 7, 7, 8, 8, 9}, histogram.getDividers());
   }
}
