package no.imr.tools.math;

import java.util.Arrays;

public final class DoubleUtils {
   private DoubleUtils() {
   }

   /// Linear interpolation and constant extrapolation.
   ///
   /// @param xValues x values, must be sorted in ascending order
   /// @param yValues y values corresponding to the x values
   /// @param x the x value to interpolate
   /// @return the interpolated y value
   public static double interpolate(double[] xValues, double[] yValues, double x) {
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
         if (Double.isNaN(x)) {
            return Double.NaN;
         }
         return yValues[i - 1]; // Constant extrapolation.
      }

      // Linear interpolation.
      double x1 = xValues[i - 1];
      double x2 = xValues[i];
      double a = (x - x1) / (x2 - x1);
      return (1 - a) * yValues[i - 1] + a * yValues[i];
   }
}
