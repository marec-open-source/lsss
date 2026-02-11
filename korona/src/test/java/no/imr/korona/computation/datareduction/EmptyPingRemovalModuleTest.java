package no.imr.korona.computation.datareduction;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.test.data.ConstantSyntheticData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class EmptyPingRemovalModuleTest {
   @Test
   void test() throws IOException {
      List<String> inputPingInfos = List.of(
            "·#····", // Ping 1
            "··##··", // Ping 2
            "······",
            "·····#", // Ping 3
            "··##·#", // Ping 4
            "······",
            "······",
            "·#··#·", // Ping 5
            "···#··"  // Ping 6
      );
      int firstPingNumber = 10;
      ConstantSyntheticData syntheticData = new ConstantSyntheticData() {
         @Override
         protected boolean hasPowerData(PingIndex pingIndex, int channel) {
            String s = inputPingInfos.get((int) (pingIndex.getPingNumber() - firstPingNumber));
            return s.charAt(channel - 1) == '#';
         }
      };
      SyntheticDataFile syntheticDataFile = syntheticData.withFirstAndLastPingNumber(firstPingNumber, firstPingNumber + inputPingInfos.size() - 1);

      ModuleContainer moduleContainer = new ModuleContainer(new Korona());

      moduleContainer.addModule(new EmptyPingRemovalModule());

      int expectedPingNumber = firstPingNumber;
      List<Integer> channelDataCounts = new ArrayList<>();
      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticDataFile.toPingReader())) {
         while (true) {
            Ping ping = computation.nextPing();
            if (ping == null) {
               break;
            }
            assertEquals(expectedPingNumber, ping.getPingNumber());
            expectedPingNumber++;
            channelDataCounts.add((int) ping.getNonNullChannelDatas().count());
         }
      }
      assertEquals(List.of(1, 2, 1, 3, 2, 1), channelDataCounts);
   }
}
