package no.imr.tools.math;

import java.util.Arrays;

public final class RunningMedian {
   private int size;
   private final float[] values;

   public RunningMedian(int capacity) {
      if (capacity < 1) {
         throw new IllegalArgumentException(Integer.toString(capacity));
      }
      values = new float[capacity];
   }

   public float getMedian() {
      return values[(size - 1) / 2];
   }

   public void clear() {
      size = 0;
   }

   public void add(float value) {
      int i = Arrays.binarySearch(values, 0, size, value);
      if (i < 0) {
         i = -(i + 1); // conversion to insertion point
      }
      System.arraycopy(values, i, values, i + 1, size - i);
      values[i] = value;
      size++;
   }

   public void remove(float value) {
      int i = Arrays.binarySearch(values, 0, size, value);
      if (i < 0) {
         throw new IllegalArgumentException(Float.toString(value));
      }
      int n = size - i - 1;
      if (n > 0) {
         System.arraycopy(values, i + 1, values, i, n);
      }
      size--;
   }

   public void replace(float oldValue, float newValue) {
      int iOld = Arrays.binarySearch(values, 0, size, oldValue);
      if (iOld < 0) {
         throw new IllegalArgumentException(Float.toString(oldValue));
      }
      int iNew = Arrays.binarySearch(values, 0, size, newValue);

      if (iNew < 0) {
         iNew = -(iNew + 1); // conversion to insertion point

         if (iNew > iOld) {
            iNew--;
         }
      }

      if (iNew > iOld) {
         System.arraycopy(values, iOld + 1, values, iOld, iNew - iOld);
      } else if (iNew < iOld) {
         System.arraycopy(values, iNew, values, iNew + 1, iOld - iNew);
      }
      //else: (iOld == iNew) => Nothing to copy

      values[iNew] = newValue;
   }

   public void apply(float[] in, float[] out) {
      apply(in, out, 0, in.length);
   }

   public void apply(float[] in, float[] out, int beginIndex, int endIndex) {
      int addOffset = values.length / 2;
      int removeOffset = values.length - addOffset;

      size = Math.min(endIndex - beginIndex, addOffset);
      System.arraycopy(in, beginIndex, values, 0, size);
      Arrays.sort(values, 0, size);

      int n2 = Math.min(endIndex, beginIndex + removeOffset);
      int n1 = Math.clamp(endIndex - addOffset, beginIndex, n2);

      for (int i = beginIndex; i < n1; i++) {
         add(in[i + addOffset]);
         out[i] = getMedian();
      }
      Arrays.fill(out, n1, n2, getMedian());

      int n3 = Math.max(n2, endIndex - addOffset);
      for (int i = n2; i < n3; i++) {
         replace(in[i - removeOffset], in[i + addOffset]);
         out[i] = getMedian();
      }

      int n4 = Math.clamp(beginIndex + removeOffset, n3, endIndex);
      Arrays.fill(out, n3, n4, getMedian());

      for (int i = n4; i < endIndex; i++) {
         remove(in[i - removeOffset]);
         out[i] = getMedian();
      }
   }
}
