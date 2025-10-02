package no.imr.korona.computation.categorization.kdtree;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * A multidimensional point.
 */
final class Point {
   private final float[] x;

   Point(int dimension, float value) {
      x = new float[dimension];
      Arrays.fill(x, value);
   }

   Point(float[] x) {
      this.x = x.clone();
   }

   Point copyWith(int i, float value) {
      Point copy = new Point(x);
      copy.x[i] = value;
      return copy;
   }

   int length() {
      return x.length;
   }

   float x(int i) {
      return x[i];
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof Point that
            && Arrays.equals(x, that.x);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(x);
   }

   float distance2(Point p, float maxDistance2) {
      float d2 = 0;
      for (int i = 0; i < x.length; i++) {
         float tmp = x[i] - p.x[i];
         d2 += tmp * tmp;
         if (d2 > maxDistance2) {
            break;
         }
      }
      return d2;
   }

   @Override
   public String toString() {
      return Arrays.toString(x);
   }
}
