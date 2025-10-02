package no.imr.korona.computation.tracking;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.datagrams.TBR0Datagram;
import no.imr.korona.data.datagrams.TNF0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

final class TrackFilterModuleComputation extends GeneralPingModuleComputation {
   private final TrackFilterModule module;
   private final Queue<PingInfo> pingInfos = new ArrayDeque<>();

   TrackFilterModuleComputation(TrackFilterModule module, ComputationContext computationContext, PingSource pingSource) {
      super(module, computationContext, pingSource);

      this.module = module;
   }

   @Override
   protected @Nullable Ping generateOutput() throws IOException {
      while (true) {
         if (getAsyncHandle().isCancelled()) {
            return null;
         }
         if (!pingInfos.isEmpty() && (pingInfos.peek().map.isEmpty() || pingInfos.size() > module.maxBufferSize.getIntValue())) {
            return pingInfos.remove().ping;
         }
         Ping ping = inputPing();
         if (ping == null) {
            PingInfo pingInfo = pingInfos.poll();
            return pingInfo != null ? pingInfo.ping : null;
         }
         addToQueue(ping);
      }
   }

   private void addToQueue(Ping ping) {
      pingInfos.add(new PingInfo(ping));

      // Collect to list first, to avoid ConcurrentModificationException.
      List<TNF0Datagram> pingItems = ping.getPingItems(TNF0Datagram.class).toList();
      pingItems.forEach(tnf0Datagram -> {
         Integer id = tnf0Datagram.getId();
         boolean removeFromPing = tnf0Datagram.isValid() != module.keepValid.getBooleanValue();
         for (PingInfo pingInfo : pingInfos) {
            pingInfo.removeId(id, removeFromPing);
         }
      });
   }

   private static final class PingInfo {
      private final Ping ping;
      private final Map<Integer, TBR0Datagram> map;

      private PingInfo(Ping ping) {
         this.ping = ping;
         map = ping.getPingItems(TBR0Datagram.class)
               .collect(Collectors.toMap(TBR0Datagram::getId, Function.identity()));
      }

      private void removeId(Integer id, boolean removeFromPing) {
         TBR0Datagram tbr0Datagram = map.remove(id);
         if (removeFromPing && tbr0Datagram != null) {
            ping.remove(tbr0Datagram);
         }
      }
   }
}
