package no.imr.korona.data.datamanager;

import no.imr.korona.data.formats.synthetic.TestSyntheticData;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.util.echogram.DefaultEchogramPingSettings;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class PingSamplerTest {
   @Test
   void noFiles() {
      DataManager dataManager = new DataManager(new DefaultDataConfiguration());
      DefaultEchogramPingSettings pingSettings = new DefaultEchogramPingSettings(dataManager.getDataFileSet(), PingMapping.DISTANCE);
      pingSettings.setPingRange(dataManager.getDataFileSet().getTotalRange());
      pingSettings.setWidth(100);
      PingSampler pingSampler = new PingSampler(dataManager, pingSettings);
      pingSampler.requestPings(PingLoadingStrategy.LEFT_TO_RIGHT);
      pingSampler.waitForPingRequest();
      assertEquals(0, pingSampler.getAvailablePings().size());
   }

   @Test
   void testPingSampler() {
      DataManager dataManager = new DataManager(new DefaultDataConfiguration());
      DataManagerTestUtils.open(dataManager, new TestSyntheticData().toSegmentHandle(1, 1000));

      int pingCount = 100;

      DataFileSet dataFileSet = dataManager.getDataFileSet();
      PingIndex firstIdx = dataFileSet.getPingIndex(dataFileSet.getTotalRange().begin().getPingNumber() + 53);
      PingRange pingRange = PingRange.of(firstIdx, dataFileSet.getPingIndex(firstIdx.getPingNumber() + pingCount));
      assertEquals(pingCount, pingRange.getPingCount());
      DefaultEchogramPingSettings pingSettings = new DefaultEchogramPingSettings(dataManager.getDataFileSet(), PingMapping.NUMBER);
      pingSettings.setPingRange(pingRange);
      PingSampler pingSampler = new PingSampler(dataManager, pingSettings);

      List<Ping> notifiedPings = new ArrayList<>();
      pingSampler.getNewPingsChangeManager().addListener(pings -> {
         assertFalse(pings.isEmpty());
         for (Ping ping : pings) {
            notifiedPings.add(ping);
            assertTrue(pingRange.contains(ping));
         }
      });

      PingLoadingStrategy pingLoadingStrategy = PingLoadingStrategy.LONGEST_GAP_LEFT_TO_RIGHT;

      { // all pings in the range
         int width = pingCount;
         pingSettings.setWidth(width);
         notifiedPings.clear();
         pingSampler.requestPings(pingLoadingStrategy);
         pingSampler.waitForPingRequest();
         assertEquals(width, pingSampler.getRequestedPingIndices().size());
         assertEquals(width, notifiedPings.size());
         List<Ping> pings = pingSampler.getAvailablePings();
         assertEquals(width, pings.size());
         for (int i = 0; i < pings.size(); i++) {
            assertEquals(firstIdx.getPingNumber() + i, pings.get(i).getPingIndex().getPingNumber());
         }
      }

      { // half of the pings in the range
         int width = pingCount / 2;
         pingSettings.setWidth(width);
         notifiedPings.clear();
         pingSampler.requestPings(pingLoadingStrategy);
         pingSampler.waitForPingRequest();
         assertEquals(width, pingSampler.getRequestedPingIndices().size());
         assertEquals(width, notifiedPings.size());
         List<Ping> pings = pingSampler.getAvailablePings();
         assertEquals(width, pings.size());
         for (int i = 0; i < pings.size(); i++) {
            assertTrue(pings.get(i).getPingIndex().getPingNumber() - (firstIdx.getPingNumber() + 2L * i) < 2);
         }
      }

      { // none of the pings in the range
         int width = 0;
         pingSettings.setWidth(width);
         notifiedPings.clear();
         pingSampler.requestPings(pingLoadingStrategy);
         pingSampler.waitForPingRequest();
         assertEquals(width, pingSampler.getRequestedPingIndices().size());
         assertEquals(width, notifiedPings.size());
         List<Ping> pings = pingSampler.getAvailablePings();
         assertEquals(width, pings.size());
      }
   }
}
