package no.imr.lsss.server.util;

import no.imr.korona.data.ping.PingMapping;
import no.imr.lsss.server.pojo.ApiPingIndex;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record ValueAndPingMapping(double value, PingMapping pingMapping) {

   public static @Nullable ValueAndPingMapping from(@Nullable String time, @Nullable Long pingNumber, @Nullable Double vesselDistance) {
      if (time != null) {
         Instant instant = Instant.parse(time);
         double timeValue = PingMapping.millisToTimeValue(instant.toEpochMilli());
         return new ValueAndPingMapping(timeValue, PingMapping.TIME);
      } else if (pingNumber != null) {
         return new ValueAndPingMapping(pingNumber, PingMapping.NUMBER);
      } else if (vesselDistance != null) {
         return new ValueAndPingMapping(vesselDistance, PingMapping.DISTANCE);
      } else {
         return null;
      }
   }

   public static @Nullable ValueAndPingMapping from(ApiPingIndex apiPingIndex) {
      return from(apiPingIndex.time, apiPingIndex.pingNumber, apiPingIndex.vesselDistance);
   }
}
