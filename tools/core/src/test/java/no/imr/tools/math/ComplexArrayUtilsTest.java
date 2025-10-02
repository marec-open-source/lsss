package no.imr.tools.math;

import org.apache.commons.numbers.complex.Complex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ComplexArrayUtilsTest {
   @Test
   void average() {
      assertEquals(Complex.ofCartesian(2, 7), ComplexArrayUtils.average(Complex.ofCartesian(1, 5), Complex.ofCartesian(3, 9)));
   }
}
