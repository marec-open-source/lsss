package no.imr.tools.swing;

import com.google.common.collect.ImmutableMap;
import no.imr.tools.Utils;
import no.imr.tools.math.MathUtils;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.Map;
import java.util.function.DoubleFunction;

/**
 * Named colors.
 */
public final class ColorUtils {
   public static final Color ALICEBLUE = new Color(0xf0f8ff);
   public static final Color ANTIQUEWHITE = new Color(0xfaebd7);
   public static final Color AQUA = Color.CYAN; // new Color(0x00ffff);
   public static final Color AQUAMARINE = new Color(0x7fffd4);
   public static final Color AZURE = new Color(0xf0ffff);
   public static final Color BEIGE = new Color(0xf5f5dc);
   public static final Color BISQUE = new Color(0xffe4c4);
   public static final Color BLACK = Color.BLACK; // new Color(0x000000);
   public static final Color BLANCHEDALMOND = new Color(0xffebcd);
   public static final Color BLUE = Color.BLUE; // new Color(0x0000ff);
   public static final Color BLUEVIOLET = new Color(0x8a2be2);
   public static final Color BROWN = new Color(0xa52a2a);
   public static final Color BURLYWOOD = new Color(0xdeb887);
   public static final Color CADETBLUE = new Color(0x5f9ea0);
   public static final Color CHARTREUSE = new Color(0x7fff00);
   public static final Color CHOCOLATE = new Color(0xd2691e);
   public static final Color CORAL = new Color(0xff7f50);
   public static final Color CORNFLOWERBLUE = new Color(0x6495ed);
   public static final Color CORNSILK = new Color(0xfff8dc);
   public static final Color CRIMSON = new Color(0xdc143c);
   public static final Color CYAN = Color.CYAN; // new Color(0x00ffff);
   public static final Color DARKBLUE = new Color(0x00008b);
   public static final Color DARKCYAN = new Color(0x008b8b);
   public static final Color DARKGOLDENROD = new Color(0xb8860b);
   public static final Color DARKGRAY = new Color(0xa9a9a9);
   public static final Color DARKGREEN = new Color(0x006400);
   public static final Color DARKKHAKI = new Color(0xbdb76b);
   public static final Color DARKMAGENTA = new Color(0x8b008b);
   public static final Color DARKOLIVEGREEN = new Color(0x556b2f);
   public static final Color DARKORANGE = new Color(0xff8c00);
   public static final Color DARKORCHID = new Color(0x9932cc);
   public static final Color DARKRED = new Color(0x8b0000);
   public static final Color DARKSALMON = new Color(0xe9967a);
   public static final Color DARKSEAGREEN = new Color(0x8fbc8f);
   public static final Color DARKSLATEBLUE = new Color(0x483d8b);
   public static final Color DARKSLATEGRAY = new Color(0x2f4f4f);
   public static final Color DARKTURQUOISE = new Color(0x00ced1);
   public static final Color DARKVIOLET = new Color(0x9400d3);
   public static final Color DEEPPINK = new Color(0xff1493);
   public static final Color DEEPSKYBLUE = new Color(0x00bfff);
   public static final Color DIMGRAY = new Color(0x696969);
   public static final Color DODGERBLUE = new Color(0x1e90ff);
   public static final Color FIREBRICK = new Color(0xb22222);
   public static final Color FLORALWHITE = new Color(0xfffaf0);
   public static final Color FORESTGREEN = new Color(0x228b22);
   public static final Color FUCHSIA = Color.MAGENTA; // new Color(0xff00ff);
   public static final Color GAINSBORO = new Color(0xdcdcdc);
   public static final Color GHOSTWHITE = new Color(0xf8f8ff);
   public static final Color GOLD = new Color(0xffd700);
   public static final Color GOLDENROD = new Color(0xdaa520);
   public static final Color GRAY = Color.GRAY; // new Color(0x808080);
   public static final Color GREEN = new Color(0x008000);
   public static final Color GREENYELLOW = new Color(0xadff2f);
   public static final Color HONEYDEW = new Color(0xf0fff0);
   public static final Color HOTPINK = new Color(0xff69b4);
   public static final Color INDIANRED = new Color(0xcd5c5c);
   public static final Color INDIGO = new Color(0x4b0082);
   public static final Color IVORY = new Color(0xfffff0);
   public static final Color KHAKI = new Color(0xf0e68c);
   public static final Color LAVENDER = new Color(0xe6e6fa);
   public static final Color LAVENDERBLUSH = new Color(0xfff0f5);
   public static final Color LAWNGREEN = new Color(0x7cfc00);
   public static final Color LEMONCHIFFON = new Color(0xfffacd);
   public static final Color LIGHTBLUE = new Color(0xadd8e6);
   public static final Color LIGHTCORAL = new Color(0xf08080);
   public static final Color LIGHTCYAN = new Color(0xe0ffff);
   public static final Color LIGHTGOLDENRODYELLOW = new Color(0xfafad2);
   public static final Color LIGHTGREEN = new Color(0x90ee90);
   public static final Color LIGHTGREY = new Color(0xd3d3d3);
   public static final Color LIGHTPINK = new Color(0xffb6c1);
   public static final Color LIGHTSALMON = new Color(0xffa07a);
   public static final Color LIGHTSEAGREEN = new Color(0x20b2aa);
   public static final Color LIGHTSKYBLUE = new Color(0x87cefa);
   public static final Color LIGHTSLATEGRAY = new Color(0x778899);
   public static final Color LIGHTSTEELBLUE = new Color(0xb0c4de);
   public static final Color LIGHTYELLOW = new Color(0xffffe0);
   public static final Color LIME = Color.GREEN; // new Color(0x00ff00);
   public static final Color LIMEGREEN = new Color(0x32cd32);
   public static final Color LINEN = new Color(0xfaf0e6);
   public static final Color MAGENTA = Color.MAGENTA; // new Color(0xff00ff);
   public static final Color MAROON = new Color(0x800000);
   public static final Color MEDIUMAQUAMARINE = new Color(0x66cdaa);
   public static final Color MEDIUMBLUE = new Color(0x0000cd);
   public static final Color MEDIUMORCHID = new Color(0xba55d3);
   public static final Color MEDIUMPURPLE = new Color(0x9370db);
   public static final Color MEDIUMSEAGREEN = new Color(0x3cb371);
   public static final Color MEDIUMSLATEBLUE = new Color(0x7b68ee);
   public static final Color MEDIUMSPRINGGREEN = new Color(0x00fa9a);
   public static final Color MEDIUMTURQUOISE = new Color(0x48d1cc);
   public static final Color MEDIUMVIOLETRED = new Color(0xc71585);
   public static final Color MIDNIGHTBLUE = new Color(0x191970);
   public static final Color MINTCREAM = new Color(0xf5fffa);
   public static final Color MISTYROSE = new Color(0xffe4e1);
   public static final Color MOCASSIN = new Color(0xffe4b5);
   public static final Color NAVAJOWHITE = new Color(0xffdead);
   public static final Color NAVY = new Color(0x000080);
   public static final Color OLDLACE = new Color(0xfdf5e6);
   public static final Color OLIVE = new Color(0x808000);
   public static final Color OLIVEDRAB = new Color(0x6b8e23);
   public static final Color ORANGE = new Color(0xffa500);
   public static final Color ORANGERED = new Color(0xff4500);
   public static final Color ORCHID = new Color(0xda70d6);
   public static final Color PALEGOLDENROD = new Color(0xeee8aa);
   public static final Color PALEGREEN = new Color(0x98fb98);
   public static final Color PALETURQUOISE = new Color(0xafeeee);
   public static final Color PALEVIOLETRED = new Color(0xdb7093);
   public static final Color PAPAYAWHIP = new Color(0xffefd5);
   public static final Color PEACHPUFF = new Color(0xffdab9);
   public static final Color PERU = new Color(0xcd853f);
   public static final Color PINK = new Color(0xffc0cb);
   public static final Color PLUM = new Color(0xdda0dd);
   public static final Color POWDERBLUE = new Color(0xb0e0e6);
   public static final Color PURPLE = new Color(0x800080);
   public static final Color RED = Color.RED; // new Color(0xff0000);
   public static final Color ROSYBROWN = new Color(0xbc8f8f);
   public static final Color ROYALBLUE = new Color(0x4169e1);
   public static final Color SADDLEBROWN = new Color(0x8b4513);
   public static final Color SALMON = new Color(0xfa8072);
   public static final Color SANDYBROWN = new Color(0xf4a460);
   public static final Color SEAGREEN = new Color(0x2e8b57);
   public static final Color SEASHELL = new Color(0xfff5ee);
   public static final Color SIENNA = new Color(0xa0522d);
   public static final Color SILVER = Color.LIGHT_GRAY; // new Color(0xc0c0c0);
   public static final Color SKYBLUE = new Color(0x87ceeb);
   public static final Color SLATEBLUE = new Color(0x6a5acd);
   public static final Color SLATEGRAY = new Color(0x708090);
   public static final Color SNOW = new Color(0xfffafa);
   public static final Color SPRINGGREEN = new Color(0x00ff7f);
   public static final Color STEELBLUE = new Color(0x4682b4);
   public static final Color TAN = new Color(0xd2b48c);
   public static final Color TEAL = new Color(0x008080);
   public static final Color THISTLE = new Color(0xd8bfd8);
   public static final Color TOMATO = new Color(0xff6347);
   public static final Color TURQUOISE = new Color(0x40e0d0);
   public static final Color VIOLET = new Color(0xee82ee);
   public static final Color WHEAT = new Color(0xf5deb3);
   public static final Color WHITE = Color.WHITE; // new Color(0xffffff);
   public static final Color WHITESMOKE = new Color(0xf5f5f5);
   public static final Color YELLOW = Color.YELLOW; // new Color(0xffff00);
   public static final Color YELLOWGREEN = new Color(0x9acd32);

