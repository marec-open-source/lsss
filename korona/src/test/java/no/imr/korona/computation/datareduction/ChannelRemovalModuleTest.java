package no.imr.korona.computation.datareduction;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class ChannelRemovalModuleTest {
   @Test
   void channels() throws IOException {
      // 18, 38, 70, 120, 200, 364
      SyntheticData syntheticData = new ChannelRemovalData();
      ModuleContainer moduleContainer = new ModuleContainer(new Korona());

      ChannelRemovalModule channelRemovalModule = moduleContainer.addModule(new ChannelRemovalModule());
      channelRemovalModule.channels.setValue(List.of(2, 3));
      channelRemovalModule.channelsFromEnd.setValue(List.of(1));
      channelRemovalModule.frequencies.setValue(List.of(38, 120, 364));
      test(moduleContainer, syntheticData, List.of(18, 200));

      channelRemovalModule.keepSpecified.setBooleanValue(true);
      test(moduleContainer, syntheticData, List.of(38, 70, 120, 364));
   }

   @Test
   void transmitMode() throws IOException {
      // 18, 38, 70, 120, 200, 364
      SyntheticData syntheticData = new ChannelRemovalData();
      ModuleContainer moduleContainer = new ModuleContainer(new Korona());

      ChannelRemovalModule channelRemovalModule = moduleContainer.addModule(new ChannelRemovalModule());
      channelRemovalModule.frequencies.setValue(List.of(18));
      test(moduleContainer, syntheticData, List.of(38, 70, 120, 200, 364));

      channelRemovalModule.transmitMode.setValue(ChannelDataRemovalModule.TransmitModeEnum.PASSIVE);
      test(moduleContainer, syntheticData, List.of(38, 70, 120));

      channelRemovalModule.transmitMode.setValue(ChannelDataRemovalModule.TransmitModeEnum.ACTIVE);
      test(moduleContainer, syntheticData, List.of(200, 364));

      channelRemovalModule.keepSpecified.setValue(true);
      test(moduleContainer, syntheticData, List.of(18, 38, 70, 120));
   }

   @Test
   void skipComputation() throws IOException {
      SyntheticDataFile syntheticDataFile = new ChannelRemovalData().withFirstAndLastPingNumber(1, 1000);
      ModuleContainer moduleContainer = new ModuleContainer(new Korona());

      ChannelRemovalModule channelRemovalModule = moduleContainer.addModule(new ChannelRemovalModule());
      // Specify none:
      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticDataFile.toPingReader())) {
         assertEquals(List.of(), computation.getModuleComputations());
      }

      // Specify all, and keep specified:
      channelRemovalModule.frequencies.setValue(List.of(200, 364));
      channelRemovalModule.transmitMode.setValue(ChannelDataRemovalModule.TransmitModeEnum.ACTIVE);
      channelRemovalModule.keepSpecified.setBooleanValue(true);
      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticDataFile.toPingReader())) {
         assertEquals(List.of(), computation.getModuleComputations());
      }
   }

   private static void test(ModuleContainer moduleContainer, SyntheticData syntheticData, List<Integer> expectedKHzs) throws IOException {
      SyntheticDataFile syntheticDataFile = syntheticData.withFirstAndLastPingNumber(1, 1);
      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticDataFile.toPingReader())) {
         List<Integer> actualKHzs = computation.getPingConfiguration().getRawFileConfiguration().getTransducers().stream()
               .map(RawFileTransducer::getKHz)
               .toList();
         assertEquals(expectedKHzs, actualKHzs);
      }
   }
}
