package no.imr.korona.util.masking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class BeamMaskErodeDilateTest {
   @Test
   void testErodeDilate() {
      FullBeamMask mask = new FullBeamMask(10);
      mask.setInside(5);
      mask.setInside(6);
      mask.setInside(7);

      FullBeamMask erodeDilate1 = BeamMaskErodeDilate.erode(mask, 1);
      erodeDilate1 = BeamMaskErodeDilate.dilate(erodeDilate1, 1);
      for (int i = 0; i < mask.size(); i++) {
         assertEquals(mask.isInside(i), erodeDilate1.isInside(i));
      }

      FullBeamMask erodeDilate3 = BeamMaskErodeDilate.erode(mask, 3);
      erodeDilate3 = BeamMaskErodeDilate.dilate(erodeDilate3, 3);
      for (int i = 0; i < mask.size(); i++) {
         assertEquals(mask.isInside(i), erodeDilate3.isInside(i));
      }

      FullBeamMask erodeDilate4 = BeamMaskErodeDilate.erode(mask, 5);
      erodeDilate4 = BeamMaskErodeDilate.dilate(erodeDilate4, 5);
      for (int i = 0; i < erodeDilate4.size(); i++) {
         assertFalse(erodeDilate4.isInside(i));
      }
   }
}
