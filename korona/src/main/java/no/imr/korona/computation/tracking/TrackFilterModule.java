package no.imr.korona.computation.tracking;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.List;

public final class TrackFilterModule extends GeneralPingModule {
   public final IntParameter maxBufferSize = new IntParameter(
         new Name("MaxBufferSize", "Max buffer size"),
         50, Unit.COUNT, ValueConstraints.gte(0),
         "Maximum number of pings in buffer in search for first track info datagrams");

   public final BooleanParameter keepValid = new BooleanParameter(
         new Name("KeepValid", "Keep valid"),
         true,
         "If checked, then remove invalid tracks");

   public TrackFilterModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            maxBufferSize,
            keepValid
      );
   }

   @Override
   public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new TrackFilterModuleComputation(this, computationContext, pingSource);
   }
}
