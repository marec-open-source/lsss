package no.imr.korona.viewer.coloring;

import no.imr.korona.color.Colormap;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.viewer.ResampleMode;
import no.imr.korona.viewer.Resampler;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.korona.viewer.variables.PerPingSettings;
import no.imr.tools.Utils;
import no.imr.tools.math.NiceNumber;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GuiText;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.NavigableSet;
import java.util.stream.Collectors;

/**
 * Convert a {@link ContinuousVariable} to color.
 */
public final class SingleValueColorConverter extends ColorConverter {
   private final ContinuousVariable continuousVariable;
   private final Colormap colormap;
   private ValueToColor valueToColor;
   private int[] colorBuffer = Utils.EMPTY_INT_ARRAY;
   private BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

   public SingleValueColorConverter(ContinuousVariable continuousVariable, Colormap colormap) {
      this.continuousVariable = continuousVariable;
      this.colormap = colormap;
      valueToColor = new ValueToColor(getSettings(), colormap);
   }

   @Override
   public ColorConverterType getType() {
      return ColorConverterType.CONTINUOUS;
   }

   @Override
   public ContinuousVariable getContinuousVariable() {
      return continuousVariable;
   }

   @Override
   public Colormap getColormap() {
      return colormap;
   }

   @Override
   public void convertToColor(Ping ping, int channel, int[] rgbs, FloatRange depthRange) {
      ContinuousVariableResult continuousVariableResult = continuousVariable.evaluate(channel, ping);
      float[] floatData = continuousVariableResult.floatData();

      if (floatData.length == 0) {
         Arrays.fill(rgbs, ValueColor.NO_DATA_RGB);
         return;
      }

      float[] values = new float[rgbs.length];
      Resampler.sampleFloatData(floatData, continuousVariableResult.depthRange(), values, depthRange, ResampleMode.AVERAGE);
      FloatRange clipRange = getVariablePerPingSettings().getClipRange(ping.getPingIndex());
      valueToColor.getRGBs(rgbs, values, clipRange);
   }

   private ContinuousVariableSettings getSettings() {
      return continuousVariable.getSettings();
   }

   private PerPingSettings getVariablePerPingSettings() {
      return getSettings().getPerPingSettings();
   }

   @Override
   public void drawLegend(Graphics2D g, int width, int height, boolean drawInteractiveControls) {
      drawColors(g, width, height, drawInteractiveControls);
      drawTexts(g, width, height, drawInteractiveControls);
   }

   @Override
   public @Nullable String getToolTipText(Point point) {
      NavigableSet<Float> lowerThresholds = getVariablePerPingSettings().getLowerThresholds();
      NavigableSet<Float> upperThresholds = getVariablePerPingSettings().getUpperThresholds();

      int minY = valueToY(getSettings().getRange().min(), colorBuffer.length);
      int maxY = valueToY(getSettings().getRange().max(), colorBuffer.length);

      int distToMin = lowerThresholds.size() <= 1 ? 99 : Math.abs(point.y - minY);
      int distToMax = upperThresholds.size() <= 1 ? 99 : Math.abs(point.y - maxY);

      if (Math.min(distToMin, distToMax) < 10) {
         return distToMin < distToMax
               ? "<html>Lower thresholds:<br>" + lowerThresholds.stream().map(Utils::toString).collect(Collectors.joining(", "))
               : "<html>Upper thresholds:<br>" + upperThresholds.stream().map(Utils::toString).collect(Collectors.joining(", "));
      }

      if (getVariablePerPingSettings().varyingClipAbove() && point.y < maxY) {
         return "Upper threshold partially active";
      }

      return null;
   }

   private void drawColors(Graphics2D g, int width, int height, boolean drawInteractiveControls) {
      if (colorBuffer.length != height) {
         colorBuffer = new int[height];
         image = g.getDeviceConfiguration().createCompatibleImage(1, height);
      }

      makeColors(colorBuffer, true);
      image.setRGB(0, 0, 1, height, colorBuffer, 0, 1);
      for (int i = 0; i < width; i++) {
         g.drawImage(image, null, i, 0);
      }
      if (getVariablePerPingSettings().varyingClipAbove()) {
         int y = valueToY(getSettings().getRange().max(), height);
         g.setColor(new Color(colormap.getAboveRGB()));
         g.fillRect(0, 0, width / 2, y);
         g.setColor(new Color(colormap.getBelowRGB()));
         g.fillRect(width / 2, 0, width / 2, y);
      }

      if (drawInteractiveControls) {
         FloatRange variableRange = getSettings().getRange();
         drawMark(g, width, height, variableRange.min(), getVariablePerPingSettings().getLowerThresholds());
         drawMark(g, width, height, variableRange.max(), getVariablePerPingSettings().getUpperThresholds());
      }
   }

