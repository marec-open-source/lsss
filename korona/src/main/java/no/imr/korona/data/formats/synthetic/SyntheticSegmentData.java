package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingIndexShift;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.track.SegmentData;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The data of a {@link SyntheticSegment}.
 */
public final class SyntheticSegmentData extends SegmentData {
   private final SyntheticData syntheticData;
   private final List<PingIndex> pingIndices;
   private final List<Bot0Datagram> bot0Datagrams;

   SyntheticSegmentData(SyntheticData syntheticData) {
      this.syntheticData = syntheticData;

      int pingCount = syntheticData.getPingCount();
      pingIndices = new ArrayList<>(pingCount);
      bot0Datagrams = new ArrayList<>(pingCount);

      for (int i = 0; i < pingCount; i++) {
         long pingNumber = syntheticData.getFirstPingNumber() + i;
         PingIndex pingIndex = syntheticData.createPingIndex(pingNumber);
         pingIndices.add(pingIndex);
         bot0Datagrams.add(syntheticData.createBot0Datagram(pingIndex));
      }
   }

   public void expand(PingIndexShift pingIndexShift) {
      long newPingNumber = syntheticData.getLastPingNumber() + 1;
      syntheticData.setFirstAndLastPingNumber(syntheticData.getFirstPingNumber(), newPingNumber);
      PingIndex newPingIndex = syntheticData.createPingIndex(newPingNumber);
      pingIndexShift.apply(newPingIndex);
      pingIndices.add(newPingIndex);
      bot0Datagrams.add(syntheticData.createBot0Datagram(newPingIndex));
   }

   @Override
   public void close() {
   }

   @Override
   public List<PingIndex> getPingIndices() {
      return pingIndices;
   }

   @Override
   public @Nullable WrapAround getWrapAround() {
      return syntheticData.getWrapAround();
   }

   @Override
   public List<Bot0Datagram> getBot0Datagrams() {
      return bot0Datagrams;
   }

   @Override
   public PingData loadPingData(PingIndex pingIndex, AsyncHandle asyncHandle) {
      return syntheticData.createPingData(pingIndex);
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return syntheticData.getPingConfiguration();
   }

   public SyntheticData getSyntheticData() {
      return syntheticData;
   }
}
