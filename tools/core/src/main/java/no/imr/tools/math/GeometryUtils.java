package no.imr.tools.math;

import no.imr.tools.math.linalg.Vec2;
import no.imr.tools.math.linalg.Vec3;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

public final class GeometryUtils {
   private GeometryUtils() {
   }

   public static float boxVolume(Vec3 box) {
      return box.x() * box.y() * box.z();
   }

   public static float boxSurface(Vec3 box) {
      return 2 * (box.x() * box.y() + box.y() * box.z() + box.z() * box.x());
   }

   public static double areaToCirclePerimeter(double area) {
      return 2 * Math.sqrt(Math.PI * area);
   }

   public static double getCircleCompactness(double area, double perimeter) {
      double circlePerimeter = areaToCirclePerimeter(area);
      return circlePerimeter / perimeter;
   }

   public static double volumeToSphereSurface(double volume) {
      return (4 * Math.PI) * Math.pow(volume * (3 / (4 * Math.PI)), 2.0 / 3.0);
   }

   public static double getSphereCompactness(double volume, double surface) {
      double sphereSurface = volumeToSphereSurface(volume);
      return sphereSurface / surface;
   }

   /**
    * Converts polar coordinates to cartesian coordinates.
    *
    * @param r     length
    * @param theta angle wrt to x-axis (rad)
    * @return the cartesian coordinates
    */
   public static Vec2 polarToCartesian(double r, double theta) {
      double x = r * Math.cos(theta);
      double y = r * Math.sin(theta);
      return new Vec2((float) x, (float) y);
   }

//   /**
//    * Converts from spherical to cartesian coordinates
//    * @param sphericalCoords vector on form [R, theta (horizontal angle), phi (vertical angle)]
//    * @return the cartesian coordinate
//    */
//   public static Vec3 sphericalToCartesian(Vec3 sphericalCoords) {
//      return sphericalToCartesian(sphericalCoords.x(), sphericalCoords.y(), sphericalCoords.z());
//   }

   /**
    * Converts from spherical to cartesian coordinates.
    *
    * @param r     length
    * @param theta horizontal angle wrt to x-axis (rad)
    * @param phi   vertical angle wrt to z-axis (rad)
    * @return the cartesian coordinate
    */
   public static Vec3 sphericalToCartesian(double r, double theta, double phi) {
      double cosTheta = Math.cos(theta);
      double sinTheta = Math.sin(theta);
      double cosPhi = Math.cos(phi);
      double sinPhi = Math.sin(phi);

      return sphericalTrigToCartesian(r, cosTheta, sinTheta, cosPhi, sinPhi);
   }

   /**
    * Converts from spherical radius and trigonometric value pairs to cartesian coordinates.
    *
    * @param r     length
    * @param theta cosine and sine pair of horizontal angle wrt to x-axis
    * @param phi   cosine and sine pair of vertical angle wrt to z-axis
    * @return the cartesian coordinate
    */
   public static Vec3 sphericalTrigToCartesian(double r, TrigAngle theta, TrigAngle phi) {
      return sphericalTrigToCartesian(r, theta.cos, theta.sin, phi.cos, phi.sin);
   }

   /**
    * Converts from spherical radius and trigonometric values to cartesian coordinates.
    *
    * @param r        length
    * @param cosTheta cosine of horizontal angle wrt to x-axis
    * @param sinTheta sine of horizontal angle wrt to x-axis
    * @param cosPhi   cosine of vertical angle wrt to z-axis
    * @param sinPhi   sine of vertical angle wrt to z-axis
    * @return the cartesian coordinate
    */
   public static Vec3 sphericalTrigToCartesian(double r, double cosTheta, double sinTheta, double cosPhi, double sinPhi) {
      double x = r * cosTheta * sinPhi;
      double y = r * sinTheta * sinPhi;
      double z = r * cosPhi;

      return new Vec3((float) x, (float) y, (float) z);
   }

   /**
    * Converts a cartesian coordinate to spherical coordinates.
    *
    * @param cartesianCoord [x, y, z]
    * @return a spherical coordinate [r, theta, phi] where theta is horizontal angle wrt x-axis, phi vertical angle wrt z-axis
    */
   public static Vec3 cartesianToSpherical(Vec3 cartesianCoord) {
      float r = cartesianCoord.length();
      double theta = Math.atan2(cartesianCoord.y(), cartesianCoord.x());
      double phi = Math.acos(cartesianCoord.z() / r);

      return new Vec3(r, (float) theta, (float) phi);
   }

   /**
    * Calculate the point of intersection between two lines.
    *
    * @param firstLine  a line
    * @param secondLine another line
    * @return the point of intersection, null if parallel lines
    */
   public static @Nullable Point2D getIntersection(Line2D firstLine, Line2D secondLine) {
      //Alternative implementation
      Point2D p1 = firstLine.getP1();
      Point2D p2 = firstLine.getP2();

      Point2D p3 = secondLine.getP1();
      Point2D p4 = secondLine.getP2();

      double fraction = (p4.getX() - p3.getX()) * (p1.getY() - p3.getY()) - (p4.getY() - p3.getY()) * (p1.getX() - p3.getX());
      double denominator = (p4.getY() - p3.getY()) * (p2.getX() - p1.getX()) - (p4.getX() - p3.getX()) * (p2.getY() - p1.getY());

      if (denominator != 0) {
         double v = fraction / denominator;
         double x = p1.getX() + v * (p2.getX() - p1.getX());
         double y = p1.getY() + v * (p2.getY() - p1.getY());
         return new Point2D.Double(x, y);
      } else {
         return null;
      }
   }

   public record TrigAngle(
         double cos,
         double sin
   ) {
      public TrigAngle(double angle) {
         this(Math.cos(angle), Math.sin(angle));
      }

      public TrigAngle plus(TrigAngle b) {
         /*
           cos (a + b) = cos a cos b - sin a sin b
           sin (a + b) = sin a cos b + sin b cos a
         */
         return new TrigAngle(
               cos * b.cos - sin * b.sin,
               sin * b.cos + b.sin * cos
         );
      }

      public TrigAngle minus(TrigAngle b) {
         /*
           cos (a - b) = cos a cos b + sin a sin b
           sin (a - b) = sin a cos b - sin b cos a
         */
         return new TrigAngle(
               cos * b.cos + sin * b.sin,
               sin * b.cos - b.sin * cos
         );
      }
   }
}
