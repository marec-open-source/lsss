package no.imr.korona.computation.offset;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.test.data.ConstantSyntheticData;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

final class HorizontalOffsetCorrectionModuleTest {
   /**
    * Tests that horizontal offset for constant data has no effect.
    */
   @Test
   void testConstant() throws IOException {
      float svValue = PowerData.logSvToSv(-50);
      SyntheticData syntheticData = new ConstantSyntheticData(10, svValue);
      syntheticData.setFirstAndLastPingNumber(1, 50);

      ModuleContainer moduleContainer = new ModuleContainer(new Korona());
      moduleContainer.getConfigFileSettings().restoreInstallationLocations();
      moduleContainer.addModule(new HorizontalOffsetCorrectionModule());

      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticData.toPingReader())) {
         int rawCount = 0;
         while (true) {
            Ping ping = computation.nextPing();
            if (ping == null) {
               break;
            }
            for (ChannelData channelData : ping.getChannelDatas()) {
               if (channelData == null) {
                  continue;
               }
               PowerData powerData = channelData.getPowerData();
               rawCount++;
               for (float sv : powerData.getSv()) {
                  assertEquals(svValue, sv);
               }
            }
         }
         assertEquals(syntheticData.getPingCount() * syntheticData.getPingConfiguration().getRawFileConfiguration().getTransducerCount(), rawCount);
      }
   }
}
