package no.imr.korona.color;

import no.imr.tools.swing.ColorUtils;

import java.awt.Color;

/**
 * RGB color with components between 0 and 1.
 */
public record RGBColor(float red, float green, float blue) {

   public int getRGB() {
      return ColorUtils.toRGB(red, green, blue);
   }

   public Color getColor() {
      return new Color(red, green, blue);
   }

   public int interpolateToRGB(RGBColor other, float otherWeight) {
      float weight = 1 - otherWeight;

      float r = weight * red + otherWeight * other.red;
      float g = weight * green + otherWeight * other.green;
      float b = weight * blue + otherWeight * other.blue;

      return ColorUtils.toRGB(r, g, b);
   }
}
