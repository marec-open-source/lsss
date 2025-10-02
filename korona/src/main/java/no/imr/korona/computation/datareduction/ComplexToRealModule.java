package no.imr.korona.computation.datareduction;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;

import java.io.IOException;
import java.util.List;

public final class ComplexToRealModule extends ConcurrentPingModule {
   final BooleanParameter computeAngles = new BooleanParameter(
         new Name("ComputeAngles", "Compute angles"),
         true,
         "If selected, then angles are computed, otherwise removed");

   final BooleanParameter keepBroadband = new BooleanParameter(
         new Name("KeepBroadband", "Keep broadband"),
         false,
         "If selected, then broadband data is not converted to real data");

   public ComplexToRealModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            computeAngles,
            keepBroadband
      );
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new ComplexToRealModuleComputation(this, computationContext, pingSource);
   }
}
