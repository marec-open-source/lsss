package no.imr.korona.computation.broadband.transferfunction;

import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFilterConfig;
import no.imr.tools.range.FloatRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class TransferFunctionUtilsTest {
   @Test
   void generateNotchFilter() {
      TransferFunction transferFunction = TransferFunctionUtils.generateNotchFilter(
            new BroadbandNotchFilterConfig(50_000, 10_000), FloatRange.of(25_000, 75_000));

      assertEquals(0, transferFunction.evaluateGainFunctionInDb(1_000), 1e-3);
      assertEquals(-0.076, transferFunction.evaluateGainFunctionInDb(25_000), 1e-3);
      assertEquals(-2.768, transferFunction.evaluateGainFunctionInDb(45_000), 1e-3);
      assertEquals(-93.951, transferFunction.evaluateGainFunctionInDb(50_000.1), 1e-3);
      assertEquals(-3.202, transferFunction.evaluateGainFunctionInDb(55_000), 1e-3);
      assertEquals(-0.242, transferFunction.evaluateGainFunctionInDb(75_000), 1e-3);
      assertEquals(-0.012, transferFunction.evaluateGainFunctionInDb(200_000), 1e-3);
   }

   @Test
   void generateNotchFilterFromConfig() {
      TransferFunction transferFunction = TransferFunctionUtils.generateNotchFilterFromConfig(new NotchFilterConfig(
            List.of(
                  new BroadbandNotchFilterConfig(40_000, 5_000),
                  new BroadbandNotchFilterConfig(60_000, 5_000)
            ),
            FloatRange.of(25_000, 75_000)
      ));

      assertEquals(0, transferFunction.evaluateGainFunctionInDb(1_000), 1e-3);
      assertEquals(-2.891, transferFunction.evaluateGainFunctionInDb(37_500), 1e-3);
      assertEquals(-87.980, transferFunction.evaluateGainFunctionInDb(40_000.1), 1e-3);
      assertEquals(-3.191, transferFunction.evaluateGainFunctionInDb(42_500), 1e-3);
      assertEquals(-3.026, transferFunction.evaluateGainFunctionInDb(57_500), 1e-3);
      assertEquals(-88.029, transferFunction.evaluateGainFunctionInDb(60_000.1), 1e-3);
      assertEquals(-3.164, transferFunction.evaluateGainFunctionInDb(62_500), 1e-3);
      assertEquals(-0.183, transferFunction.evaluateGainFunctionInDb(75_000), 1e-3);
      assertEquals(-0.006, transferFunction.evaluateGainFunctionInDb(200_000), 1e-3);
   }
}
