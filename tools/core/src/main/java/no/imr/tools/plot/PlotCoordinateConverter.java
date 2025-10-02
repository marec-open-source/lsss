package no.imr.tools.plot;

import org.jfree.chart.ChartPanel;
import org.jfree.data.Range;

import java.awt.geom.Rectangle2D;

public final class PlotCoordinateConverter {
   private final double xDataMin;
   private final double yDataMax;
   private final double xScreenMin;
   private final double yScreenMin;
   private final double xScreenToDataRatio;
   private final double yScreenToDataRatio;

   public PlotCoordinateConverter(ChartPanel chartPanel) {
      Rectangle2D screenDataArea = chartPanel.getScreenDataArea();
      Range xRange = chartPanel.getChart().getXYPlot().getDomainAxis().getRange();
      Range yRange = chartPanel.getChart().getXYPlot().getRangeAxis().getRange();
      xDataMin = xRange.getLowerBound();
      yDataMax = yRange.getUpperBound();
      xScreenMin = screenDataArea.getMinX();
      yScreenMin = screenDataArea.getMinY();
      xScreenToDataRatio = screenDataArea.getWidth() / xRange.getLength();
      yScreenToDataRatio = screenDataArea.getHeight() / yRange.getLength();
   }

   public double xDataToScreen(double xData) {
      return xScreenMin + (xData - xDataMin) * xScreenToDataRatio;
   }

   public double yDataToScreen(double yData) {
      return yScreenMin + (yDataMax - yData) * yScreenToDataRatio;
   }

   public double xScreenToData(double xScreen) {
      return xDataMin + (xScreen - xScreenMin) / xScreenToDataRatio;
   }

   public double yScreenToData(double yScreen) {
      return yDataMax - (yScreen - yScreenMin) / yScreenToDataRatio;
   }

   public Rectangle2D screenToData(Rectangle2D screenRectangle) {
      Rectangle2D.Double dataRectangle = new Rectangle2D.Double();
      dataRectangle.setFrameFromDiagonal(
            xScreenToData(screenRectangle.getMinX()), yScreenToData(screenRectangle.getMinY()),
            xScreenToData(screenRectangle.getMaxX()), yScreenToData(screenRectangle.getMaxY()));
      return dataRectangle;
   }
}
