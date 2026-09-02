package no.imr.korona.computation.datareduction;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

public final class EmptyPingRemovalModule extends GeneralPingModule {
   public EmptyPingRemovalModule() {
   }

   @Override
   public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new EmptyPingRemovalModuleComputation(this, computationContext, pingSource);
   }

   private static final class EmptyPingRemovalModuleComputation extends GeneralPingModuleComputation {
      private long nextPingNumber;
      private boolean firstTime = true;

      private EmptyPingRemovalModuleComputation(EmptyPingRemovalModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);
      }

      @Override
      protected @Nullable Ping generateOutput() throws IOException {
         while (true) {
            Ping ping = inputPing();
            if (ping == null) {
               return null;
            }
            if (ping.getFirstAvailableChannelData() != null) {
               Ping fixedPing = fixPingNumber(ping);
               nextPingNumber = fixedPing.getPingNumber() + 1;
               return fixedPing;
            }
         }
      }

      private Ping fixPingNumber(Ping ping) {
         if (firstTime) {
            firstTime = false;
            return ping;
         }
         if (nextPingNumber == ping.getPingNumber()) {
            return ping;
         }
         DefaultPingIndex newPingIndex = new DefaultPingIndex(ping.getPingIndex());
         newPingIndex.setPingNumber(nextPingNumber);
         DefaultPing newPing = new DefaultPing(ping.getPingConfiguration(), newPingIndex, ping.getBot0Datagram());
         newPing.addAll(ping.getPingItems());
         return newPing;
      }
   }
}
