package no.imr.korona.computation.broadband.pulsecompressionfilter;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.Name;

import java.io.IOException;
import java.util.List;

public final class PulseCompressionFilterModule extends ConcurrentPingModule {
   public PulseCompressionFilterModule() {
   }

   @Override
   public List<Name> getRequiredConfigFileServiceNames() {
      return List.of(PulseCompressionFiltersFileService.NAME);
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new PulseCompressionFilterModuleComputation(this, computationContext, pingSource);
   }
}
