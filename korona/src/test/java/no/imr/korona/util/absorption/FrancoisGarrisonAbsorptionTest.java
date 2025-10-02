package no.imr.korona.util.absorption;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class FrancoisGarrisonAbsorptionTest {
   @Test
   void test() {
      FrancoisGarrisonAbsorption absorption = new FrancoisGarrisonAbsorption(8, 30, 6, 15);
      assertEquals(0.0026794407779269, absorption.getAbsorption(18_000), 1e-16);
      assertEquals(0.0092202154051727, absorption.getAbsorption(38_000), 1e-16);
      assertEquals(0.0193284677594871, absorption.getAbsorption(70_000), 1e-16);
      assertEquals(0.0297159164379340, absorption.getAbsorption(120_000), 1e-16);
      assertEquals(0.0427807360019236, absorption.getAbsorption(200_000), 1e-16);

      absorption = new FrancoisGarrisonAbsorption(7, 7, 25, 10);
      assertEquals(0.0003639852099853, absorption.getAbsorption(18_000), 1e-16);
      assertEquals(0.0015311822225096, absorption.getAbsorption(38_000), 1e-16);
      assertEquals(0.0047738334250553, absorption.getAbsorption(70_000), 1e-16);
      assertEquals(0.0117299787061108, absorption.getAbsorption(120_000), 1e-16);
      assertEquals(0.0237706215431084, absorption.getAbsorption(200_000), 1e-16);
   }
}
