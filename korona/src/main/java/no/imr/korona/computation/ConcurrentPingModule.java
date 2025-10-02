package no.imr.korona.computation;

import no.imr.korona.data.ping.PingSource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * Base class for modules that can process pings concurrently in any order.
 */
public abstract non-sealed class ConcurrentPingModule extends BaseModule {
   protected ConcurrentPingModule() {
   }

   @Override
   public abstract @Nullable ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException, IgnoreModuleComputationException;
}
