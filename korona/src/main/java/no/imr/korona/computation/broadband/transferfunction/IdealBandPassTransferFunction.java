package no.imr.korona.computation.broadband.transferfunction;

import no.imr.tools.range.FloatRange;
import org.apache.commons.numbers.complex.Complex;

public record IdealBandPassTransferFunction(
      double startFrequency,
      double stopFrequency,
      double stopBandDistance
) implements TransferFunction {

   public IdealBandPassTransferFunction(FloatRange frequencyRange, double stopBandDistance) {
      this(frequencyRange.min(), frequencyRange.max(), stopBandDistance);
   }

   @Override
   public Complex evaluateGainFunction(double frequency) {
      if (frequency <= startFrequency - stopBandDistance || frequency >= stopFrequency + stopBandDistance) {
         return Complex.ZERO;
      }
      double x; // (-1, 1)
      if (frequency >= startFrequency) {
         if (frequency <= stopFrequency) {
            return Complex.ONE;
         }
         x = (frequency - stopFrequency) / stopBandDistance;
      } else {
         x = (frequency - startFrequency) / stopBandDistance;
      }
      //double gain = 1 - Math.abs(x); // Linear
      double gain = 0.5 * (1 + Math.cos(Math.PI * x)); // Hann function
      return Complex.ofCartesian(gain, 0);
   }
}
