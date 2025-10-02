package no.imr.lsss.server.pojo;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.annotations.ReflectionEntryPoint;
import no.imr.tools.range.FloatRange;

import java.util.ArrayList;
import java.util.List;

public final class ApiPingMask {
   @JsonUnwrapped
   public ApiPingIndex pingIndex;
   public List<ApiFloatRange> depthRanges;

   @ReflectionEntryPoint
   public ApiPingMask() {
      pingIndex = new ApiPingIndex();
      depthRanges = new ArrayList<>();
   }

   public ApiPingMask(PingIndex pingIndex, List<FloatRange> depthRanges) {
      this.pingIndex = new ApiPingIndex(pingIndex);
      this.depthRanges = depthRanges.stream()
            .map(ApiFloatRange::new)
            .toList();
   }

   @Override
   public String toString() {
      return "ApiPingMask{" +
            "pingIndex=" + pingIndex +
            ", depthRanges=" + depthRanges +
            '}';
   }
}
