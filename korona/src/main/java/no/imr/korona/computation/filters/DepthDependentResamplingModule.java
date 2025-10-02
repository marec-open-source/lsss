package no.imr.korona.computation.filters;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.List;
import java.util.Optional;

public final class DepthDependentResamplingModule extends ConcurrentPingModule {
   public final FloatParameter minFrequency = new FloatParameter(
         new Name("MinFrequency", "Min frequency"),
         150, Unit.KHZ,
         "Apply resampling to channels with at least this frequency");

   public final OptionalFloatParameter maxFrequency = new OptionalFloatParameter(
         new Name("MaxFrequency", "Max frequency"),
         Optional.empty(), Unit.KHZ,
         "Apply resampling to channels with at most this frequency");

   public final FloatParameter sampleDistanceAtRange100 = new FloatParameter(
         new Name("SampleDistanceAtRange100", "Sample distance at range 100"),
         0, Unit.METER, ValueConstraints.gte(0f),
         "Sample distance at 100 m range");

   public DepthDependentResamplingModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            minFrequency,
            maxFrequency,
            sampleDistanceAtRange100
      );
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new DepthDependentResamplingModuleComputation(this, computationContext, pingSource);
   }
}
