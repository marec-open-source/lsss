package no.imr.tools.math;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RunningMedianTest {
   @Test
   void random() {
      JUnitUtils.runWithRandom(random -> {
         int maxSize = 100;
         float[] values = new float[random.nextInt(maxSize)];
         if (random.nextBoolean()) {
            // Random float values
            for (int i = 0; i < values.length; i++) {
               values[i] = random.nextFloat();
            }
         } else {
            // Lots of duplicate int values
            for (int i = 0; i < values.length; i++) {
               values[i] = random.nextInt(maxSize);
            }
         }
         float[] result = new float[values.length];

         int filterSize = 1 + random.nextInt(maxSize);

         int endIndex = values.length == 0 ? 0 : random.nextInt(values.length);
         int beginIndex = endIndex == 0 ? 0 : random.nextInt(endIndex);
         RunningMedian runningMedian = new RunningMedian(filterSize);
         runningMedian.apply(values, result, beginIndex, endIndex);

         float[] control = controlFiltering(values, filterSize, beginIndex, endIndex);

         assertArrayEquals(control, result);
      });
   }

   private static float[] controlFiltering(float[] values, int filterSize, int beginIndex, int endIndex) {
      int rightRadius = filterSize / 2;
      int leftRadius = filterSize - rightRadius - 1;

      float[] control = new float[values.length];
      float[] tmp = new float[filterSize];
      for (int i = beginIndex; i < endIndex; i++) {
         int begin = Math.max(i - leftRadius, beginIndex);
         int end = Math.min(i + rightRadius + 1, endIndex);
         int n = end - begin;
         System.arraycopy(values, begin, tmp, 0, n);
         control[i] = Median.quickSelect(tmp, 0, n);
      }
      return control;
   }
}
