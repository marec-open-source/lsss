package no.imr.tools.math;

import no.imr.tools.Max;
import no.imr.tools.Min;

/**
 * Algorithms for finding the median.
 */
public final class Median {
   private Median() {
   }

   public static int quickSelect(int[] arr) {
      return quickSelect(arr, 0, arr.length);
   }

   public static int quickSelect(int[] arr, int beginIndex, int endIndex) {
      int medianIndex = (beginIndex + endIndex - 1) >>> 1; // Overflow safe middle value
      return QuickSelect.get(arr, medianIndex, beginIndex, endIndex);
   }

   public static double quickSelect(double[] arr) {
      return quickSelect(arr, 0, arr.length);
   }

   public static double quickSelect(double[] arr, int beginIndex, int endIndex) {
      int medianIndex = (beginIndex + endIndex - 1) >>> 1; // Overflow safe middle value
      return QuickSelect.get(arr, medianIndex, beginIndex, endIndex);
   }

   public static float quickSelect(float[] arr) {
      return quickSelect(arr, 0, arr.length);
   }

   public static float quickSelect(float[] arr, int beginIndex, int endIndex) {
      int medianIndex = (beginIndex + endIndex - 1) >>> 1; // Overflow safe middle value
      return QuickSelect.get(arr, medianIndex, beginIndex, endIndex);
   }

   public static float of(float a, float b, float c) {
      if (a < b) {
         if (c < a) return a;
         if (c > b) return b;
      } else {
         if (c < b) return b;
         if (c > a) return a;
      }
      return c;
   }

   public static float of(float a0, float a1, float a2, float a3, float a4, float a5, float a6, float a7, float a8) {
      if (a1 > a2) {
         float tmp = a1;
         a1 = a2;
         a2 = tmp;
      }
      if (a0 > a1) {
         float tmp = a0;
         a0 = a1;
         a1 = tmp;
      }
      if (a1 > a2) {
         float tmp = a1;
         a1 = a2;
         a2 = tmp;
      }

      if (a4 > a5) {
         float tmp = a4;
         a4 = a5;
         a5 = tmp;
      }
      if (a3 > a4) {
         float tmp = a3;
         a3 = a4;
         a4 = tmp;
      }
      if (a4 > a5) {
         float tmp = a4;
         a4 = a5;
         a5 = tmp;
      }

      if (a7 > a8) {
         float tmp = a7;
         a7 = a8;
         a8 = tmp;
      }
      if (a6 > a7) {
         float tmp = a6;
         a6 = a7;
         a7 = tmp;
      }
      if (a7 > a8) {
         float tmp = a7;
         a7 = a8;
         a8 = tmp;
      }

      a2 = Min.of(a2, a5, a8);
      a4 = of(a1, a4, a7);
      a6 = Max.of(a0, a3, a6);

      return of(a2, a4, a6);
   }
}
