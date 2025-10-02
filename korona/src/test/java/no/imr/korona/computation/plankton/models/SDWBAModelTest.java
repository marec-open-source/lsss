package no.imr.korona.computation.plankton.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SDWBAModelTest {
   @Test
   void testModel() {
      SDWBAModel sdwbaModel = new SDWBAModel();
      sdwbaModel.setParameterSetName(SDWBAModel.SDWBAParameterSetName.N114);

      double f = 120000;
      double L = 30E-3;

      double backscatter = sdwbaModel.getBackscatter(L, f);
      double TS1 = 10 * Math.log10(backscatter);
      assertEquals(-76.77871207070308, TS1, 0.0001);

/*
double TS1 = getTSForFixedL(5, 38.35E-3, sdwbaModel);
double TS2 = getTSForFixedL(20, 38.35E-3, sdwbaModel);
double TS3 = getTSForFixedL(40, 38.35E-3, sdwbaModel);
double TS4 = getTSForFixedL(120, 38.35E-3, sdwbaModel);
*/

      double volume = sdwbaModel.getBioVolume(38.35E-3);
      assertEquals(4.2074434732687794E-7, volume, 0.0001E-7);
   }

   private static double getTSForFixedL(double kL, double L, SDWBAModel model) {
      double k = kL / L;
      double freq = (k * 1456) / (2 * Math.PI);

      double backscatter = model.getBackscatter(L, freq);

      return 10 * Math.log10(backscatter);// - 20*Math.log10(L/38.35E-3);
   }

   private static double getTSForFixedFreq(double kL, double frequency, SDWBAModel model) {
      double k = (2 * Math.PI * frequency) / 1456;
      double L = kL / k;

      double backscatter = model.getBackscatter(L, frequency);

      return 10 * Math.log10(backscatter) - 20 * Math.log10(L / 38.35E-3);
   }

   private static double kFromFreq(double frequency) {
      return frequency * 2 * Math.PI / 1456;
   }
}
