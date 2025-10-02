package no.imr.korona.computation.datareduction;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class ChannelDataRemovalModuleTest {
   @Test
   void channels() throws IOException {
      // 18, 38, 70, 120, 200, 364
      SyntheticData syntheticData = new ChannelRemovalData();

      ModuleContainer moduleContainer = new ModuleContainer(new Korona());

      ChannelDataRemovalModule channelDataRemovalModule = moduleContainer.addModule(new ChannelDataRemovalModule());
      channelDataRemovalModule.channels.setValue(List.of(2, 3));
      channelDataRemovalModule.channelsFromEnd.setValue(List.of(1));
      channelDataRemovalModule.frequencies.setValue(List.of(38, 120, 364));
      test(moduleContainer, syntheticData, List.of(18, 200));

      channelDataRemovalModule.keepSpecified.setBooleanValue(true);
      test(moduleContainer, syntheticData, List.of(38, 70, 120, 364));
   }

   @Test
   void transmitMode() throws IOException {
      // 18, 38, 70, 120, 200, 364
      SyntheticData syntheticData = new ChannelRemovalData();

      ModuleContainer moduleContainer = new ModuleContainer(new Korona());

      ChannelDataRemovalModule channelDataRemovalModule = moduleContainer.addModule(new ChannelDataRemovalModule());
      channelDataRemovalModule.frequencies.setValue(List.of(18));
      test(moduleContainer, syntheticData, List.of(38, 70, 120, 200, 364));

      channelDataRemovalModule.transmitMode.setValue(ChannelDataRemovalModule.TransmitModeEnum.PASSIVE);
      test(moduleContainer, syntheticData, List.of(38, 70, 120));

      channelDataRemovalModule.transmitMode.setValue(ChannelDataRemovalModule.TransmitModeEnum.ACTIVE);
      test(moduleContainer, syntheticData, List.of(200, 364));

      channelDataRemovalModule.keepSpecified.setValue(true);
      test(moduleContainer, syntheticData, List.of(18, 38, 70, 120));
   }

   private static void test(ModuleContainer moduleContainer, SyntheticData syntheticData, List<Integer> expectedKHzs) throws IOException {
      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticData.toPingReader())) {
         List<Integer> allKHzs = computation.getPingConfiguration().getRawFileConfiguration().getTransducers().stream()
               .map(RawFileTransducer::getKHz)
               .toList();
         assertEquals(List.of(18, 38, 70, 120, 200, 364), allKHzs);

         Ping ping = computation.nextPing();
         assertNotNull(ping);
         List<Integer> actualKHzs = ping.getNonNullChannelDatas()
               .map(channelData -> channelData.getTransducer().getKHz())
               .toList();
         assertEquals(expectedKHzs, actualKHzs);
      }
   }
}
