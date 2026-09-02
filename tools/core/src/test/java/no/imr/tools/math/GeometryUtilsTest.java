package no.imr.tools.math;

import no.imr.tools.math.linalg.Vec3;
import org.junit.jupiter.api.Test;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.*;

final class GeometryUtilsTest {
   @Test
   void areaToCirclePerimeter() {
      assertEquals(0, GeometryUtils.areaToCirclePerimeter(0));
      assertEquals(2 * Math.PI, GeometryUtils.areaToCirclePerimeter(Math.PI));
      double r = 1.239753;
      assertEquals(2 * Math.PI * r, GeometryUtils.areaToCirclePerimeter(Math.PI * r * r));
   }

   @Test
   void volumeToSphereSurface() {
      assertEquals(0, GeometryUtils.volumeToSphereSurface(0));
      assertEquals(4 * Math.PI, GeometryUtils.volumeToSphereSurface((4.0 / 3.0) * Math.PI), 1e-14);
      double r = 1.239753;
      assertEquals(4 * Math.PI * r * r, GeometryUtils.volumeToSphereSurface((4.0 / 3.0) * Math.PI * r * r * r));
   }

   @Test
   void cartesianToSpherical() {
      assertEquals(new Vec3(0, 0, 0), GeometryUtils.cartesianToSpherical(new Vec3(0, 0, 0)));
      assertEquals(new Vec3(1, 0, (float) (Math.PI / 2)), GeometryUtils.cartesianToSpherical(new Vec3(1, 0, 0)));
      assertEquals(new Vec3(1, (float) (Math.PI / 2), (float) (Math.PI / 2)), GeometryUtils.cartesianToSpherical(new Vec3(0, 1, 0)));
      assertEquals(new Vec3(1, 0, 0), GeometryUtils.cartesianToSpherical(new Vec3(0, 0, 1)));
   }

   @Test
   void intersection() {
      assertEquals(new Point2D.Double(1, 2), GeometryUtils.getIntersection(new Line2D.Double(0, 0, 2, 4), new Line2D.Double(0, 2, 1, 2)));
      assertEquals(new Point2D.Double(-1, -2), GeometryUtils.getIntersection(new Line2D.Double(0, 0, 2, 4), new Line2D.Double(0, -2, 1, -2)));
      assertNull(GeometryUtils.getIntersection(new Line2D.Double(0, 0, 2, 4), new Line2D.Double(1, 0, 3, 4)));
   }

   @Test
   void trigAngle() {
      GeometryUtils.TrigAngle a = new GeometryUtils.TrigAngle(Math.toRadians(10));
      GeometryUtils.TrigAngle b = new GeometryUtils.TrigAngle(Math.toRadians(20));
      GeometryUtils.TrigAngle c = new GeometryUtils.TrigAngle(Math.toRadians(30));
      check(c, a.plus(b));
      check(c, b.plus(a));
      check(a, c.minus(b));
      check(b, c.minus(a));
   }

   private static void check(GeometryUtils.TrigAngle expected, GeometryUtils.TrigAngle actual) {
      assertEquals(expected.cos(), actual.cos(), 1e-15);
      assertEquals(expected.sin(), actual.sin(), 1e-15);
   }
}
