package no.imr.korona.util.masking;

import org.dom4j.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class BeamMaskTest {
   @Test
   void allSamplesOutside() {
      BeamMask beamMask = new BeamMask(100);
      Element element = beamMask.toXml(1, true);
      BeamMask beamMask2 = new BeamMask(element);
      assertEquals(beamMask.size(), beamMask2.size());
      assertTrue(beamMask2.isEmpty());
   }

   @Test
   void testBeamStoreAndRestore() {
      int size = 512;
      BeamMask beamMask = new BeamMask(size);
      beamMask.add(150, 512);

      Element element = beamMask.toXml(1, true);

      BeamMask beamMask2 = new BeamMask(element);

      assertArrayEquals(beamMask.getCompactBeamMask().getIndexes(), beamMask2.getCompactBeamMask().getIndexes());
   }
}