   /**
    * Make an array or rgb colors.
    *
    * @param colorBuffer the rgb array
    * @param reversed    if reversed then increasing index corresponds to decreasing value
    */
   public void makeColors(int[] colorBuffer, boolean reversed) {
      float delta = getDeltaValue(colorBuffer.length);

      float value;
      if (reversed) {
         value = getSettings().getMaxRange().max();
         delta = -delta;
      } else {
         value = getSettings().getMaxRange().min();
      }

      FloatRange clipRange = getSettings().getEffectiveRange();
      for (int i = 0; i < colorBuffer.length; i++, value += delta) {
         colorBuffer[i] = valueToColor.getRGB(value, clipRange);
      }
   }

   private void drawTexts(Graphics2D g, int width, int height, boolean drawInteractiveControls) {
      int textHeight = g.getFontMetrics().getHeight();
      int labelCount = height / (textHeight * 3);
      if (labelCount == 0) {
         return;
      }

      FloatRange maxRange = getSettings().getMaxRange();
      double delta = NiceNumber.niceNumber(maxRange.getSize() / labelCount, true);
      delta = Math.max(delta, getSettings().getDelta());

      String tickFormat = Utils.getPrecisionString(delta);

      double min = Math.ceil(maxRange.min() / delta) * delta;
      double max = Math.floor(maxRange.max() / delta) * delta;
      int tickCount = (int) Math.round(Math.abs(max - min) / delta) + 1;

      for (int i = 0; i < tickCount; i++) {
         double value = min + i * delta;
         int y = valueToY((float) value, height);
         String text = Utils.format(tickFormat, value);
         GuiText.draw(g, text, Color.BLACK, 2, y,
               GuiText.HorizontalAlignment.LEFT, GuiText.VerticalAlignment.CENTER, null);
      }

      if (drawInteractiveControls) {
         String thresholdFormat = Utils.getPrecisionString(getSettings().getDelta());
         FloatRange variableRange = getSettings().getRange();
         drawThresholdText(g, width, height, variableRange.min(), thresholdFormat, getVariablePerPingSettings().getLowerThresholds());
         drawThresholdText(g, width, height, variableRange.max(), thresholdFormat, getVariablePerPingSettings().getUpperThresholds());
      }
   }

   private void drawThresholdText(Graphics2D g, int width, int height, float value, String format, NavigableSet<Float> thresholds) {
      String text = Utils.format(format, value);
      GuiText.draw(g, text, thresholds.size() > 1 ? Color.RED : ColorUtils.SEASHELL,
            width - 1, valueToY(value, height),
            GuiText.HorizontalAlignment.RIGHT, GuiText.VerticalAlignment.CENTER, null);
   }

   public float getDeltaValue(int n) {
      return getSettings().getMaxRange().getSize() / (n - 1);
   }

   private int valueToY(float value, int height) {
      float delta = getDeltaValue(height);
      return Math.round((getSettings().getMaxRange().max() - value) / delta);
   }

   private void drawMark(Graphics2D g, int width, int height, float value, NavigableSet<Float> thresholds) {
      if (thresholds.size() > 1) {
         g.setColor(Color.RED);
         int y0 = valueToY(thresholds.first(), height);
         int y1 = valueToY(thresholds.last(), height);
         int x = width / 2;
         g.drawLine(x - 3, y0 - 3, x, y0);
         g.drawLine(x + 3, y0 - 3, x, y0);
         g.drawLine(x, y0 - 1, x, y1 + 1);
         g.drawLine(x - 3, y1 + 3, x, y1);
         g.drawLine(x + 3, y1 + 3, x, y1);
      } else {
         g.setColor(Color.BLACK);
      }
      int y = valueToY(value, height);
      g.drawLine(0, y, width, y);
   }

   public int getRGB(float value) {
      return valueToColor.getRGB(value, getSettings().getEffectiveRange());
   }

   @Override
   public void update() {
      valueToColor = new ValueToColor(getSettings(), colormap);
   }
}
