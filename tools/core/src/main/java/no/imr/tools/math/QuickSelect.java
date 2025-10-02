package no.imr.tools.math;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Finds the kth smallest value of an array using the <a href="https://en.wikipedia.org/wiki/Quickselect">Quickselect</a> algorithm.
 * <p>
 * NB: The input array is modified.
 * <p>
 * Code is based on "Fast median search: an ANSI C implementation" by Nicolas Devillard.
 * See <a href="http://ndevilla.free.fr/median/median/">http://ndevilla.free.fr/median/median/</a>
 */
public final class QuickSelect {
   private QuickSelect() {
   }

   public static int get(int[] arr, int targetIndex) {
      return get(arr, targetIndex, 0, arr.length);
   }

   public static int get(int[] arr, int targetIndex, int beginIndex, int endIndex) {
      int low = beginIndex;
      int high = endIndex - 1;
      for (; ; ) {
         if (high <= low) { /* One element only */
            return arr[targetIndex];
         }

         if (high == low + 1) {  /* Two elements only */
            if (arr[low] > arr[high]) {
               ArrayMath.swap(arr, low, high);
            }
            return arr[targetIndex];
         }

         /* Find median of low, middle and high items; swap into position low */
         int middle = (low + high) >>> 1; // Overflow safe middle value
         if (arr[middle] > arr[high]) {
            ArrayMath.swap(arr, middle, high);
         }
         if (arr[low] > arr[high]) {
            ArrayMath.swap(arr, low, high);
         }
         if (arr[middle] > arr[low]) {
            ArrayMath.swap(arr, middle, low);
         }

         /* Swap low item (now in position middle) into position (low+1) */
         ArrayMath.swap(arr, middle, low + 1);

         /* Nibble from each end towards middle, swapping items when stuck */
         int ll = low + 1;
         int hh = high;
         for (; ; ) {
            do {
               ll++;
            } while (arr[low] > arr[ll]);
            do {
               hh--;
            } while (arr[hh] > arr[low]);

            if (hh < ll) {
               break;
            }

            ArrayMath.swap(arr, ll, hh);
         }

         /* Swap middle item (in position low) back into correct position */
         ArrayMath.swap(arr, low, hh);

         /* Re-set active partition */
         if (hh <= targetIndex) {
            low = ll;
         }
         if (hh >= targetIndex) {
            high = hh - 1;
         }
      }
   }

   public static double get(double[] arr, int targetIndex) {
      return get(arr, targetIndex, 0, arr.length);
   }

   public static double get(double[] arr, int targetIndex, int beginIndex, int endIndex) {
      int low = beginIndex;
      int high = endIndex - 1;
      for (; ; ) {
         if (high <= low) { /* One element only */
            return arr[targetIndex];
         }

         if (high == low + 1) {  /* Two elements only */
            if (arr[low] > arr[high]) {
               ArrayMath.swap(arr, low, high);
            }
            return arr[targetIndex];
         }

         /* Find median of low, middle and high items; swap into position low */
         int middle = (low + high) >>> 1; // Overflow safe middle value
         if (arr[middle] > arr[high]) {
            ArrayMath.swap(arr, middle, high);
         }
         if (arr[low] > arr[high]) {
            ArrayMath.swap(arr, low, high);
         }
         if (arr[middle] > arr[low]) {
            ArrayMath.swap(arr, middle, low);
         }

         /* Swap low item (now in position middle) into position (low+1) */
         ArrayMath.swap(arr, middle, low + 1);

         /* Nibble from each end towards middle, swapping items when stuck */
         int ll = low + 1;
         int hh = high;
         for (; ; ) {
            do {
               ll++;
            } while (arr[low] > arr[ll]);
            do {
               hh--;
            } while (arr[hh] > arr[low]);

            if (hh < ll) {
               break;
            }

            ArrayMath.swap(arr, ll, hh);
         }

         /* Swap middle item (in position low) back into correct position */
         ArrayMath.swap(arr, low, hh);

         /* Re-set active partition */
         if (hh <= targetIndex) {
            low = ll;
         }
         if (hh >= targetIndex) {
            high = hh - 1;
         }
      }
   }

