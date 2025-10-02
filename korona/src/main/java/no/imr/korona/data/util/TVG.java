package no.imr.korona.data.util;

import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.util.KoronaUtils;

import java.util.Arrays;

/**
 * TVG-function 20log(r).
 */
final class TVG {
   private final Parameters parameters;
   private volatile float[] tvg = new float[0];

   TVG(Parameters parameters) {
      this.parameters = parameters;
   }

   /**
    * Returns an array containing the linear TVG.
    *
    * @param offset the index of the first sample
    * @param count  the number of samples
    * @return the linear TVG
    */
   TvgArray getTvgArray(int offset, int count) {
      int length = Math.max(1, offset + count);
      float[] tvg = this.tvg;
      if (tvg.length < length) {
         tvg = computeTvg(parameters, tvg, length);
         this.tvg = tvg;
      }
      return new TvgArray(tvg, offset);
   }

   private static float[] computeTvg(Parameters parameters, float[] previousTvg, int newLength) {
      float[] tvg = Arrays.copyOf(previousTvg, newLength);
      for (int i = previousTvg.length; i < newLength; i++) {
         float r = Math.max(i * parameters.sampleDistance - parameters.rangeCorrection, parameters.sampleDistance);
         double logTVG = 20 * Math.log10(r) + 2 * parameters.absorptionCoefficient * r;
         tvg[i] = (float) KoronaUtils.fromDB(logTVG);
      }
      return tvg;
   }

   record Parameters(float absorptionCoefficient, float sampleDistance, float rangeCorrection) {
      Parameters(ChannelData channelData) {
         this(channelData.getAbsorptionCoefficient(), channelData.getSampleDistance(), channelData.getTvgRangeCorrection());
      }
   }
}
