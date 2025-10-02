package no.imr.korona.computation;

import no.imr.korona.data.ping.PingSource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

public abstract non-sealed class GeneralPingModule extends BaseModule {
   protected GeneralPingModule() {
   }

   @Override
   public abstract @Nullable GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException;
}
