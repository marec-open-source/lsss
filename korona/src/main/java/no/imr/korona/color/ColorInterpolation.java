package no.imr.korona.color;

import java.util.List;

@FunctionalInterface
public interface ColorInterpolation {
   int valueToRGB(float value);

   static ColorInterpolation linear(HSBColor start, HSBColor stop) {
      return value -> start.interpolateToRGB(stop, value);
   }

   static ColorInterpolation linear(List<RGBColor> colors) {
      return value -> {
         float iAsFloat = value * (colors.size() - 1);
         int i = (int) Math.floor(iAsFloat);
         if (i < 0) {
            return colors.getFirst().getRGB();
         }
         if (i + 1 >= colors.size()) {
            return colors.getLast().getRGB();
         }

         RGBColor a = colors.get(i);
         RGBColor b = colors.get(i + 1);

         float bWeight = iAsFloat - i;
         return a.interpolateToRGB(b, bWeight);
      };
   }

   static ColorInterpolation stepwise(List<RGBColor> colors) {
      return value -> {
         int i = Math.clamp((int) Math.floor(value * colors.size()), 0, colors.size() - 1);
         return colors.get(i).getRGB();
      };
   }
}
