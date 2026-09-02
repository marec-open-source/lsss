package no.imr.tools.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class QuantileTest {
   @Test
   void quantileIndex() {
      assertEquals(0, Quantile.quantileIndex(0, 10));
      assertEquals(1, Quantile.quantileIndex(0.1, 10));
      assertEquals(8, Quantile.quantileIndex(0.9, 10));
      assertEquals(9, Quantile.quantileIndex(1, 10));
      assertThrows(IllegalArgumentException.class, () -> Quantile.quantileIndex(-0.01, 10));
      assertThrows(IllegalArgumentException.class, () -> Quantile.quantileIndex(1.01, 10));
      assertThrows(IllegalArgumentException.class, () -> Quantile.quantileIndex(0.5, 0));
      assertThrows(IllegalArgumentException.class, () -> Quantile.quantileIndex(Double.NaN, 10));
   }

   @Test
   void quickSelect() {
      assertEquals(0, Quantile.quickSelect(new float[]{-1, 9, 7, 5, 0, 1, -100}, 0.4));
      assertEquals(5, Quantile.quickSelect(new float[]{-1, 9, 7, 5, 0, 1, -100}, 0.6));

      assertEquals(0, Quantile.quickSelect(new double[]{-1, 9, 7, 5, 0, 1, -100}, 0.4));
      assertEquals(5, Quantile.quickSelect(new double[]{-1, 9, 7, 5, 0, 1, -100}, 0.6));

      assertThrows(IllegalArgumentException.class, () -> Quantile.quickSelect(new double[]{}, 0.5));
   }
}
