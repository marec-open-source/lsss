package no.imr.korona.color;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ColormapsTest {
   @Test
   void contrastCoarse() {
      assertEquals(0xffffffff, Colormaps.CONTRAST_COARSE.getBelowRGB());
      assertEquals(0xfff2f2ff, Colormaps.CONTRAST_COARSE.getRGB(0));
      assertEquals(0xffd9e0ff, Colormaps.CONTRAST_COARSE.getRGB(0.1f));
      assertEquals(0xffffbf00, Colormaps.CONTRAST_COARSE.getRGB(0.7f));
      assertEquals(0xff990000, Colormaps.CONTRAST_COARSE.getRGB(1));
      assertEquals(0xff990000, Colormaps.CONTRAST_COARSE.getAboveRGB());
   }

   @Test
   void redGreen() {
      assertEquals(0xffcc001a, Colormaps.RED_GREEN.getBelowRGB());
      assertEquals(0xffcc001a, Colormaps.RED_GREEN.getRGB(0));
      assertEquals(0xffd63347, Colormaps.RED_GREEN.getRGB(0.1f));
      assertEquals(0xff99eba3, Colormaps.RED_GREEN.getRGB(0.7f));
      assertEquals(0xff00cc1a, Colormaps.RED_GREEN.getRGB(1));
      assertEquals(0xff00cc1a, Colormaps.RED_GREEN.getAboveRGB());
   }
}
