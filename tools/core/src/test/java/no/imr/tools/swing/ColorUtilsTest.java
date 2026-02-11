package no.imr.tools.swing;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

final class ColorUtilsTest {
   @Test
   void parseColor() {
      testColorName("red", Color.RED);
      testColorName("crimson", ColorUtils.CRIMSON);
      testColorName("#1234ef", new Color(0x1234ef));
      testColorName("#0034ef", new Color(0x0034ef));
      assertEquals(Color.RED, ColorUtils.parseColor("#FF0000")); // Uppercase is valid.
      assertEquals(Color.RED, ColorUtils.parseColor("ff0000")); // Initial '#' is optional.
      assertNull(ColorUtils.parseColor("NoNoNo"));
   }

   private static void testColorName(String name, Color color) {
      assertEquals(name, ColorUtils.colorToNameOrHex(color));
      assertEquals(color, ColorUtils.parseColor(name));
   }

   @Test
   void relativeLuminance() {
      assertEquals(0, ColorUtils.relativeLuminance(Color.BLACK));
      assertEquals(1, ColorUtils.relativeLuminance(Color.WHITE));
   }

   @Test
   void contrastingBlackOrWhite() {
      assertEquals(Color.WHITE, ColorUtils.contrastingBlackOrWhite(Color.BLACK));
      assertEquals(Color.BLACK, ColorUtils.contrastingBlackOrWhite(Color.WHITE));
   }

   @Test
   void toRGB() {
      JUnitUtils.runWithRandom(random -> {
         float r = random.nextFloat();
         float g = random.nextFloat();
         float b = random.nextFloat();
         assertEquals(new Color(r, g, b).getRGB(), ColorUtils.toRGB(r, g, b));
      });
   }
}
