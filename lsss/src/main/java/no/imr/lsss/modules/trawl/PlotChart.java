package no.imr.lsss.modules.trawl;

import no.imr.tools.Utils;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.swing.UiUtils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.ui.HorizontalAlignment;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;

final class PlotChart {
   static final GradientPaint YELLOW = new GradientPaint(0, 0, Color.YELLOW, 0, 0, Color.LIGHT_GRAY);
   static final GradientPaint GREEN = new GradientPaint(0, 0, Color.GREEN, 0, 0, new Color(0x004000));

   private PlotChart() {
   }

   static ChartPanel createChart(String title, Plot plot) {
      JFreeChart chart = new JFreeChart(plot);
      chart.setBackgroundPaint(Color.WHITE);
      chart.addSubtitle(PlotUtils.newTextTitle(title, HorizontalAlignment.RIGHT));
      return PlotUtils.newChartPanel(chart);
   }

   static <T extends Axis> T axis(T axis) {
      axis.setLabelFont(UiUtils.labelFont().deriveFont(Font.PLAIN, 14));
      axis.setTickLabelFont(UiUtils.labelFont().deriveFont(Font.PLAIN, 12));
      if (axis instanceof NumberAxis numberAxis) {
         numberAxis.setNumberFormatOverride(Utils.createDecimalFormat("#.###"));
      }
      return axis;
   }
}
