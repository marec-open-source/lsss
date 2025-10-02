package no.imr.korona.computation.broadband.transferfunction;

import no.imr.korona.computation.broadband.PulseCompression;
import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFilterConfig;
import no.imr.korona.data.ping.items.configuration.PulseForm;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.range.FloatRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class TransferFunctionTest {
   @Test
   void applyToTimeSignal() {
      TransferFunction transferFunction = new IdealBandPassTransferFunction(FloatRange.of(50_000, 80_000), 5_000)
            .multiply(TransferFunctionUtils.generateNotchFilter(new BroadbandNotchFilterConfig(60_000, 2_000), FloatRange.of(55_000, 95_000)));

      double samplingFrequency = 1500000.0;
      double[] signal = PulseCompression.generateFullSentSignal(0.017755682f, 0.002048f, samplingFrequency, FloatRange.of(55_000, 95_000), PulseForm.BROADBAND_LINEAR_UP);
      double[] result = transferFunction.applyToTimeSignal(signal, samplingFrequency, 0);

      assertEquals(signal.length, result.length);
      assertEquals(-0.05999024381451695, ArrayMath.sum(result));
      assertEquals(0.11432401986872112, result[0]);
      assertEquals(-0.298712534090624, result[100]);
      assertEquals(-0.7979703284033789, result[900]);
      assertEquals(-0.8720192899234085, result[1415]);
      assertEquals(-0.028901054185272113, result[3000]);
   }

   @Test
   void multiply() {
      TransferFunction identity = new IdentityTransferFunction();
      TransferFunction a = new LinearTransferFunction(new double[]{1, 2, 3}, new double[]{4, 5, 6});
      TransferFunction b = new LinearTransferFunction(new double[]{7, 8, 9}, new double[]{10, 11, 12});
      assertSame(a, identity.multiply(a));
      assertSame(a, a.multiply(identity));
      assertInstanceOf(LinearTransferFunction.class, a.multiply(b));
   }
}
