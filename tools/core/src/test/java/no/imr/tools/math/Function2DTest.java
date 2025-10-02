package no.imr.tools.math;

import no.imr.tools.math.linalg.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class Function2DTest {
   @Test
   void constant() {
      Function2D f = Function2D.constant(7);
      assertEquals(7, f.eval(1, 1));
      assertEquals(7, f.eval(-2, 2));
   }

   @Test
   void linear() {
      Function2D f = Function2D.linear(new Vec3(1, 1, 1), 2, 3);
      assertEquals(1, f.eval(1, 1));
      assertEquals(6, f.eval(2, 2));
      assertEquals(-1, f.eval(-3, 3));
   }

   @Test
   void sphere() {
      Function2D f = Function2D.sphere(new Vec3(1, 1, 1), 2, true);
      assertEquals(3, f.eval(1, 1));
      assertEquals(1, f.eval(3, 1));
      assertEquals(1, f.eval(1, -1));
      assertEquals(Double.NaN, f.eval(3, 3));
   }
}
