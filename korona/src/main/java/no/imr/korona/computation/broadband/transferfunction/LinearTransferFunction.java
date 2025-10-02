package no.imr.korona.computation.broadband.transferfunction;

import no.imr.tools.math.ComplexArgumentPolynomial;
import org.apache.commons.numbers.complex.Complex;

public record LinearTransferFunction(
      ComplexArgumentPolynomial numerator,
      ComplexArgumentPolynomial denominator
) implements TransferFunction {

   public LinearTransferFunction(double[] numerator, double[] denominator) {
      this(new ComplexArgumentPolynomial(numerator), new ComplexArgumentPolynomial(denominator));
   }

   @Override
   public Complex evaluateGainFunction(double frequency) {
      Complex complexFrequency = Complex.ofCartesian(0, frequency);
      return numerator.eval(complexFrequency).divide(denominator.eval(complexFrequency));
   }

   @Override
   public TransferFunction multiply(TransferFunction otherTransferFunction) {
      if (otherTransferFunction instanceof LinearTransferFunction otherLinearTransferFunction) {
         return new LinearTransferFunction(
               numerator.multiply(otherLinearTransferFunction.numerator),
               denominator.multiply(otherLinearTransferFunction.denominator)
         );
      }
      return TransferFunction.super.multiply(otherTransferFunction);
   }
}
