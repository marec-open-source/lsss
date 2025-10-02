package no.imr.korona.computation.noise;

import no.imr.korona.data.ping.Ping;
import org.jspecify.annotations.Nullable;

/**
 * Passes on only histogram data with a minimum quality.
 */
final class QualityMask extends BaseNoiseMask {
   private final float minimumQuality;

   /**
    * Construction of a QualityMask.
    *
    * @param minimumQuality the minimum allowed quality of noise data
    */
   QualityMask(float minimumQuality) {
      this.minimumQuality = minimumQuality;
   }

   @Override
   @Nullable HistogramData mask(@Nullable HistogramData histogramData, Ping ping, RangeValues rangeValues) {
      if (histogramData == null || histogramData.isEmpty()) {
         return null;
      }
      if (histogramData.getQuality().value < minimumQuality) {
         return null;
      }
      return histogramData;
   }
}
