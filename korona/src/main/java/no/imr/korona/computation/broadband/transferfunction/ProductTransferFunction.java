package no.imr.korona.computation.broadband.transferfunction;

import org.apache.commons.numbers.complex.Complex;

public record ProductTransferFunction(
      TransferFunction transferFunction,
      TransferFunction otherTransferFunction
) implements TransferFunction {
   @Override
   public Complex evaluateGainFunction(double frequency) {
      return transferFunction.evaluateGainFunction(frequency).multiply(otherTransferFunction.evaluateGainFunction(frequency));
   }
}
