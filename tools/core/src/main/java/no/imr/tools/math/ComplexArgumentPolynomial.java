package no.imr.tools.math;

import org.apache.commons.numbers.complex.Complex;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Polynomial with real coefficients that can be evaluated with complex arguments.
 */
public final class ComplexArgumentPolynomial {
   private final double[] coefficients;

   public ComplexArgumentPolynomial(double[] coefficients) {
      /* coefficients in increasing order, ie constant term first, leading coefficient last */
      this.coefficients = coefficients;
   }

   public Complex eval(Complex x) {
      /* evaluation of polynomial using Horner scheme*/
      double reArg = x.getReal();
      double imArg = x.getImaginary();

      double re = 0;
      double im = 0;

      for (int i = coefficients.length - 1; i >= 0; i--) {
         double reNew = reArg * re - imArg * im;
         double imNew = reArg * im + imArg * re;
         re = reNew + coefficients[i];
         im = imNew;
      }

      return Complex.ofCartesian(re, im);
   }

   public int degree() {
      return coefficients.length - 1;
   }

   public double getCoefficient(int degree) {
      if (degree < coefficients.length) {
         return coefficients[degree];
      } else {
         return 0.0;
      }
   }

   public double[] getCoefficients() {
      return coefficients;
   }

   public ComplexArgumentPolynomial multiply(ComplexArgumentPolynomial otherPolynomial) {
      if (degree() > otherPolynomial.degree()) {
         return otherPolynomial.multiply(this);
      }
      int newOrder = degree() + otherPolynomial.degree();
      double[] newCoefficients = new double[newOrder + 1];

      for (int k = 0; k < newCoefficients.length; k++) {
         for (int i = Math.max(k - degree(), 0); i <= Math.min(k, otherPolynomial.degree()); i++) {
            newCoefficients[k] += getCoefficient(k - i) * otherPolynomial.getCoefficient(i);
         }
      }
      return new ComplexArgumentPolynomial(newCoefficients);
   }

   public ComplexArgumentPolynomial pow(int exponent) {
      if (exponent < 0) {
         throw new IllegalArgumentException("Negative exponent: " + exponent);
      }
      return switch (exponent) {
         case 0 -> new ComplexArgumentPolynomial(new double[]{1});
         case 1 -> this;
         default -> MathUtils.pow(this, ComplexArgumentPolynomial::multiply, exponent);
      };
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (obj == this) {
         return true;
      }
      return obj instanceof ComplexArgumentPolynomial that
            && Arrays.equals(coefficients, that.coefficients);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(coefficients);
   }
}
