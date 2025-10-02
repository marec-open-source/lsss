package no.imr.lsss.server.pojo;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import no.imr.korona.data.ping.PingIndex;
import org.jspecify.annotations.Nullable;

public final class ApiEchogramPoint {
   @JsonUnwrapped
   public ApiPingIndex pingIndex;
   public @Nullable Float z;

   public ApiEchogramPoint() {
      pingIndex = new ApiPingIndex();
   }

   public ApiEchogramPoint(PingIndex pingIndex) {
      this.pingIndex = new ApiPingIndex(pingIndex);
   }

   public ApiEchogramPoint(PingIndex pingIndex, @Nullable Float z) {
      this(pingIndex);

      this.z = z;
   }

   @Override
   public String toString() {
      return "ApiEchogramPoint{" +
            "pingIndex=" + pingIndex +
            ", z=" + z +
            '}';
   }
}
