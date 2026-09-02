package no.imr.tools.math;

import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.function.DoubleUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

final class Function1DTest {
   @Test
   void constant() {
      DoubleUnaryOperator f = Function1D.constant(7);
      assertEquals(7, f.applyAsDouble(-1));
      assertEquals(7, f.applyAsDouble(0));
   }

   @Test
   void linear() {
      DoubleUnaryOperator f = Function1D.linear(new Point2D.Double(0, 0), new Point2D.Double(1, 2));
      assertEquals(-2, f.applyAsDouble(-1));
      assertEquals(0, f.applyAsDouble(0));
      assertEquals(1, f.applyAsDouble(0.5));
      assertEquals(2, f.applyAsDouble(1));
      assertEquals(3, f.applyAsDouble(1.5));
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
      DoubleUnaryOperator f = Function1D.interpolate(x, y);
      for (int i = 0; i < x.length; i++) {
         assertEquals(y[i], f.applyAsDouble(x[i]));
      }
      assertEquals(3, f.applyAsDouble(-0.1));
      assertEquals(3, f.applyAsDouble(-1e6));
      assertEquals(9, f.applyAsDouble(5.1));
      assertEquals(9, f.applyAsDouble(1e6));

      assertEquals(2.5, f.applyAsDouble(0.5));
      assertEquals(2, f.applyAsDouble(1));
      assertEquals(1.5, f.applyAsDouble(1.5));

      assertEquals(2, f.applyAsDouble(3));

      assertEquals(6, f.applyAsDouble(4.5));
   }
}
