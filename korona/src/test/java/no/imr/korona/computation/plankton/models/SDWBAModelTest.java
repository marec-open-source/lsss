package no.imr.korona.computation.plankton.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SDWBAModelTest {
   @Test
   void testModel() {
      SDWBAModel sdwbaModel = new SDWBAModel();
      sdwbaModel.setParameterSetName(SDWBAModel.SDWBAParameterSetName.N114);

      double f = 120000;
      double L = 30e-3;

      double backscatter = sdwbaModel.getBackscatter(L, f);
      double TS1 = 10 * Math.log10(backscatter);
      assertEquals(-76.77871207070308, TS1, 0.0001);

      double volume = sdwbaModel.getBioVolume(38.35e-3);
      assertEquals(4.2074434732687794e-7, volume, 0.0001e-7);
   }
}
