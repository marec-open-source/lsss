package no.imr.korona.computation.noise;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.List;

public final class NoiseMedianQuantificationModule extends GeneralPingModule {
   public final IntParameter pingHistory = new IntParameter(
         new Name("PingHistory", "Ping history"),
         10, Unit.COUNT, ValueConstraints.gte(0),
         "Number of historical pings to use when computing new noise values");

   public final IntParameter noiseSamplesPerBeam = new IntParameter(
         new Name("NoiseSamplesPerBeam", "Noise samples per beam"),
         100, Unit.COUNT, ValueConstraints.gt(0),
         "Number of samples to extract per beam for noise calculation (median)");

   public final FloatParameter detectionDistance = new FloatParameter(
         new Name("DetectionDistance", "Detection distance"),
         50, Unit.METER, ValueConstraints.gte(0f),
         "The distance for each beam used for noise detection");

   public final BooleanParameter innermostDistance = new BooleanParameter(
         new Name("InnermostDistance", "Innermost distance"),
         false,
         "Whether to use the innermost or outermost part of the beam");

   public NoiseMedianQuantificationModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            pingHistory,
            noiseSamplesPerBeam,
            detectionDistance,
            innermostDistance
      );
   }

   @Override
   public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new NoiseMedianQuantificationModuleComputation(this, computationContext, pingSource);
   }
}
