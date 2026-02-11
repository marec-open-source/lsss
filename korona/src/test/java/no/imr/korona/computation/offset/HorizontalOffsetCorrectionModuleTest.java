package no.imr.korona.computation.offset;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
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
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData(10, svValue).withFirstAndLastPingNumber(1, 50);

      ModuleContainer moduleContainer = new ModuleContainer(new Korona());
      moduleContainer.getConfigFileSettings().restoreInstallationLocations();
      moduleContainer.addModule(new HorizontalOffsetCorrectionModule());

      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticDataFile.toPingReader())) {
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
         assertEquals(syntheticDataFile.getPingCount() * syntheticDataFile.getPingConfiguration().getRawFileConfiguration().getTransducerCount(), rawCount);
      }
   }
}
