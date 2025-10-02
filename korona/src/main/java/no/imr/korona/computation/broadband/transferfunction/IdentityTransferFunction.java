package no.imr.korona.computation.broadband.transferfunction;

import org.apache.commons.numbers.complex.Complex;

public record IdentityTransferFunction() implements TransferFunction {
   @Override
   public Complex evaluateGainFunction(double frequency) {
      return Complex.ONE;
   }

   @Override
   public TransferFunction multiply(TransferFunction otherTransferFunction) {
      return otherTransferFunction;
   }

   @Override
   public double[] applyToTimeSignal(double[] timeSignal, double samplingFrequency, double slope) {
      return timeSignal;
   }
}
