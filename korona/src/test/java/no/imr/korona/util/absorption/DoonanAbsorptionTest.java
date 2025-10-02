package no.imr.korona.util.absorption;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DoonanAbsorptionTest {
   @Test
   void test() {
      DoonanAbsorption absorption = new DoonanAbsorption(30, 6, 15);
      assertEquals(0.0024209695520590, absorption.getAbsorption(18_000), 1e-16);
      assertEquals(0.0087733607740077, absorption.getAbsorption(38_000), 1e-16);
      assertEquals(0.0192117511944080, absorption.getAbsorption(70_000), 1e-16);
      assertEquals(0.0304024132329547, absorption.getAbsorption(120_000), 1e-16);
      assertEquals(0.0441051792067904, absorption.getAbsorption(200_000), 1e-16);
   }
}
