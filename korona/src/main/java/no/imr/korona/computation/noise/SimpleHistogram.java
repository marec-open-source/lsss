package no.imr.korona.computation.noise;

import java.util.Arrays;

/**
 * All intervals have equal length.
 */
final class SimpleHistogram extends BaseHistogram {
   /**
    * All intervals have equal length.
    *
    * @param lowLimit the lower limit
    */
   SimpleHistogram(double lowLimit) {
      super(lowLimit);
   }

   @Override
   float[] makeLimits(float low, float high, int cellCount) {
      float[] limits = new float[cellCount + 1];
      float delta = (high - low) / cellCount;
      for (int i = 0; i < cellCount + 1; i++) {
         limits[i] = low + i * delta;
      }
      return limits;
   }

   @Override
   float[] expandLimits(float[] limits, float newUpperLimit, int maxCellCount) {
      if (newUpperLimit <= limits[limits.length - 1]) {
         return limits;
      }
      float delta = limits[1] - limits[0];
      int n = (int) Math.ceil((newUpperLimit - limits[0]) / delta);
      n = Math.min(n, maxCellCount);
      float[] newLimits = Arrays.copyOf(limits, n + 1);
      for (int i = limits.length; i < newLimits.length; i++) {
         newLimits[i] = newLimits[i - 1] + delta;
      }
      return newLimits;
   }
}
