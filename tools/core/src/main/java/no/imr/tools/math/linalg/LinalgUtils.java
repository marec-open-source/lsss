package no.imr.tools.math.linalg;

import no.imr.tools.Utils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class LinalgUtils {
   private LinalgUtils() {
   }

   public static double angle(Vec2 vec1, Vec2 vec2) {
      return Math.acos(vec1.dot(vec2) / (vec1.length() * vec2.length()));
   }

   public static double angle(Vec3 vec1, Vec3 vec2) {
      return Math.acos(vec1.dot(vec2) / (vec1.length() * vec2.length()));
   }

   private static boolean clockWiseAngle(Vec2 vec1, Vec2 vec2) {
      return vec1.cross(vec2) > 0;
   }

   /**
    * Checks if a point lies within a triangle.
    *
    * @param point   a point
    * @param cornerA first corner of triangle
    * @param cornerB second corner of triangle
    * @param cornerC third corner of triangle
    * @return {@code true} if the point lies within the triangle
    */
   public static boolean pointInTriangle(Vec2 point, Vec2 cornerA, Vec2 cornerB, Vec2 cornerC) {
      Vec2 v1 = cornerA.minus(point);
      Vec2 v2 = cornerB.minus(point);
      Vec2 v3 = cornerC.minus(point);

      boolean dir = clockWiseAngle(v1, v2);
      return clockWiseAngle(v2, v3) == dir && clockWiseAngle(v3, v1) == dir;
   }

   /**
    * Creates an orthonormal basis orthogonal to the provided unit vector.
    *
    * @param normal a unit vector
    * @return a list of 2 unit vectors perpendicular to each other and to {@code normal}
    */
   public static List<Vec3> createOrthonormalBasis(Vec3 normal) {
      float a = Math.abs(normal.x());
      float b = Math.abs(normal.y());
      float c = Math.abs(normal.z());

      Vec3 basis1;
      if (a < b && a < c) {
         // x is smallest
         float f = Utils.hypot(b, c);
         basis1 = new Vec3(0, normal.z() / f, -normal.y() / f);
      } else if (b < a && b < c) {
         // y is smallest
         float f = Utils.hypot(a, c);
         basis1 = new Vec3(normal.z() / f, 0, -normal.x() / f);
      } else {
         // z is smallest
         float f = Utils.hypot(a, b);
         basis1 = new Vec3(normal.y() / f, -normal.x() / f, 0);
      }

      Vec3 basis2 = normal.cross(basis1);
      return List.of(basis1, basis2);
   }

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
    * Finds the direction best fitting a list of points using svd.
    *
    * @param points points
    * @return direction as a unit vector
    */
   public static OrthogonalRegressionContainer findOrthogonalRegressionDirection(List<Vec3> points) {
      Vec3 mean = findMean(points);
      int n = points.size();
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

   public static float distanceToPlane(Vec3 pos, Vec3 planePos, Vec3 planeNormal) {
      return planeNormal.dot(pos.minus(planePos));
   }

   public static @Nullable Vec3 planeIntersection(Ray ray, Vec3 planePos, Vec3 planeNormal) {
      float distance = (planePos.dot(planeNormal) - ray.origin().dot(planeNormal)) / ray.direction().dot(planeNormal);
      return distance < 0 ? null : ray.origin().plus(ray.direction().times(distance));
   }
}
