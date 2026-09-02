package no.imr.tools.range;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A set consisting of half open intervals.
 */
public interface RangeSet<K extends Comparable<? super K>> extends Iterable<Range<K>> {
   boolean contains(K key);

   boolean containsAny(Range<K> range);

   boolean containsAll(Range<K> range);

   default boolean containsNone(Range<K> range) {
      return !containsAny(range);
   }

   int size();

   boolean isEmpty();

   /**
    * Adds a range to this set.
    *
    * @param range the range to add
    */
   default void add(Range<K> range) {
      add(range.begin(), range.end());
   }

   /**
    * Adds a range to this set.
    *
    * @param begin the lower range boundary, inclusively
    * @param end   the upper range boundary, exclusively
    */
   void add(K begin, K end);

   default void addAll(RangeSet<K> rangeSet) {
      rangeSet.forEach(this::add);
   }

   /**
    * Removes a range from this set.
    *
    * @param range the range to remove
    */
   default void remove(Range<K> range) {
      remove(range.begin(), range.end());
   }

   default void removeAll(RangeSet<K> rangeSet) {
      rangeSet.forEach(this::remove);
   }

   /**
    * Removes a range from this set.
    *
    * @param begin the lower range boundary, inclusively
    * @param end   the upper range boundary, exclusively
    */
   void remove(K begin, K end);

   /**
    * Removes all ranges in this set.
    */
   void clear();

   Stream<Range<K>> stream();

   Stream<Range<K>> stream(Range<K> range);

   /**
    * Returns an iterator for iterating through the ranges in this set.
    *
    * @return an iterator
    */
   @Override
   default Iterator<Range<K>> iterator() {
      return stream().iterator();
   }

   @Override
   default void forEach(Consumer<? super Range<K>> action) {
      stream().forEach(action);
   }

   default void forEachBeginEnd(BiConsumer<K, K> action) {
      forEach(range -> action.accept(range.begin(), range.end()));
   }
}
