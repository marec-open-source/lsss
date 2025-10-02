package no.imr.korona.computation;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

public abstract non-sealed class SimplePingModuleComputation extends BaseModuleComputation {
   private boolean hasCalledEndOfInput;

   protected SimplePingModuleComputation(SimplePingModule module, ComputationContext computationContext, PingSource pingSource) {
      super(module, computationContext, pingSource);
   }

   @Override
   @Nullable Ping nextProcessedPing() throws IOException {
      Ping ping = nextConvertedInputPing();
      if (ping != null) {
         processPing(ping);
      } else {
         if (!hasCalledEndOfInput) {
            hasCalledEndOfInput = true;
            endOfInput();
         }
      }
      return ping;
   }

   protected void processPing(Ping ping) throws IOException {
   }

   protected void endOfInput() throws IOException {
   }
}
