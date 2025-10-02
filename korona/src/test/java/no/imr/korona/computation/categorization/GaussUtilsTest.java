package no.imr.korona.computation.categorization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class GaussUtilsTest {
   @Test
   void testQuantileValue() {
      assertEquals(3.0394418, GaussUtils.quantileValue(0.1f, 5), 0.001); //number taken from math.uah.distributions implementation
      assertEquals(3.3277295, GaussUtils.quantileValue(0.05f, 5), 0.001); //number taken from math.uah.distributions implementation
   }

   @Test
   void testOutlierFractionProbability() {
      assertEquals(0.009861636, GaussUtils.outlierFractionProbability(0.1f, 5), 0.00001); //number taken from math.uah.distributions implementation
   }
}
