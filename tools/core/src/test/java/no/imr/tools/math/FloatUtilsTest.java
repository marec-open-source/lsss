package no.imr.tools.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class FloatUtilsTest {
   @Test
   void interpolate() {
      float[] xValues = {1, 2, 4};
      float[] yValues = {2, 4, 3};
      assertEquals(2, FloatUtils.interpolate(xValues, yValues, 0));
      assertEquals(4, FloatUtils.interpolate(xValues, yValues, 2));
      assertEquals(3.75f, FloatUtils.interpolate(xValues, yValues, 2.5f));
      assertEquals(3, FloatUtils.interpolate(xValues, yValues, 5));
      assertEquals(3, FloatUtils.interpolate(xValues, yValues, Float.POSITIVE_INFINITY));
      assertEquals(Float.NaN, FloatUtils.interpolate(xValues, yValues, Float.NaN));
   }
}
