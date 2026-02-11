package no.imr.tools.math;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

final class ArrayMathTest {
   @Test
   void multiplyLong() {
      long[] values = {-1, 0, 2};
      ArrayMath.multiply(values, 3);
      assertArrayEquals(new long[]{-3, 0, 6}, values);
   }

   @Test
   void multiplyFloat() {
      float[] values = {-1, 0, 2};
      ArrayMath.multiply(values, 3);
      assertArrayEquals(new float[]{-3, 0, 6}, values);
   }

   @Test
   void multiplyDouble() {
      double[] values = {-1, 0, 2};
      ArrayMath.multiply(values, 3);
      assertArrayEquals(new double[]{-3, 0, 6}, values);
   }

   @Test
   void divideFloat() {
      float[] values = {3, 6, 12};
      ArrayMath.divide(values, 3);
      assertArrayEquals(new float[]{1, 2, 4}, values);
   }

   @Test
   void divideDouble() {
      double[] values = {3, 6, 12};
      ArrayMath.divide(values, 3);
      assertArrayEquals(new double[]{1, 2, 4}, values);
   }

   @Test
   void interpolate() {
      float[] values = {0, 0, 0, 3};
      ArrayMath.interpolate(values, 0, 3);
      float[] expected = {0, 1, 2, 3};
      assertArrayEquals(expected, values);
   }

   @Test
   void fourierResample() {
      assertArrayEquals(new double[]{1, 1}, ArrayMath.fourierResample(new double[]{1, 1, 1, 1}, 2)); // radix2 in- and output
      assertArrayEquals(new double[]{1, 1}, ArrayMath.fourierResample(new double[]{1, 1, 1, 1, 1}, 2)); // prime, non-radix2 input - radix2 output
      assertArrayEquals(new double[]{1, 1}, ArrayMath.fourierResample(new double[]{1, 1, 1, 1, 1, 1}, 2)); // non-radix2 input - radix2 output
      assertArrayEquals(new double[]{1, 1, 1}, ArrayMath.fourierResample(new double[]{1, 1, 1, 1}, 3)); // radix2 input - prime, non-radix2 output

      assertArrayEquals(new double[]{0, 1}, ArrayMath.fourierResample(new double[]{0, 1, 0, 1}, 2)); // radix2 in- and output
      assertArrayEquals(new double[]{0, 1}, ArrayMath.fourierResample(new double[]{0, 1, 0, 1, 0, 1}, 2)); // non-radix2 input - radix2 output

      assertArrayEquals(new double[]{3}, ArrayMath.fourierResample(new double[]{0, 2, 4, 6}, 1)); // resampling to scalar = mean
   }

   @Test
   void resample() {
      assertArrayEquals(new float[]{1, 1, 1}, ArrayMath.resample(new float[]{1}, 3));
      assertArrayEquals(new float[]{1, 1, 1}, ArrayMath.resample(new float[]{1, 1}, 3));
      assertArrayEquals(new float[]{1, 1, 1}, ArrayMath.resample(new float[]{1, 1, 1}, 3));

      float[] oneToTen = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

      assertThrows(IllegalArgumentException.class, () -> ArrayMath.resample(oneToTen, -0.01, 1, 0));
      assertThrows(IllegalArgumentException.class, () -> ArrayMath.resample(oneToTen, 9, 10.01, 0));
      assertThrows(IllegalArgumentException.class, () -> ArrayMath.resample(oneToTen, 3, 2, 0));

      assertArrayEquals(new float[]{}, ArrayMath.resample(oneToTen, 0.5, 1.5, 0));
      assertArrayEquals(new float[]{1.5f}, ArrayMath.resample(oneToTen, 0.5, 1.5, 1));
      assertArrayEquals(new float[]{1, 2}, ArrayMath.resample(oneToTen, 0.5, 1.5, 2));
      assertArrayEquals(new float[]{1, 1.5f, 2}, ArrayMath.resample(oneToTen, 0.5, 1.5, 3));
      assertArrayEquals(new float[]{1, 1, 2, 2}, ArrayMath.resample(oneToTen, 0.5, 1.5, 4));
      assertArrayEquals(new float[]{1, 1, 1.5f, 2, 2}, ArrayMath.resample(oneToTen, 0.5, 1.5, 5));

      assertArrayEquals(oneToTen, ArrayMath.resample(oneToTen, 0, 10, 10));
      assertArrayEquals(new float[]{1.5f, 3.5f, 5.5f, 7.5f, 9.5f}, ArrayMath.resample(oneToTen, 0, 10, 5));
      assertArrayEquals(new float[]{5.5f}, ArrayMath.resample(oneToTen, 0, 10, 1));

      JUnitUtils.runWithRandom(random -> {
         float[] values = JUnitUtils.createRandomFloatArray(random, random.nextInt(1, 100));
         int iBegin = random.nextInt(0, values.length);
         int iEnd = random.nextInt(iBegin + 1, values.length + 1);
         double mean = ArrayMath.mean(values, iBegin, iEnd);
         float[] resampledValues = ArrayMath.resample(values, iBegin, iEnd, random.nextInt(1, 100));
         assertEquals(mean, ArrayMath.mean(resampledValues), mean * 1e-6);
      });
   }

   @Test
   void meanByte() {
      byte[] values = {1, 2, 3, 4, 5, 6, 7};
      assertEquals(3, ArrayMath.mean(values, 1, 4));
   }

   @Test
   void meanInt() {
      int[] values = {1, 2, 3, 4, 5, 6, 7};
      assertEquals(4, ArrayMath.mean(values));
   }

   @Test
   void meanFloat() {
      float[] values = {1, 2, 3, 4, 5, 6, 7};
      assertEquals(4, ArrayMath.mean(values));
      assertEquals(3, ArrayMath.mean(values, 1, 4));
   }

   @Test
   void partition() {
      int[] values = {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
      assertEquals(5, ArrayMath.partition(values, 5));
      for (int i = 0; i < values.length; i++) {
         assertTrue(i < 5 ^ values[i] > 5);
      }

      assertEquals(2, ArrayMath.partition(new int[]{1, 0, 1, 1, 1, 0, 1, 1}, 0));

      assertEquals(0, ArrayMath.partition(new int[]{1, 2, 3}, 0));
      assertEquals(3, ArrayMath.partition(new int[]{1, 2, 3}, 4));

      assertEquals(0, ArrayMath.partition(new int[]{}, 0));
   }

   @Test
   void partitionRandom() {
      JUnitUtils.runWithRandom(random -> {
         int[] values = random.ints(random.nextInt(20), 0, 20).toArray();
         int pivot = random.nextInt(-5, 25);

         int[] sorted = values.clone();
         Arrays.sort(sorted);
         int iSorted = 0;
         while (iSorted < sorted.length && sorted[iSorted] <= pivot) {
            iSorted++;
         }

         int iPartition = ArrayMath.partition(values, pivot);
         assertEquals(iSorted, iPartition);

         for (int i = 0; i < values.length; i++) {
            assertTrue(i < iPartition ^ values[i] > pivot);
         }
      });
   }
}
