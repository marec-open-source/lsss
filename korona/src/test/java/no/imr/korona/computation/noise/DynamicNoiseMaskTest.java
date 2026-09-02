package no.imr.korona.computation.noise;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DynamicNoiseMaskTest {
   @Test
   void test() {
      DynamicNoiseMask.RangeLimits a = new DynamicNoiseMask.RangeLimits(1000, 100, 50, 60, 70);
      DynamicNoiseMask.RangeLimits b = new DynamicNoiseMask.RangeLimits(2000, 200, 60, 80, 100);
      assertEquals(new DynamicNoiseMask.RangeLimits(1100, 110, 51, 62, 73),
            DynamicNoiseMask.RangeLimits.interpolate(1100, a, b));
   }
}
