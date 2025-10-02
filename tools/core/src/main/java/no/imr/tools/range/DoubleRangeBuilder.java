package no.imr.tools.range;

public final class DoubleRangeBuilder {
   private double min = Double.POSITIVE_INFINITY;
   private double max = Double.NEGATIVE_INFINITY;

   public DoubleRangeBuilder() {
   }

   @Override
   public String toString() {
      return min + ", " + max;
   }

   public void expand(double value) {
      if (value < min) min = value;
      if (value > max) max = value;
   }

   public void expand(double value1, double value2) {
      if (value1 < value2) {
         if (value1 < min) min = value1;
         if (value2 > max) max = value2;
      } else {
         if (value2 < min) min = value2;
         if (value1 > max) max = value1;
      }
   }

   public void expand(DoubleRangeBuilder builder) {
      if (builder.min < min) min = builder.min;
      if (builder.max > max) max = builder.max;
   }

   public boolean isInitialized() {
      return min <= max;
   }

   public double getMin() {
      return min;
   }

   public double getMax() {
      return max;
   }

   public double getSize() {
      return max - min;
   }

   public DoubleRange toDoubleRange() {
      return DoubleRange.of(min, max);
   }
}
