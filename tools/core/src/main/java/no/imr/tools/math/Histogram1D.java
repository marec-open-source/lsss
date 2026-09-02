package no.imr.tools.math;

import no.imr.tools.range.DoubleRange;
import no.imr.tools.range.FloatRange;

public final class Histogram1D {
   private final double min;
   private final double delta;
   private final int[] counts;
   private int belowCount;
   private int aboveCount;

   private Histogram1D(double min, double delta, int binCount) {
      this.min = min;
      this.delta = delta;
      counts = new int[binCount];
   }

   public static Histogram1D fromDelta(FloatRange range, float delta) {
      FloatRange roundedRange = range.expandToMultipleOf(delta);
      int binCount = Math.round(roundedRange.getSize() / delta);
      return new Histogram1D(roundedRange.min(), delta, binCount);
   }

   public static Histogram1D fromDelta(DoubleRange range, double delta) {
      DoubleRange roundedRange = range.expandToMultipleOf(delta);
      int binCount = (int) Math.round(roundedRange.getSize() / delta);
      return new Histogram1D(roundedRange.begin(), delta, binCount);
   }

   public static Histogram1D fromDeltaAndBinCount(double min, double delta, int binCount) {
      return new Histogram1D(min, delta, binCount);
   }

   public static Histogram1D fromBinCount(FloatRange range, int binCount) {
      float delta = range.getSize() / binCount;
      return new Histogram1D(range.min(), delta, binCount);
   }

   public double getMin() {
      return min;
   }

   public double getDelta() {
      return delta;
   }

   public int[] getCounts() {
      return counts;
   }

   public int getBelowCount() {
      return belowCount;
   }

   public int getAboveCount() {
      return aboveCount;
   }

   public int valueToIndex(double value) {
      return (int) Math.floor((value - min) / delta);
   }

   public double indexToValue(int index) {
      return min + index * delta;
   }

   public int valueToCount(double value) {
      int i = valueToIndex(value);
      if (i < 0) {
         return belowCount;
      } else if (i >= counts.length) {
         return aboveCount;
      } else {
         return counts[i];
      }
   }

   public void addValue(double value) {
      int i = valueToIndex(value);
      if (i < 0) {
         belowCount++;
      } else if (i >= counts.length) {
         aboveCount++;
      } else {
         counts[i]++;
      }
   }

   public void addValues(float[] values) {
      for (float value : values) {
         addValue(value);
      }
   }

   public long getTotalCount() {
      long totalCount = (long) belowCount + aboveCount;
      for (int count : counts) {
         totalCount += count;
      }
      return totalCount;
   }

   public int getLowerQuantileIndex(double quantile) {
      if (quantile < 0 || quantile > 0.5) {
         throw new IllegalArgumentException("Invalid quantile: " + quantile);
      }
      double targetCount = getTotalCount() * quantile;
      long cumulativeCount = belowCount;
      for (int i = 0; i < counts.length; i++) {
         cumulativeCount += counts[i];
         if (cumulativeCount >= targetCount) {
            return i;
         }
      }
      return counts.length - 1;
   }

   public int getUpperQuantileIndex(double quantile) {
      if (quantile < 0 || quantile > 0.5) {
         throw new IllegalArgumentException("Invalid quantile: " + quantile);
      }
      double targetCount = getTotalCount() * quantile;
      long cumulativeCount = aboveCount;
      for (int i = counts.length - 1; i >= 0; i--) {
         cumulativeCount += counts[i];
         if (cumulativeCount >= targetCount) {
            return i;
         }
      }
      return 0;
   }
}
