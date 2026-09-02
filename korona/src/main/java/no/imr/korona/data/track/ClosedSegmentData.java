package no.imr.korona.data.track;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.formats.missing.MissingBot0Datagram;
import no.imr.korona.data.ping.EmptyPingIndex;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.WrapAround;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.util.List;

final class ClosedSegmentData extends SegmentData {
   private final PingConfiguration pingConfiguration = PingConfiguration.newEmpty();

   ClosedSegmentData() {
   }

   @Override
   public List<? extends PingIndex> getPingIndices() {
      return List.of(EmptyPingIndex.INSTANCE);
   }

   @Override
   public @Nullable WrapAround getWrapAround() {
      return null;
   }

   @Override
   public List<Bot0Datagram> getBot0Datagrams() {
      return List.of(new MissingBot0Datagram(getRawFileConfiguration(), EmptyPingIndex.INSTANCE));
   }

   @Override
   public void close() {
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return pingConfiguration;
   }

   @Override
   public PingData loadPingData(PingIndex pingIndex, AsyncHandle asyncHandle) {
      return new PingData(pingConfiguration);
   }
}
