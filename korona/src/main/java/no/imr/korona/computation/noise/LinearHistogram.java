package no.imr.korona.computation.noise;

import java.util.Arrays;

/**
 * Intervals increase with a constant term, \Delta x_{i+1} = \Delta x_i + \delta.
 */
final class LinearHistogram extends BaseHistogram {
   /**
    * Creates a histogram where the cell widths increase linearly.
    *
    * @param lowLimit the lower limit
    */
   LinearHistogram(double lowLimit) {
      super(lowLimit);
   }

   @Override
   float[] makeLimits(float low, float high, int cellCount) {
      float[] limits = new float[cellCount + 1];
      float delta = (high - low) / (cellCount * (cellCount + 1) / 2.0f);
      limits[0] = low;
      for (int i = 1; i < cellCount + 1; i++) {
         limits[i] = limits[i - 1] + i * delta;
      }
      return limits;
   }

   @Override
   float[] expandLimits(float[] limits, float newUpperLimit, int maxCellCount) {
      if (newUpperLimit <= limits[limits.length - 1]) {
         return limits;
      }
      float delta = limits[1] - limits[0];
      int n = (int) Math.ceil(
            -0.5 + 2 * Math.sqrt(2 * (newUpperLimit - limits[0]) / delta));
      n = Math.min(n, maxCellCount);
      float[] newLimits = Arrays.copyOf(limits, n + 1);
      for (int i = limits.length; i < newLimits.length; i++) {
         newLimits[i] = newLimits[i - 1] + i * delta;
      }
      return newLimits;
   }
}
