package no.imr.lsss.incubator.modules.broadband;

import no.imr.korona.computation.broadband.notchfilter.BroadbandTemporalNotchFilterConfig;
import no.imr.tools.Max;
import no.imr.tools.Min;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.math.OnlineAverageAndVariance;
import no.imr.tools.range.FloatRange;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

final class FrequencyPeakDetector {
   private final List<PeakPlotInfo> peakPlotInfos = new ArrayList<>();
   private final List<BroadbandTemporalNotchFilterConfig> result = new ArrayList<>();

   FrequencyPeakDetector() {
   }

   private static boolean isLocalMaximum(float[] values, int i) {
      float v = values[i];
      float before = values[i - 1];
      float after = values[i + 1];
      return v > before && v > after;
   }

   private static int[] findLocalMaxima(float[] values, int iBegin, int iEnd) {
      return IntStream.range(iBegin + 1, iEnd)
            .filter(i -> isLocalMaximum(values, i))
            .toArray();
   }

   private static List<ProminenceData> peakProminences(float[] values, int[] indices, int wLen) {
      List<ProminenceData> prominences = new ArrayList<>();
      for (int index : indices) {
         prominences.add(peakProminence(values, index, wLen));
      }
      return prominences;
   }

   private static ProminenceData peakProminence(float[] values, int index, int wLen) {
      float peakValue = values[index];
      float minBefore = Float.POSITIVE_INFINITY;
      float minAfter = Float.POSITIVE_INFINITY;
      int iMin = 0;
      int iMax = values.length - 1;
      if (wLen >= 2) {
         iMin = Max.of(index - wLen / 2, iMin);
         iMax = Min.of(index + wLen / 2, iMax);
      }
      int leftBase = index;
      int rightBase = index;
      for (int i = index; i >= iMin; i--) {
         float value = values[i];
         if (value < minBefore) {
            minBefore = value;
            leftBase = i;
         }
         if (value > peakValue) {
            break;
         }
      }
      for (int i = index; i <= iMax; i++) {
         float value = values[i];
         if (value < minAfter) {
            minAfter = value;
            rightBase = i;
         }
         if (value > peakValue) {
            break;
         }
      }
      return new ProminenceData(peakValue - Max.of(minBefore, minAfter), leftBase, rightBase);
   }

   private static float peakWidth(float[] values, int index, ProminenceData prominence, float peakWidthRelativeHeight) {
      float peakHeight = values[index];
      float evaluationHeight = peakHeight - peakWidthRelativeHeight * prominence.prominence;
      int lowIndex = index;
      while (lowIndex > prominence.leftBase && values[lowIndex] > evaluationHeight) {
         lowIndex--;
      }
      float lowIndexInterp = lowIndex + (evaluationHeight - values[lowIndex]) / (values[lowIndex + 1] - values[lowIndex]);
      int highIndex = index;
      while (highIndex < prominence.rightBase && values[highIndex] > evaluationHeight) {
         highIndex++;
      }
      float highIndexInterp = highIndex - (evaluationHeight - values[highIndex]) / (values[highIndex - 1] - values[highIndex]);
      return highIndexInterp - lowIndexInterp;
   }

   private static float peakWidthScalingFactor(float peakWidthRelativeHeight, float peakWidthScalingHeight) {
      return (float) Math.sqrt(Math.log(1 - peakWidthScalingHeight) / Math.log(1 - peakWidthRelativeHeight));
   }

   private static float[] peakWidths(float[] values, int[] indices, List<ProminenceData> prominences, float peakWidthRelativeHeight, float sampleDistance) {
      float[] widths = new float[indices.length];
      for (int i = 0; i < indices.length; i++) {
         widths[i] = peakWidth(values, indices[i], prominences.get(i), peakWidthRelativeHeight) * sampleDistance;
      }
      return widths;
   }

   void detect(float[] means, float[] std, float peakHeightThreshold, FloatRange peakWidthLimit, float peakWidthRelativeHeight,
               boolean useWidthScaling, float firstFrequency, float deltaFrequency, float peakWidthScalingHeight, float prominenceThreshold) {
      int[] peaks = findLocalMaxima(means, 0, means.length - 1);
      float searchWidth = 5 * peakWidthLimit.max();
      List<ProminenceData> prominences = peakProminences(means, peaks, (int) (searchWidth * 1000 / deltaFrequency));
      float[] widths = peakWidths(means, peaks, prominences, peakWidthRelativeHeight, deltaFrequency);
      if (useWidthScaling) {
         float scalingFactor = peakWidthScalingFactor(peakWidthRelativeHeight, peakWidthScalingHeight);
         ArrayMath.multiply(widths, scalingFactor);
      }

      for (int i = 0; i < peaks.length; i++) {
         float prominence = prominences.get(i).prominence;
         float width = widths[i] / 1000;
         if (prominence >= prominenceThreshold && prominence >= peakHeightThreshold * std[peaks[i]] && peakWidthLimit.containsIncludingEnd(width)) {
            BroadbandTemporalNotchFilterConfig filterConfig = new BroadbandTemporalNotchFilterConfig();
            filterConfig.rejectionFrequency.setFloatValue((firstFrequency + peaks[i] * deltaFrequency) / 1000);
            filterConfig.bandwidth.setFloatValue(width);
            result.add(filterConfig);
            peakPlotInfos.add(new PeakPlotInfo(peaks[i], prominences.get(i), std[peaks[i]], width));
         }
      }
   }

   List<PeakPlotInfo> getPeakPlotInfos() {
      return peakPlotInfos;
   }

   List<BroadbandTemporalNotchFilterConfig> getResult() {
      return result;
   }

   record ProminenceData(float prominence, int leftBase, int rightBase) {
   }

   record PeakPlotInfo(int peakIndex, ProminenceData prominenceData, float std, float widthKHz) {
   }

   record DetectionInfo(OnlineAverageAndVariance averageAndVariance, List<PeakPlotInfo> peakPlotInfos, float firstFrequency, float deltaFrequency) {
   }
}
