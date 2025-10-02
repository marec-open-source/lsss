package no.imr.lsss.modules.trawl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SaDataTest {
   @Test
   void missing() {
      assertNull(SaData.getInstance().getSaSpecies(""));
      assertNull(SaData.getInstance().getSaSpecies("XXX"));
   }

   @Test
   void fish() {
      SaSpecies.SaFish s = (SaSpecies.SaFish) SaData.getInstance().getSaSpecies("TORSK");
      assertNotNull(s);
      assertEquals(25, s.getSweepWidth());
      assertEquals(25, s.getSweepWithCorrected(0.5));
      assertEquals(-68, s.getTS(1));
      assertEquals(-68.9151, s.getTS(0.9), 1e-4);
      assertEquals(1.2746e-6, s.getSigma(0.8), 1e-10);
   }

   @Test
   void plankton() {
      SaSpecies.SaPlankton s = (SaSpecies.SaPlankton) SaData.getInstance().getSaSpecies("KRILL");
      assertNotNull(s);
      assertEquals(25, s.getSweepWidth());
      assertEquals(0.03, s.getWeightNumberConstant());
      assertEquals(-50, s.getTS());
      assertEquals(1.2566e-4, s.getSigma(), 1e-8);
   }
}
