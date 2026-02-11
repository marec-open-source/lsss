package no.imr.korona.computation.datareduction;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.List;

public final class DownsamplingModule extends ConcurrentPingModule {
   public enum Method {
      NONE, FACTOR, SAMPLE_SIZE
   }

   public final ObjectParameter<Method> downsamplingMethod = new ObjectParameter<>(
         new Name("Downsampling"),
         Method.FACTOR, Method.values(),
         "Downsampling method");

   public final IntParameter downsamplingFactor = new IntParameter(
         new Name("DownsamplingFactor", "Downsampling factor"),
         1, Unit.DIMENSIONLESS, ValueConstraints.gte(1),
         "Downsampling factor");

   public final FloatParameter downsamplingSampleSize = new FloatParameter(
         new Name("DownsamplingSampleSize", "Downsampling sample size"),
         0.01f, Unit.METER, ValueConstraints.gt(0f),
         "Downsampling factor is set to give approximately this sample size");

   public DownsamplingModule() {
      downsamplingMethod.addListenerAndNotify(method -> {
         downsamplingFactor.setVisible(method == Method.FACTOR);
         downsamplingSampleSize.setVisible(method == Method.SAMPLE_SIZE);
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            downsamplingMethod,
            downsamplingFactor,
            downsamplingSampleSize
      );
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new DownsamplingModuleComputation(this, computationContext, pingSource);
   }
}
