package no.imr.korona.viewer.coloring;

import no.imr.korona.color.DiscreteColorMapping;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.viewer.ResampleMode;
import no.imr.korona.viewer.Resampler;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.korona.viewer.variables.DiscreteVariableResult;
import no.imr.tools.range.FloatRange;

import java.util.Arrays;

/**
 * Convert from category to color.
 */
public final class CategoryColorConverter extends DiscreteColorConverter {
   public CategoryColorConverter(DiscreteVariable discreteVariable) {
      super(discreteVariable);
   }

   @Override
   public ColorConverterType getType() {
      return ColorConverterType.DISCRETE;
   }

   @Override
   public void convertToColor(Ping ping, int channel, int[] rgbs, FloatRange depthRange) {
      DiscreteVariableResult discreteVariableResult = getDiscreteVariable().evaluate(ping);
      byte[] byteData = discreteVariableResult.byteData();

      if (byteData.length == 0) {
         Arrays.fill(rgbs, ValueColor.NO_DATA_RGB);
         return;
      }

      byte[] categories = new byte[rgbs.length];
      Resampler.sampleByteData(byteData, discreteVariableResult.depthRange(), categories, depthRange, ResampleMode.NEAREST);

      getRGBs(rgbs, categories);
   }

   private void getRGBs(int[] rgbs, byte[] categories) {
      DiscreteColorMapping discreteColorMapping = getDiscreteVariable().getSettings().getDiscreteColorMapping();

      for (int i = 0; i < rgbs.length; i++) {
         byte category = categories[i];
         if (category == ValueColor.NO_DATA_BYTE) {
            rgbs[i] = ValueColor.NO_DATA_RGB;
            continue;
         }

         rgbs[i] = discreteColorMapping.getRGB(category);
      }
   }
}
