package no.imr.korona.util.masking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class FullBeamMaskTest {
   @Test
   void toCompactBeamMask() {
      FullBeamMask fullBeamMask = new FullBeamMask(20);
      fullBeamMask.setInside(2);
      fullBeamMask.setInside(5);
      fullBeamMask.setInside(6);
      fullBeamMask.setInside(19);
      assertArrayEquals(new int[]{2, 3, 5, 7, 19, 20}, fullBeamMask.toCompactBeamMask().getIndexes());
      fullBeamMask.setOutside(2);
      fullBeamMask.setOutside(19);
      assertArrayEquals(new int[]{5, 7}, fullBeamMask.toCompactBeamMask().getIndexes());
   }
}