   private ColorUtils() {
   }

   public static String colorToNameOrHex(Color color) {
      for (Map.Entry<String, Color> entry : getNameToColor().entrySet()) {
         if (entry.getValue().equals(color)) {
            return entry.getKey();
         }
      }
      return colorToHex(color);
   }

   public static String colorToHex(Color color) {
      return colorToHex(color.getRGB());
   }

   public static String colorToHex(int rgb) {
      return Utils.format("#%06x", 0x00ffffff & rgb);
   }

   public static @Nullable Color parseColor(String s) {
      if (s.startsWith("#")) {
         try {
            int rgb = Integer.parseInt(s, 1, s.length(), 16);
            return new Color(rgb);
         } catch (NumberFormatException _) {
            return null;
         }
      }
      // Check if it is a named color.
      Color color = getNameToColor().get(s);
      if (color != null) {
         return color;
      }
      // Finally, try hex format without initial "#".
      try {
         int rgb = Integer.parseInt(s, 16);
         return new Color(rgb);
      } catch (NumberFormatException _) {
         return null;
      }
   }

   public static Map<String, Color> getNameToColor() {
      return LazyNameToColorHolder.NAME_TO_COLOR;
   }

   public static double relativeLuminance(Color color) {
      // Reference: https://www.w3.org/TR/WCAG20/#relativeluminancedef
      double r = color.getRed() / 255.0;
      double g = color.getGreen() / 255.0;
      double b = color.getBlue() / 255.0;
      r = r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
      g = g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
      b = b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);
      return 0.2126 * r + 0.7152 * g + 0.0722 * b;
   }

   public static double contrastRatio(Color a, Color b) {
      // Reference: https://www.w3.org/TR/WCAG20/#contrast-ratiodef
      // Contrast ratio = (L1 + 0.05) / (L2 + 0.05), where L1 is the lighter color.
      double rla = relativeLuminance(a) + 0.05;
      double rlb = relativeLuminance(b) + 0.05;
      return rla > rlb ? rla / rlb : rlb / rla;
   }

   public static Color contrastingBlackOrWhite(Color color) {
      // Reference: https://www.w3.org/TR/WCAG20/#contrast-ratiodef
      // Choose back or white with the highest contrast ratio = (L1 + 0.05) / (L2 + 0.05),
      // where L1 is the lighter color.
      // So choose black if (L + 0.05) / (0 + 0.05) > (1 + 0.05) / (L + 0.05)
      // => L > sqrt(1.05 * 0.05) - 0.05
      return relativeLuminance(color) > 0.17912878474779204 ? Color.BLACK : Color.WHITE;
   }

   public static Color contrastingColorWithSameHue(Color color, double targetContrastRatio) {
      float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
      float h = hsb[0];
      Color colorMaxSB = Color.getHSBColor(h, 1, 1);
      double yMaxSB = contrastRatio(color, colorMaxSB) - targetContrastRatio;
      if (yMaxSB >= 0) {
         float s = hsb[1];
         float b = hsb[2];
         DoubleFunction<Color> toColor = x -> Color.getHSBColor(h, (float) (s * (1 - x) + x), (float) (b * (1 - x) + x));
         double xsb = MathUtils.findRoot(0, 1, x -> contrastRatio(color, toColor.apply(x)) - targetContrastRatio, 1e-6, 1e-6);
         return toColor.apply(xsb);
      }

      double ys1 = yMaxSB;
      double yb1 = yMaxSB;
      float step = 0.1f;
      float x1 = 1;
      while (true) {
         float x2 = x1 - step;
         if (x2 <= 0) {
            return contrastingBlackOrWhite(color);
         }
         double ys2 = contrastRatio(color, Color.getHSBColor(h, x2, 1)) - targetContrastRatio;
         double yb2 = contrastRatio(color, Color.getHSBColor(h, 1, x2)) - targetContrastRatio;
         if (ys2 >= 0 || yb2 >= 0) {
            if (ys2 > yb2) {
               double alpha = ys2 / (ys2 - ys1);
               double x = alpha * x1 + (1 - alpha) * x2;
               return Color.getHSBColor(h, (float) x, 1);
            } else {
               double alpha = yb2 / (yb2 - yb1);
               double x = alpha * x1 + (1 - alpha) * x2;
               return Color.getHSBColor(h, 1, (float) x);
            }
         }
         x1 = x2;
         ys1 = ys2;
         yb1 = yb2;
      }
   }

   public static int toRGB(float red, float green, float blue) {
      int r = Math.clamp((int) (red * 255 + 0.5), 0, 0xff);
      int g = Math.clamp((int) (green * 255 + 0.5), 0, 0xff);
      int b = Math.clamp((int) (blue * 255 + 0.5), 0, 0xff);
      return (0xff << 24) | (r << 16) | (g << 8) | b;
   }

   /**
    * Holder class for lazy initialization.
    */
   private static final class LazyNameToColorHolder {
      private static final ImmutableMap<String, Color> NAME_TO_COLOR = ImmutableMap.<String, Color>builder()
            .put("aliceblue", ALICEBLUE)
            .put("antiquewhite", ANTIQUEWHITE)
            .put("aqua", AQUA)
            .put("aquamarine", AQUAMARINE)
            .put("azure", AZURE)
            .put("beige", BEIGE)
            .put("bisque", BISQUE)
            .put("black", BLACK)
            .put("blanchedalmond", BLANCHEDALMOND)
            .put("blue", BLUE)
            .put("blueviolet", BLUEVIOLET)
            .put("brown", BROWN)
            .put("burlywood", BURLYWOOD)
            .put("cadetblue", CADETBLUE)
            .put("chartreuse", CHARTREUSE)
            .put("chocolate", CHOCOLATE)
            .put("coral", CORAL)
            .put("cornflowerblue", CORNFLOWERBLUE)
            .put("cornsilk", CORNSILK)
            .put("crimson", CRIMSON)
            .put("cyan", CYAN)
            .put("darkblue", DARKBLUE)
            .put("darkcyan", DARKCYAN)
            .put("darkgoldenrod", DARKGOLDENROD)
            .put("darkgray", DARKGRAY)
            .put("darkgreen", DARKGREEN)
            .put("darkkhaki", DARKKHAKI)
            .put("darkmagenta", DARKMAGENTA)
            .put("darkolivegreen", DARKOLIVEGREEN)
            .put("darkorange", DARKORANGE)
            .put("darkorchid", DARKORCHID)
            .put("darkred", DARKRED)
            .put("darksalmon", DARKSALMON)
            .put("darkseagreen", DARKSEAGREEN)
            .put("darkslateblue", DARKSLATEBLUE)
            .put("darkslategray", DARKSLATEGRAY)
            .put("darkturquoise", DARKTURQUOISE)
            .put("darkviolet", DARKVIOLET)
            .put("deeppink", DEEPPINK)
            .put("deepskyblue", DEEPSKYBLUE)
            .put("dimgray", DIMGRAY)
            .put("dodgerblue", DODGERBLUE)
            .put("firebrick", FIREBRICK)
            .put("floralwhite", FLORALWHITE)
            .put("forestgreen", FORESTGREEN)
            .put("fuchsia", FUCHSIA)
            .put("gainsboro", GAINSBORO)
            .put("ghostwhite", GHOSTWHITE)
            .put("gold", GOLD)
            .put("goldenrod", GOLDENROD)
            .put("gray", GRAY)
            .put("green", GREEN)
            .put("greenyellow", GREENYELLOW)
            .put("honeydew", HONEYDEW)
            .put("hotpink", HOTPINK)
            .put("indianred", INDIANRED)
            .put("indigo", INDIGO)
            .put("ivory", IVORY)
            .put("khaki", KHAKI)
            .put("lavender", LAVENDER)
            .put("lavenderblush", LAVENDERBLUSH)
            .put("lawngreen", LAWNGREEN)
            .put("lemonchiffon", LEMONCHIFFON)
            .put("lightblue", LIGHTBLUE)
            .put("lightcoral", LIGHTCORAL)
            .put("lightcyan", LIGHTCYAN)
            .put("lightgoldenrodyellow", LIGHTGOLDENRODYELLOW)
            .put("lightgreen", LIGHTGREEN)
            .put("lightgrey", LIGHTGREY)
            .put("lightpink", LIGHTPINK)
            .put("lightsalmon", LIGHTSALMON)
            .put("lightseagreen", LIGHTSEAGREEN)
            .put("lightskyblue", LIGHTSKYBLUE)
            .put("lightslategray", LIGHTSLATEGRAY)
            .put("lightsteelblue", LIGHTSTEELBLUE)
            .put("lightyellow", LIGHTYELLOW)
            .put("lime", LIME)
            .put("limegreen", LIMEGREEN)
            .put("linen", LINEN)
            .put("magenta", MAGENTA)
            .put("maroon", MAROON)
            .put("mediumaquamarine", MEDIUMAQUAMARINE)
            .put("mediumblue", MEDIUMBLUE)
            .put("mediumorchid", MEDIUMORCHID)
            .put("mediumpurple", MEDIUMPURPLE)
            .put("mediumseagreen", MEDIUMSEAGREEN)
            .put("mediumslateblue", MEDIUMSLATEBLUE)
            .put("mediumspringgreen", MEDIUMSPRINGGREEN)
            .put("mediumturquoise", MEDIUMTURQUOISE)
            .put("mediumvioletred", MEDIUMVIOLETRED)
            .put("midnightblue", MIDNIGHTBLUE)
            .put("mintcream", MINTCREAM)
            .put("mistyrose", MISTYROSE)
            .put("moccasin", MOCASSIN)
            .put("navajowhite", NAVAJOWHITE)
            .put("navy", NAVY)
            .put("oldlace", OLDLACE)
            .put("olive", OLIVE)
            .put("olivedrab", OLIVEDRAB)
            .put("orange", ORANGE)
            .put("orangered", ORANGERED)
            .put("orchid", ORCHID)
            .put("palegoldenrod", PALEGOLDENROD)
            .put("palegreen", PALEGREEN)
            .put("paleturquoise", PALETURQUOISE)
            .put("palevioletred", PALEVIOLETRED)
            .put("papayawhip", PAPAYAWHIP)
            .put("peachpuff", PEACHPUFF)
            .put("peru", PERU)
            .put("pink", PINK)
            .put("plum", PLUM)
            .put("powderblue", POWDERBLUE)
            .put("purple", PURPLE)
            .put("red", RED)
            .put("rosybrown", ROSYBROWN)
            .put("royalblue", ROYALBLUE)
            .put("saddlebrown", SADDLEBROWN)
            .put("salmon", SALMON)
            .put("sandybrown", SANDYBROWN)
            .put("seagreen", SEAGREEN)
            .put("seashell", SEASHELL)
            .put("sienna", SIENNA)
            .put("silver", SILVER)
            .put("skyblue", SKYBLUE)
            .put("slateblue", SLATEBLUE)
            .put("slategray", SLATEGRAY)
            .put("snow", SNOW)
            .put("springgreen", SPRINGGREEN)
            .put("steelblue", STEELBLUE)
            .put("tan", TAN)
            .put("teal", TEAL)
            .put("thistle", THISTLE)
            .put("tomato", TOMATO)
            .put("turquoise", TURQUOISE)
            .put("violet", VIOLET)
            .put("wheat", WHEAT)
            .put("white", WHITE)
            .put("whitesmoke", WHITESMOKE)
            .put("yellow", YELLOW)
            .put("yellowgreen", YELLOWGREEN)
            .build();

      private LazyNameToColorHolder() {
      }
   }
}
