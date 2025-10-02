package no.imr.korona.computation.noise;

import no.imr.korona.data.ping.Ping;
import org.jspecify.annotations.Nullable;

/**
 * Removes values with too high level.
 */
final class HighLevelNoiseMask extends BaseNoiseMask {
   private final float threshold;

   HighLevelNoiseMask(float threshold) {
      this.threshold = threshold;
   }

   @Override
   @Nullable HistogramData mask(@Nullable HistogramData histogramData, Ping ping, RangeValues rangeValues) {
      if (histogramData == null || histogramData.isEmpty()) {
         return null;
      }

      float[] data = histogramData.getData();
      float[] ranges = histogramData.getRanges();

      int removeCount = 0;
      for (float value : data) {
         if (value > threshold) {
            removeCount++;
         }
      }

      if (removeCount == 0) {
         return histogramData;
      }

      int n = data.length - removeCount;
      HistogramData newHistogramData = new HistogramData(n, histogramData);
      float[] newData = newHistogramData.getData();
      float[] newRanges = newHistogramData.getRanges();

      int iNew = 0;
      for (int i = 0; i < data.length; i++) {
         float value = data[i];
         if (value <= threshold) {
            newData[iNew] = data[i];
            newRanges[iNew] = ranges[i];
            iNew++;
         }
      }
      assert iNew == n;

      return newHistogramData;
   }
}
