package no.imr.tools.math;

import org.apache.commons.numbers.complex.Complex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ComplexArrayUtilsTest {
   @Test
   void averageOfTwoValues() {
      assertEquals(Complex.ofCartesian(2, 7), ComplexArrayUtils.average(
            Complex.ofCartesian(1, 5),
            Complex.ofCartesian(3, 9)
      ));
   }

   @Test
   void averageOfArray() {
      assertEquals(Complex.ofCartesian(Double.NaN, Double.NaN), ComplexArrayUtils.average(ComplexArray.EMPTY));
      assertEquals(Complex.ofCartesian(1, 5), ComplexArrayUtils.average(ComplexArray.of(new double[]{1, 5})));
      assertEquals(Complex.ofCartesian(2, 7), ComplexArrayUtils.average(ComplexArray.of(new double[]{1, 5, 3, 9})));
   }
}
