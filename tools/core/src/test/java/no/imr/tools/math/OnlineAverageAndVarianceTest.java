package no.imr.tools.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class OnlineAverageAndVarianceTest {
   @Test
   void unweightedAverageAndVariance() {
      OnlineAverageAndVariance averageAndVariance = new OnlineAverageAndVariance(2);
      float[] values = {1, 1, 1, 1};
      for (float value : values) {
         averageAndVariance.update(new float[]{value, 2 * value}, 1);
      }
      assertArrayEquals(new float[]{1, 2}, averageAndVariance.getMeans());
      assertArrayEquals(new float[]{0, 0}, averageAndVariance.getVar());

      averageAndVariance.update(new float[]{3, 3}, 1);

      assertArrayEquals(new float[]{1.4f, 2.2f}, averageAndVariance.getMeans());
      assertArrayEquals(new float[]{0.8f, 0.2f}, averageAndVariance.getVar());
   }

   @Test
   void weightedAverageAndVariance() {
      OnlineAverageAndVariance averageAndVariance = new OnlineAverageAndVariance(2);
      float[] values = {1, 1, 1, 1};
      for (float value : values) {
         averageAndVariance.update(new float[]{value, 2 * value}, 1);
      }
      assertArrayEquals(new float[]{1, 2}, averageAndVariance.getMeans());
      assertArrayEquals(new float[]{0, 0}, averageAndVariance.getVar());

      averageAndVariance.update(new float[]{3, 3}, 4);

      assertArrayEquals(new float[]{2, 2.5f}, averageAndVariance.getMeans());
      assertArrayEquals(new float[]{1.25f, 0.3125f}, averageAndVariance.getVar());
   }
}
