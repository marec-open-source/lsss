package no.imr.tools.range;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities for ranges and related classes.
 */
public final class RangeUtils {
   public static final Range<Long> ALL_LONGS = new DefaultRange<>(Long.MIN_VALUE, Long.MAX_VALUE);
   public static final Range<Float> ALL_FLOATS = new DefaultRange<>(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

   private static final RangeMap<?, ?> EMPTY_RANGE_MAP = new EmptyRangeMap<>();
   private static final RangeSet<?> EMPTY_RANGE_SET = new RangeMapBasedRangeSet<>(RangeUtils.<Integer, Boolean>emptyRangeMap());

   private RangeUtils() {
   }

   public static <K extends Comparable<? super K>, V> void replaceValues(RangeMap<K, V> rangeMap, Range<K> range, V value, Predicate<V> predicate) {
      List<Range<K>> ranges = rangeMap.stream(range)
            .filter(entry -> predicate.test(entry.value()))
            .map(RangeMap.Entry::range)
            .toList();
      // Modify rangeMap after iteration is done.
      ranges.forEach(r -> rangeMap.put(r, value));
   }

   public static <K extends Comparable<? super K>, V extends Comparable<? super V>> NavigableSet<V> getValueSet(RangeMap<K, V> rangeMap, Range<K> range) {
      return rangeMap.stream(range)
            .map(RangeMap.Entry::value)
            .collect(Collectors.toCollection(TreeSet::new));
   }

   public static <K extends Comparable<? super K>, V> @Nullable V getCompletelyMappedSingleValue(RangeMap<K, V> rangeMap, Range<K> range) {
      Iterator<RangeMap.Entry<K, V>> iterator = rangeMap.stream(range).iterator();
      if (iterator.hasNext()) {
         RangeMap.Entry<K, V> entry = iterator.next();
         if (!iterator.hasNext() && entry.range().equals(range)) {
            return entry.value();
         }
      }
      return null;
   }

   public static <K extends Comparable<? super K>> List<K> subList(List<K> list, Range<K> range) {
      if (range.isEmpty()) {
         return List.of();
      }
      int iBegin = Collections.binarySearch(list, range.begin());
      if (iBegin < 0) {
         iBegin = -1 - iBegin;
      }
      int iEnd = Collections.binarySearch(list, range.end());
      if (iEnd < 0) {
         iEnd = -1 - iEnd;
      }
      return list.subList(iBegin, iEnd);
   }

   public static <K extends Comparable<? super K>> RangeSet<K> toComplement(RangeSet<K> rangeSet, Range<K> range) {
      RangeSet<K> complement = new ArrayRangeSet<>(range);
      rangeSet.stream(range)
            .forEach(complement::remove);
      return complement;
   }

   @SuppressWarnings("unchecked")
   public static <K extends Comparable<K>, V> RangeMap<K, V> emptyRangeMap() {
      return (RangeMap<K, V>) EMPTY_RANGE_MAP;
   }

   @SuppressWarnings("unchecked")
   public static <K extends Comparable<K>> RangeSet<K> emptyRangeSet() {
      return (RangeSet<K>) EMPTY_RANGE_SET;
   }

   /**
    * Immutable empty RangeMap.
    */
   private static final class EmptyRangeMap<K extends Comparable<? super K>, V> implements RangeMap<K, V> {
      private EmptyRangeMap() {
      }

      @Override
      public RangeMap<K, V> copy() {
         return this;
      }

      @Override
      public void put(K beginKey, K endKey, @Nullable V value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public @Nullable V get(K key) {
         return null;
      }

      @Override
      public void clear() {
      }

      @Override
      public int size() {
         return 0;
      }

      @Override
      public boolean isEmpty() {
         return true;
      }

      @Override
      public Stream<Entry<K, V>> stream() {
         return Stream.empty();
      }

      @Override
      public Stream<Entry<K, V>> stream(Range<K> range) {
         return Stream.empty();
      }
   }
}
