package no.imr.korona.computation;

import no.imr.korona.data.ping.PingSource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * Base class for modules that process one ping at the time.
 */
public abstract non-sealed class SimplePingModule extends BaseModule {
   protected SimplePingModule() {
   }

   @Override
   public abstract @Nullable SimplePingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException, IgnoreModuleComputationException;
}
