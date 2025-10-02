package no.imr.korona.color;

import java.awt.Color;

/**
 * HSB color with components between 0 and 1.
 */
public record HSBColor(float hue, float saturation, float brightness) {

   public int getRGB() {
      return Color.HSBtoRGB(hue, saturation, brightness);
   }

   public Color getColor() {
      return Color.getHSBColor(hue, saturation, brightness);
   }

   public int interpolateToRGB(HSBColor other, float otherWeight) {
      float weight = 1 - otherWeight;

      float h = weight * hue + otherWeight * other.hue;
      float s = weight * saturation + otherWeight * other.saturation;
      float b = weight * brightness + otherWeight * other.brightness;

      return Color.HSBtoRGB(h, s, b);
   }
}
