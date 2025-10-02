package no.imr.tools.plot;

public final class ArrayDataset extends BaseXYDataset {
   private final String name;
   private final float[] y;
   private final float minX;
   private final float deltaX;

   public ArrayDataset(String name, float[] y, float minX, float deltaX) {
      this.name = name;
      this.y = y;
      this.minX = minX;
      this.deltaX = deltaX;
   }

   @Override
   public int getSeriesCount() {
      return 1;
   }

   @Override
   public Comparable<?> getSeriesKey(int series) {
      return name;
   }

   @Override
   public int getItemCount(int series) {
      return y.length;
   }

   @Override
   public double getXValue(int series, int item) {
      return minX + item * deltaX;
   }

   @Override
   public double getYValue(int series, int item) {
      return y[item];
   }
}
