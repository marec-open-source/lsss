package no.imr.korona.computation;

import no.imr.korona.data.ping.PingSource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/// Base class for modules that have full control over pulling input pings and generating output pings.
public abstract non-sealed class GeneralPingModule extends BaseModule {
   protected GeneralPingModule() {
   }

   @Override
   public abstract @Nullable GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException;
}
