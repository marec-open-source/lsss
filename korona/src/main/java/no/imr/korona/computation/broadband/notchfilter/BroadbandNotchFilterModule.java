package no.imr.korona.computation.broadband.notchfilter;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.Name;

import java.io.IOException;
import java.util.List;

public final class BroadbandNotchFilterModule extends ConcurrentPingModule {
   public BroadbandNotchFilterModule() {
   }

   @Override
   public List<Name> getRequiredConfigFileServiceNames() {
      return List.of(BroadbandNotchFiltersFileService.NAME);
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new BroadbandNotchFilterModuleComputation(this, computationContext, pingSource);
   }
}
