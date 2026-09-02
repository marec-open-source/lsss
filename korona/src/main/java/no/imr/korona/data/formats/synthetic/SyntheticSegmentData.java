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
final class SyntheticSegmentData extends SegmentData {
   private final SyntheticDataFile syntheticDataFile;
   private final List<PingIndex> pingIndices;
   private final List<Bot0Datagram> bot0Datagrams;

   SyntheticSegmentData(SyntheticDataFile syntheticDataFile) {
      this.syntheticDataFile = syntheticDataFile;

      int pingCount = syntheticDataFile.getPingCount();
      pingIndices = new ArrayList<>(pingCount);
      bot0Datagrams = new ArrayList<>(pingCount);

      for (int i = 0; i < pingCount; i++) {
         long pingNumber = syntheticDataFile.getFirstPingNumber() + i;
         PingIndex pingIndex = syntheticDataFile.createPingIndex(pingNumber);
         pingIndices.add(pingIndex);
         bot0Datagrams.add(syntheticDataFile.createBot0Datagram(pingIndex));
      }
   }

   void expand(PingIndexShift pingIndexShift) {
      long newPingNumber = syntheticDataFile.getLastPingNumber() + 1;
      syntheticDataFile.setLastPingNumber(newPingNumber);
      PingIndex newPingIndex = syntheticDataFile.createPingIndex(newPingNumber);
      pingIndexShift.apply(newPingIndex);
      pingIndices.add(newPingIndex);
      bot0Datagrams.add(syntheticDataFile.createBot0Datagram(newPingIndex));
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
      return syntheticDataFile.getSyntheticData().getWrapAround(syntheticDataFile);
   }

   @Override
   public List<Bot0Datagram> getBot0Datagrams() {
      return bot0Datagrams;
   }

   @Override
   public PingData loadPingData(PingIndex pingIndex, AsyncHandle asyncHandle) {
      return syntheticDataFile.createPingData(pingIndex);
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return syntheticDataFile.getPingConfiguration();
   }

   SyntheticDataFile getSyntheticDataFile() {
      return syntheticDataFile;
   }
}
