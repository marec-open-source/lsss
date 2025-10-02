package no.imr.tools.range;

import no.imr.tools.Utils;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class IntRangeSet implements Iterable<IntRange> {
   private int[] indexes;

   public IntRangeSet() {
      indexes = Utils.EMPTY_INT_ARRAY;
   }

   public IntRangeSet(int begin, int end) {
      if (begin < end) {
         indexes = new int[]{begin, end};
      } else {
         indexes = Utils.EMPTY_INT_ARRAY;
      }
   }

   public IntRangeSet(List<Integer> indexList) {
      if (indexList.size() % 2 != 0) {
         throw new IllegalArgumentException(String.valueOf(indexList.size()));
      }
      indexes = Utils.toInts(indexList);
   }

   public IntRangeSet(List<Integer> indexList, int maxIndex) {
      int n = indexList.size();
      indexes = new int[n + n % 2];
      for (int i = 0; i < n; i++) {
         indexes[i] = indexList.get(i);
      }
      if (n % 2 != 0) {
         indexes[n] = maxIndex;
      }
   }

   @Override
   public String toString() {
      return Arrays.toString(indexes);
   }

   public IntRangeSet createCopy() {
      IntRangeSet copy = new IntRangeSet();
      copy.indexes = indexes.clone();
      return copy;
   }

   public int[] getIndexes() {
      return indexes;
   }

   public boolean contains(int value) {
      int i = Arrays.binarySearch(indexes, value);
      if (i >= 0) {
         return i % 2 == 0;
      } else {
         i = -1 - i;
         return i % 2 != 0;
      }
   }

   public boolean isEmpty() {
      return indexes.length == 0;
   }

   public void clear() {
      indexes = Utils.EMPTY_INT_ARRAY;
   }

   public boolean add(int begin, int end) {
      int i0 = Arrays.binarySearch(indexes, begin);
      int i1 = Arrays.binarySearch(indexes, end);

      if (i0 < 0) {
         i0 = -1 - i0;
      }
      int low = i0 - i0 % 2;

      int high;
      if (i1 >= 0) {
         high = i1 + (i1 + 1) % 2;
      } else {
         i1 = -1 - i1;
         high = i1 - (i1 + 1) % 2;
      }

      return replaceRange(begin, end, low, high);
   }

   public boolean remove(int begin, int end) {
      int i0 = Arrays.binarySearch(indexes, begin);
      int i1 = Arrays.binarySearch(indexes, end);

      if (i0 < 0) {
         i0 = -1 - i0;
      }
      int low = i0 - (i0 + 1) % 2;

      int high;
      if (i1 >= 0) {
         high = i1 + i1 % 2;
      } else {
         i1 = -1 - i1;
         high = i1 - i1 % 2;
      }

      return replaceRange(begin, end, low, high);
   }

   private boolean replaceRange(int begin, int end, int low, int high) {
      if (end <= begin) {
         return false;
      }

      if (high < low) {
         // Insert new range
         int[] newIndexes = reallocate(low, high);
         newIndexes[low] = begin;
         newIndexes[low + 1] = end;
         indexes = newIndexes;
      } else if (high == low + 1) {
         // Only one existing range affected
         if ((low < 0 || begin >= indexes[low]) && (high >= indexes.length || end <= indexes[high])) {
            // Specified range ok
            return false;
         }
         if (low >= 0) {
            indexes[low] = Math.min(indexes[low], begin);
         }
         if (low + 1 < indexes.length) {
            indexes[low + 1] = Math.max(indexes[high], end);
         }
      } else {
         // Coalesce existing ranges
         int[] newIndexes = reallocate(low, high);
         if (low >= 0) {
            newIndexes[low] = Math.min(indexes[low], begin);
         }
         if (low + 1 < newIndexes.length) {
            newIndexes[low + 1] = Math.max(indexes[high], end);
         }
         indexes = newIndexes;
      }

      return true;
   }

   public void clampRange(int begin, int end) {
      if (isEmpty()) {
         return;
      }
      remove(indexes[0], begin);
      if (isEmpty()) {
         return;
      }
      remove(end, indexes[indexes.length - 1]);
   }

   private int[] reallocate(int low, int high) {
      int indexesToRemove = high + 1 - (low + 2);
      int newLength = indexes.length - indexesToRemove;
      if (newLength == 0) {
         return Utils.EMPTY_INT_ARRAY;
      }
      int[] newIndexes = new int[newLength];
      if (low > 0) {
         System.arraycopy(indexes, 0, newIndexes, 0, low);
      }
      if (low + 2 < newIndexes.length) {
         System.arraycopy(indexes, high + 1, newIndexes, low + 2, newIndexes.length - (low + 2));
      }
      return newIndexes;
   }

   public boolean add(IntRangeSet intRangeSet) {
      boolean changed = false;
      int[] indexes = intRangeSet.getIndexes();
      for (int i = 0; i < indexes.length; ) {
         int begin = indexes[i++];
         int end = indexes[i++];
         changed |= add(begin, end);
      }
      return changed;
   }

   public boolean remove(IntRangeSet intRangeSet) {
      boolean changed = false;
      int[] indexes = intRangeSet.getIndexes();
      for (int i = 0; i < indexes.length; ) {
         int begin = indexes[i++];
         int end = indexes[i++];
         changed |= remove(begin, end);
      }
      return changed;
   }

   public boolean intersects(IntRangeSet intRangeSet) {
      return intersects(indexes, intRangeSet.getIndexes());
   }

   private static boolean intersects(int[] indexes0, int[] indexes1) {
      if (indexes0.length == 0 || indexes1.length == 0) {
         return false;
      }

      int beginIndex0 = indexes0[0];
      int endIndex0 = indexes0[1];
      int beginIndex1 = indexes1[0];
      int endIndex1 = indexes1[1];
      int i0 = 2;
      int i1 = 2;
      while (true) {
         if (beginIndex0 < endIndex1 && beginIndex1 < endIndex0) {
            return true;
         }

         if (endIndex0 <= beginIndex1 && i0 < indexes0.length) {
            beginIndex0 = indexes0[i0++];
            endIndex0 = indexes0[i0++];
            continue;
         }
         if (endIndex1 <= beginIndex0 && i1 < indexes1.length) {
            beginIndex1 = indexes1[i1++];
            endIndex1 = indexes1[i1++];
            continue;
         }

         return false;
      }
   }

   public String getDeltaString() {
      StringBuilder sb = new StringBuilder();
      int lastIndex = 0;
      for (int index : indexes) {
         if (!sb.isEmpty()) {
            sb.append(' ');
         }
         sb.append(index - lastIndex);
         lastIndex = index;
      }
      return sb.toString();
   }

   public Stream<IntRange> stream() {
      return IntStream.range(0, indexes.length / 2)
            .mapToObj(i -> new IntRange(indexes[2 * i], indexes[2 * i + 1]));
   }

   @Override
   public Iterator<IntRange> iterator() {
      return stream().iterator();
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof IntRangeSet that
            && Arrays.equals(indexes, that.indexes);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(indexes);
   }
}
