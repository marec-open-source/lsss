package no.imr.tools.math;

import java.util.Arrays;

public final class FloatUtils {
   private FloatUtils() {
   }

   /// Linear interpolation and constant extrapolation.
   ///
   /// @param xValues x values, must be sorted in ascending order
   /// @param yValues y values corresponding to the x values
   /// @param x the x value to interpolate
   /// @return the interpolated y value
   public static float interpolate(float[] xValues, float[] yValues, float x) {
      if (xValues.length != yValues.length) {
         throw new IllegalArgumentException(xValues.length + " != " + yValues.length);
      }
      if (xValues.length == 0) {
         throw new IllegalArgumentException("Empty arrays");
      }

      int i = Arrays.binarySearch(xValues, x);
      if (i >= 0) {
         return yValues[i];
      }

      i = -(i + 1); // Conversion to insertion point.
      if (i == 0) {
         return yValues[0]; // Constant extrapolation.
      }
      if (i == xValues.length) {
         if (Float.isNaN(x)) {
            return Float.NaN;
         }
         return yValues[i - 1]; // Constant extrapolation.
      }

      // Linear interpolation.
      float x1 = xValues[i - 1];
      float x2 = xValues[i];
      float a = (x - x1) / (x2 - x1);
      return (1 - a) * yValues[i - 1] + a * yValues[i];
   }
}
