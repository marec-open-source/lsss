package no.imr.tools;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public final class ImmutableUtils {
   private ImmutableUtils() {
   }

   // List --------------------------

   public static <E> ImmutableList<E> add(List<E> list, E element) {
      return ImmutableList.<E>builderWithExpectedSize(list.size() + 1)
            .addAll(list)
            .add(element)
            .build();
   }

   public static <E> ImmutableList<E> add(List<E> list, int index, E element) {
      if (index < 0 || index > list.size()) {
         throw new IndexOutOfBoundsException(index + ", size = " + list.size());
      }
      ImmutableList.Builder<E> builder = ImmutableList.builderWithExpectedSize(list.size() + 1);
      addAll(builder, list, 0, index);
      builder.add(element);
      addAll(builder, list, index, list.size());
      return builder.build();
   }

   public static <E> ImmutableList<E> remove(List<E> list, int index) {
      Objects.checkIndex(index, list.size());
      ImmutableList.Builder<E> builder = ImmutableList.builderWithExpectedSize(list.size() - 1);
      addAll(builder, list, 0, index);
      addAll(builder, list, index + 1, list.size());
      return builder.build();
   }

   public static <E> ImmutableList<E> removeAll(List<E> list, Set<E> elements) {
      return list.stream()
            .filter(Predicate.not(elements::contains))
            .collect(ImmutableList.toImmutableList());
   }

   public static <E> ImmutableList<E> set(List<E> list, int index, E element) {
      Objects.checkIndex(index, list.size());
      ImmutableList.Builder<E> builder = ImmutableList.builderWithExpectedSize(list.size());
      addAll(builder, list, 0, index);
      builder.add(element);
      addAll(builder, list, index + 1, list.size());
      return builder.build();
   }

   private static <E> void addAll(ImmutableList.Builder<E> builder, List<E> list, int beginIndex, int endIndex) {
      for (int i = beginIndex; i < endIndex; i++) {
         builder.add(list.get(i));
      }
   }

   // Map ---------------------------

   public static <K, V> ImmutableMap<K, V> put(ImmutableMap<K, V> map, K key, V value) {
      V existingValue = map.get(key);
      if (value.equals(existingValue)) {
         return map;
      }
      if (existingValue == null) {
         return ImmutableMap.<K, V>builderWithExpectedSize(map.size() + 1)
               .putAll(map)
               .put(key, value)
               .build();
      }
      ImmutableMap.Builder<K, V> builder = ImmutableMap.builderWithExpectedSize(map.size());
      map.forEach((k, v) -> {
         if (!k.equals(key)) {
            builder.put(k, v);
         }
      });
      return builder
            .put(key, value)
            .build();
   }

   public static <K, V> ImmutableMap<K, V> putAll(ImmutableMap<K, V> map, Map<K, V> entries) {
      ImmutableMap.Builder<K, V> builder = ImmutableMap.builderWithExpectedSize(map.size() + entries.size());
      putAll(builder, map, entries);
      return builder.build();
   }

   private static <K, V> void putAll(ImmutableMap.Builder<K, V> builder, ImmutableMap<K, V> map, Map<K, V> entries) {
      map.forEach((k, v) -> {
         if (!entries.containsKey(k)) {
            builder.put(k, v);
         }
      });
      builder.putAll(entries);
   }

   public static <K, V> ImmutableMap<K, V> remove(ImmutableMap<K, V> map, K key) {
      if (!map.containsKey(key)) {
         return map;
      }
      ImmutableMap.Builder<K, V> builder = ImmutableMap.builderWithExpectedSize(map.size() - 1);
      map.forEach((k, v) -> {
         if (!k.equals(key)) {
            builder.put(k, v);
         }
      });
      return builder.build();
   }

   // Sorted map ---------------------------

   public static <K, V> ImmutableSortedMap<K, V> putAll(ImmutableSortedMap<K, V> map, Map<K, V> entries) {
      ImmutableSortedMap.Builder<K, V> builder = new ImmutableSortedMap.Builder<>(map.comparator());
      putAll(builder, map, entries);
      return builder.build();
   }

   // Set ---------------------------

   public static <E> ImmutableSet<E> add(ImmutableSet<E> set, E element) {
      if (set.contains(element)) {
         return set;
      }
      return ImmutableSet.<E>builderWithExpectedSize(set.size() + 1)
            .addAll(set)
            .add(element)
            .build();
   }

   public static <E> ImmutableSet<E> addAll(ImmutableSet<E> set, Collection<E> elements) {
      if (set.containsAll(elements)) {
         return set;
      }
      return ImmutableSet.<E>builderWithExpectedSize(set.size() + elements.size())
            .addAll(set)
            .addAll(elements)
            .build();
   }

   public static <E> ImmutableSet<E> remove(ImmutableSet<E> set, E element) {
      if (!set.contains(element)) {
         return set;
      }
      ImmutableSet.Builder<E> builder = ImmutableSet.builderWithExpectedSize(set.size() - 1);
      set.forEach(e -> {
         if (!element.equals(e)) {
            builder.add(e);
         }
      });
      return builder.build();
   }
}
