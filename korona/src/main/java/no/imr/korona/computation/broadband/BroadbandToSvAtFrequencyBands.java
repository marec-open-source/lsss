package no.imr.korona.computation.broadband;

import no.imr.korona.computation.broadband.transferfunction.IdealBandPassTransferFunction;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.TvgArray;
import no.imr.korona.util.KoronaUtils;
import no.imr.korona.util.absorption.Absorption;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.math.MathUtils;
import no.imr.tools.range.FloatRange;

import java.util.List;

public final class BroadbandToSvAtFrequencyBands {
   private final BroadbandData broadbandData;
   private final List<FloatRange> frequencyRanges;
   private final ComplexArray averageValues;
   private final Absorption absorption;
   private final List<PulseCompression> pulseCompressions;

   public BroadbandToSvAtFrequencyBands(BroadbandData broadbandData, List<FloatRange> frequencyRanges, float stopBandDistance) {
      this.broadbandData = broadbandData;
      this.frequencyRanges = frequencyRanges;
      absorption = broadbandData.getAbsorption();
      averageValues = broadbandData.computeAverageComplexValues();
      pulseCompressions = frequencyRanges.stream()
            .map(frequencyRange -> PulseCompressionCache.getPulseCompression(new PulseCompressionConfig(
                  broadbandData,
                  broadbandData.getPulseCompression().getConfig().filterChain(),
                  new IdealBandPassTransferFunction(frequencyRange, stopBandDistance),
                  broadbandData.getPulseCompression().getConfig().responseTransferFunction())))
            .toList();
   }

   public List<PulseCompression> getPulseCompressions() {
      return pulseCompressions;
   }

   public void calculateSv(int frequencyIndex, float[] sv) {
      /* calculates logSv from a pulse compressed signal in the same fashion as in PowerData.
       * Differences: the signal is pulse compressed with a part of the sent signal only
       * this affects tauEff (through PiecewisePulseCompression)
       * other adjustments: psi and gf set to be at the frequency at the centre of the selected band used for pulse compression
       */
      PulseCompression pulseCompression = pulseCompressions.get(frequencyIndex);
      double centerFrequencySplitBand = frequencyRanges.get(frequencyIndex).getCenter();
      double centerFrequencyWholeBand = broadbandData.getCenterFrequency();

      double constantFactor = getConstantFactor(pulseCompression, centerFrequencySplitBand);
      ComplexArray avgPc = pulseCompression.computePulseCompressedSignal(averageValues);
      TvgArray tvg = broadbandData.getTVGArray();

      int downSamplingFactor = avgPc.length() / sv.length;
      double twoTimesAbsorptionCorrection = 2 * (absorption.getAbsorption(centerFrequencySplitBand) - absorption.getAbsorption(centerFrequencyWholeBand));
      for (int i = 0; i < sv.length; i++) {
         double svSum = 0;
         for (int j = 0; j < downSamplingFactor; j++) {
            int originalIndex = i * downSamplingFactor + j;
            float sampleRange = broadbandData.getTvgRange(originalIndex);
            svSum += constantFactor * avgPc.abs2(originalIndex) * tvg.get(originalIndex) * KoronaUtils.fromDB(twoTimesAbsorptionCorrection * sampleRange);
         }
         sv[i] = MathUtils.avoidInfinity((float) (svSum / downSamplingFactor));
      }
   }

   private double getConstantFactor(PulseCompression pulseCompression, double f) {
      double tauEff = pulseCompression.getTauEff();
      double c = broadbandData.getSoundVelocity();
      double lambda = c / f;
      double psi = broadbandData.getPsi(f);
      double g = broadbandData.getGain(f);
      double pt = broadbandData.getTransmitPower();
      double cSv = Math.pow(10, -(2 * g + psi) / 10)
            * (32 * Math.PI * Math.PI)
            / (pt * lambda * lambda * c * tauEff);
      return PowerData.IMR_CONSTANT * broadbandData.getPrxFactor(f) * cSv;
   }
}
