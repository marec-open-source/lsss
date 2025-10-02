package no.imr.lsss.modules.trawl;

import no.imr.tools.Utils;
import no.imr.tools.swing.UiUtils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.CategoryDataset;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Paint;
import java.text.DecimalFormat;

final class PlotStatisticalBarChart {
   private PlotStatisticalBarChart() {
   }

   static ChartPanel plot(String title, @Nullable String xLabel, @Nullable String yLabel, CategoryDataset dataset, Paint paint) {
      CategoryAxis categoryAxis = PlotChart.axis(new CategoryAxis(yLabel));
      ValueAxis valueAxis = PlotChart.axis(new NumberAxis(xLabel));

      DecimalFormat valueFormat = Utils.createDecimalFormat("#.###");

      StatisticalBarRenderer renderer = new StatisticalBarRenderer();
      renderer.setMaximumBarWidth(0.15);
      renderer.setDrawBarOutline(false);
      renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator("{2}", valueFormat));
      renderer.setDefaultItemLabelsVisible(true);
      renderer.setDefaultItemLabelFont(UiUtils.labelFont());
      renderer.setDefaultPositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.CENTER));
      renderer.setShadowVisible(false);
      renderer.setErrorIndicatorPaint(Color.RED);
      renderer.setSeriesPaint(0, paint);

      CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis, renderer);
      plot.setOrientation(PlotOrientation.HORIZONTAL);
      plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
      plot.setBackgroundPaint(Color.LIGHT_GRAY);
      plot.setRangeGridlinePaint(Color.WHITE);
      plot.setRangeGridlinesVisible(true);
      plot.setDomainGridlinePaint(Color.WHITE);
      plot.setDomainGridlinesVisible(true);

      return PlotChart.createChart(title, plot);
   }
}
