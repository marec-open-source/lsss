package no.imr.korona.computation.filters;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
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
            "······", // Ping 1
            "·#····",
            "··#···",
            "···#·#",
            "·····#", // Ping 2
            "··#··#", // Ping 3
            "·#··#·",
            "···#··"
      );
      List<Integer> expectedChannelDataCounts = List.of(4, 1, 5);

      int firstPingNumber = 10;
      ConstantSyntheticData syntheticData = new ConstantSyntheticData() {
         @Override
         public boolean hasPowerData(PingIndex pingIndex, int channel) {
            String s = inputPingInfos.get((int) (pingIndex.getPingNumber() - firstPingNumber));
            return s.charAt(channel - 1) == '#';
         }

         @Override
         public float getHeave(PingIndex pingIndex) {
            return pingIndex.getPingNumber() + 1.01f;
         }

         @Override
         public float getRoll(PingIndex pingIndex) {
            return pingIndex.getPingNumber() + 2.01f;
         }

         @Override
         public float getPitch(PingIndex pingIndex) {
            return pingIndex.getPingNumber() + 3.01f;
         }

         @Override
         public float getHeading(PingIndex pingIndex) {
            return pingIndex.getPingNumber() + 4.01f;
         }
      };
      SyntheticDataFile syntheticDataFile = syntheticData.withFirstAndLastPingNumber(firstPingNumber, firstPingNumber + inputPingInfos.size() - 1);

      ModuleContainer moduleContainer = new ModuleContainer(new Korona());

      moduleContainer.addModule(new PingCollapsingModule());

      int expectedPingNumber = 1;
      List<Integer> channelDataCounts = new ArrayList<>();
      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticDataFile.toPingReader())) {
         while (true) {
            Ping ping = computation.nextPing();
            if (ping == null) {
               break;
            }
            assertEquals(expectedPingNumber, ping.getPingNumber());
            expectedPingNumber++;
            List<ChannelData> channelDataList = ping.getNonNullChannelDatas().toList();
            channelDataCounts.add(channelDataList.size());

            long timePingNumber = switch ((int) ping.getPingNumber()) {
               case 1 -> firstPingNumber;
               case 2 -> firstPingNumber + 4;
               case 3 -> firstPingNumber + 5;
               default -> throw new AssertionError();
            };
            assertEquals(syntheticData.getInstant(timePingNumber), ping.getInstant());

            long heavePingNumber = switch ((int) ping.getPingNumber()) {
               case 1 -> firstPingNumber + 1;
               case 2 -> firstPingNumber + 4;
               case 3 -> firstPingNumber + 5;
               default -> throw new AssertionError();
            };
            PingIndex heavePingIndex = syntheticDataFile.createPingIndex(heavePingNumber);
            for (ChannelData channelData : channelDataList) {
               assertEquals(ping.getInstant(), channelData.getInstant());
               assertEquals(syntheticData.getHeave(heavePingIndex), channelData.getHeave());
               assertEquals(syntheticData.getRoll(heavePingIndex), channelData.getRoll());
               assertEquals(syntheticData.getPitch(heavePingIndex), channelData.getPitch());
               assertEquals(syntheticData.getHeading(heavePingIndex), channelData.getHeading());
            }
         }
      }
      assertEquals(expectedChannelDataCounts, channelDataCounts);
   }
}
