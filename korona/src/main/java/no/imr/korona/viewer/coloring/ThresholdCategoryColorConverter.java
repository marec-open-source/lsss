package no.imr.korona.viewer.coloring;

import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.tools.range.FloatRange;

import java.awt.Graphics2D;

public final class ThresholdCategoryColorConverter extends DiscreteAndContinuousColorConverter {
   public ThresholdCategoryColorConverter(DiscreteVariable discreteVariable, ContinuousVariable continuousVariable) {
      super(discreteVariable, continuousVariable);
   }

   @Override
   public ColorConverterType getType() {
      return ColorConverterType.DISCRETE_THRESHOLD;
   }

   @Override
   void getRGBs(int[] rgbs, byte[] categories, float[] values) {
      int unknownRGB = getDiscreteVariable().getUnknownCategory().getColor().getRGB();
      FloatRange effectiveRange = getContinuousVariable().getSettings().getEffectiveRange();

      for (int i = 0; i < rgbs.length; i++) {
         byte category = categories[i];
         if (category == ValueColor.NO_DATA_BYTE) {
            rgbs[i] = ValueColor.NO_DATA_RGB;
            continue;
         }

         float value = values[i];
         if (!effectiveRange.contains(value) && !ValueColor.isNoDataFloat(value)) {
            rgbs[i] = unknownRGB;
            continue;
         }

         rgbs[i] = getDiscreteVariable().getSettings().getDiscreteColorMapping().getRGB(category);
      }
   }

   @Override
   void drawAddedLegend(Graphics2D g, int width, int height) {
      drawMarks(g, width, height);
      drawText(g, height);
   }
}
