package no.imr.tools.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DoubleUtilsTest {
   @Test
   void interpolate() {
      double[] xValues = {1, 2, 4};
      double[] yValues = {2, 4, 3};
      assertEquals(2, DoubleUtils.interpolate(xValues, yValues, 0));
      assertEquals(4, DoubleUtils.interpolate(xValues, yValues, 2));
      assertEquals(3.75, DoubleUtils.interpolate(xValues, yValues, 2.5));
      assertEquals(3, DoubleUtils.interpolate(xValues, yValues, 5));
      assertEquals(3, DoubleUtils.interpolate(xValues, yValues, Double.POSITIVE_INFINITY));
      assertEquals(Double.NaN, DoubleUtils.interpolate(xValues, yValues, Double.NaN));
   }
}
