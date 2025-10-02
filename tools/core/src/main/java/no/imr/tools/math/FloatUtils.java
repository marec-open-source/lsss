package no.imr.tools.math;

import java.util.Arrays;

public final class FloatUtils {
   private FloatUtils() {
   }

   public static float interpolate(float[] xValues, float[] yValues, float x) {
      int i = Arrays.binarySearch(xValues, x);
      if (i >= 0) {
         return yValues[i];
      }

      i = -(i + 1); // Conversion to insertion point
      if (i == 0) {
         return yValues[0]; // Constant extrapolation
      }
      if (i == xValues.length) {
         return yValues[i - 1]; // Constant extrapolation
      }

      // Linear interpolation
      float x1 = xValues[i - 1];
      float x2 = xValues[i];
      float a = (x - x1) / (x2 - x1);
      return (1 - a) * yValues[i - 1] + a * yValues[i];
   }
}
