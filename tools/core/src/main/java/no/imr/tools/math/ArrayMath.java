package no.imr.tools.math;

import no.imr.tools.Utils;
import no.imr.tools.misc.FloatUnaryOperator;
import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Arrays;

public final class ArrayMath {
   private ArrayMath() {
   }

   public static void add(float[] values, float addend) {
      for (int i = 0; i < values.length; i++) {
         values[i] += addend;
      }
   }

   public static void add(int[] values, int[] addends) {
      requireSameLength(values, addends);
      for (int i = 0; i < values.length; i++) {
         values[i] += addends[i];
      }
   }

   public static void add(float[] values, float[] addends) {
      requireSameLength(values, addends);
      for (int i = 0; i < values.length; i++) {
         values[i] += addends[i];
      }
   }

   public static void add(double[] values, double[] addends) {
      requireSameLength(values, addends);
      for (int i = 0; i < values.length; i++) {
         values[i] += addends[i];
      }
   }

   public static void multiply(long[] values, long factor) {
      for (int i = 0; i < values.length; i++) {
         values[i] *= factor;
      }
   }

   public static void multiply(float[] values, float factor) {
      for (int i = 0; i < values.length; i++) {
         values[i] *= factor;
      }
   }

   public static void multiply(double[] values, double factor) {
      for (int i = 0; i < values.length; i++) {
         values[i] *= factor;
      }
   }

   public static void multiply(float[] values, float[] factors) {
      requireSameLength(values, factors);
      for (int i = 0; i < values.length; i++) {
         values[i] *= factors[i];
      }
   }

   public static void multiply(double[] values, double[] factors) {
      requireSameLength(values, factors);
      for (int i = 0; i < values.length; i++) {
         values[i] *= factors[i];
      }
   }

   public static void divide(float[] values, float divisor) {
      for (int i = 0; i < values.length; i++) {
         values[i] /= divisor;
      }
   }

   public static void divide(double[] values, double divisor) {
      for (int i = 0; i < values.length; i++) {
         values[i] /= divisor;
      }
   }

   public static int maxIndex(float[] values) {
      float maxValue = values[0];
      int maxValueIndex = 0;
      for (int i = 1; i < values.length; i++) {
         float value = values[i];
         if (value > maxValue) {
            maxValue = value;
            maxValueIndex = i;
         }
      }
      return maxValueIndex;
   }

   public static void interpolate(float[] values, int minIndex, int maxIndex) {
      float a = values[minIndex];
      float b = values[maxIndex];
      float f = (b - a) / (maxIndex - minIndex);
      for (int i = minIndex + 1; i < maxIndex; i++) {
         values[i] = a + (i - minIndex) * f;
      }
   }

   public static double[] fourierResample(double[] values, int newLength) {
      int initialLength = values.length;
      if (initialLength == 0) {
         return values;
      }
      DoubleFFT_1D fft = FftCache.getDouble1D(initialLength);
      fft.realForward(values);
      values = Arrays.copyOf(values, newLength);
      fft = FftCache.getDouble1D(newLength);
      fft.realInverse(values, true);
      multiply(values, (double) values.length / initialLength);
      return values;
   }

   public static float[] fourierResample(float[] values, int newLength) {
      return Utils.toFloats(fourierResample(Utils.toDoubles(values), newLength));
   }

   public static float[] resample(float[] values, int newLength) {
      return resample(values, 0, values.length, newLength);
   }

   public static float[] resample(float[] values, double beginIndex, double endIndex, int newLength) {
      if (beginIndex < 0 || endIndex > values.length || beginIndex > endIndex) {
         throw new IllegalArgumentException(values.length + ", " + beginIndex + ", " + endIndex);
      }
      float[] newValues = new float[newLength];
      double delta = (endIndex - beginIndex) / newLength;
      for (int newIndex = 0; newIndex < newLength; newIndex++) {
         double iBeginAsDouble = beginIndex + newIndex * delta;
         double iEndAsDouble = beginIndex + (newIndex + 1) * delta;
         int iBegin = (int) Math.ceil(iBeginAsDouble);
         int iEnd = (int) Math.floor(iEndAsDouble);
         if (iBegin > iEnd) {
            newValues[newIndex] = values[iEnd];
            continue;
         }
         double sum = 0;
         if (iBegin > 0) {
            sum += values[iBegin - 1] * (iBegin - iBeginAsDouble);
         }
         for (int i = iBegin; i < iEnd; i++) {
            sum += values[i];
         }
         if (iEnd < values.length) {
            sum += values[iEnd] * (iEndAsDouble - iEnd);
         }
         newValues[newIndex] = (float) (sum / delta);
      }
      return newValues;
   }

