package no.imr.tools.math;

import org.apache.commons.numbers.complex.Complex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ComplexArgumentPolynomialTest {
   private static final Complex COMPLEX_ARG = Complex.ofCartesian(13, 17);
   private static final Complex REAL_ARG = Complex.ofCartesian(13, 0);
   private static final Complex IMAGINARY_ARG = Complex.ofCartesian(0, 17);

   private static final ComplexArgumentPolynomial CONSTANT_ONE_POLY = new ComplexArgumentPolynomial(new double[]{1}); // 1
   private static final ComplexArgumentPolynomial IDENTITY = new ComplexArgumentPolynomial(new double[]{0, 1}); // x
   private static final ComplexArgumentPolynomial QUADRATIC = new ComplexArgumentPolynomial(new double[]{0, 0, 1}); // x^2
   private static final ComplexArgumentPolynomial MIXED_SECOND_ORDER = new ComplexArgumentPolynomial(new double[]{2, -1, 1}); // x^2 - x + 2
   private static final ComplexArgumentPolynomial CUBIC = new ComplexArgumentPolynomial(new double[]{0, 0, 0, 1});

   @Test
   void eval() {
      assertEquals(Complex.ONE, CONSTANT_ONE_POLY.eval(COMPLEX_ARG));
      assertEquals(Complex.ONE, CONSTANT_ONE_POLY.eval(REAL_ARG));
      assertEquals(Complex.ONE, CONSTANT_ONE_POLY.eval(IMAGINARY_ARG));

      assertEquals(COMPLEX_ARG, IDENTITY.eval(COMPLEX_ARG));
      assertEquals(REAL_ARG, IDENTITY.eval(REAL_ARG));
      assertEquals(IMAGINARY_ARG, IDENTITY.eval(IMAGINARY_ARG));

      assertEquals(COMPLEX_ARG.multiply(COMPLEX_ARG), QUADRATIC.eval(COMPLEX_ARG));
      assertEquals(REAL_ARG.multiply(REAL_ARG), QUADRATIC.eval(REAL_ARG));
      assertEquals(IMAGINARY_ARG.multiply(IMAGINARY_ARG), QUADRATIC.eval(IMAGINARY_ARG));

      Complex two = Complex.ofCartesian(2, 0);
      assertEquals(two.subtract(COMPLEX_ARG).add(COMPLEX_ARG.multiply(COMPLEX_ARG)), MIXED_SECOND_ORDER.eval(COMPLEX_ARG));
      assertEquals(two.subtract(REAL_ARG).add(REAL_ARG.multiply(REAL_ARG)), MIXED_SECOND_ORDER.eval(REAL_ARG));
      assertEquals(two.subtract(IMAGINARY_ARG).add(IMAGINARY_ARG.multiply(IMAGINARY_ARG)), MIXED_SECOND_ORDER.eval(IMAGINARY_ARG));
   }

   @Test
   void multiply() {
      assertEquals(CONSTANT_ONE_POLY, CONSTANT_ONE_POLY.multiply(CONSTANT_ONE_POLY));

      assertEquals(IDENTITY, IDENTITY.multiply(CONSTANT_ONE_POLY));
      assertEquals(IDENTITY, CONSTANT_ONE_POLY.multiply(IDENTITY));

      assertEquals(QUADRATIC, IDENTITY.multiply(IDENTITY));

      assertEquals(CUBIC, QUADRATIC.multiply(IDENTITY));
      assertEquals(CUBIC, IDENTITY.multiply(QUADRATIC));

      assertEquals(new ComplexArgumentPolynomial(new double[]{0, 2, -1, 1}), MIXED_SECOND_ORDER.multiply(IDENTITY));
      assertEquals(new ComplexArgumentPolynomial(new double[]{0, 2, -1, 1}), IDENTITY.multiply(MIXED_SECOND_ORDER));

      assertNotEquals(QUADRATIC, MIXED_SECOND_ORDER.multiply(IDENTITY));
      assertNotEquals(CUBIC, MIXED_SECOND_ORDER.multiply(IDENTITY));
   }

   @Test
   void pow() {
      assertThrows(IllegalArgumentException.class, () -> IDENTITY.pow(-1));
      assertEquals(CONSTANT_ONE_POLY, IDENTITY.pow(0));
      assertEquals(IDENTITY, IDENTITY.pow(1));
      assertEquals(QUADRATIC, IDENTITY.pow(2));
      assertEquals(CUBIC, IDENTITY.pow(3));
      assertEquals(new ComplexArgumentPolynomial(new double[]{4, -4, 5, -2, 1}), MIXED_SECOND_ORDER.pow(2));
   }
}
