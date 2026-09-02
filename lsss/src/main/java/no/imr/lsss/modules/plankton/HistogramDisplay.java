package no.imr.lsss.modules.plankton;

import no.imr.korona.computation.plankton.PlanktonRectangle;
import no.imr.korona.data.datagrams.Pic0Datagram;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.plot.HistogramDataset;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.swing.ViewHolder;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.Range;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;

final class HistogramDisplay {
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private final Pic0Datagram.PlanktonCategory planktonCategory;
   private final JFreeChart chart;
   private final XYPlot plot;
   private double minX = Double.POSITIVE_INFINITY;
   private double maxX = Double.NEGATIVE_INFINITY;

   HistogramDisplay(Pic0Datagram.PlanktonCategory planktonCategory) {
      this.planktonCategory = planktonCategory;

      NumberAxis xAxis = PlotUtils.newNumberAxis("Size [mm]");
      xAxis.setAutoRangeIncludesZero(false);
      ValueAxis yAxis = PlotUtils.newNumberAxis("Abundance [1 / m³]");
      XYBarRenderer renderer = new XYBarRenderer();
      renderer.setDefaultToolTipGenerator(PlotUtils.newXYToolTipGenerator());
      renderer.setShadowVisible(false);
      plot = new XYPlot(null, xAxis, yAxis, renderer) {
         @Override
         public Range getDataRange(ValueAxis axis) {
            if (axis == getDomainAxis()) {
               return minX > maxX ? NumberAxis.DEFAULT_RANGE : new Range(minX * 1e3, maxX * 1e3);
            } else {
               return super.getDataRange(axis);
            }
         }
      };
      plot.setDomainPannable(true);
      plot.setRangePannable(true);

      chart = new JFreeChart(this.planktonCategory.getName(), JFreeChart.DEFAULT_TITLE_FONT, plot, false);
      chart.setBackgroundPaint(Color.WHITE);
   }

   @Override
   public String toString() {
      return planktonCategory + " (" + minX + ", " + maxX + ")";
   }

   Pic0Datagram.PlanktonCategory getPlanktonCategory() {
      return planktonCategory;
   }

   XYPlot getPlot() {
      return plot;
   }

   JComponent getComponent() {
      return viewHolder.getComponent();
   }

   void setHistogram(Histogram histogram) {
      float[] dividers = histogram.getDividers();
      if (dividers.length > 0) {
         updateRange(dividers[0], dividers[dividers.length - 1]);
      }
      XYInfo xyInfo = new XYInfo(
            new ParameterExport("size", Unit.MILLIMETER, ExportTransform.round(1000)),
            new ParameterExport("abundance", new Unit("1 / m³", "1 / m^3"), ExportTransform.round(1000)));
      float[] dividerMM = dividers.clone();
      ArrayMath.multiply(dividerMM, 1e3f);
      HistogramDataset dataset = new HistogramDataset(planktonCategory.getName(), dividerMM, histogram.getValues(), xyInfo);
      SwingUtilities.invokeLater(() -> plot.setDataset(dataset));
   }

   void updateRange(List<PlanktonRectangle> planktonRectangles) {
      for (PlanktonRectangle planktonRectangle : planktonRectangles) {
         double[] dividers = planktonRectangle.getSizeHistogram().getDividers();
         if (dividers.length > 0) {
            updateRange(dividers[0], dividers[dividers.length - 1]);
         }
      }
   }

   private void updateRange(double x1, double x2) {
      minX = Math.min(minX, x1);
      maxX = Math.max(maxX, x2);
   }

   float getFixedBinSize() {
      return maxX >= 1e-2 ? 1e-3f : 1e-4f;
   }

   private static final class View implements ViewHolder.View {
      private final JPanel panel = new JPanel(new BorderLayout());

      private View(HistogramDisplay histogramDisplay) {
         panel.add(PlotUtils.newChartPanel(histogramDisplay.chart));
         panel.setBackground(Color.WHITE);
      }

      @Override
      public JComponent getComponent() {
         return panel;
      }
   }
}
