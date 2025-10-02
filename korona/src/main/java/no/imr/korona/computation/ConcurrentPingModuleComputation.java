package no.imr.korona.computation;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

public abstract non-sealed class ConcurrentPingModuleComputation extends BaseModuleComputation {
   protected ConcurrentPingModuleComputation(ConcurrentPingModule module, ComputationContext computationContext, PingSource pingSource) {
      super(module, computationContext, pingSource);
   }

   public void prepareForConcurrentProcessing() {
      discardPeekedPingSourcePings();
   }

   @Override
   protected final Bot0Datagram convertBot0(Ping ping) {
      return convertBot0(ping.getBot0Datagram());
   }

   public Bot0Datagram convertBot0(Bot0Datagram bot0Datagram) {
      return bot0Datagram;
   }

   public Ping convertAndProcess(Ping ping) throws IOException {
      ping = convertPing(ping);
      processPing(ping);
      return ping;
   }

   @Override
   @Nullable Ping nextProcessedPing() throws IOException {
      Ping ping = nextConvertedInputPing();
      if (ping != null) {
         processPing(ping);
      }
      return ping;
   }

   protected void processPing(Ping ping) throws IOException {
   }
}
