package no.imr.tools.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class NiceNumberTest {
   @Test
   void niceNumber() {
      assertEquals(1, NiceNumber.niceNumber(0, true));

      assertEquals(1e-300, NiceNumber.niceNumber(0.9e-300, true));
      assertEquals(1e-300, NiceNumber.niceNumber(1.1e-300, true));

      assertEquals(0.1, NiceNumber.niceNumber(0.09, true));
      assertEquals(0.1, NiceNumber.niceNumber(0.11, true));

      assertEquals(1, NiceNumber.niceNumber(0.9, true));
      assertEquals(1, NiceNumber.niceNumber(1.1, true));

      assertEquals(10, NiceNumber.niceNumber(9, true));
      assertEquals(10, NiceNumber.niceNumber(11, true));

      assertEquals(1e300, NiceNumber.niceNumber(0.9e300, true));
      assertEquals(1e300, NiceNumber.niceNumber(1.1e300, true));

      assertEquals(Double.NaN, NiceNumber.niceNumber(Double.NaN, true));

      assertThrows(IllegalArgumentException.class, () -> NiceNumber.niceNumber(-1, true));
      assertThrows(IllegalArgumentException.class, () -> NiceNumber.niceNumber(-1e-10, true));
   }
}
