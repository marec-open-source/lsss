package no.imr.korona.computation.filters;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.math.ArrayMath;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

final class DepthDependentResamplingModuleTest {
   @Test
   void test() throws IOException {
      // Sample distance is approximately 19 cm => 524 samples to get to 100 m.
      float[] sv = new float[524];
      for (int i = 0; i < sv.length; i++) {
         sv[i] = i % 2 == 0 ? 0 : 2;
      }
      SyntheticData syntheticData = new SyntheticData() {
         @Override
         public void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
            powerData.setSv(sv.clone());
         }
      };
      SyntheticDataFile syntheticDataFile = syntheticData.withFirstAndLastPingNumber(1, 1);

      ModuleContainer moduleContainer = new ModuleContainer(new Korona());

      DepthDependentResamplingModule module = moduleContainer.addModule(new DepthDependentResamplingModule());
      module.minFrequency.setFloatValue(38);
      module.sampleDistanceAtRange100.setFloatValue(10);

      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticDataFile.toPingReader())) {
         Ping ping = computation.nextPing();
         assertNotNull(ping);

         PowerData powerData1 = ping.getPowerData(1);
         assertNotNull(powerData1);
         assertArrayEquals(sv, powerData1.getSv());

         PowerData powerData2 = ping.getPowerData(2);
         assertNotNull(powerData2);
         assertNotEquals(sv[sv.length - 1], powerData2.getSv()[sv.length - 1]); // => Sv did change.
         assertEquals(1, ArrayMath.mean(powerData2.getSv()));
      }
   }
}
