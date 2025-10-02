package no.imr.korona.computation.filters;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.test.data.ConstantSyntheticData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class PingCollapsingModuleTest {
   @Test
   void test() throws IOException {
      List<String> inputPingInfos = List.of(
            "·#····", // Ping 1
            "··#···",
            "···#·#",
            "·····#", // Ping 2
            "··#··#", // Ping 3
            "·#··#·",
            "···#··"
      );
      int firstPingNumber = 10;
      ConstantSyntheticData syntheticData = new ConstantSyntheticData() {
         @Override
         protected boolean hasPowerData(PingIndex pingIndex, int channel) {
            String s = inputPingInfos.get((int) (pingIndex.getPingNumber() - firstPingNumber));
            return s.charAt(channel - 1) == '#';
         }
      };
      syntheticData.setFirstAndLastPingNumber(firstPingNumber, firstPingNumber + inputPingInfos.size() - 1);

      ModuleContainer moduleContainer = new ModuleContainer(new Korona());

      moduleContainer.addModule(new PingCollapsingModule());

      int expectedPingNumber = 1;
      List<Integer> channelDataCounts = new ArrayList<>();
      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticData.toPingReader())) {
         while (true) {
            Ping ping = computation.nextPing();
            if (ping == null) {
               break;
            }
            assertEquals(expectedPingNumber, ping.getPingNumber());
            expectedPingNumber++;
            List<ChannelData> channelDataList = ping.getNonNullChannelDatas().toList();
            channelDataCounts.add(channelDataList.size());
            for (ChannelData channelData : channelDataList) {
               assertEquals(ping.getNTDate(), channelData.getNTDate());
            }
         }
      }
      assertEquals(List.of(4, 1, 5), channelDataCounts);
   }
}
