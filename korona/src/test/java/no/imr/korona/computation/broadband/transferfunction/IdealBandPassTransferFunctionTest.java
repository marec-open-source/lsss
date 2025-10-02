package no.imr.korona.computation.broadband.transferfunction;

import org.apache.commons.numbers.complex.Complex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class IdealBandPassTransferFunctionTest {
   @Test
   void test() {
      IdealBandPassTransferFunction transferFunction = new IdealBandPassTransferFunction(90, 110, 10);
      assertEquals(Complex.ZERO, transferFunction.evaluateGainFunction(75));
      assertEquals(Complex.ZERO, transferFunction.evaluateGainFunction(80));

      assertEquals(Complex.ofCartesian(0.14644660940672627, 0), transferFunction.evaluateGainFunction(82.5));
      assertEquals(Complex.ofCartesian(0.5, 0), transferFunction.evaluateGainFunction(85));
      assertEquals(Complex.ofCartesian(0.8535533905932737, 0), transferFunction.evaluateGainFunction(87.5));

      assertEquals(Complex.ONE, transferFunction.evaluateGainFunction(90));
      assertEquals(Complex.ONE, transferFunction.evaluateGainFunction(100));
      assertEquals(Complex.ONE, transferFunction.evaluateGainFunction(110));

      assertEquals(Complex.ofCartesian(0.8535533905932737, 0), transferFunction.evaluateGainFunction(112.5));
      assertEquals(Complex.ofCartesian(0.5, 0), transferFunction.evaluateGainFunction(115));
      assertEquals(Complex.ofCartesian(0.14644660940672627, 0), transferFunction.evaluateGainFunction(117.5));

      assertEquals(Complex.ZERO, transferFunction.evaluateGainFunction(120));
      assertEquals(Complex.ZERO, transferFunction.evaluateGainFunction(125));
   }
}
