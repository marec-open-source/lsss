package no.imr.lsss.modules.echogramplot;

import no.imr.tools.plot.BaseXYDataset;
import no.imr.tools.plot.XYInfo;

final class EchogramPlotDataset extends BaseXYDataset {
   private final String name;
   private final double[] x;
   private final float[] y;
   private final XYInfo xyInfo;

   EchogramPlotDataset(String name, double[] x, float[] y, XYInfo xyInfo) {
      this.name = name;
      this.x = x;
      this.y = y;
      this.xyInfo = xyInfo;
   }

   float[] getYValues() {
      return y;
   }

   @Override
   public double getXValue(int series, int item) {
      return x[item];
   }

   @Override
   public double getYValue(int series, int item) {
      return y[item];
   }

   @Override
   public int getSeriesCount() {
      return 1;
   }

   @Override
   public String getSeriesKey(int series) {
      return name;
   }

   @Override
   public int getItemCount(int series) {
      return y.length;
   }

   @Override
   public XYInfo getXYInfo(int series) {
      return xyInfo;
   }

   boolean containsIsolatedValues() {
      int consecutiveValues = 0;
      for (float value : y) {
         if (Float.isNaN(value)) {
            if (consecutiveValues == 1) {
               return true;
            }
            consecutiveValues = 0;
         } else {
            consecutiveValues++;
         }
      }
      return consecutiveValues == 1;
   }
}
