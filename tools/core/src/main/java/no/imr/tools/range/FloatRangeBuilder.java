package no.imr.tools.range;

/**
 * For building a {@link FloatRange} containing a set of values.
 */
public final class FloatRangeBuilder {
   private float min = Float.POSITIVE_INFINITY;
   private float max = Float.NEGATIVE_INFINITY;

   public FloatRangeBuilder() {
   }

   @Override
   public String toString() {
      return min + ", " + max;
   }

   public void expand(double value) {
      expand((float) value);
   }

   public void expand(float value) {
      if (value < min) min = value;
      if (value > max) max = value;
   }

   public void expand(float[] values) {
      for (float value : values) {
         expand(value);
      }
   }

   public void expand(FloatRange range) {
      if (!range.isEmpty()) {
         if (range.min() < min) min = range.min();
         if (range.max() > max) max = range.max();
      }
   }

   public void expand(FloatRangeBuilder builder) {
      if (builder.min < min) min = builder.min;
      if (builder.max > max) max = builder.max;
   }

   public boolean isInitialized() {
      return min <= max;
   }

   public FloatRange toFloatRange() {
      return FloatRange.of(min, max);
   }
}
