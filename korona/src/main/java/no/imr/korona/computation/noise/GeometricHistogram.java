package no.imr.korona.computation.noise;

import java.util.Arrays;

/**
 * Interval refined with a constant factor, \Delta x_{i+1} = \Delta x_i * factor.
 */
final class GeometricHistogram extends BaseHistogram {
   private final double refinementFactor;

   /**
    * Constructs a histogram with cell widths increasing with the factor refinementFactor.
    *
    * @param lowLimit         the lower limit
    * @param refinementFactor the cell ratio
    */
   GeometricHistogram(double lowLimit, double refinementFactor) {
      super(lowLimit);

      this.refinementFactor = refinementFactor;
   }

   @Override
   float[] makeLimits(float low, float high, int cellCount) {
      float[] limits = new float[cellCount + 1];
      double sum = (1 - Math.pow(refinementFactor, cellCount)) / (1 - refinementFactor);
      double binSize = (high - low) / sum;
      limits[0] = low;
      for (int i = 1; i < cellCount + 1; i++) {
         limits[i] = limits[i - 1] + (float) binSize;
         binSize *= refinementFactor;
      }
      return limits;
   }

   @Override
   float[] expandLimits(float[] limits, float newUpperLimit, int maxCellCount) {
      if (newUpperLimit <= limits[limits.length - 1]) {
         return limits;
      }
      float delta = (newUpperLimit - limits[0]) / (limits[1] - limits[0]);
      int n = (int) Math.ceil(Math.log(1 - delta * (1 - refinementFactor))
            / Math.log(refinementFactor));
      n = Math.min(n, maxCellCount);
      float[] newLimits = Arrays.copyOf(limits, n + 1);
      double binSize = limits[limits.length - 1] - limits[limits.length - 2];
      for (int i = limits.length; i < newLimits.length; i++) {
         binSize *= refinementFactor;
         newLimits[i] = newLimits[i - 1] + (float) binSize;
      }
      return newLimits;
   }
}
