package no.imr.korona.computation.noise;

import no.imr.korona.data.ping.Ping;
import org.jspecify.annotations.Nullable;

/**
 * IntervalNoiseMask.
 * Picks noise values in a specified interval.
 */
final class IntervalNoiseMask extends BaseNoiseMask {
   private final float minRange;
   private final float maxRange;

   IntervalNoiseMask(float minRange, float maxRange) {
      this.minRange = minRange;
      this.maxRange = maxRange;
   }

   @Override
   @Nullable HistogramData mask(@Nullable HistogramData histogramData, Ping ping, RangeValues rangeValues) {
      if (histogramData == null || histogramData.isEmpty()) {
         return null;
      }

      if (maxRange < minRange) {
         return null;
      }

      float[] valueData = histogramData.getData();
      float[] rangeData = histogramData.getRanges();
      float[] tempValueData = new float[valueData.length];
      float[] tempRangeData = new float[rangeData.length];
      int tempListIndex = 0;

      for (int i = 0; i < valueData.length; i++) {
         float range = rangeData[i];
         if (range > maxRange) {
            break;
         }
         if (range >= minRange) {
            tempRangeData[tempListIndex] = range;
            tempValueData[tempListIndex] = valueData[i];
            tempListIndex++;
         }
      }

      if (tempListIndex == 0) {
         return null;
      }

      HistogramData hist = new HistogramData(tempListIndex, histogramData);
      float[] values = hist.getData();
      float[] ranges = hist.getRanges();
      System.arraycopy(tempValueData, 0, values, 0, tempListIndex);
      System.arraycopy(tempRangeData, 0, ranges, 0, tempListIndex);
      return hist;
   }
}
