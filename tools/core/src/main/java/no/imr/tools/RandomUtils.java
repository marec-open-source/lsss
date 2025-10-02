package no.imr.tools;

import no.imr.tools.range.FloatRange;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Utility functions using {@link Random}.
 */
public final class RandomUtils {
   private RandomUtils() {
   }

   public static long newSeed() {
      return ThreadLocalRandom.current().nextLong();
   }

   private static int index(Random random, int size) {
      if (size <= 0) {
         throw new IllegalArgumentException();
      }
      return random.nextInt(size);
   }

   public static <T> T get(Random random, List<T> list) {
      return list.get(index(random, list.size()));
   }

   public static <T> T get(Random random, T[] array) {
      return array[index(random, array.length)];
   }

   public static <T> T get(Random random, Collection<T> collection) {
      return collection.stream()
            .skip(index(random, collection.size()))
            .findFirst()
            .orElseThrow();
   }

   public static <T> List<T> getSubList(Random random, List<T> list) {
      if (list.size() <= 1) {
         return list;
      }
      int a = random.nextInt(list.size() - 1);
      int b = random.nextInt(a + 1, list.size() + 1);
      return list.subList(a, b);
   }

   public static <T> List<T> getSelection(Random random, Collection<T> collection) {
      return stream(random, collection).toList();
   }

   public static FloatRange getSubRange(Random random, FloatRange range) {
      float a = random.nextFloat(range.min(), range.max());
      float b = random.nextFloat(range.min(), range.max());
      return FloatRange.ofUnsorted(a, b);
   }

   public static <T> Stream<T> stream(Random random, Collection<T> collection, int newSize) {
      return collection.stream()
            .filter(new Predicate<>() {
               private int wanted = newSize;
               private int available = collection.size();

               @Override
               public boolean test(T t) {
                  if (random.nextInt(available) < wanted) {
                     available--;
                     wanted--;
                     return true;
                  } else {
                     available--;
                     return false;
                  }
               }
            });
   }

   public static <T> Stream<T> stream(Random random, Collection<T> collection) {
      return stream(random, collection, random.nextInt(collection.size() + 1));
   }
}