   public static long sum(byte[] values) {
      return sum(values, 0, values.length);
   }

   public static long sum(byte[] values, int iBegin, int iEnd) {
      long sum = 0;
      for (int i = iBegin; i < iEnd; i++) {
         sum += values[i];
      }
      return sum;
   }

   public static long sum(int[] values) {
      return sum(values, 0, values.length);
   }

   public static long sum(int[] values, int iBegin, int iEnd) {
      long sum = 0;
      for (int i = iBegin; i < iEnd; i++) {
         sum += values[i];
      }
      return sum;
   }

   public static double sum(float[] values) {
      return sum(values, 0, values.length);
   }

   public static double sum(float[] values, int iBegin, int iEnd) {
      double sum = 0;
      for (int i = iBegin; i < iEnd; i++) {
         sum += values[i];
      }
      return sum;
   }

   public static double sum(double[] values) {
      double sum = 0;
      for (double value : values) {
         sum += value;
      }
      return sum;
   }

   public static float sqSum(float[] values) {
      double sum = 0;
      for (float value : values) {
         sum += value * value;
      }
      return (float) sum;
   }

   public static double sqSum(double[] values) {
      double sum = 0;
      for (double value : values) {
         sum += value * value;
      }
      return sum;
   }

   public static void swap(int[] values, int i, int j) {
      int tmp = values[i];
      values[i] = values[j];
      values[j] = tmp;
   }

   public static void swap(float[] values, int i, int j) {
      float tmp = values[i];
      values[i] = values[j];
      values[j] = tmp;
   }

   public static void swap(double[] values, int i, int j) {
      double tmp = values[i];
      values[i] = values[j];
      values[j] = tmp;
   }

   public static float mean(byte[] values, int iBegin, int iEnd) {
      return (float) sum(values, iBegin, iEnd) / values.length;
   }

   public static float mean(int[] values) {
      return (float) sum(values) / values.length;
   }

   public static float mean(float[] values) {
      return mean(values, 0, values.length);
   }

   public static float mean(float[] values, int iBegin, int iEnd) {
      int count = 0;
      double m = 0;
      for (int i = iBegin; i < iEnd; i++) {
         count++;
         m += (values[i] - m) / count;
      }
      return (float) m;
   }

   public static int partition(int[] values, int pivotValue) {
      int low = 0;
      int high = values.length - 1;
      while (true) {
         while (low <= high && values[low] <= pivotValue) {
            low++;
         }
         if (low >= high) {
            return low;
         }
         while (high > low && values[high] > pivotValue) {
            high--;
         }
         swap(values, low, high);
      }
   }

   public static void round(float[] values, float roundingFactor) {
      for (int i = 0; i < values.length; i++) {
         values[i] = Utils.round(values[i], roundingFactor);
      }
   }

   public static void map(float[] values, FloatUnaryOperator operator) {
      for (int i = 0; i < values.length; i++) {
         values[i] = operator.applyAsFloat(values[i]);
      }
   }

   public static void requireSameLength(int[] a, int[] b) {
      if (a.length != b.length) {
         throw new IllegalArgumentException(a.length + " != " + b.length);
      }
   }

   public static void requireSameLength(float[] a, float[] b) {
      if (a.length != b.length) {
         throw new IllegalArgumentException(a.length + " != " + b.length);
      }
   }

   public static void requireSameLength(double[] a, double[] b) {
      if (a.length != b.length) {
         throw new IllegalArgumentException(a.length + " != " + b.length);
      }
   }
}
