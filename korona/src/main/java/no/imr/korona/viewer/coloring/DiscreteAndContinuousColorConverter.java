package no.imr.korona.viewer.coloring;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.viewer.ResampleMode;
import no.imr.korona.viewer.Resampler;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.korona.viewer.variables.DiscreteVariableResult;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiText;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Arrays;

public abstract class DiscreteAndContinuousColorConverter extends DiscreteColorConverter {
   private final ContinuousVariable continuousVariable;

   DiscreteAndContinuousColorConverter(DiscreteVariable discreteVariable, ContinuousVariable continuousVariable) {
      super(discreteVariable);

      this.continuousVariable = continuousVariable;
   }

   @Override
   public ContinuousVariable getContinuousVariable() {
      return continuousVariable;
   }

   @Override
   public void convertToColor(Ping ping, int channel, int[] rgbs, FloatRange depthRange) {
      DiscreteVariableResult discreteVariableResult = getDiscreteVariable().evaluate(ping);
      byte[] byteData = discreteVariableResult.byteData();

      ContinuousVariableResult continuousVariableResult = continuousVariable.evaluate(channel, ping);
      float[] floatData = continuousVariableResult.floatData();

      if (byteData.length == 0 || floatData.length == 0) {
         Arrays.fill(rgbs, ValueColor.NO_DATA_RGB);
         return;
      }

      byte[] categories = new byte[rgbs.length];
      Resampler.sampleByteData(byteData, discreteVariableResult.depthRange(), categories, depthRange, ResampleMode.NEAREST);

      float[] values = new float[rgbs.length];
      Resampler.sampleFloatData(floatData, continuousVariableResult.depthRange(), values, depthRange, ResampleMode.AVERAGE);

      getRGBs(rgbs, categories, values);
   }

   abstract void getRGBs(int[] rgbs, byte[] categories, float[] values);

   void drawText(Graphics2D g, int height) {
      GuiText.draw(g, continuousVariable.getDisplayName(), Color.BLACK, 1, height, GuiText.HorizontalAlignment.LEFT, GuiText.VerticalAlignment.BOTTOM, null);
   }

   void drawMarks(Graphics2D g, int width, int height) {
      FloatRange maxRange = continuousVariable.getSettings().getMaxRange();
      FloatRange range = continuousVariable.getSettings().getRange();

      g.setColor(Color.BLACK);
      drawMark(g, width, height, maxRange.valueToFraction(range.min()));
      drawMark(g, width, height, maxRange.valueToFraction(range.max()));
   }

   private static void drawMark(Graphics2D g, int width, int height, float fraction) {
      int y = (int) ((1 - fraction) * height);
      g.drawLine(0, y, width, y);
   }
}
