package no.imr.tools.math;

import no.imr.tools.Min;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Extracts peaks from a non-negative value array using a multiscale method.
 * <p>
 * Inspired by
 * Scholkmann, F., Boss, J., &amp; Wolf, M. (2012). "An efficient algorithm for automatic
 * peak detection in noisy periodic and quasi-periodic signals". Algorithms, 5(4), 588-603.
 */
public final class PeakFinding {
   private PeakFinding() {
   }

   public static List<Peak> getPeaks(float[] values) {
      return getPeaks(values, values.length / 2, 0.01f);
   }

   public static List<Peak> getPeaks(float[] values, int maxScale, float thresholdFactor) {
      int[] maxPeakScales = calculateMaxPeakScales(values, maxScale);
      int[] nonZeroMaxPeakScales = Arrays.stream(maxPeakScales)
            .filter(value -> value != 0)
            .toArray();
      float scaleThreshold = calculateRobustAutoThreshold(nonZeroMaxPeakScales, 0.1f);
      if (scaleThreshold == 0) {
         return List.of();
      }

      List<Peak> peaks = new ArrayList<>();

      for (int i = 2; i < values.length - 2; i++) {
         if (maxPeakScales[i] >= scaleThreshold) {
            float peakValue = values[i];
            float lowerPeakThreshold = thresholdFactor * peakValue;

            int start = i - 1;
            int stop = i + 1;

            while (start > 0) {
               float value = values[start];
               if (value < lowerPeakThreshold || value >= peakValue) {
                  break;
               }
               start--;
            }

            while (stop < values.length - 1) {
               float value = values[stop];
               if (value < lowerPeakThreshold || value >= peakValue) {
                  break;
               }
               stop++;
            }

            peaks.add(new Peak(i, start, stop + 1)); // +1 to convert stop index to end index
         }
      }

      return peaks;
   }

   private static float calculateRobustNewThreshold(int[] values, float splitValue) {
      int i = ArrayMath.partition(values, (int) Math.floor(splitValue));
      if (i == 0 || i == values.length) {
         return splitValue;
      }
      int medianLow = Median.quickSelect(values, 0, i);
      int medianHigh = Median.quickSelect(values, i, values.length);
      return 0.5f * (medianLow + medianHigh);
   }

   private static float calculateRobustAutoThreshold(int[] values, float tolerance) {
      if (values.length == 0) {
         return 0;
      }
      float threshold = Median.quickSelect(values);
      while (true) {
         float newThreshold = calculateRobustNewThreshold(values, threshold);
         if (Math.abs(newThreshold - threshold) <= tolerance) {
            return threshold;
         }
         threshold = newThreshold;
      }
   }

   private static int[] calculateMaxPeakScales(float[] values, int maxScale) {
      int[] maxPeakScales = new int[values.length];

      for (int i = 0; i < maxPeakScales.length; i++) {
         int maxNeighbourShift = Min.of(maxScale, i, maxPeakScales.length - i - 1);
         float centerValue = values[i];
         for (int neighbourShift = 1; neighbourShift <= maxNeighbourShift; neighbourShift++) {
            if (centerValue > values[i - neighbourShift] && centerValue > values[i + neighbourShift]) {
               maxPeakScales[i] = neighbourShift;
            } else {
               break;
            }
         }
      }

      return maxPeakScales;
   }

   public record Peak(int peakIndex, int beginIndex, int endIndex) {
   }
}
