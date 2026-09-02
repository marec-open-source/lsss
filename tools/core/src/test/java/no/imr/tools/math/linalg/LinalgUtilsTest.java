package no.imr.tools.math.linalg;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class LinalgUtilsTest {
   @Test
   void angle() {
      assertEquals(Math.PI / 2, LinalgUtils.angle(new Vec2(1, 0), new Vec2(0, 1)));
      assertEquals(Math.PI / 2, LinalgUtils.angle(new Vec3(1, 0, 0), new Vec3(0, 0, 1)));

      // These vectors give `dot(a, b) / (|a|*|b|)` outside of [-1, 1] due to rounding and converting float to double:
      assertEquals(0, LinalgUtils.angle(new Vec2(0.1f, 0.1f), new Vec2(0.937852f, 0.9380717f)));
      assertEquals(0, LinalgUtils.angle(new Vec3(0.1f, 0.1f, 0.1f), new Vec3(0.82194793f, 0.82243353f, 0.8223702f)));

      // Vector with a length of 0:
      assertEquals(Double.NaN, LinalgUtils.angle(new Vec2(1, 1), new Vec2(0, 0)));
      assertEquals(Double.NaN, LinalgUtils.angle(new Vec3(1, 1, 1), new Vec3(0, 0, 0)));
   }

   @Test
   void findMean() {
      JUnitUtils.assertEquals(new Vec3(Float.NaN, Float.NaN, Float.NaN), LinalgUtils.findMean(List.of()));
      JUnitUtils.assertEquals(Vec3.ZERO, LinalgUtils.findMean(List.of(Vec3.ZERO)));
      JUnitUtils.assertEquals(new Vec3(2, 3, 4), LinalgUtils.findMean(List.of(
            new Vec3(1, 2, 3), new Vec3(3, 4, 5))));
   }

   @Test
   void findOrthogonalRegressionDirection() {
      assertThrows(IllegalArgumentException.class, () -> LinalgUtils.findOrthogonalRegressionDirection(List.of()));
      assertThrows(IllegalArgumentException.class, () -> LinalgUtils.findOrthogonalRegressionDirection(List.of(Vec3.ZERO)));

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
   void signedDistanceToPlane() {
      assertEquals(-1, LinalgUtils.signedDistanceToPlane(new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(1, 0, 0)));
      assertEquals(2, LinalgUtils.signedDistanceToPlane(new Vec3(3, 0, 0), new Vec3(1, 0, 0), new Vec3(1, 0, 0)));
   }

   @Test
   void planeIntersection() {
      Ray ray = new Ray(new Vec3(0, 0, 0), new Vec3(1, 0, 0));
      assertEquals(new Vec3(2, 0, 0), LinalgUtils.planeIntersection(ray, new Vec3(2, 2, 2), new Vec3(1, 0, 0)));
      assertNull(LinalgUtils.planeIntersection(ray, new Vec3(-2, 2, 2), new Vec3(1, 0, 0)));
      assertNull(LinalgUtils.planeIntersection(ray, new Vec3(-2, 2, 2), new Vec3(0, 1, 0)));
   }
}
