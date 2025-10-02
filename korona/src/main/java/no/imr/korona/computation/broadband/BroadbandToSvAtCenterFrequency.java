package no.imr.korona.computation.broadband;

import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.TvgArray;
import no.imr.tools.Utils;
import no.imr.tools.math.ComplexArray;

public final class BroadbandToSvAtCenterFrequency {
   private BroadbandToSvAtCenterFrequency() {
   }

   public static void computeSvAtCenterFrequency(BroadbandData broadbandData, float[] sv) {
      double constantFactor = getConstantFactor(broadbandData);
      ComplexArray avgPc = broadbandData.getAveragePulseCompressedSignal();
      TvgArray tvg = broadbandData.getTVGArray();

      for (int i = 0; i < sv.length; i++) {
         sv[i] = Utils.avoidInfinity((float) (constantFactor * avgPc.abs2(i) * tvg.get(i)));
      }
   }

   public static float computeSvAtCenterFrequency(BroadbandData broadbandData, int sampleIndex) {
      double constantFactor = getConstantFactor(broadbandData);
      ComplexArray avgPc = broadbandData.getAveragePulseCompressedSignal();
      TvgArray tvg = broadbandData.getTVGArray();

      return Utils.avoidInfinity((float) (constantFactor * avgPc.abs2(sampleIndex) * tvg.get(sampleIndex)));
   }

   private static double getConstantFactor(BroadbandData broadbandData) {
      PulseCompression pulseCompression = broadbandData.getPulseCompression();
      double tauEff = pulseCompression.getTauEff();
      double f = broadbandData.getCenterFrequency();
      double c = broadbandData.getSoundVelocity();
      double lambda = c / f;
      double psi = broadbandData.getPsi(f);
      double G = broadbandData.getGain(f);
      double pt = broadbandData.getTransmitPower();
      double cSv = Math.pow(10, -(2 * G + psi) / 10)
            * (32 * Math.PI * Math.PI)
            / (pt * lambda * lambda * c * tauEff);
      return PowerData.IMR_CONSTANT * broadbandData.getPrxFactor(f) * cSv;
   }
}
