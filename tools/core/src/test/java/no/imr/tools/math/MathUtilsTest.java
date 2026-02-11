package no.imr.tools.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MathUtilsTest {
   @Test
   void findRoot() {
      assertEquals(1, MathUtils.findRoot(0.33, 13.1, x -> x * x - 1, 1e-5, 1e-5), 1e-5);
      assertEquals(-1, MathUtils.findRoot(-100, 0.5, x -> x * x - 1, 1e-5, 1e-5), 1e-5);
   }

   @Test
   void pow() {
      assertThrows(IllegalArgumentException.class, () -> MathUtils.pow(1, Math::multiplyExact, -1));
      assertThrows(IllegalArgumentException.class, () -> MathUtils.pow(1, Math::multiplyExact, 0));
      for (int i = 1; i <= 39; i++) {
         assertEquals(Math.powExact(3L, i), MathUtils.pow(3L, Math::multiplyExact, i));
      }
      assertEquals(Math.pow(1.1, 49), MathUtils.pow(1.1, (x, y) -> x * y, 49), 1e-13);
   }
}
