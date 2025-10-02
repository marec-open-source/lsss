package no.imr.korona.color;

import no.imr.tools.ResourceUtils;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Definitions of some colormaps.
 */
public final class Colormaps {
   public static final Colormap RAINBOW = makeSimpleColormap("Rainbow",
         Color.WHITE, new HSBColor(250.0f / 360f, 0.6f, 1.0f), new HSBColor(0.0f, 0.8f, 0.8f));

   public static final Colormap GREYLEVELS = makeSimpleColormap("Greylevels",
         Color.WHITE, new HSBColor(300.0f / 360f, 0.0f, 1.0f), new HSBColor(0.0f, 0.0f, 0.1f));

   public static final Colormap COMBINED = makeSimpleColormap("Combined",
         Color.WHITE, new HSBColor(300.0f / 360f, 0.1f, 1.0f), new HSBColor(0.0f, 1.0f, 0.5f));

   public static final Colormap NIGHT = makeSimpleColormap("Night",
         Color.BLACK, new HSBColor(300.0f / 360f, 0.6f, 0.0f), new HSBColor(0.0f, 1.0f, 0.8f));

   public static final Colormap CONTRAST_COARSE = parseStepwiseColormap("Contrast (coarse)",
         "no/imr/korona/resources/colors/contrastCoarse.txt");

   public static final Colormap CONTRAST_FINE = parseLinearColormap("Contrast (fine)",
         "no/imr/korona/resources/colors/contrastFine.txt");

   public static final Colormap JET = parseLinearColormap("Jet",
         "no/imr/korona/resources/colors/jet.txt");

   public static final Colormap HOT = parseLinearColormap("Hot",
         "no/imr/korona/resources/colors/hot.txt");

   public static final Colormap PEROLF = parseLinearColormap("Perolf",
         "no/imr/korona/resources/colors/perolf.txt");

   public static final Colormap BONE = parseLinearColormap("Bone",
         "no/imr/korona/resources/colors/bone.txt");

   public static final Colormap RED_GREEN = parseLinearColormap("RedGreen",
         "no/imr/korona/resources/colors/redGreen.txt");

   public static final Colormap EK500 = parseStepwiseColormap("EK500",
         "no/imr/korona/resources/colors/ek500.txt");

   public static final Colormap EK60 = parseLinearColormap("EK60",
         "no/imr/korona/resources/colors/ek60.txt");

   public static final Colormap DIVERGING_BLUE_RED = parseLinearColormap("DivergingBlueRed",
         "no/imr/korona/resources/colors/divergingBlueRed.txt");

   public static final Colormap SEQUENTIAL_YELLOW_RED = parseLinearColormap("SequentialYellowRed",
         "no/imr/korona/resources/colors/sequentialYellowRed.txt");

   public static final Colormap SEQUENTIAL_YELLOW_RED_NIGHT = parseLinearColormap("SequentialYellowRedNight",
         "no/imr/korona/resources/colors/sequentialYellowRedNight.txt");

   public static final Colormap SEQUENTIAL_YELLOW_RED_DISCRETE = parseStepwiseColormap("SequentialYellowRedD",
         "no/imr/korona/resources/colors/sequentialYellowRedDiscrete.txt");

   public static final Colormap DIVERGING_GRAY_RED = parseLinearColormap("DivergingGrayRed",
         "no/imr/korona/resources/colors/divergingGrayRed.txt");

   public static final List<Colormap> ALL = List.of(
         RAINBOW,
         GREYLEVELS,
         COMBINED,
         CONTRAST_COARSE,
         CONTRAST_FINE,
         JET,
         HOT,
         PEROLF,
         BONE,
         RED_GREEN,
         EK500,
         EK60,
         NIGHT,
         SEQUENTIAL_YELLOW_RED,
         SEQUENTIAL_YELLOW_RED_DISCRETE,
         SEQUENTIAL_YELLOW_RED_NIGHT,
         DIVERGING_BLUE_RED,
         DIVERGING_GRAY_RED
   );

   private Colormaps() {
   }

   private static Colormap makeSimpleColormap(String name, Color belowColor, HSBColor start, HSBColor stop) {
      return new Colormap(name, belowColor, stop.getColor(), ColorInterpolation.linear(start, stop));
   }

   private static Colormap parseStepwiseColormap(String name, String resourceName) {
      return parseColormap(name, resourceName, false);
   }

   public static Colormap parseLinearColormap(String name, String resourceName) {
      return parseColormap(name, resourceName, true);
   }

   private static Colormap parseColormap(String name, String resourceName, boolean linearInterpolation) {
      // Layout of colormap file:
      // belowColor
      //
      // color 0
      // color 1
      // ...
      // color n-1
      //
      // aboveColor

      String[] words = ResourceUtils.getString(resourceName).trim().split("\\s+");
      assert words.length % 3 == 0;

      int n = words.length / 3 - 2; // -2 because of belowColor and aboveColor

      Color belowColor = parseRGBColor(words, 0).getColor();
      Color aboveColor = parseRGBColor(words, n + 1).getColor();

      List<RGBColor> rgbColors = IntStream.rangeClosed(1, n)
            .mapToObj(i -> parseRGBColor(words, i))
            .toList();

      ColorInterpolation colorInterpolation = linearInterpolation
            ? ColorInterpolation.linear(rgbColors)
            : ColorInterpolation.stepwise(rgbColors);
      return new Colormap(name, belowColor, aboveColor, colorInterpolation);
   }

   private static RGBColor parseRGBColor(String[] words, int iColor) {
      int iWord = 3 * iColor;
      float r = Float.parseFloat(words[iWord]);
      float g = Float.parseFloat(words[iWord + 1]);
      float b = Float.parseFloat(words[iWord + 2]);
      return new RGBColor(r, g, b);
   }

   public static @Nullable Colormap getByName(String name) {
      for (Colormap colormap : ALL) {
         if (colormap.getName().equals(name)) {
            return colormap;
         }
      }
      return null;
   }
}
