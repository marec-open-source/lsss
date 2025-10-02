package no.imr.tools.math;

import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.*;

final class Function1DTest {
   @Test
   void identity() {
      Function1D f = Function1D.identity();
      assertEquals(-1, f.eval(-1));
      assertEquals(7, f.eval(7));
   }

   @Test
   void constant() {
      Function1D f = Function1D.constant(7);
      assertEquals(7, f.eval(-1));
      assertEquals(7, f.eval(0));
   }

   @Test
   void linear() {
      Function1D f = Function1D.linear(new Point2D.Double(0, 0), new Point2D.Double(1, 2));
      assertEquals(-2, f.eval(-1));
      assertEquals(0, f.eval(0));
      assertEquals(1, f.eval(0.5));
      assertEquals(2, f.eval(1));
      assertEquals(3, f.eval(1.5));
   }

   @Test
   void interpolate() {
      assertThrows(IllegalArgumentException.class, () -> {
         Function1D.interpolate(new double[1], new double[2]);
      });
      assertThrows(IllegalArgumentException.class, () -> {
         Function1D.interpolate(new double[0], new double[0]);
      });

      double[] x = {0, 2, 4, 5};
      double[] y = {3, 1, 3, 9};
      Function1D f = Function1D.interpolate(x, y);
      for (int i = 0; i < x.length; i++) {
         assertEquals(y[i], f.eval(x[i]));
      }
      assertEquals(3, f.eval(-0.1));
      assertEquals(3, f.eval(-1e6));
      assertEquals(9, f.eval(5.1));
      assertEquals(9, f.eval(1e6));

      assertEquals(2.5, f.eval(0.5));
      assertEquals(2, f.eval(1));
      assertEquals(1.5, f.eval(1.5));

      assertEquals(2, f.eval(3));

      assertEquals(6, f.eval(4.5));
   }
}
