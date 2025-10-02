package no.imr.korona.util.schools;

import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.range.FloatRangeBuilder;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import java.util.List;

public record OrthogonalRectangularHull(
      Vec3 center,
      Vec3 extent,
      float rotation // Angle between first principal axis and ship forward direction.
) {
   public static OrthogonalRectangularHull create(List<Vec3> points) {
      if (points.size() < 2) {
         if (points.isEmpty()) {
            throw new IllegalArgumentException("Empty point list");
         }
         return new OrthogonalRectangularHull(points.getFirst(), Vec3.ZERO, 0);
      }
      double xMean = 0;
      double yMean = 0;
      for (Vec3 point : points) {
         xMean += point.x();
         yMean += point.y();
      }
      xMean /= points.size();
      yMean /= points.size();
      double xx = 0;
      double xy = 0;
      double yy = 0;
      for (Vec3 point : points) {
         double x = point.x() - xMean;
         double y = point.y() - yMean;
         xx += x * x;
         xy += x * y;
         yy += y * y;
      }
      RealMatrix a = new Array2DRowRealMatrix(2, 2);
      a.setEntry(0, 0, xx);
      a.setEntry(0, 1, xy);
      a.setEntry(1, 0, xy);
      a.setEntry(1, 1, yy);
      a = a.scalarMultiply(1.0 / (points.size() - 1));
      SingularValueDecomposition svd = new SingularValueDecomposition(a);

      double[] firstColumn = svd.getU().getColumn(0);
      double[] secondColumn = svd.getU().getColumn(1);
      Vec3 firstAxis = new Vec3((float) firstColumn[0], (float) firstColumn[1], 0);
      Vec3 secondAxis = new Vec3((float) secondColumn[0], (float) secondColumn[1], 0);

      FloatRangeBuilder firstAxisBuilder = new FloatRangeBuilder();
      FloatRangeBuilder secondAxisBuilder = new FloatRangeBuilder();
      FloatRangeBuilder depthRangeBuilder = new FloatRangeBuilder();
      for (Vec3 point : points) {
         float firstLength = point.dot(firstAxis);
         float secondLength = point.dot(secondAxis);
         firstAxisBuilder.expand(firstLength);
         secondAxisBuilder.expand(secondLength);
         depthRangeBuilder.expand(point.z());
      }
      float firstAxisCenter = firstAxisBuilder.toFloatRange().getCenter();
      float secondAxisCenter = secondAxisBuilder.toFloatRange().getCenter();
      float thirdAxisCenter = depthRangeBuilder.toFloatRange().getCenter();
      Vec3 center = firstAxis.times(firstAxisCenter).plus(secondAxis.times(secondAxisCenter)).plus(new Vec3(0, 0, 1).times(thirdAxisCenter));
      Vec3 extent = new Vec3(firstAxisBuilder.toFloatRange().getSize(), secondAxisBuilder.toFloatRange().getSize(), depthRangeBuilder.toFloatRange().getSize());
      float rotation = (float) Math.atan2(firstAxis.y(), firstAxis.x());
      return new OrthogonalRectangularHull(center, extent, rotation);
   }
}
