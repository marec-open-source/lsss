package no.imr.korona.computation.noise;

import no.imr.korona.data.ping.Ping;
import no.imr.tools.math.ArrayMath;
import org.jspecify.annotations.Nullable;

/**
 * A running averager that averages the data, while preserving the amount of data.
 */
final class RunningAverager extends BaseNoiseMask {
   private final int smoothInterval;

   /**
    * Constructs a running averager.
    *
    * @param smoothInterval the number of samples the average over
    */
   RunningAverager(int smoothInterval) {
      this.smoothInterval = smoothInterval;
   }

   @Override
   @Nullable HistogramData mask(@Nullable HistogramData histogramData, Ping ping, RangeValues rangeValues) {
      if (histogramData == null || histogramData.isEmpty()) {
         return null;
      }

      float[] valueList = histogramData.getData();
      float[] rangeList = histogramData.getRanges();

      float[] tempValueList = new float[valueList.length];
      for (int i = 0, n = valueList.length; i < n; i++) {
         int jBegin = Math.max(0, i - smoothInterval / 2);
         int jEnd = Math.min(n, i + smoothInterval / 2 + 1);
         tempValueList[i] = ArrayMath.mean(valueList, jBegin, jEnd);
      }
      HistogramData hist = new HistogramData(tempValueList.length, histogramData);
      float[] values = hist.getData();
      float[] ranges = hist.getRanges();
      System.arraycopy(tempValueList, 0, values, 0, tempValueList.length);
      System.arraycopy(rangeList, 0, ranges, 0, rangeList.length);
      return hist;
   }
}
