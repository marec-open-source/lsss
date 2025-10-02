package no.imr.lsss.server.pojo;

import no.imr.korona.data.ping.PingIndex;
import org.jspecify.annotations.Nullable;

public final class ApiPingIndex {
   public @Nullable String time;
   public @Nullable Long pingNumber;
   public @Nullable Double vesselDistance;

   ApiPingIndex() {
   }

   ApiPingIndex(PingIndex pingIndex) {
      time = pingIndex.getInstant().toString();
      pingNumber = pingIndex.getPingNumber();
      vesselDistance = pingIndex.getVesselDistance();
   }

   @Override
   public String toString() {
      return "ApiPingIndex{" +
            "time='" + time + '\'' +
            ", pingNumber=" + pingNumber +
            ", vesselDistance=" + vesselDistance +
            '}';
   }
}
