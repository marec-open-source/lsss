package no.imr.korona.computation.filters;

import no.imr.korona.computation.BaseMatrixModule;
import no.imr.korona.computation.BaseMatrixModuleComputation;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.UnionList;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.List;

public final class ErodeLowValuesModule extends BaseMatrixModule {
   public final FloatParameter threshold = new FloatParameter(
         new Name("Threshold"),
         -70, Unit.DB,
         "Threshold value");

   public final FloatParameter replacementValue = new FloatParameter(
         new Name("ReplacementValue", "Replacement value"),
         -120, Unit.DB,
         "Value to set to samples near a sample with value <= 'Threshold'");

   public final FloatParameter verticalExtent = new FloatParameter(
         new Name("VerticalExtent", "Vertical extent"),
         1, Unit.METER, ValueConstraints.gte(0f),
         "A sample below the threshold will erode all samples closer than this distance vertically");

   public ErodeLowValuesModule() {
      super(1, ValueType.LOG_SV, false);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return new UnionList<>(super.getParameters(), List.of(
            threshold,
            replacementValue,
            verticalExtent
      ));
   }

   @Override
   public BaseMatrixModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new ErodeLowValuesModuleComputation(this, computationContext, pingSource);
   }

   private static final class ErodeLowValuesModuleComputation extends BaseMatrixModuleComputation {
      private final ErodeLowValuesModule module;

      private ErodeLowValuesModuleComputation(ErodeLowValuesModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);

         this.module = module;
      }

      @Override
      protected void doPing(int startDepth, int endDepth, int channelIndex) {
         float threshold = module.threshold.getFloatValue();
         float replacementValue = module.replacementValue.getFloatValue();
         int n = (int) Math.ceil(module.verticalExtent.getFloatValue() / getMeterPerSampleDepth(channelIndex));

         int distToLow = n;
         boolean previousLow = false;

         for (int depth = startDepth; depth <= endDepth; depth++) {
            boolean low = getRawDataFromBuffer(channelIndex, depth, -1) <= threshold
                  || getRawDataFromBuffer(channelIndex, depth, 0) <= threshold
                  || getRawDataFromBuffer(channelIndex, depth, 1) <= threshold;

            if (low) {
               if (previousLow) {
                  setData(channelIndex, depth, replacementValue);
               } else {
                  for (int d = Math.max(depth - n, startDepth); d <= depth; d++) {
                     setData(channelIndex, d, replacementValue);
                  }
               }
               distToLow = 0;
            } else {
               distToLow++;
               if (distToLow <= n) {
                  setData(channelIndex, depth, replacementValue);
               }
            }
            previousLow = low;
         }
      }
   }
}
