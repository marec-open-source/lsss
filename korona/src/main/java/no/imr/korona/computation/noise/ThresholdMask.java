package no.imr.korona.computation.noise;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import org.jspecify.annotations.Nullable;

/**
 * Masks values above a threshold and reflections below bottom.
 */
final class ThresholdMask extends BaseNoiseMask {
   private final float threshold;

   /**
    * Creates a ThresholdMask with a specified threshold.
    *
    * @param threshold the threshold
    */
   ThresholdMask(float threshold) {
      this.threshold = threshold;
   }

   @Override
   @Nullable HistogramData mask(@Nullable HistogramData histogramData, Ping ping, RangeValues rangeValues) {
      if (histogramData == null || histogramData.isEmpty()) {
         return null;
      }
      PowerData powerData = histogramData.getPowerData();
      float bottom = rangeValues.getChannelBottomRange();
      boolean[] invalid = new boolean[powerData.getCount()];
      float[] logSv = powerData.getLogSv();
      int iEnd = Math.min(powerData.getCount(), powerData.rangeToSampleIndex(bottom));
      for (int i = 0; i < iEnd; i++) {
         if (logSv[i] > threshold) {
            invalid[i] = true;
            float range = powerData.getSampleRange(i);

            {
               // invalidate reflection with surface
               int index = powerData.rangeToSampleIndex(bottom + range);
               if (index >= 0 && index < powerData.getCount()) {
                  invalid[index] = true;
               }
            }

            {
               // invalidate reflection with bottom
               int index = powerData.rangeToSampleIndex(2 * bottom - range);
               if (index >= 0 && index < powerData.getCount()) {
                  invalid[index] = true;
               }
            }
         }
      }
      //Log.global.fine("invalidCount = " + GaussUtils.count(invalid));

      float[] valueData = histogramData.getData();
      float[] rangeData = histogramData.getRanges();
      float[] tempValueData = new float[valueData.length];
      float[] tempRangeData = new float[rangeData.length];
      int tempListIndex = 0;

      for (int i = 0; i < valueData.length; i++) {
         float range = rangeData[i];
         int index = powerData.rangeToSampleIndex(range);
         if (!invalid[index]) {
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
