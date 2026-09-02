package no.imr.tools.math.linalg;

import no.imr.tools.math.MathUtils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class LinalgUtils {
   private LinalgUtils() {
   }

   /// {@return the angle between the vectors, or NaN if any of them has a length of 0}
   public static double angle(Vec2 vec1, Vec2 vec2) {
      return MathUtils.acosClamped(vec1.dot(vec2) / (vec1.length() * vec2.length()));
   }

   /// {@return the angle between the vectors, or NaN if any of them has a length of 0}
   public static double angle(Vec3 vec1, Vec3 vec2) {
      return MathUtils.acosClamped(vec1.dot(vec2) / (vec1.length() * vec2.length()));
   }

   /// {@return a vector with componentwise mean, or NaN if no values}
   public static Vec3 findMean(List<Vec3> points) {
      double xSum = 0;
      double ySum = 0;
      double zSum = 0;
      for (Vec3 p : points) {
         xSum += p.x();
         ySum += p.y();
         zSum += p.z();
      }
      int n = points.size();
      return new Vec3((float) xSum / n, (float) ySum / n, (float) zSum / n);
   }

   /**
    * Finds the direction best fitting a list of points using SVD.
    *
    * @param points points
    * @return direction as a unit vector
    */
   public static OrthogonalRegressionContainer findOrthogonalRegressionDirection(List<Vec3> points) {
      Vec3 mean = findMean(points);
      int n = points.size();
      if (n < 2) {
         throw new IllegalArgumentException("Too few points: " + n);
      }
      Array2DRowRealMatrix m = new Array2DRowRealMatrix(3, n);
      for (int i = 0; i < n; i++) {
         Vec3 p = points.get(i);
         m.setEntry(0, i, p.x() - mean.x());
         m.setEntry(1, i, p.y() - mean.y());
         m.setEntry(2, i, p.z() - mean.z());
      }
      SingularValueDecomposition svd = new SingularValueDecomposition(m);
      RealMatrix u = svd.getU();
      float x = (float) u.getEntry(0, 0);
      float y = (float) u.getEntry(1, 0);
      float z = (float) u.getEntry(2, 0);
      double[] singularValues = svd.getSingularValues();
      float ratio = singularValues[1] > 0 ? (float) (singularValues[0] / singularValues[1]) : Float.MAX_VALUE;
      return new OrthogonalRegressionContainer(new Vec3(x, y, z), ratio);
   }

   public static float distanceToLine(Vec3 pos, Ray ray) {
      return ray.direction().cross(ray.origin().minus(pos)).length() / ray.direction().length();
   }

   public static float signedDistanceToPlane(Vec3 pos, Vec3 planePos, Vec3 planeNormal) {
      return planeNormal.dot(pos.minus(planePos));
   }

   public static @Nullable Vec3 planeIntersection(Ray ray, Vec3 planePos, Vec3 planeNormal) {
      float rayDotNormal = ray.direction().dot(planeNormal);
      if (rayDotNormal == 0) {
         return null; // The ray is parallel to the plane.
      }
      float distance = (planePos.dot(planeNormal) - ray.origin().dot(planeNormal)) / rayDotNormal;
      return distance < 0 ? null : ray.origin().plus(ray.direction().times(distance));
   }
}
