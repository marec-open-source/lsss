package no.imr.korona.computation;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

public abstract non-sealed class GeneralPingModuleComputation extends BaseModuleComputation {
   protected GeneralPingModuleComputation(GeneralPingModule module, ComputationContext computationContext, PingSource pingSource) {
      super(module, computationContext, pingSource);
   }

   protected abstract @Nullable Ping generateOutput() throws IOException;

   @Override
   @Nullable Ping nextProcessedPing() throws IOException {
      return generateOutput();
   }

   protected final @Nullable Ping inputPing() throws IOException {
      return nextConvertedInputPing();
   }
}
