package no.imr.tools.math.linalg;

import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.*;

final class Vec3Test {
   @Test
   void point() {
      Point2D p = new Point2D.Float(1, -2);
      assertEquals(p, new Vec3(p).toPoint());
   }

   @Test
   void vec2() {
      Vec2 vec2 = new Vec2(1, -2);
      assertEquals(vec2, vec2.toVec3().toVec2());
   }

   @Test
   void plus() {
      assertEquals(new Vec3(7, 9, 11), new Vec3(2, 3, 4).plus(5, 6, 7));
      assertEquals(new Vec3(7, 9, 11), new Vec3(2, 3, 4).plus(new Vec3(5, 6, 7)));
   }

   @Test
   void minus() {
      assertEquals(new Vec3(-5, -3, -1), new Vec3(2, 3, 4).minus(7, 6, 5));
      assertEquals(new Vec3(-5, -3, -1), new Vec3(2, 3, 4).minus(new Vec3(7, 6, 5)));
   }

   @Test
   void times() {
      assertEquals(new Vec3(10, 18, 28), new Vec3(2, 3, 4).times(5, 6, 7));
      assertEquals(new Vec3(10, 18, 28), new Vec3(2, 3, 4).times(new Vec3(5, 6, 7)));
   }

   @Test
   void div() {
      assertEquals(new Vec3(2, 50, 250), new Vec3(10, 100, 1000).div(5, 2, 4));
      assertEquals(new Vec3(2, 50, 250), new Vec3(10, 100, 1000).div(new Vec3(5, 2, 4)));
   }

   @Test
   void unit() {
      assertEquals(new Vec3(1, 0, 0), new Vec3(2, 0, 0).unit());
      assertEquals(Vec3.ZERO, Vec3.ZERO.unit());
   }

   @Test
   void abs() {
      assertEquals(new Vec3(1, 2, 3), new Vec3(-1, -2, -3).abs());
   }
}
