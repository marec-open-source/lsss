package no.imr.tools.range;

import no.imr.tools.Utils;
import org.jspecify.annotations.Nullable;

public final class DoubleRange {
   public static final DoubleRange EMPTY_RANGE = new DoubleRange(0, 0);

   private final double begin;
   private final double end;

   private DoubleRange(double begin, double end) {
      this.begin = begin;
      this.end = end;
   }

   public static DoubleRange of(double begin, double end) {
      return begin <= end ? new DoubleRange(begin, end) : EMPTY_RANGE;
   }

   // Methods similar for all range classes:

   public double begin() {
      return begin;
   }

   public double end() {
      return end;
   }

   @Override
   public String toString() {
      return "[" + Utils.toString(begin) + ", " + Utils.toString(end) + ")";
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof DoubleRange that
            && Double.doubleToLongBits(begin) == Double.doubleToLongBits(that.begin)
            && Double.doubleToLongBits(end) == Double.doubleToLongBits(that.end);
   }

   @Override
   public int hashCode() {
      int result = Double.hashCode(begin);
      result = 31 * result + Double.hashCode(end);
      return result;
   }

   public boolean isEmpty() {
      return begin == end;
   }

   public boolean contains(double value) {
      return value >= begin && value < end;
   }

   public boolean contains(DoubleRange range) {
      return range.isEmpty() ||
            range.begin >= begin && range.end <= end;
   }

   public boolean intersects(DoubleRange range) {
      double a = Math.max(begin, range.begin);
      double b = Math.min(end, range.end);
      return a < b;
   }

   public double clamp(double value) {
      return Math.clamp(value, begin, end);
   }

   public DoubleRange clamp(DoubleRange range) {
      return of(clamp(range.begin), clamp(range.end));
   }

   // Type specific methods:

   public FloatRange toFloatRange() {
      return FloatRange.of(begin, end);
   }

   public double getCenter() {
      return (end + begin) / 2;
   }

   public double getSize() {
      return end - begin;
   }

   public double valueToFraction(double value) {
      double size = getSize();
      return size == 0 ? 0 : (value - begin) / size;
   }

   public double fractionToValue(double fraction) {
      return begin + fraction * getSize();
   }

   public DoubleRange expandToIncludeMax() {
      return of(begin, Math.nextUp(end));
   }

   public DoubleRange expandToMultipleOf(double delta) {
      double newMin = Math.floor(begin / delta) * delta;
      double newMax = Math.ceil(end / delta) * delta;
      return of(newMin, newMax);
   }

   public DoubleRange shift(double sizeFactor) {
      double size = getSize();
      double newBegin = begin + sizeFactor * size;
      return of(newBegin, newBegin + size);
   }

   public DoubleRange shiftToBeContainedIn(DoubleRange range) {
      if (begin < range.begin) {
         return of(range.begin, Math.min(range.end, range.begin + getSize()));
      }
      if (end > range.end) {
         return of(Math.max(range.begin, range.end - getSize()), range.end);
      }
      return this;
   }
}
