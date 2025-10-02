package no.imr.lsss.modules.trawl;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.IntervalXYDataset;

import java.awt.Color;

final class PlotHistogramChart {
   private PlotHistogramChart() {
   }

   static ChartPanel plot(String title, IntervalXYDataset dataset) {
      NumberAxis xAxis = PlotChart.axis(new NumberAxis());
      xAxis.setAutoRangeIncludesZero(false);
      ValueAxis yAxis = PlotChart.axis(new NumberAxis());
      yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

      XYBarRenderer renderer = new XYBarRenderer();
      renderer.setDefaultToolTipGenerator(new StandardXYToolTipGenerator());
      renderer.setShadowVisible(false);

      XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
      plot.setBackgroundPaint(Color.LIGHT_GRAY);
      plot.setRangeGridlinePaint(Color.WHITE);
      plot.setDomainGridlinePaint(Color.WHITE);

      return PlotChart.createChart(title, plot);
   }
}
