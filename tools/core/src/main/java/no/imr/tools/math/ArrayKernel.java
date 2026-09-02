package no.imr.tools.math;

import java.util.Objects;

public final class ArrayKernel {
   private final float[] weights;
   private final int center;

   private ArrayKernel(float[] weights, int center) {
      Objects.checkIndex(center, weights.length);
      this.weights = weights;
      this.center = center;
   }

   public float[] smooth(float[] input) {
      float[] result = new float[input.length];
      smooth(input, result);
      return result;
   }

   public void smooth(float[] input, float[] result) {
      ArrayMath.requireSameLength(input, result);
      for (int i = 0; i < input.length; i++) {
         int begin = Math.max(-i, -center);
         int end = Math.min(input.length - i, weights.length - center);
         double valueSum = 0;
         double weightSum = 0;
         for (int j = begin; j < end; j++) {
            float weight = weights[center + j];
            weightSum += weight;
            valueSum += weight * input[i + j];
         }
         result[i] = (float) (valueSum / weightSum);
      }
   }

   public static ArrayKernel createGaussian(double standardDeviation) {
      if (standardDeviation < 0) {
         throw new IllegalArgumentException(standardDeviation + " < 0");
      }
      if (standardDeviation == 0) {
         return new ArrayKernel(new float[]{1}, 0);
      }
      int center = (int) Math.ceil(3 * standardDeviation);
      int n = 2 * center + 1;
      float[] gaussian = new float[n];
      double denominator = 2 * standardDeviation * standardDeviation;
      double sum = 0;
      for (int i = 0; i < gaussian.length; i++) {
         double x = i - center;
         double y = Math.exp(-x * x / denominator);
         sum += y;
         gaussian[i] = (float) y;
      }
      ArrayMath.divide(gaussian, (float) sum);
      return new ArrayKernel(gaussian, center);
   }
}
