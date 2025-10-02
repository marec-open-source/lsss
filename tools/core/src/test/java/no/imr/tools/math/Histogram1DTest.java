package no.imr.tools.math;

import no.imr.tools.range.FloatRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class Histogram1DTest {
   @Test
   void quantile() {
      Histogram1D histogram1D = Histogram1D.fromDelta(FloatRange.of(0, 10), 1);
      for (int i = 0; i < 10; i++) {
         histogram1D.addValue(i);
      }
      assertEquals(10, histogram1D.getTotalCount());
      assertEquals(0, histogram1D.getLowerQuantileIndex(0.1));
      assertEquals(9, histogram1D.getUpperQuantileIndex(0.1));
      assertEquals(4, histogram1D.getLowerQuantileIndex(0.5));
      assertEquals(5, histogram1D.getUpperQuantileIndex(0.5));

      for (int i = 0; i < 10; i++) {
         histogram1D.addValue(-1);
      }
      assertEquals(20, histogram1D.getTotalCount());
      assertEquals(0, histogram1D.getLowerQuantileIndex(0.1));
      assertEquals(0, histogram1D.getLowerQuantileIndex(0.5));
   }

   @Test
   void quantileMostValuesBelow() {
      Histogram1D histogram1D = Histogram1D.fromDelta(FloatRange.of(0, 10), 1);
      histogram1D.addValue(-1);
      assertEquals(1, histogram1D.getTotalCount());
      assertEquals(0, histogram1D.getLowerQuantileIndex(0.1));
      assertEquals(0, histogram1D.getLowerQuantileIndex(0.5));
      assertEquals(0, histogram1D.getUpperQuantileIndex(0.1));
      assertEquals(0, histogram1D.getUpperQuantileIndex(0.5));
   }

   @Test
   void quantileMostValuesAbove() {
      Histogram1D histogram1D = Histogram1D.fromDelta(FloatRange.of(0, 10), 1);
      histogram1D.addValue(11);
      assertEquals(1, histogram1D.getTotalCount());
      assertEquals(9, histogram1D.getLowerQuantileIndex(0.1));
      assertEquals(9, histogram1D.getLowerQuantileIndex(0.5));
      assertEquals(9, histogram1D.getUpperQuantileIndex(0.1));
      assertEquals(9, histogram1D.getUpperQuantileIndex(0.5));
   }
}
