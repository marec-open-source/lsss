package no.imr.tools.plot;

/**
 * A simple histogram dataset represented by arrays.
 */
public final class HistogramDataset extends BaseIntervalXYDataset {
   private final String name;
   private final float[] dividers;
   private final float[] values;
   private final XYInfo xyInfo;

   public HistogramDataset(String name, float[] dividers, float[] values, XYInfo xyInfo) {
      if (!(dividers.length == values.length + 1 || dividers.length == 0 && values.length == 0)) {
         throw new IllegalArgumentException("Incompatible array lengths");
      }
      this.name = name;
      this.dividers = dividers;
      this.values = values;
      this.xyInfo = xyInfo;
   }

   @Override
   public int getSeriesCount() {
      return 1;
   }

   @Override
   public Comparable<String> getSeriesKey(int series) {
      return name;
   }

   @Override
   public int getItemCount(int series) {
      return values.length;
   }

   @Override
   public double getXValue(int series, int item) {
      return (dividers[item] + dividers[item + 1]) / 2;
   }

   @Override
   public double getYValue(int series, int item) {
      return values[item];
   }

   @Override
   public double getStartXValue(int series, int item) {
      return dividers[item];
   }

   @Override
   public double getEndXValue(int series, int item) {
      return dividers[item + 1];
   }

   @Override
   public double getStartYValue(int series, int item) {
      return 0;
   }

   @Override
   public double getEndYValue(int series, int item) {
      return values[item];
   }

   @Override
   public XYInfo getXYInfo(int series) {
      return xyInfo;
   }
}
