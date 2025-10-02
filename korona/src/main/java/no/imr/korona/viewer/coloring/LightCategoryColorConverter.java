package no.imr.korona.viewer.coloring;

import no.imr.korona.color.DiscreteColor;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.tools.Utils;
import no.imr.tools.range.FloatRange;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Convert from two values (category + some other) to color.
 */
public final class LightCategoryColorConverter extends DiscreteAndContinuousColorConverter {
   private final FloatRange lightRange;
   private int[] colorBuffer = Utils.EMPTY_INT_ARRAY;
   private BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
   private float valueToBrightnessFactor;
   private float valueToBrightnessConstant;

   public LightCategoryColorConverter(DiscreteVariable discreteVariable, ContinuousVariable continuousVariable) {
      this(discreteVariable, continuousVariable, FloatRange.of(0.2f, 0.9f));
   }

   public LightCategoryColorConverter(DiscreteVariable discreteVariable, ContinuousVariable continuousVariable, FloatRange lightRange) {
      super(discreteVariable, continuousVariable);

      this.lightRange = lightRange;
      update();
   }

   @Override
   public ColorConverterType getType() {
      return ColorConverterType.DISCRETE_LIGHT;
   }

   @Override
   void getRGBs(int[] rgbs, byte[] categories, float[] values) {
      updateValueToBrightness();

      FloatRange effectiveRange = getContinuousVariable().getSettings().getEffectiveRange();

      for (int i = 0; i < rgbs.length; i++) {
         byte category = categories[i];
         if (category == ValueColor.NO_DATA_BYTE) {
            rgbs[i] = ValueColor.NO_DATA_RGB;
            continue;
         }
         if (getDiscreteVariable().getSettings().isCategorySpecial(category)) {
            rgbs[i] = getDiscreteVariable().getSettings().getDiscreteColorMapping().getRGB(category);
            continue;
         }

         float value = values[i];
         if (ValueColor.isNoDataFloat(value)) {
            rgbs[i] = ValueColor.NO_DATA_RGB;
            continue;
         }
         if (!effectiveRange.contains(value)) {
            rgbs[i] = getDiscreteVariable().getUnknownCategory().getColor().getRGB();
            continue;
         }

         float brightness = Math.clamp(valueToBrightnessFactor * value + valueToBrightnessConstant, 0, 1);
         DiscreteColor discreteColor = getDiscreteVariable().getSettings().getDiscreteColorMapping().valueToDiscreteColor(category);
         rgbs[i] = Color.HSBtoRGB(discreteColor.getHue(), discreteColor.getSaturation(), brightness);
      }
   }

   public int getRgb(byte category, float value) {
      int[] rgb = new int[1];
      getRGBs(rgb, new byte[]{category}, new float[]{value});
      return rgb[0];
   }

   private void updateValueToBrightness() {
      FloatRange range = getContinuousVariable().getSettings().getRange();
      float scale = lightRange.getSize() / range.getSize();
      boolean proportional = getContinuousVariable().getSettings().isProportional();
      valueToBrightnessFactor = proportional ? scale : -scale;
      valueToBrightnessConstant = proportional
            ? lightRange.min() - range.min() * valueToBrightnessFactor
            : lightRange.max() - range.min() * valueToBrightnessFactor;
   }

   @Override
   public void drawAddedLegend(Graphics2D g, int width, int height) {
      drawColorTable(g, width, height);
      drawMarks(g, width, height);
      drawText(g, height);
   }

   private void drawColorTable(Graphics2D g, int width, int height) {
      if (colorBuffer.length != height) {
         colorBuffer = new int[height];
         image = g.getDeviceConfiguration().createCompatibleImage(1, height);
      }

      FloatRange maxRange = getContinuousVariable().getSettings().getMaxRange();
      FloatRange range = getContinuousVariable().getSettings().getRange();

      int yMin = Math.round(height * (1 - maxRange.valueToFraction(range.max())));
      int yMax = Math.round(height * (1 - maxRange.valueToFraction(range.min())));
      for (int y = yMin; y < yMax; y++) {
         float fraction = (y - yMin) / (float) (yMax - yMin);
         if (getContinuousVariable().getSettings().isProportional()) {
            fraction = 1 - fraction;
         }
         float brightness = lightRange.fractionToValue(fraction);
         colorBuffer[y] = Color.HSBtoRGB(0, 0, brightness);
      }
      Arrays.fill(colorBuffer, 0, yMin, getContinuousVariable().getSettings().isClipAbove() ? ValueColor.CLIP_DATA_RGB : colorBuffer[yMin]);
      Arrays.fill(colorBuffer, yMax, height, ValueColor.CLIP_DATA_RGB);

      image.setRGB(0, 0, 1, height, colorBuffer, 0, 1);
      for (int i = width * 2 / 3 - 1; i < width; i++) {
         g.drawImage(image, null, i, 0);
      }
   }
}
