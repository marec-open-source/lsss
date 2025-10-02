package no.imr.tools.math;

import org.junit.jupiter.api.Test;

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
