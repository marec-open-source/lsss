package no.imr.korona.computation.noise;

import no.imr.korona.data.ping.Ping;
import org.jspecify.annotations.Nullable;

/**
 * Averages the data in histogramData (reduces amount of data).
 */
final class Averager extends BaseNoiseMask {
   private final int averageInterval;

   /**
    * Constructs an averager.
    *
    * @param averageInterval the number of samples to average over
    */
   Averager(int averageInterval) {
      this.averageInterval = averageInterval;
   }

   @Override
   @Nullable HistogramData mask(@Nullable HistogramData histogramData, Ping ping, RangeValues rangeValues) {
      if (histogramData == null || histogramData.isEmpty()) {
         return null;
      }
      float[] valueList = histogramData.getData();
      float[] rangeList = histogramData.getRanges();
      float[] tempValueList = new float[valueList.length];
      float[] tempRangeList = new float[valueList.length];
      int count = 0;
      float value = 0;
      float range = 0;
      int tempListIndex = 0;
      for (int i = 0; i < valueList.length; i++) {
         count++;
         //value += PowerConverter.power2Sv(rawODatagramData[i]) / tvg.gByIndex(i);
         value += valueList[i];
         range += rangeList[i];
         if (count == averageInterval) {
            tempValueList[tempListIndex] = value / averageInterval;
            tempRangeList[tempListIndex] = range / averageInterval;
            count = 0;
            value = 0;
            range = 0;
            tempListIndex++;
         }
      }
      if (tempListIndex == 0) {
         return null;
      }
      HistogramData hist = new HistogramData(tempListIndex, histogramData);
      float[] values = hist.getData();
      float[] ranges = hist.getRanges();
      System.arraycopy(tempValueList, 0, values, 0, tempListIndex);
      System.arraycopy(tempRangeList, 0, ranges, 0, tempListIndex);
      return hist;
   }
}
