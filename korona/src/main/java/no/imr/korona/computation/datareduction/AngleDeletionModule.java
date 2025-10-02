package no.imr.korona.computation.datareduction;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.PingSource;

/**
 * Deletes angles from all raw datagrams.
 */
public final class AngleDeletionModule extends ConcurrentPingModule {
   public AngleDeletionModule() {
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new AngleDeletionModuleComputation(this, computationContext, pingSource);
   }
}
