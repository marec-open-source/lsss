package no.imr.tools.math;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

final class OnlineAverageAndVarianceTest {
   @Test
   void hasMeansAndVariances() {
      OnlineAverageAndVariance averageAndVariance = new OnlineAverageAndVariance(2);
      assertFalse(averageAndVariance.hasMeans());
      assertFalse(averageAndVariance.hasVariances());
      assertArrayEquals(new float[]{Float.NaN, Float.NaN}, averageAndVariance.getMeans());
      assertArrayEquals(new float[]{Float.NaN, Float.NaN}, averageAndVariance.getVariances());
      assertArrayEquals(new float[]{Float.NaN, Float.NaN}, averageAndVariance.getStdErrs());

      averageAndVariance.update(new float[]{1, 2}, 1);
      assertTrue(averageAndVariance.hasMeans());
      assertFalse(averageAndVariance.hasVariances());
      assertArrayEquals(new float[]{1, 2}, averageAndVariance.getMeans());
      assertArrayEquals(new float[]{Float.NaN, Float.NaN}, averageAndVariance.getVariances());
      assertArrayEquals(new float[]{Float.NaN, Float.NaN}, averageAndVariance.getStdErrs());

      averageAndVariance.update(new float[]{1, 2}, 1);
      assertTrue(averageAndVariance.hasMeans());
      assertTrue(averageAndVariance.hasVariances());
      assertArrayEquals(new float[]{1, 2}, averageAndVariance.getMeans());
      assertArrayEquals(new float[]{0, 0}, averageAndVariance.getVariances());
      assertArrayEquals(new float[]{0, 0}, averageAndVariance.getStdErrs());
   }

   @Test
   void meanAndVariance() {
      double[] values = {-1, 0, 0, 1, 2, 2, 3, 3, 3, 3, 4, 4, 5};
      double mean = Arrays.stream(values).sum() / values.length;
      double variance = Arrays.stream(values).map(v -> MathUtils.sq(v - mean)).sum() / (values.length - 1);
      double stdErr = Math.sqrt(variance / values.length);

      OnlineAverageAndVariance averageAndVariance = new OnlineAverageAndVariance(1);
      for (double value : values) {
         averageAndVariance.update(new float[]{(float) value}, 1);
      }
      assertEquals(mean, averageAndVariance.getMeans()[0], 1e-5);
      assertEquals(variance, averageAndVariance.getVariances()[0], 1e-5);
      assertEquals(stdErr, averageAndVariance.getStdErrs()[0], 1e-6);
   }

   @Test
   void unweightedAverageAndVariance() {
      OnlineAverageAndVariance averageAndVariance = new OnlineAverageAndVariance(2);
      float[] values = {1, 1, 1, 1};
      for (float value : values) {
         averageAndVariance.update(new float[]{value, 2 * value}, 1);
      }
      assertArrayEquals(new float[]{1, 2}, averageAndVariance.getMeans());
      assertArrayEquals(new float[]{0, 0}, averageAndVariance.getVariances());

      averageAndVariance.update(new float[]{3, 3}, 1);

      assertArrayEquals(new float[]{1.4f, 2.2f}, averageAndVariance.getMeans());
      assertArrayEquals(new float[]{0.8f, 0.2f}, averageAndVariance.getVariances());
   }

   @Test
   void weightedAverageAndVariance() {
      OnlineAverageAndVariance averageAndVariance = new OnlineAverageAndVariance(2);
      averageAndVariance.update(new float[]{0, 0}, 0);

      float[] values = {1, 1, 1, 1};
      for (float value : values) {
         averageAndVariance.update(new float[]{value, 2 * value}, 1);
      }
      assertArrayEquals(new float[]{1, 2}, averageAndVariance.getMeans());
      assertArrayEquals(new float[]{0, 0}, averageAndVariance.getVariances());

      averageAndVariance.update(new float[]{3, 3}, 4);

      assertArrayEquals(new float[]{2, 2.5f}, averageAndVariance.getMeans());
      assertArrayEquals(new float[]{1.25f, 0.3125f}, averageAndVariance.getVariances());
   }
}
