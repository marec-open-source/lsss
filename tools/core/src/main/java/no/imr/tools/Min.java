package no.imr.tools;

public final class Min {
   private Min() {
   }

   public static int of(int a, int b) {
      return Math.min(a, b);
   }

   public static long of(long a, long b) {
      return Math.min(a, b);
   }

   public static float of(float a, float b) {
      return Math.min(a, b);
   }

   public static double of(double a, double b) {
      return Math.min(a, b);
   }

   public static int of(int a, int b, int c) {
      return Math.min(Math.min(a, b), c);
   }

   public static long of(long a, long b, long c) {
      return Math.min(Math.min(a, b), c);
   }

   public static float of(float a, float b, float c) {
      return Math.min(Math.min(a, b), c);
   }

   public static double of(double a, double b, double c) {
      return Math.min(Math.min(a, b), c);
   }

   public static int of(int... values) {
      return of(values, 0, values.length);
   }

   public static int of(int[] values, int beginIndex, int endIndex) {
      int result = Integer.MAX_VALUE;
      for (int i = beginIndex; i < endIndex; i++) {
         int value = values[i];
         if (value < result) {
            result = value;
         }
      }
      return result;
   }

   public static float of(float... values) {
      return of(values, 0, values.length);
   }

   public static float of(float[] values, int beginIndex, int endIndex) {
      float result = Float.POSITIVE_INFINITY;
      for (int i = beginIndex; i < endIndex; i++) {
         float value = values[i];
         if (value < result) {
            result = value;
         }
      }
      return result;
   }

   public static <T extends Comparable<? super T>> T of(T a, T b) {
      return a.compareTo(b) < 0 ? a : b;
   }
}
