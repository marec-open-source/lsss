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
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.CategoryDataset;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;

final class PlotBarChart {
   private static double lastUsedUpperMargin = 0;

   private PlotBarChart() {
   }

   static ChartPanel plot(String title, @Nullable String xLabel, @Nullable String yLabel, CategoryDataset dataset, @Nullable Paint paint) {
      CategoryAxis categoryAxis = PlotChart.axis(new CategoryAxis(yLabel));
      ValueAxis valueAxis = PlotChart.axis(new NumberAxis(xLabel));
      valueAxis.setUpperMargin(lastUsedUpperMargin);

      DecimalFormat valueFormat = Utils.createDecimalFormat("#.###");

      BarRenderer renderer = new BarRenderer();
      renderer.setMaximumBarWidth(0.15);
      renderer.setDrawBarOutline(false);
      renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator("{2}", valueFormat));
      renderer.setDefaultItemLabelsVisible(true);
      renderer.setDefaultItemLabelFont(UiUtils.labelFont());
      renderer.setDefaultPositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.OUTSIDE3, TextAnchor.CENTER_LEFT));
      renderer.setShadowVisible(false);
      renderer.setSeriesPaint(0, paint);

      CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis, renderer) {
         private final String maxValueString = valueFormat.format(findMaxValue(dataset));
         private double lastDataWidth;

         @Override
         public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor, PlotState parentState, PlotRenderingInfo state) {
            super.draw(g2, area, anchor, parentState, state);

            double dataWidth = state.getDataArea().getWidth();
            if (lastDataWidth == dataWidth) {
               return;
            }
            lastDataWidth = dataWidth;
            double labelWidth = g2.getFontMetrics().stringWidth(maxValueString) + 5;
            lastUsedUpperMargin = dataWidth > labelWidth ? labelWidth / (dataWidth - labelWidth) : dataWidth;
            valueAxis.setUpperMargin(lastUsedUpperMargin);
         }
      };
      plot.setOrientation(PlotOrientation.HORIZONTAL);
      plot.setBackgroundPaint(Color.LIGHT_GRAY);
      plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
      plot.setRangeGridlinePaint(Color.WHITE);
      plot.setRangeGridlinesVisible(true);
      plot.setDomainGridlinePaint(Color.WHITE);
      plot.setDomainGridlinesVisible(true);

      return PlotChart.createChart(title, plot);
   }

   private static double findMaxValue(CategoryDataset dataset) {
      double maxValue = 0;
      int columnCount = dataset.getColumnCount();
      int rowCount = dataset.getRowCount();
      for (int row = 0; row < rowCount; row++) {
         for (int column = 0; column < columnCount; column++) {
            double value = dataset.getValue(row, column).doubleValue();
            maxValue = Math.max(maxValue, value);
         }
      }
      return maxValue;
   }
}
