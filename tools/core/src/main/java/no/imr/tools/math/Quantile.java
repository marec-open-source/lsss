package no.imr.tools.math;

public final class Quantile {
   private Quantile() {
   }

   public static int quantileIndex(double quantile, int length) {
      if (!(quantile >= 0 && quantile <= 1)) { // Also handles NaN.
         throw new IllegalArgumentException("quantile: " + quantile);
      }
      if (length <= 0) {
         throw new IllegalArgumentException("length: " + length);
      }
      return (int) Math.round(quantile * (length - 1));
   }

   public static float quickSelect(float[] values, double quantile) {
      return QuickSelect.get(values, quantileIndex(quantile, values.length));
   }

   public static double quickSelect(double[] values, double quantile) {
      return QuickSelect.get(values, quantileIndex(quantile, values.length));
   }
}
