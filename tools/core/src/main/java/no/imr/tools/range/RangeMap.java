package no.imr.tools.range;

import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A map from half open intervals to values.
 * No {@code null} keys are allowed.
 */
public interface RangeMap<K extends Comparable<? super K>, V> extends Iterable<RangeMap.Entry<K, V>> {
   RangeMap<K, V> copy();

   /**
    * Associates a range with a value.
    *
    * @param range the range
    * @param value the value associated with the range [beginKey, endKey)
    */
   default void put(Range<K> range, @Nullable V value) {
      put(range.begin(), range.end(), value);
   }

   /**
    * Associates a range with a value.
    *
    * @param beginKey the lower range boundary, inclusively
    * @param endKey   the upper range boundary, exclusively
    * @param value    the value associated with the range [beginKey, endKey)
    */
   void put(K beginKey, K endKey, @Nullable V value);

   default void put(Entry<K, V> entry) {
      put(entry.range, entry.value);
   }

   default void putAll(RangeMap<K, V> rangeMap) {
      rangeMap.forEach(this::put);
   }

   /**
    * Removes the mapped values in a range.
    *
    * @param range the range
    */
   default void remove(Range<K> range) {
      put(range, null);
   }

   /**
    * Removes the mapped values in a range.
    *
    * @param beginKey the lower range boundary, inclusively
    * @param endKey   the upper range boundary, exclusively
    */
   default void remove(K beginKey, K endKey) {
      put(beginKey, endKey, null);
   }

   /**
    * Returns the value associated with the range containing a specified key.
    *
    * @param key a key
    * @return the corresponding value
    */
   @Nullable V get(K key);

   default V getOrDefault(K key, V defaultValue) {
      V value = get(key);
      return value != null ? value : defaultValue;
   }

   default boolean containsKey(K key) {
      return get(key) != null;
   }

   default boolean containsAnyKey(Range<K> range) {
      return stream(range).anyMatch(entry -> true);
   }

   default boolean containsAllKeys(Range<K> range) {
      AtomicReference<K> key = new AtomicReference<>(range.begin());
      stream(range).forEach(entry -> {
         if (key.get().equals(entry.range.begin())) {
            key.set(entry.range.end());
         }
      });
      return key.get().equals(range.end());
   }

   default boolean containsNoKeys(Range<K> range) {
      return stream(range).noneMatch(entry -> true);
   }

   /**
    * Removes all entries from this RangeMap.
    */
   void clear();

   /**
    * Return the number of entries currently in this map.
    *
    * @return number of entries
    */
   int size();

   /**
    * Test if map is empty.
    *
    * @return {@code true} if empty
    */
   boolean isEmpty();

   Stream<Entry<K, V>> stream();

   Stream<Entry<K, V>> stream(Range<K> range);

   /**
    * Returns an iterator for iterating through the entries in this map.
    *
    * @return an iterator
    */
   @Override
   default Iterator<Entry<K, V>> iterator() {
      return stream().iterator();
   }

   @Override
   default void forEach(Consumer<? super Entry<K, V>> action) {
      stream().forEach(action);
   }

   /**
    * An entry in a RangeMap.
    */
   record Entry<K extends Comparable<? super K>, V>(Range<K> range, V value) {
      @Override
      public String toString() {
         return range + " = " + value;
      }
   }
}
