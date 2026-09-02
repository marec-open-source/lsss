package no.imr.tools.plot;

import no.imr.tools.math.Histogram1D;
import org.jspecify.annotations.Nullable;

/**
 * A histogram dataset using a {@link Histogram1D}.
 */
public final class Histogram1DDataset extends BaseIntervalXYDataset {
   private final String name;
   private final double min;
   private final double delta;
   private final int[] counts;
   private int @Nullable [] startCounts;

   public Histogram1DDataset(String name, Histogram1D histogram1D) {
      this.name = name;
      min = histogram1D.getMin();
      delta = histogram1D.getDelta();
      counts = histogram1D.getCounts();
   }

   public void setStartCounts(int[] startCounts) {
      if (startCounts.length != counts.length) {
         throw new IllegalArgumentException(startCounts.length + " != " + counts.length);
      }
      this.startCounts = startCounts;
      fireDatasetChanged();
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
      return counts.length;
   }

   @Override
   public double getXValue(int series, int item) {
      return min + delta * (item + 0.5);
   }

   @Override
   public double getYValue(int series, int item) {
      return counts[item];
   }

   @Override
   public double getStartXValue(int series, int item) {
      return min + delta * item;
   }

   @Override
   public double getEndXValue(int series, int item) {
      return min + delta * (item + 1);
   }

   @Override
   public double getStartYValue(int series, int item) {
      int[] startCounts = this.startCounts;
      return startCounts != null ? startCounts[item] : 0;
   }

   @Override
   public double getEndYValue(int series, int item) {
      return counts[item];
   }
}
