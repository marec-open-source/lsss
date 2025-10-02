package no.imr.tools.range;

import no.imr.tools.Max;
import no.imr.tools.Min;
import org.jspecify.annotations.Nullable;

/**
 * A half open interval [begin, end).
 */
public abstract class Range<E extends Comparable<? super E>> {
   private final E begin;
   private final E end;

   /**
    * Creates a new range.
    *
    * @param begin the beginning
    * @param end   the end
    * @throws IllegalArgumentException if {@code begin > end}
    */
   protected Range(E begin, E end) {
      int comparison = begin.compareTo(end);
      if (comparison > 0) {
         throw new IllegalArgumentException(begin + " > " + end);
      }
      this.begin = begin;
      this.end = comparison == 0 ? begin : end; // Enables use of == in isEmpty.
   }

   // Methods similar for all range classes:

   public E begin() {
      return begin;
   }

   public E end() {
      return end;
   }

   @Override
   public String toString() {
      return "[" + begin + ", " + end + ")";
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof Range<?> that
            && begin.equals(that.begin)
            && end.equals(that.end);
   }

   @Override
   public int hashCode() {
      int result = begin.hashCode();
      result = 31 * result + end.hashCode();
      return result;
   }

   public boolean isEmpty() {
      return begin == end; // Can do this because of test in constructor.
   }

   public boolean isBeginOrEnd(E value) {
      return begin.equals(value)
            || end.equals(value);
   }

   public boolean contains(E value) {
      return begin.compareTo(value) <= 0
            && end.compareTo(value) > 0;
   }

   public boolean containsExcludingBegin(E value) {
      return begin.compareTo(value) < 0
            && end.compareTo(value) > 0;
   }

   public boolean containsIncludingEnd(E value) {
      return begin.compareTo(value) <= 0
            && end.compareTo(value) >= 0;
   }

   public boolean contains(Range<E> range) {
      if (range.isEmpty()) {
         return true;
      }
      return begin.compareTo(range.begin) <= 0
            && end.compareTo(range.end) >= 0;
   }

   public boolean intersects(Range<E> range) {
      E a = Max.of(begin, range.begin);
      E b = Min.of(end, range.end);
      return a.compareTo(b) < 0;
   }

   public Range<E> union(Range<E> range) {
      if (range.isEmpty()) {
         return this;
      }
      if (isEmpty()) {
         return range;
      }
      E min = Min.of(begin, range.begin);
      E max = Max.of(end, range.end);
      return new DefaultRange<>(min, max);
   }

   public boolean touches(Range<E> range) {
      if (isEmpty() || range.isEmpty()) {
         return false;
      }
      return end.compareTo(range.begin) >= 0 && range.end.compareTo(begin) >= 0;
   }

   public E clamp(E value) {
      if (begin.compareTo(value) >= 0) {
         return begin;
      }
      if (end.compareTo(value) <= 0) {
         return end;
      }
      return value;
   }

   // Type specific methods:

   // Here goes type specific methods of specialized range classes.
}