   public static float get(float[] arr, int targetIndex) {
      return get(arr, targetIndex, 0, arr.length);
   }

   public static float get(float[] arr, int targetIndex, int beginIndex, int endIndex) {
      int low = beginIndex;
      int high = endIndex - 1;
      for (; ; ) {
         if (high <= low) { /* One element only */
            return arr[targetIndex];
         }

         if (high == low + 1) {  /* Two elements only */
            if (arr[low] > arr[high]) {
               ArrayMath.swap(arr, low, high);
            }
            return arr[targetIndex];
         }

         /* Find median of low, middle and high items; swap into position low */
         int middle = (low + high) >>> 1; // Overflow safe middle value
         if (arr[middle] > arr[high]) {
            ArrayMath.swap(arr, middle, high);
         }
         if (arr[low] > arr[high]) {
            ArrayMath.swap(arr, low, high);
         }
         if (arr[middle] > arr[low]) {
            ArrayMath.swap(arr, middle, low);
         }

         /* Swap low item (now in position middle) into position (low+1) */
         ArrayMath.swap(arr, middle, low + 1);

         /* Nibble from each end towards middle, swapping items when stuck */
         int ll = low + 1;
         int hh = high;
         for (; ; ) {
            do {
               ll++;
            } while (arr[low] > arr[ll]);
            do {
               hh--;
            } while (arr[hh] > arr[low]);

            if (hh < ll) {
               break;
            }

            ArrayMath.swap(arr, ll, hh);
         }

         /* Swap middle item (in position low) back into correct position */
         ArrayMath.swap(arr, low, hh);

         /* Re-set active partition */
         if (hh <= targetIndex) {
            low = ll;
         }
         if (hh >= targetIndex) {
            high = hh - 1;
         }
      }
   }

   public static <T> T get(List<T> arr, Comparator<? super T> comparator, int targetIndex) {
      return get(arr, comparator, targetIndex, 0, arr.size());
   }

   public static <T> T get(List<T> arr, Comparator<? super T> comparator, int targetIndex, int beginIndex, int endIndex) {
      int low = beginIndex;
      int high = endIndex - 1;
      for (; ; ) {
         if (high <= low) { /* One element only */
            return arr.get(targetIndex);
         }

         if (high == low + 1) {  /* Two elements only */
            if (comparator.compare(arr.get(low), arr.get(high)) > 0) {
               Collections.swap(arr, low, high);
            }
            return arr.get(targetIndex);
         }

         /* Find median of low, middle and high items; swap into position low */
         int middle = (low + high) >>> 1; // Overflow safe middle value
         if (comparator.compare(arr.get(middle), arr.get(high)) > 0) {
            Collections.swap(arr, middle, high);
         }
         if (comparator.compare(arr.get(low), arr.get(high)) > 0) {
            Collections.swap(arr, low, high);
         }
         if (comparator.compare(arr.get(middle), arr.get(low)) > 0) {
            Collections.swap(arr, middle, low);
         }

         /* Swap low item (now in position middle) into position (low+1) */
         Collections.swap(arr, middle, low + 1);

         /* Nibble from each end towards middle, swapping items when stuck */
         int ll = low + 1;
         int hh = high;
         for (; ; ) {
            do {
               ll++;
            } while (comparator.compare(arr.get(low), arr.get(ll)) > 0);
            do {
               hh--;
            } while (comparator.compare(arr.get(hh), arr.get(low)) > 0);

            if (hh < ll) {
               break;
            }

            Collections.swap(arr, ll, hh);
         }

         /* Swap middle item (in position low) back into correct position */
         Collections.swap(arr, low, hh);

         /* Re-set active partition */
         if (hh <= targetIndex) {
            low = ll;
         }
         if (hh >= targetIndex) {
            high = hh - 1;
         }
      }
   }
}
