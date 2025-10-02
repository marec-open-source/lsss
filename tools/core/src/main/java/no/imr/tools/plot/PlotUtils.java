package no.imr.tools.plot;

import no.imr.tools.SmartNumberFormat;
import no.imr.tools.swing.UiUtils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.Pannable;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.Zoomable;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.Locale;

public final class PlotUtils {
   private PlotUtils() {
   }

   public static ChartPanel newChartPanel(@Nullable JFreeChart chart) {
      return new PlotChartPanel(chart);
   }

   public static XYPlot newXYPlot(String xLabel, String yLabel) {
      return new XYPlot(null, newNumberAxis(xLabel), newNumberAxis(yLabel), null);
   }

   public static NumberAxis newNumberAxis(@Nullable String label) {
      NumberAxis axis = new NumberAxis(label);
      axis.setNumberFormatOverride(new SmartNumberFormat());
      axis.setAutoRangeIncludesZero(false);
      axis.setAutoRangeStickyZero(false);
      return axis;
   }

   public static StandardXYToolTipGenerator newStandardXYToolTipGenerator() {
      NumberFormat numberInstance = NumberFormat.getNumberInstance(Locale.ENGLISH);
      return new StandardXYToolTipGenerator(StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT, numberInstance, numberInstance);
   }

   public static StandardXYItemRenderer newStandardXYItemRenderer(int type) {
      return new StandardXYItemRenderer(type, newStandardXYToolTipGenerator());
   }

   public static JFreeChart newChart(@Nullable String title, Plot plot, boolean showLegends) {
      JFreeChart chart = new JFreeChart(title, null, plot, showLegends);
      chart.setBackgroundPaint(Color.WHITE);
      return chart;
   }

   public static JFreeChart newEmptyChart() {
      return newChart(null, new XYPlot(), false);
   }

   public static TextTitle newTextTitle(String text, HorizontalAlignment horizontalAlignment) {
      TextTitle textTitle = new TextTitle(text);
      textTitle.setHorizontalAlignment(horizontalAlignment);
      textTitle.setFont(UiUtils.labelFont());
      textTitle.setPaint(UiUtils.labelForeground());
      return textTitle;
   }

   public static int nextDatasetIndex(XYPlot plot) {
      int i = plot.getDatasetCount();
      while (i > 0 && plot.getDataset(i - 1) == null) {
         i--;
      }
      return i;
   }

   public static Rectangle2D screenToData(ChartPanel chartPanel, Rectangle2D screenRectangle) {
      return new PlotCoordinateConverter(chartPanel).screenToData(screenRectangle);
   }

   public static void enableMouseWheelZooming(ChartPanel chartPanel) {
      chartPanel.addMouseWheelListener(e -> {
         mouseWheelZoom(chartPanel, e);
      });
   }

   public static void mouseWheelZoom(ChartPanel chartPanel, MouseWheelEvent e) {
      JFreeChart chart = chartPanel.getChart();
      if (chart == null) {
         return;
      }
      PlotRenderingInfo info = chartPanel.getChartRenderingInfo().getPlotInfo();
      Point2D p = chartPanel.translateScreenToJava2D(e.getPoint());
      if (!info.getDataArea().contains(p)) {
         return;
      }
      Plot plot = chart.getPlot();
      boolean notifyState = plot.isNotify();
      plot.setNotify(false);
      double zoomFactor = Math.pow(1.1, -e.getWheelRotation());
      if (chartPanel.isDomainZoomable()) {
         if (plot instanceof Zoomable zoomable && (e.getModifiersEx() == 0 || e.getModifiersEx() == MouseEvent.CTRL_DOWN_MASK)) {
            zoomable.zoomDomainAxes(zoomFactor, info, p, true);
         }
         if (plot instanceof Pannable pannable && e.getModifiersEx() == (MouseEvent.CTRL_DOWN_MASK | MouseEvent.ALT_DOWN_MASK)) {
            pannable.panDomainAxes(e.getWheelRotation() / 20.0, info, p);
         }
      }
      if (chartPanel.isRangeZoomable()) {
         if (plot instanceof Zoomable zoomable && (e.getModifiersEx() == 0 || e.getModifiersEx() == MouseEvent.SHIFT_DOWN_MASK)) {
            zoomable.zoomRangeAxes(zoomFactor, info, p, true);
         }
         if (plot instanceof Pannable pannable && e.getModifiersEx() == (MouseEvent.SHIFT_DOWN_MASK | MouseEvent.ALT_DOWN_MASK)) {
            pannable.panRangeAxes(-e.getWheelRotation() / 20.0, info, p);
         }
      }
      plot.setNotify(notifyState);
   }

   public static void preserveDomainAxisRange(JFreeChart oldChart, JFreeChart newChart) {
      XYPlot oldPlot = oldChart.getXYPlot();
      XYPlot newPlot = newChart.getXYPlot();
      if (oldPlot == null || newPlot == null) {
         return;
      }
      ValueAxis oldDomainAxis = oldPlot.getDomainAxis();
      ValueAxis newDomainAxis = newPlot.getDomainAxis();
      if (oldDomainAxis != null && newDomainAxis != null) {
         newDomainAxis.setRange(oldDomainAxis.getRange());
      }
   }

   public static void preserveRangeAxisRange(JFreeChart oldChart, JFreeChart newChart) {
      XYPlot oldPlot = oldChart.getXYPlot();
      XYPlot newPlot = newChart.getXYPlot();
      if (oldPlot == null || newPlot == null) {
         return;
      }
      ValueAxis oldRangeAxis = oldPlot.getRangeAxis();
      ValueAxis newRangeAxis = newPlot.getRangeAxis();
      if (oldRangeAxis != null && newRangeAxis != null) {
         newRangeAxis.setRange(oldRangeAxis.getRange());
      }
   }

   public static void preserveAxisRanges(JFreeChart oldChart, JFreeChart newChart) {
      preserveDomainAxisRange(oldChart, newChart);
      preserveRangeAxisRange(oldChart, newChart);
   }
}
