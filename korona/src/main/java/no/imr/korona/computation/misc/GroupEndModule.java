package no.imr.korona.computation.misc;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import org.jspecify.annotations.Nullable;

public final class GroupEndModule extends ConcurrentPingModule {
   public GroupEndModule() {
   }

   @Override
   public @Nullable ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return null;
   }
}
