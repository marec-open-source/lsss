package no.imr.tools.range;

import org.jspecify.annotations.Nullable;

public final class IntRange {
   private final int begin;
   private final int end;

   public IntRange(int begin, int end) {
      if (begin > end) {
         throw new IllegalArgumentException(begin + " > " + end);
      }
      this.begin = begin;
      this.end = end;
   }

   // Methods similar for all range classes:

   public int begin() {
      return begin;
   }

   public int end() {
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
      return obj instanceof IntRange that
            && begin == that.begin
            && end == that.end;
   }

   @Override
   public int hashCode() {
      int result = begin;
      result = 31 * result + end;
      return result;
   }

   public boolean isEmpty() {
      return begin == end;
   }

   public boolean containsExcludingBegin(int value) {
      return begin < value && value < end;
   }

   public int clamp(int value) {
      return Math.clamp(value, begin, end);
   }

   // Type specific methods:

   public int getSize() {
      return end - begin;
   }
}
