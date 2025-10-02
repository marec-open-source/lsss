package no.imr.korona.data.buffer;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.DataUtils;
import no.imr.tools.CyclicBoundedList;
import org.jspecify.annotations.Nullable;

/**
 * A ping buffer keeping the last pings in memory.
 */
public final class BoundedPingBuffer extends PingBuffer {
   private final CyclicBoundedList<Ping> pings;

   public BoundedPingBuffer(int maxPings) {
      pings = new CyclicBoundedList<>(maxPings);
   }

   @Override
   public void newPing(Ping ping) {
      pings.add(ping);
      super.newPing(ping);
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return pings.isEmpty()
            ? PingConfiguration.newEmpty()
            : pings.getFirst().getPingConfiguration();
   }

   @Override
   public PingRange getTotalRange() {
      return pings.isEmpty()
            ? PingRange.EMPTY_RANGE
            : PingRange.of(pings.getFirst().getPingIndex(), pings.getLast().getPingIndex());
   }

   @Override
   public PingIndex getClosestPingIndex(double value, PingMapping pingMapping) {
      Ping ping = DataUtils.getClosestPingIndex(pings, value, pingMapping);
      return ping.getPingIndex();
   }

   @Override
   public @Nullable PingIndex getContainingPingIndex(double value, PingMapping pingMapping) {
      Ping ping = getPing(value, pingMapping);
      return ping != null ? ping.getPingIndex() : null;
   }

   @Override
   public @Nullable Ping getPing(double value, PingMapping pingMapping) {
      return DataUtils.getContainingPingIndex(pings, null, value, pingMapping);
   }
}
