package no.imr.tools.plot;

import no.imr.tools.math.Histogram2D;

/**
 * An XYZ dataset using a {@link Histogram2D}.
 */
public final class Histogram2DDataset extends BaseXYZDataset {
   private final String name;
   private final int nx;
   private final int ny;
   private final float minX;
   private final float deltaX;
   private final float minY;
   private final float deltaY;
   private final int[][] counts;
   private final XYZInfo xyzInfo;

   // item = i * ny + j;

   public Histogram2DDataset(String name, Histogram2D histogram2D, XYZInfo xyzInfo) {
      this.name = name;
      counts = histogram2D.getCounts();
      nx = counts.length;
      ny = nx == 0 ? 0 : counts[0].length;
      minX = histogram2D.getMinX();
      deltaX = histogram2D.getDeltaX();
      minY = histogram2D.getMinY();
      deltaY = histogram2D.getDeltaY();
      this.xyzInfo = xyzInfo;
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
      return nx * ny;
   }

   @Override
   public double getXValue(int series, int item) {
      int i = item / ny;
      return minX + (i + 0.5) * deltaX;
   }

   @Override
   public double getYValue(int series, int item) {
      int j = item % ny;
      return minY + (j + 0.5) * deltaY;
   }

   @Override
   public double getZValue(int series, int item) {
      int i = item / ny;
      int j = item % ny;
      return counts[i][j];
   }

   @Override
   public XYZInfo getXYZInfo(int series) {
      return xyzInfo;
   }
}
