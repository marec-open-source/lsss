package no.imr.tools.math.linalg;

import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.*;

final class Vec2Test {
   @Test
   void point() {
      Point2D p = new Point2D.Float(1, -2);
      assertEquals(p, new Vec2(p).toPoint());
   }

   @Test
   void plus() {
      assertEquals(new Vec2(7, 9), new Vec2(2, 3).plus(5, 6));
      assertEquals(new Vec2(7, 9), new Vec2(2, 3).plus(new Vec2(5, 6)));
   }

   @Test
   void minus() {
      assertEquals(new Vec2(-5, -3), new Vec2(2, 3).minus(7, 6));
      assertEquals(new Vec2(-5, -3), new Vec2(2, 3).minus(new Vec2(7, 6)));
   }

   @Test
   void times() {
      assertEquals(new Vec2(10, 15), new Vec2(2, 3).times(5));
      assertEquals(new Vec2(10, 18), new Vec2(2, 3).times(5, 6));
      assertEquals(new Vec2(10, 18), new Vec2(2, 3).times(new Vec2(5, 6)));
   }

   @Test
   void div() {
      assertEquals(new Vec2(2, 20), new Vec2(10, 100).div(5));
      assertEquals(new Vec2(2, 50), new Vec2(10, 100).div(5, 2));
      assertEquals(new Vec2(2, 50), new Vec2(10, 100).div(new Vec2(5, 2)));
   }

   @Test
   void unit() {
      assertEquals(new Vec2(1, 0), new Vec2(2, 0).unit());
      assertEquals(new Vec2(0, 0), new Vec2(0, 0).unit());
   }
}
