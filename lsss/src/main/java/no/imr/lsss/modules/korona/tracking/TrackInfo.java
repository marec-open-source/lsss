package no.imr.lsss.modules.korona.tracking;

import no.imr.korona.data.datagrams.Cat0Datagram;
import no.imr.korona.data.ping.PingRange;
import org.jspecify.annotations.Nullable;

public record TrackInfo(TrackId trackId, @Nullable Cat0Datagram cat0Datagram, PingRange pingRange) {
   @Override
   public String toString() {
      return trackId + ", " + pingRange;
   }
}
