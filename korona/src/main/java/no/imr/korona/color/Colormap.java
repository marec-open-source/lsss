package no.imr.korona.color;

import java.awt.Color;

/**
 * For converting one value to color.
 */
public final class Colormap {
   private final String name;
   private final int belowColor;
   private final int aboveColor;
   private final ColorInterpolation colorInterpolation;

   public Colormap(String name, Color belowColor, Color aboveColor, ColorInterpolation colorInterpolation) {
      this.name = name;
      this.belowColor = belowColor.getRGB();
      this.aboveColor = aboveColor.getRGB();
      this.colorInterpolation = colorInterpolation;
   }

   @Override
   public String toString() {
      return name;
   }

   public String getName() {
      return name;
   }

   /**
    * Convert single value to color.
    *
    * @param value input value between 0..1
    * @return color rgb
    */
   public int getRGB(float value) {
      return colorInterpolation.valueToRGB(value);
   }

   /**
    * Get color below value.
    *
    * @return color rgb
    */
   public int getBelowRGB() {
      return belowColor;
   }

   /**
    * Get color above value.
    *
    * @return color rgb
    */
   public int getAboveRGB() {
      return aboveColor;
   }
}
