package no.imr.tools.math.linalg;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class LinalgUtilsTest {
   @Test
   void angle() {
      assertEquals(Math.PI / 2, LinalgUtils.angle(new Vec2(1, 0), new Vec2(0, 1)));
      assertEquals(Math.PI / 2, LinalgUtils.angle(new Vec3(1, 0, 0), new Vec3(0, 0, 1)));
   }

   @Test
   void pointInTriangle() {
      Vec2 point0 = new Vec2(1, 1);

      Vec2 triangleCornerA = new Vec2(0, 0);
      Vec2 triangleCornerB = new Vec2(3, 0);
      Vec2 triangleCornerC = new Vec2(0, 3);

      //anti-clockwise triangle
      assertTrue(LinalgUtils.pointInTriangle(point0, triangleCornerA, triangleCornerB, triangleCornerC));
      //clockwise triangle
      assertTrue(LinalgUtils.pointInTriangle(point0, triangleCornerA, triangleCornerC, triangleCornerB));

      Vec2 point1 = new Vec2(1, -1);
      assertFalse(LinalgUtils.pointInTriangle(point1, triangleCornerA, triangleCornerB, triangleCornerC));

      Vec2 point2 = new Vec2(3, 3);
      assertFalse(LinalgUtils.pointInTriangle(point2, triangleCornerA, triangleCornerB, triangleCornerC));

      Vec2 point3 = new Vec2(-1, 1);
      assertFalse(LinalgUtils.pointInTriangle(point3, triangleCornerA, triangleCornerB, triangleCornerC));
   }

   @Test
   void createOrthonormalBasis() {
      testNormal(new Vec3(1, 1, 1).unit());
      testNormal(new Vec3(1, 2, 3).unit());
      testNormal(new Vec3(3, 1, 2).unit());
      testNormal(new Vec3(2, 3, 1).unit());
   }

   private static void testNormal(Vec3 normal) {
      List<Vec3> basis = LinalgUtils.createOrthonormalBasis(normal);

      assertEquals(0, normal.dot(basis.get(0)), 0.001);
      assertEquals(0, normal.dot(basis.get(1)), 0.001);
      assertEquals(0, basis.get(0).dot(basis.get(1)), 0.001);
   }

   @Test
   void findOrthogonalRegressionDirection() {
      Vec3 dir = new Vec3(0.1f, 0.5f, -0.9f).unit();
      List<Vec3> points = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
         points.add(dir.times(i));
      }
      assertEquals(1, Math.abs(dir.dot(LinalgUtils.findOrthogonalRegressionDirection(points).direction())), 1e-6);

      points = List.of(Vec3.ZERO, new Vec3(1, 0.1f, 0), new Vec3(2, 0, 0));
      assertEquals(1, Math.abs(new Vec3(1, 0, 0).dot(LinalgUtils.findOrthogonalRegressionDirection(points).direction())), 1e-6);

      points = List.of(Vec3.ZERO, new Vec3(1, 100, 0), new Vec3(2, 0, 0));
      assertEquals(1, Math.abs(new Vec3(0, 1, 0).dot(LinalgUtils.findOrthogonalRegressionDirection(points).direction())), 1e-6);
   }

   @Test
   void distanceToLine() {
      assertEquals(1, LinalgUtils.distanceToLine(new Vec3(0, 0, 0), new Ray(new Vec3(1, 0, 0), new Vec3(0, 1, 0))));
      assertEquals(1, LinalgUtils.distanceToLine(new Vec3(0, 0, 0), new Ray(new Vec3(-1, 0, 0), new Vec3(0, 1, 0))));
   }

   @Test
   void distanceToPlane() {
      assertEquals(-1, LinalgUtils.distanceToPlane(new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(1, 0, 0)));
   }

   @Test
   void planeIntersection() {
      Ray ray = new Ray(new Vec3(0, 0, 0), new Vec3(1, 0, 0));
      assertEquals(new Vec3(2, 0, 0), LinalgUtils.planeIntersection(ray, new Vec3(2, 2, 2), new Vec3(1, 0, 0)));
      assertNull(LinalgUtils.planeIntersection(ray, new Vec3(-2, 2, 2), new Vec3(1, 0, 0)));
   }
}
