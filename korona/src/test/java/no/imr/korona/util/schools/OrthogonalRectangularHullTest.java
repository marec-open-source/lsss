package no.imr.korona.util.schools;

import no.imr.tools.math.linalg.Vec2;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class OrthogonalRectangularHullTest {
   @Test
   void basic() {
      assertThrows(IllegalArgumentException.class, () -> OrthogonalRectangularHull.create(List.of()));

      assertEquals(new OrthogonalRectangularHull(new Vec3(1, 2, 3), new Vec3(0, 0, 0), 0),
            OrthogonalRectangularHull.create(List.of(new Vec3(1, 2, 3))));

      check(new Vec3(5, 0, 0), new Vec3(10, 0, 0), 0,
            OrthogonalRectangularHull.create(List.of(
                  new Vec3(0, 0, 0),
                  new Vec3(10, 0, 0))),
            0);

      check(new Vec3(5, 1, 1), new Vec3(10, 2, 2), 0,
            OrthogonalRectangularHull.create(List.of(
                  new Vec3(0, 0, 0),
                  new Vec3(10, 0, 0),
                  new Vec3(0, 2, 2),
                  new Vec3(10, 2, 2))),
            0);

      check(new Vec3(0, 5, 0), new Vec3(10, 0, 0), Math.PI / 2,
            OrthogonalRectangularHull.create(List.of(
                  new Vec3(0, 0, 0),
                  new Vec3(0, 10, 0))),
            0);

      check(new Vec3(5, 5, 0), new Vec3((float) (10 * Math.sqrt(2)), 0, 0), Math.PI / 4,
            OrthogonalRectangularHull.create(List.of(
                  new Vec3(0, 0, 0),
                  new Vec3(10, 10, 0))),
            1e-5f);
   }

   private static void check(Vec3 center, Vec3 extent, double rotation, OrthogonalRectangularHull orh, float delta) {
      JUnitUtils.assertEquals(center, orh.center(), delta);
      JUnitUtils.assertEquals(extent, orh.extent(), delta);
      assertEquals(1, Math.abs(rotToVec(rotation).dot(rotToVec(orh.rotation()))), delta);
   }

   private static Vec2 rotToVec(double rotation) {
      return new Vec2((float) Math.cos(rotation), (float) Math.sin(rotation));
   }

   @Test
   void testTranslationIndependence() {
      List<Vec3> points1 = List.of(
            new Vec3(1, 1, 0),
            new Vec3(-1, -1, 0),
            new Vec3(0.5f, -0.5f, 0),
            new Vec3(-0.5f, 0.5f, 0));

      Vec2 truePrincipalVector = new Vec2(1 / (float) Math.sqrt(2), 1 / (float) Math.sqrt(2));

      OrthogonalRectangularHull orh1 = OrthogonalRectangularHull.create(points1);

      assertEquals(Math.PI / 4, orh1.rotation(), 1e-7);

      Vec2 pv1 = rotToVec(orh1.rotation());
      assertEquals(1, Math.abs(pv1.dot(truePrincipalVector)), 1e-7f);

      List<Vec3> points2 = points1.stream()
            .map(point -> point.plus(5, 10, 0))
            .toList();

      OrthogonalRectangularHull orh2 = OrthogonalRectangularHull.create(points2);

      assertEquals(Math.PI / 4, orh2.rotation(), 1e-7);

      Vec2 pv2 = rotToVec(orh2.rotation());
      assertEquals(1, Math.abs(pv2.dot(truePrincipalVector)), 1e-7f);
   }
}
