package no.imr.tools.range;

import no.imr.tools.Utils;
import org.jspecify.annotations.Nullable;

/**
 * A range represented by two floats: {@code [min, max)}.
 * The minimum value is included, but the maximum value is excluded.
 * This class is immutable.
 */
public final class FloatRange implements no.marec.lsss.api.util.FloatRange {
   /**
    * Contains all floats.
    */
   public static final FloatRange ALL = new FloatRange(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

   /**
    * The empty FloatRange.
    */
   public static final FloatRange EMPTY_RANGE = new FloatRange(0, 0);

   private final float min;
   private final float max;

   private FloatRange(float min, float max) {
      this.min = min;
      this.max = max;
   }

   public static FloatRange of(float min, float max) {
      return min <= max ? new FloatRange(min, max) : EMPTY_RANGE;
   }

   public static FloatRange of(double min, double max) {
      return of((float) min, (float) max);
   }

   public static FloatRange of(Range<? extends Number> range) {
      return of(range.begin().floatValue(), range.end().floatValue());
   }

   public static FloatRange ofUnsorted(float value1, float value2) {
      return value1 <= value2 ? of(value1, value2) : of(value2, value1);
   }

   public static FloatRange ofMinAndSize(float min, float size) {
      return of(min, min + size);
   }

   public static FloatRange ofCenterAndSize(float center, float size) {
      return ofCenterAndRadius(center, size / 2);
   }

   public static FloatRange ofCenterAndRadius(float center, float radius) {
      return of(center - radius, center + radius);
   }

   // Methods similar for all range classes:

   public float min() {
      return min;
   }

   public float max() {
      return max;
   }

   @Override
   public float begin() {
      return min;
   }

   @Override
   public float end() {
      return max;
   }

   @Override
   public String toString() {
      return "[" + Utils.toString(min) + ", " + Utils.toString(max) + ")";
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof FloatRange that
            && Float.floatToIntBits(min) == Float.floatToIntBits(that.min)
            && Float.floatToIntBits(max) == Float.floatToIntBits(that.max);
   }

   @Override
   public int hashCode() {
      int result = Float.floatToIntBits(min);
      result = 31 * result + Float.floatToIntBits(max);
      return result;
   }

   public boolean isEmpty() {
      return min == max;
   }

   public boolean contains(float value) {
      return value >= min && value < max;
   }

   public boolean containsExcludingBegin(float value) {
      return value > min && value < max;
   }

   public boolean containsIncludingEnd(float value) {
      return value >= min && value <= max;
   }

   public boolean contains(FloatRange range) {
      return range.isEmpty() ||
            range.min >= min && range.max <= max;
   }

   public boolean intersects(FloatRange range) {
      float a = Math.max(min, range.min);
      float b = Math.min(max, range.max);
      return a < b;
   }

   public FloatRange intersection(FloatRange range) {
      if (range.isEmpty()) {
         return EMPTY_RANGE;
      }
      float a = Math.max(min, range.min);
      float b = Math.min(max, range.max);
      return of(a, b);
   }

   public FloatRange union(FloatRange range) {
      if (isEmpty()) {
         return range;
      }
      if (range.isEmpty()) {
         return this;
      }
      float newMin = Math.min(min, range.min);
      float newMax = Math.max(max, range.max);
      return of(newMin, newMax);
   }

   public boolean touches(FloatRange range) {
      if (range.isEmpty()) {
         return false;
      }
      return max >= range.min && range.max >= min;
   }

   /**
    * Clamps a value to the closure of this range.
    *
    * @param value a value
    * @return the closest value in {@code [min, max]}
    */
   public float clamp(float value) {
      return Math.clamp(value, min, max);
   }

   // Type specific methods:

   public Range<Float> toRange() {
      return new DefaultRange<>(min, max);
   }

   public float getCenter() {
      return (min + max) / 2;
   }

   public float getSize() {
      return max - min;
   }

   public float distanceTo(float value) {
      if (value < min) {
         return min - value;
      }
      if (value > max) {
         return value - max;
      }
      return 0;
   }

   /**
    * Converts a value between min and max to a fraction between 0 and 1.
    *
    * @param value a value
    * @return {@code (value - min) / (max - min)}
    */
   public float valueToFraction(float value) {
      return (value - min) / (max - min);
   }

   /**
    * Converts a fraction between 0 and 1 to a value between min and max.
    *
    * @param fraction a fraction
    * @return {@code min + fraction * (max - min)}
    */
   public float fractionToValue(float fraction) {
      return min + fraction * (max - min);
   }

   /**
    * Expands this range so that min and max are not equal.
    *
    * @return the expanded range
    */
   public FloatRange expandToNonDegenerated() {
      if (min < max) {
         return this;
      }
      return of(Math.nextDown(min), Math.nextUp(max));
   }

   public FloatRange expandToIncludeMax() {
      return of(min, Math.nextUp(max));
   }

   /**
    * Expands this range so that min and max are integer multiples of {@code delta}.
    *
    * @param delta a value {@code > 0}
    * @return the expanded range
    */
   public FloatRange expandToMultipleOf(double delta) {
      float newMin = (float) (Math.floor(min / delta) * delta);
      float newMax = (float) (Math.ceil(max / delta) * delta);
      return of(newMin, newMax);
   }

   public FloatRange expand(float delta) {
      return of(min - delta, max + delta);
   }

   /**
    * Shrinks this range so that min and max are integer multiples of {@code delta}.
    *
    * @param delta a value {@code > 0}
    * @return the shrunk range, or {@link #EMPTY_RANGE} if this range cannot be shrunken
    */
   public FloatRange shrinkToMultipleOf(double delta) {
      float newMin = (float) (Math.ceil(min / delta) * delta);
      float newMax = (float) (Math.floor(max / delta) * delta);
      return of(newMin, newMax);
   }

   public FloatRange roundToMultipleOf(double delta) {
      float newMin = (float) (Math.round(min / delta) * delta);
      float newMax = (float) (Math.round(max / delta) * delta);
      return of(newMin, newMax);
   }

   public FloatRange shift(float shift) {
      float size = getSize();
      float newMin = min + shift * size;
      return of(newMin, newMin + size);
   }

   public FloatRange shiftToBeContainedIn(FloatRange range) {
      if (min < range.min) {
         return of(range.min, Math.min(range.max, range.min + getSize()));
      }
      if (max > range.max) {
         return of(Math.max(range.min, range.max - getSize()), range.max);
      }
      return this;
   }

   public FloatRange add(float amount) {
      return of(min + amount, max + amount);
   }

   public FloatRange multiply(float factor) {
      return of(min * factor, max * factor);
   }

   public FloatRange shrink(float amount) {
      return of(min + amount, max - amount);
   }

   public FloatRange shrinkByFraction(float fraction) {
      return shrink(getSize() * fraction);
   }

   public FloatRange zoom(float zoomFactor) {
      return zoom(zoomFactor, getCenter());
   }

   public FloatRange zoom(float zoomFactor, float reference) {
      return of(reference - zoomFactor * (reference - min), reference + zoomFactor * (max - reference));
   }
}
