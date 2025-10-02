package no.imr.tools.range;

import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

public final class CopyOnWriteRangeMap<K extends Comparable<? super K>, V> implements RangeMap<K, V> {
   private final Object lock = new Object();
   private volatile @Nullable RangeMap<K, V> readable = new ArrayRangeMap<>();
   private @Nullable RangeMap<K, V> writeable; // Only accessed when locked => no need for volatile

   public CopyOnWriteRangeMap() {
   }

   private RangeMap<K, V> getReadable() {
      // No locking yet => Multiple concurrent callers
      RangeMap<K, V> value = readable;
      if (value == null) {
         synchronized (lock) {
            value = writeable;
            if (value != null) {
               readable = value;
               writeable = null;
            } else {
               value = readable;
               if (value == null) {
                  throw new IllegalStateException();
               }
            }
         }
      }
      return value;
   }

   private RangeMap<K, V> getWriteable() {
      // Lock is obtained => Only one caller
      RangeMap<K, V> value = writeable;
      if (value == null) {
         value = readable;
         if (value == null) {
            throw new IllegalStateException();
         }
         value = value.copy();
         writeable = value;
         readable = null;
      }
      return value;
   }

   // -------- Read methods:

   @Override
   public RangeMap<K, V> copy() {
      return getReadable().copy();
   }

   @Override
   public @Nullable V get(K key) {
      return getReadable().get(key);
   }

   @Override
   public int size() {
      return getReadable().size();
   }

   @Override
   public boolean isEmpty() {
      return getReadable().isEmpty();
   }

   @Override
   public Stream<Entry<K, V>> stream() {
      return getReadable().stream();
   }

   @Override
   public Stream<Entry<K, V>> stream(Range<K> range) {
      return getReadable().stream(range);
   }

   // -------- Write methods:

   @Override
   public void put(K beginKey, K endKey, @Nullable V value) {
      synchronized (lock) {
         getWriteable().put(beginKey, endKey, value);
      }
   }

   @Override
   public void putAll(RangeMap<K, V> rangeMap) {
      synchronized (lock) {
         getWriteable().putAll(rangeMap);
      }
   }

   @Override
   public void clear() {
      synchronized (lock) {
         getWriteable().clear();
      }
   }
}
