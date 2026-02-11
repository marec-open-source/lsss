package no.imr.korona.color;

/**
 * For converting values to color.
 */
public record Colormap(
      String name,
      int belowRGB,
      int aboveRGB,
      ColorInterpolation colorInterpolation
) {
   @Override
   public String toString() {
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
}
