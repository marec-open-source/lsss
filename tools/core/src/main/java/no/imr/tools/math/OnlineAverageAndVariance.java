package no.imr.tools.math;

import java.util.Arrays;

public final class OnlineAverageAndVariance {
   private final float[] means;
   private final float[] m2s;
   private int count;
   private float totalWeight;

   public OnlineAverageAndVariance(int length) {
      means = new float[length];
      m2s = new float[length];
   }

   public void update(float[] values, float weight) {
      if (weight <= 0) {
         return;
      }
      float tmpWeight = totalWeight + weight;
      float weightFactor = weight / tmpWeight;
      if (values.length != means.length) {
         values = ArrayMath.resample(values, means.length);
      }
      for (int i = 0; i < means.length; i++) {
         float delta = values[i] - means[i];
         float r = delta * weightFactor;
         means[i] += r;
         m2s[i] += delta * totalWeight * r;
      }
      totalWeight = tmpWeight;
      count++;
   }

   public boolean hasMeans() {
      return count > 0;
   }

   public float[] getMeans() {
      if (count == 0) {
         return getNaNs();
      }
      return means;
   }

   public boolean hasVariances() {
      return count > 1;
   }

   public float[] getVariances() {
      if (count <= 1) {
         return getNaNs();
      }
      float factor = count / (totalWeight * (count - 1));
      float[] variances = m2s.clone();
      ArrayMath.multiply(variances, factor);
      return variances;
   }

   public float[] getStdErrs() {
      if (count <= 1) {
         return getNaNs();
      }
      float[] stdErrs = getVariances();
      for (int i = 0; i < stdErrs.length; i++) {
         stdErrs[i] = (float) Math.sqrt(stdErrs[i] / count);
      }
      return stdErrs;
   }

   private float[] getNaNs() {
      float[] nans = new float[means.length];
      Arrays.fill(nans, Float.NaN);
      return nans;
   }
}
