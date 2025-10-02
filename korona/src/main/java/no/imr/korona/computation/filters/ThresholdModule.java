package no.imr.korona.computation.filters;

import no.imr.korona.computation.BaseMatrixModule;
import no.imr.korona.computation.BaseMatrixModuleComputation;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.UnionList;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;

import java.util.List;

/**
 * Masks out values in specific range.
 * <p>
 * This module  sets all values &gt;= threshold to value aboveThreshold if cutAbove is true, otherwise they remain unchanged<br>
 * similarly it sets all values &lt;  threshold to value belowThreshold if cutBelow is true, otherwise they remain unchanged
 */
public final class ThresholdModule extends BaseMatrixModule {
   public final FloatParameter thresholdDB = new FloatParameter(
         new Name("Threshold"),
         -70, Unit.DB,
         "Threshold value");

   public final BooleanParameter cutBelow = new BooleanParameter(
         new Name("CutBelow", "Cut below"),
         false,
         "If selected, all samples with value < 'Threshold' will become 'Value below'");

   public final BooleanParameter cutAbove = new BooleanParameter(
         new Name("CutAbove", "Cut above"),
         false,
         "If selected, all samples with value ≥ 'Threshold' will become 'Value above'");

   public final FloatParameter belowValDB = new FloatParameter(
         new Name("BelowVal", "Value below"),
         -120, Unit.DB,
         "Value assigned to samples with value < 'Threshold'");

   public final FloatParameter aboveValDB = new FloatParameter(
         new Name("AboveVal", "Value above"),
         0, Unit.DB,
         "Value assigned to samples with value ≥ 'Threshold'");

   public ThresholdModule() {
      super(0);

      cutBelow.addListenerAndNotify(belowValDB::setEnabled);
      cutAbove.addListenerAndNotify(aboveValDB::setEnabled);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return new UnionList<>(super.getParameters(), List.of(
            thresholdDB,
            cutBelow,
            cutAbove,
            belowValDB,
            aboveValDB
      ));
   }

   @Override
   public BaseMatrixModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new ThresholdModuleComputation(this, computationContext, pingSource);
   }

   private static final class ThresholdModuleComputation extends BaseMatrixModuleComputation {
      private final ThresholdModule module;

      private ThresholdModuleComputation(ThresholdModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);
         this.module = module;
      }

      @Override
      protected void doPing(int startDepth, int endDepth, int channelIndex) {
         float threshold = valueType.getValueFromLogSv(module.thresholdDB.getFloatValue());

         if (module.cutAbove.getBooleanValue()) {
            float aboveVal = valueType.getValueFromLogSv(module.aboveValDB.getFloatValue());
            for (int depth = startDepth; depth <= endDepth; depth++) {
               float val = getRawDataFromBuffer(channelIndex, depth, 0);
               if (val >= threshold) {
                  setData(channelIndex, depth, aboveVal);
               }
            }
         }

         if (module.cutBelow.getBooleanValue()) {
            float belowVal = valueType.getValueFromLogSv(module.belowValDB.getFloatValue());
            for (int depth = startDepth; depth <= endDepth; depth++) {
               float val = getRawDataFromBuffer(channelIndex, depth, 0);
               if (val < threshold) {
                  setData(channelIndex, depth, belowVal);
               }
            }
         }
      }
   }
}
