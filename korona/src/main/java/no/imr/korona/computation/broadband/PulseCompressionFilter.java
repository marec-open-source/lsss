package no.imr.korona.computation.broadband;

import no.imr.tools.math.ComplexArray;
import no.imr.tools.math.ComplexArrayUtils;

public record PulseCompressionFilter(int stage, int decimationFactor, ComplexArray coefficients) {

   ComplexArray apply(ComplexArray signal) {
      ComplexArray result = ComplexArrayUtils.convFull(signal, coefficients);
      return ComplexArrayUtils.downsample(result, decimationFactor);
   }
}
