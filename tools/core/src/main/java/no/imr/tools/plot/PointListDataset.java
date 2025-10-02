package no.imr.tools.plot;

import no.imr.tools.math.linalg.Vec2;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Implementation of an XYDataset using a list of points.
 */
public final class PointListDataset extends BaseXYDataset {
   private final String name;
   private final List<Vec2> points;
   private final @Nullable XYInfo xyInfo;

   public PointListDataset(String name, List<Vec2> points, @Nullable XYInfo xyInfo) {
      this.name = name;
      this.points = points;
      this.xyInfo = xyInfo;
   }

   public PointListDataset(Graph graph) {
      this(graph.getName(), graph.getPoints(), graph.getXYInfo());
   }

   @Override
   public double getXValue(int series, int item) {
      return points.get(item).x();
   }

   @Override
   public double getYValue(int series, int item) {
      return points.get(item).y();
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
      return points.size();
   }

   @Override
   public @Nullable XYInfo getXYInfo(int series) {
      return xyInfo;
   }
}
