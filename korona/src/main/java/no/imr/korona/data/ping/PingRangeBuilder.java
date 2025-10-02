package no.imr.korona.data.ping;

import no.imr.korona.data.datamanager.PingContainer;
import org.jspecify.annotations.Nullable;

public final class PingRangeBuilder {
   private @Nullable PingIndex min;
   private @Nullable PingIndex max;

   public PingRangeBuilder() {
   }

   public void add(PingIndex pingIndex) {
      if (min == null || max == null) {
         min = pingIndex;
         max = pingIndex;
         return;
      }
      if (pingIndex.getPingNumber() < min.getPingNumber()) {
         min = pingIndex;
      } else if (pingIndex.getPingNumber() > max.getPingNumber()) {
         max = pingIndex;
      }
   }

   public PingRange build(PingContainer pingContainer) {
      if (min == null || max == null) {
         return PingRange.EMPTY_RANGE;
      }
      PingIndex end = pingContainer.nextOrSame(max);
      return PingRange.of(min, end);
   }
}
