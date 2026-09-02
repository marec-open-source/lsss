package no.imr.korona.computation.filters;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.test.data.ConstantSyntheticData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class FillMissingDataModuleTest {
   @Test
   void test() throws IOException {
      List<String> inputPingInfos = List.of(
            "······",  // '·': Missing channel data
            "0·#···",  // '0': Empty channel data (count = 0)
            "···#·#",  // '#': Normal channel data (count > 0)
            "··00·#",
            "··#··#",
            "·#·0#·",
            "·0·#··",
            "·······"
      );
      List<Integer> expectedEmptyChannelDataCounts = List.of(0, 1, 0, 0, 0, 0, 0, 0);
      List<Integer> expectedNonEmptyChannelDataCounts = List.of(0, 1, 3, 3, 3, 5, 5, 5);

      int firstPingNumber = 10;
      ConstantSyntheticData syntheticData = new ConstantSyntheticData() {
         private char getInfoChar(PingIndex pingIndex, int channel) {
            String s = inputPingInfos.get((int) (pingIndex.getPingNumber() - firstPingNumber));
            return s.charAt(channel - 1);
         }

         @Override
         public boolean hasPowerData(PingIndex pingIndex, int channel) {
            return getInfoChar(pingIndex, channel) != '·';
         }

         @Override
         public void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
            if (getInfoChar(pingIndex, powerData.getChannel()) == '0') {
               powerData.setCount(0);
            } else {
               super.defineSampleValues(powerData, pingIndex);
            }
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

      moduleContainer.addModule(new FillMissingDataModule());

      int expectedPingNumber = firstPingNumber;
      List<Integer> emptyChannelDataCounts = new ArrayList<>();
      List<Integer> nonEmptyChannelDataCounts = new ArrayList<>();
      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticDataFile.toPingReader())) {
         while (true) {
            Ping ping = computation.nextPing();
            if (ping == null) {
               break;
            }
            assertEquals(expectedPingNumber, ping.getPingNumber());
            expectedPingNumber++;
            List<ChannelData> channelDatas = ping.getNonNullChannelDatas().toList();
            int emptyChannelDataCount = 0;
            int nonEmptyChannelDataCount = 0;

            PingIndex heavePingIndex = ping.getPingIndex();
            if (heavePingIndex.getPingNumber() == firstPingNumber + inputPingInfos.size() - 1) {
               // This ping originally had no channel data => Use heave etc. from previous ping.
               heavePingIndex = syntheticDataFile.createPingIndex(heavePingIndex.getPingNumber() - 1);
            }
            for (ChannelData channelData : channelDatas) {
               if (channelData.getCount() == 0) {
                  emptyChannelDataCount++;
               } else {
                  nonEmptyChannelDataCount++;
               }
               assertEquals(syntheticData.getInstant(ping.getPingNumber()), channelData.getInstant());
               assertEquals(syntheticData.getHeave(heavePingIndex), channelData.getHeave());
               assertEquals(syntheticData.getRoll(heavePingIndex), channelData.getRoll());
               assertEquals(syntheticData.getPitch(heavePingIndex), channelData.getPitch());
               assertEquals(syntheticData.getHeading(heavePingIndex), channelData.getHeading());
            }
            emptyChannelDataCounts.add(emptyChannelDataCount);
            nonEmptyChannelDataCounts.add(nonEmptyChannelDataCount);
         }
      }
      assertEquals(expectedEmptyChannelDataCounts, emptyChannelDataCounts);
      assertEquals(expectedNonEmptyChannelDataCounts, nonEmptyChannelDataCounts);
   }
}
