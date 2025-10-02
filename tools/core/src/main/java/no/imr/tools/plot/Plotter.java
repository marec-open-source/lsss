package no.imr.tools.plot;

import no.imr.tools.range.FloatRange;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.Range;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * For creating xy-plots.
 */
public final class Plotter {
   private @Nullable String title;
   private @Nullable String xLabel;
   private @Nullable String yLabel;
   private final List<Graph> graphs;
   private boolean showLegends;
   private @Nullable Supplier<NumberAxis> xAxisSupplier;
   private @Nullable Supplier<NumberAxis> yAxisSupplier;
   private @Nullable Range xRange;
   private @Nullable Range yRange;

   public Plotter(List<Graph> graphs) {
      this.graphs = graphs;
   }

   public Plotter title(String title) {
      this.title = title;
      return this;
   }

   public Plotter showLegends() {
      showLegends = true;
      return this;
   }

   public Plotter xAxis(String xLabel) {
      this.xLabel = xLabel;
      return this;
   }

   public Plotter xAxis(Supplier<NumberAxis> xAxis) {
      xAxisSupplier = xAxis;
      return this;
   }

   public Plotter yAxis(String yLabel) {
      this.yLabel = yLabel;
      return this;
   }

   public Plotter yAxis(Supplier<NumberAxis> yAxis) {
      yAxisSupplier = yAxis;
      return this;
   }

   public Plotter xRange(double xMin, double xMax) {
      return xRange(new Range(xMin, xMax));
   }

   public Plotter xRange(@Nullable FloatRange xRange) {
      return xRange(xRange != null ? new Range(xRange.min(), xRange.max()) : null);
   }

   public Plotter xRange(@Nullable Range xRange) {
      this.xRange = xRange;
      return this;
   }

   public Plotter yRange(double yMin, double yMax) {
      return yRange(new Range(yMin, yMax));
   }

   public Plotter yRange(@Nullable FloatRange yRange) {
      return yRange(yRange != null ? new Range(yRange.min(), yRange.max()) : null);
   }

   public Plotter yRange(@Nullable Range yRange) {
      this.yRange = yRange;
      return this;
   }

   public JFreeChart createChart() {
      NumberAxis xAxis = xAxisSupplier != null ? xAxisSupplier.get() : PlotUtils.newNumberAxis(xLabel);
      NumberAxis yAxis = yAxisSupplier != null ? yAxisSupplier.get() : PlotUtils.newNumberAxis(yLabel);

      setRangeIfNonEmpty(xAxis, xRange);
      setRangeIfNonEmpty(yAxis, yRange);

      XYPlot plot = new XYPlot(null, xAxis, yAxis, null);
      plot.setDrawingSupplier(new DefaultDrawingSupplier());
      plot.setDomainPannable(true);
      plot.setRangePannable(true);
      update(plot);

      return PlotUtils.newChart(title, plot, showLegends);
   }

   private static void setRangeIfNonEmpty(NumberAxis axis, @Nullable Range range) {
      if (range != null && range.getLength() > 0) {
         axis.setRange(range);
      }
   }

   private void update(XYPlot plot) {
      // Temporarily turn off auto range to avoid calculating auto ranges for every call to setDataset / setRenderer
      boolean domainAxisAutoRange = plot.getDomainAxis().isAutoRange();
      boolean rangeAxisAutoRange = plot.getRangeAxis().isAutoRange();
      plot.getDomainAxis().setAutoRange(false);
      plot.getRangeAxis().setAutoRange(false);

      for (int i = 0; i < graphs.size(); i++) {
         Graph graph = graphs.get(i);
         plot.setDataset(i, new PointListDataset(graph));

         XYItemRenderer renderer = graph.getRenderer().get();
         renderer.setSeriesPaint(0, graph.getColor());
         renderer.setSeriesStroke(0, graph.getStroke());
         plot.setRenderer(i, renderer);
      }

      List<Integer> toBeRemoved = plot.getDatasets().entrySet().stream()
            .filter(e -> e.getKey() >= graphs.size() && e.getValue() != null)
            .map(Map.Entry::getKey)
            .toList();
      for (int i : toBeRemoved) {
         plot.setDataset(i, null);
      }

      plot.getDomainAxis().setAutoRange(domainAxisAutoRange);
      plot.getRangeAxis().setAutoRange(rangeAxisAutoRange);
   }

   public ChartPanel createChartPanel() {
      return PlotUtils.newChartPanel(createChart());
   }

   public void update(ChartPanel chartPanel) {
      JFreeChart chart = chartPanel.getChart();
      if (chart == null) {
         chartPanel.setChart(createChart());
      } else {
         update(chart.getXYPlot());
      }
   }
}
