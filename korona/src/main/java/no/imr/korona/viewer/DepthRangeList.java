package no.imr.korona.viewer;

import no.imr.tools.range.FloatRange;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Class for handling a list of depth ranges (from a number of pings).
 */
final class DepthRangeList {
   private static final int MAX_COUNT = 100;
   private static final float STD_DEV_FACTOR = 3;

   private final Deque<FloatRange> depthRanges = new ArrayDeque<>(MAX_COUNT);

   // These sums are updated continually and are kept as longs to avoid accumulation errors.
   private long minSum;
   private long minSqSum;
   private long maxSum;
   private long maxSqSum;

   DepthRangeList() {
   }

   synchronized void addDepthRange(FloatRange depthRange) {
      if (depthRanges.size() >= MAX_COUNT) {
         subtract(depthRanges.removeLast());
      }
      add(depthRange);
      depthRanges.addFirst(depthRange);
   }

   private void subtract(FloatRange depthRange) {
      long min = (long) Math.floor(depthRange.min());
      minSum -= min;
      minSqSum -= min * min;

      long max = (long) Math.ceil(depthRange.max());
      maxSum -= max;
      maxSqSum -= max * max;
   }

   private void add(FloatRange depthRange) {
      long min = (long) Math.floor(depthRange.min());
      minSum += min;
      minSqSum += min * min;

      long max = (long) Math.ceil(depthRange.max());
      maxSum += max;
      maxSqSum += max * max;
   }

   synchronized void getAllDepthRanges(List<FloatRange> result, int maxCount) {
      result.clear();
      for (FloatRange depthRange : depthRanges) {
         if (result.size() >= maxCount) {
            break;
         }
         result.add(depthRange);
      }
   }

   synchronized FloatRange getMaxRange() {
      FloatRange maxRange = FloatRange.EMPTY_RANGE;
      for (FloatRange depthRange : depthRanges) {
         maxRange = maxRange.union(depthRange);
      }
      return maxRange;
   }

   synchronized FloatRange getAutoRange() {
      int n = depthRanges.size();
      if (n == 0) {
         return FloatRange.EMPTY_RANGE;
      }

      float minMean = minSum / (float) n;
      float maxMean = maxSum / (float) n;

      //NB: Argument to sqrt calculated as long to avoid rounding errors which could result in something negative
      float minStdDev = (float) (Math.sqrt(minSqSum * n - minSum * minSum) / n);
      float maxStdDev = (float) (Math.sqrt(maxSqSum * n - maxSum * maxSum) / n);

      float minRadius = STD_DEV_FACTOR * minStdDev + 1;
      float minAcceptableMin = minMean - minRadius;
      float maxAcceptableMin = minMean + minRadius;

      float maxRadius = STD_DEV_FACTOR * maxStdDev + 1;
      float minAcceptableMax = maxMean - maxRadius;
      float maxAcceptableMax = maxMean + maxRadius;

      float selectedMin = Float.POSITIVE_INFINITY;
      float selectedMax = Float.NEGATIVE_INFINITY;
      for (FloatRange depthRange : depthRanges) {
         float min = depthRange.min();
         if (min < selectedMin && min >= minAcceptableMin && min <= maxAcceptableMin) {
            selectedMin = min;
         }
         float max = depthRange.max();
         if (max > selectedMax && max >= minAcceptableMax && max <= maxAcceptableMax) {
            selectedMax = max;
         }
      }

      if (selectedMin > selectedMax) {
         selectedMin = minAcceptableMin;
         selectedMax = maxAcceptableMax;
      }

      return FloatRange.of(selectedMin, selectedMax);
   }
}
