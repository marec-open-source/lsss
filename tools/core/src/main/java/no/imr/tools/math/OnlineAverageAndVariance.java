package no.imr.tools.math;

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
      return means;
   }

   public boolean hasVariances() {
      return count > 1;
   }

   public float[] getVariances() {
      float factor = count / (totalWeight * (count - 1));
      float[] variances = new float[m2s.length];
      for (int i = 0; i < variances.length; i++) {
         variances[i] = m2s[i] * factor;
      }
      return variances;
   }

   public float[] getStdErrs() {
      float[] stdErrs = getVariances();
      for (int i = 0; i < stdErrs.length; i++) {
         stdErrs[i] = (float) Math.sqrt(stdErrs[i] / count);
      }
      return stdErrs;
   }
}
