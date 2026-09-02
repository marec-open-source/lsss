package no.imr.tools.range;

import java.util.stream.Stream;

/**
 * Implementation of a RangeSet using a {@link RangeMap}.
 */
class RangeMapBasedRangeSet<K extends Comparable<? super K>> implements RangeSet<K> {
   private final RangeMap<K, Boolean> rangeMap;

   RangeMapBasedRangeSet(RangeMap<K, Boolean> rangeMap) {
      this.rangeMap = rangeMap;
   }

   @Override
   public boolean contains(K key) {
      return rangeMap.containsKey(key);
   }

   @Override
   public boolean containsAny(Range<K> range) {
      return rangeMap.containsAnyKey(range);
   }

   @Override
   public boolean containsAll(Range<K> range) {
      return rangeMap.containsAllKeys(range);
   }

   @Override
   public int size() {
      return rangeMap.size();
   }

   @Override
   public boolean isEmpty() {
      return rangeMap.isEmpty();
   }

   @Override
   public void add(K begin, K end) {
      rangeMap.put(begin, end, true);
   }

   @Override
   public void remove(K begin, K end) {
      rangeMap.remove(begin, end);
   }

   @Override
   public void clear() {
      rangeMap.clear();
   }

   @Override
   public Stream<Range<K>> stream() {
      return rangeMap.stream()
            .map(RangeMap.Entry::range);
   }

   @Override
   public Stream<Range<K>> stream(Range<K> range) {
      return rangeMap.stream(range)
            .map(RangeMap.Entry::range);
   }
}
