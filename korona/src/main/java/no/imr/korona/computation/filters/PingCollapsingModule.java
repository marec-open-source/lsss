package no.imr.korona.computation.filters;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.channel.ChannelData;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

public final class PingCollapsingModule extends GeneralPingModule {
   public PingCollapsingModule() {
   }

   @Override
   public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new PingCollapsingModuleComputation(this, computationContext, pingSource);
   }

   private static final class PingCollapsingModuleComputation extends GeneralPingModuleComputation {
      // Start all files with ping number 1 to avoid black missing segments when viewing multiple files in LSSS.
      private long pingNumber = 1;
      private @Nullable Ping nextPing;

      private PingCollapsingModuleComputation(PingCollapsingModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);
      }

      @Override
      protected @Nullable Ping generateOutput() throws IOException {
         Ping ping;
         if (nextPing != null) {
            ping = nextPing;
            nextPing = null;
         } else {
            ping = inputPing();
         }
         if (ping == null) {
            return null;
         }

         DefaultPingIndex newPingIndex = new DefaultPingIndex(ping.getPingIndex());
         newPingIndex.setPingNumber(pingNumber++);
         DefaultPing result = new DefaultPing(ping.getPingConfiguration(), newPingIndex, ping.getBot0Datagram());
         result.addAll(ping.getPingItems());

         while (true) {
            nextPing = inputPing();
            if (nextPing == null || hasOverlappingChannelData(nextPing, result)) {
               break;
            }
            for (PingItem pingItem : nextPing.getPingItems()) {
               if (pingItem instanceof ChannelData) {
                  pingItem.setNTDate(result.getNTDate());
               }
               result.add(pingItem);
            }
         }

         return result;
      }

      private static boolean hasOverlappingChannelData(Ping a, Ping b) {
         return a.getNonNullChannelDatas().anyMatch(channelData -> b.getChannelData(channelData.getChannel()) != null);
      }
   }
}
