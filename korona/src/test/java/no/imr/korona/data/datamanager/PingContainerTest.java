package no.imr.korona.data.datamanager;

import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

final class PingContainerTest {
   @Test
   void getPingIndexClamped() {
      List<PingIndex> pingIndices = IntStream.range(10, 20)
            .<PingIndex>mapToObj(i -> new DefaultPingIndex(Instant.ofEpochSecond(i), i, i, null))
            .toList();
      PingContainer pingContainer = EchogramUtils.listPingContainer(PingConfiguration.newEmpty(), pingIndices);
      PingRange totalRange = pingContainer.getTotalRange();
      assertEquals(totalRange.begin(), pingContainer.getPingIndexClamped(-100));
      assertEquals(totalRange.begin(), pingContainer.getPingIndexClamped(9));
      assertEquals(totalRange.begin(), pingContainer.getPingIndexClamped(10));
      assertEquals(pingIndices.get(1), pingContainer.getPingIndexClamped(11));
      assertEquals(pingIndices.get(5), pingContainer.getPingIndexClamped(15));
      assertEquals(pingIndices.get(9), pingContainer.getPingIndexClamped(19));
      assertEquals(totalRange.end(), pingContainer.getPingIndexClamped(20));
      assertEquals(totalRange.end(), pingContainer.getPingIndexClamped(21));
      assertEquals(totalRange.end(), pingContainer.getPingIndexClamped(100));
   }

   @Test
   void getClosestPingIndex() {
      List<PingIndex> pingIndices = IntStream.range(10, 20)
            .<PingIndex>mapToObj(i -> new DefaultPingIndex(Instant.ofEpochSecond(i), i, i, null))
            .toList();
      PingContainer pingContainer = EchogramUtils.listPingContainer(PingConfiguration.newEmpty(), pingIndices);
      PingRange totalRange = pingContainer.getTotalRange();
      assertEquals(totalRange.begin(), pingContainer.getClosestPingIndex(-100, PingMapping.DISTANCE));
      assertEquals(totalRange.begin(), pingContainer.getClosestPingIndex(9, PingMapping.DISTANCE));
      assertEquals(totalRange.begin(), pingContainer.getClosestPingIndex(10, PingMapping.DISTANCE));
      assertEquals(pingIndices.get(1), pingContainer.getClosestPingIndex(11, PingMapping.DISTANCE));
      assertEquals(pingIndices.get(5), pingContainer.getClosestPingIndex(15, PingMapping.DISTANCE));
      assertEquals(pingIndices.get(9), pingContainer.getClosestPingIndex(19, PingMapping.DISTANCE));
      assertEquals(totalRange.end(), pingContainer.getClosestPingIndex(20, PingMapping.DISTANCE));
      assertEquals(totalRange.end(), pingContainer.getClosestPingIndex(21, PingMapping.DISTANCE));
      assertEquals(totalRange.end(), pingContainer.getClosestPingIndex(100, PingMapping.DISTANCE));
   }
}
