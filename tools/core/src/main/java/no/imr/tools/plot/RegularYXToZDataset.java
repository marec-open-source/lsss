package no.imr.tools.plot;

public final class RegularYXToZDataset extends BaseXYZDataset {
   private final String name;
   private final double minX;
   private final double deltaX;
   private final double minY;
   private final double deltaY;
   private final float[][] yxToZ;
   private final XYZInfo xyzInfo;
   private final int ny;
   private final int nx;

   // item = j * nx + i;

   public RegularYXToZDataset(String name,
                              double minX, double deltaX,
                              double minY, double deltaY,
                              float[][] yxToZ,
                              XYZInfo xyzInfo) {
      this.name = name;
      this.yxToZ = yxToZ;
      ny = yxToZ.length;
      nx = ny == 0 ? 0 : yxToZ[0].length;
      this.minX = minX;
      this.deltaX = deltaX;
      this.minY = minY;
      this.deltaY = deltaY;
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
      int i = item % nx;
      return minX + (i + 0.5) * deltaX;
   }

   @Override
   public double getYValue(int series, int item) {
      int j = item / nx;
      return minY + (j + 0.5) * deltaY;
   }

   @Override
   public double getZValue(int series, int item) {
      int i = item % nx;
      int j = item / nx;
      return yxToZ[j][i];
   }

   @Override
   public XYZInfo getXYZInfo(int series) {
      return xyzInfo;
   }
}
