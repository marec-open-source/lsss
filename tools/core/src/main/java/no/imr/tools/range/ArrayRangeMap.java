package no.imr.tools.range;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implementation of a RangeMap.
 */
public final class ArrayRangeMap<K extends Comparable<? super K>, V> implements RangeMap<K, V> {
   private final List<PointEntry<K, @Nullable V>> entries;

   private ArrayRangeMap(List<PointEntry<K, @Nullable V>> entries) {
      this.entries = entries;
   }

   public ArrayRangeMap() {
      this(new ArrayList<>());
   }

   @Override
   public RangeMap<K, V> copy() {
      return new ArrayRangeMap<>(new ArrayList<PointEntry<K, @Nullable V>>(entries));
   }

   @Override
   public void put(K beginKey, K endKey, @Nullable V value) {
      // Test for empty range.
      if (beginKey.compareTo(endKey) >= 0) {
         return;
      }

      // Inserted range start point.
      int iBegin = Collections.binarySearch(entries, beginKey);
      V leftValue;
      if (iBegin >= 0) {
         leftValue = entries.get(iBegin).value;
         entries.set(iBegin, new PointEntry<>(beginKey, value));
      } else {
         iBegin = -1 - iBegin;
         leftValue = iBegin == 0 ? null : entries.get(iBegin - 1).value;
         entries.add(iBegin, new PointEntry<>(beginKey, value));
      }

      // Insert end point.
      int iEnd = Collections.binarySearch(entries, endKey);
      if (iEnd >= 0) {
         // End point of the inserted range is already there.
      } else {
         iEnd = -1 - iEnd;
         if (iEnd == iBegin + 1) { // Inserted range contained in one existing range.
            entries.add(iEnd, new PointEntry<>(endKey, leftValue));
         } else { // Inserted range begins and ends in different existing ranges.
            entries.add(iEnd, new PointEntry<>(endKey, entries.get(iEnd - 1).value));
         }
      }

      // Combine with neighbouring ranges if values are equal.
      if (iBegin > 0 && Objects.equals(value, entries.get(iBegin - 1).value)) {
         iBegin--;
      }
      if (iEnd < entries.size() - 1 && Objects.equals(value, entries.get(iEnd).value)) {
         iEnd++;
      }

      if (value == null) {
         if (iBegin == 0) {
            iBegin--;
         }
         if (iEnd == entries.size() - 1) {
            iEnd++;
         }
      }

      // Remove old entries contained in the inserted range.
      iBegin++;
      if (iBegin < iEnd) {
         entries.subList(iBegin, iEnd).clear();
      }
   }

   @Override
   public @Nullable V get(K key) {
      int i = Collections.binarySearch(entries, key);
      if (i >= 0) {
         return entries.get(i).value;
      } else {
         i = -1 - i;
         return i == 0 ? null : entries.get(i - 1).value;
      }
   }

   @Override
   public void clear() {
      entries.clear();
   }

   @Override
   public int size() {
      int rangeCount = 0;
      for (PointEntry<K, @Nullable V> entry : entries) {
         if (entry.value != null) {
            rangeCount++;
         }
      }
      return rangeCount;
   }

   @Override
   public boolean isEmpty() {
      return entries.isEmpty();
   }

   int entryCount() {
      return entries.size();
   }

   @Override
   public Stream<Entry<K, V>> stream() {
      return IntStream.range(0, entries.size() - 1)
            .mapToObj(i -> {
               PointEntry<K, @Nullable V> entry = entries.get(i);
               V value = entry.value;
               if (value == null) {
                  // This entry was only end of previous range, and not begin of next.
                  return null;
               }
               return new Entry<>(new DefaultRange<>(entry.key, entries.get(i + 1).key), value);
            })
            .filter(Objects::nonNull);
   }

   @Override
   public Stream<Entry<K, V>> stream(Range<K> range) {
      if (range.isEmpty()) {
         return Stream.empty();
      }
      int iBegin = Collections.binarySearch(entries, range.begin());
      if (iBegin < 0) {
         // Begin at one before insertion index
         iBegin = Math.max(0, -2 - iBegin);
      }
      int iEnd = Collections.binarySearch(entries, range.end());
      if (iEnd < 0) {
         // End at a valid index
         iEnd = Math.min(entries.size() - 1, -1 - iEnd);
      }
      int iBeginFinal = iBegin;
      int iEndMinus1 = iEnd - 1;
      return IntStream.range(iBegin, iEnd)
            .mapToObj(i -> {
               PointEntry<K, @Nullable V> entry = entries.get(i);
               V value = entry.value;
               if (value == null) {
                  // This entry was only end of previous range, and not begin of next.
                  return null;
               }
               K beginKey = entry.key;
               if (i == iBeginFinal && beginKey.compareTo(range.begin()) < 0) {
                  beginKey = range.begin();
               }
               K endKey = entries.get(i + 1).key;
               if (i == iEndMinus1 && endKey.compareTo(range.end()) > 0) {
                  endKey = range.end();
               }
               return new Entry<>(new DefaultRange<>(beginKey, endKey), value);
            })
            .filter(Objects::nonNull);
   }

   /**
    * The beginning of a range. The range continues to, but not including, the next entry.
    * If the map is non-empty, there is always a last entry with value null representing the end of the last range.
    */
   private record PointEntry<K extends Comparable<? super K>, V extends @Nullable Object>(
         K key,
         V value
   ) implements Comparable<K> {
      @Override
      public int compareTo(K other) {
         return key.compareTo(other);
      }
   }
}
