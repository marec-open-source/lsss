package no.imr.korona.region;

import no.imr.korona.data.ping.Ping;
import no.imr.tools.range.FloatRangeSet;

@FunctionalInterface
public interface ConditionalPingMask {
   ConditionalPingMask EMPTY = ping -> FloatRangeSet.of();

   FloatRangeSet getMask(Ping ping);

   default boolean isEmpty() {
      return this == EMPTY;
   }
}
