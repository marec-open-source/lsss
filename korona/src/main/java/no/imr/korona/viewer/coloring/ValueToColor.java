package no.imr.korona.viewer.coloring;

import no.imr.korona.color.Colormap;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.tools.range.FloatRange;

/**
 * Converts values to color.
 */
final class ValueToColor {
   private final float min;
   private final float delta;
   private final int belowRGB;
   private final int aboveRGB;
   private final int[] colorTable;

   ValueToColor(ContinuousVariableSettings settings, Colormap colormap) {
      min = settings.getRange().min();
      delta = settings.getDelta();
      belowRGB = colormap.belowRGB();
      aboveRGB = colormap.aboveRGB();
      colorTable = createColorTable(colormap, delta / settings.getRange().getSize());
   }

   void getRGBs(int[] rgbs, float[] values, FloatRange clipRange) {
      for (int i = 0; i < rgbs.length; i++) {
         rgbs[i] = getRGB(values[i], clipRange);
      }
   }

   int getRGB(float value, FloatRange clipRange) {
      if (!clipRange.contains(value)) {
         return belowRGB;
      }

      if (ValueColor.isNoDataFloat(value)) {
         return ValueColor.NO_DATA_RGB;
      }

      int i = (int) Math.floor((value - min) / delta);
      if (i < 0) {
         return belowRGB;
      } else if (i >= colorTable.length) {
         return aboveRGB;
      } else {
         return colorTable[i];
      }
   }

   private static int[] createColorTable(Colormap colormap, float step) {
      int n = Math.round(1 / step);
      int[] colorTable = new int[n];

      for (int i = 0; i < n; i++) {
         colorTable[i] = colormap.getRGB(i * step);
      }

      return colorTable;
   }
}
