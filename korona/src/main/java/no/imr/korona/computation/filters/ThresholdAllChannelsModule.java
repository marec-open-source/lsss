package no.imr.korona.computation.filters;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ObjectParameterValue;

import java.util.List;
import java.util.Optional;

public final class ThresholdAllChannelsModule extends ConcurrentPingModule {
   enum Criterion implements ObjectParameterValue {
      ABOVE_OR_EQUAL("≥ Above or equal to threshold"),
      ABOVE("> Strictly above threshold"),
      BELOW("< Strictly below threshold"),
      BELOW_OR_EQUAL("≤ Below or equal to threshold");

      private final String label;

      Criterion(String label) {
         this.label = label;
      }

      @Override
      public String getDisplayLabel() {
         return label;
      }
   }

   final ObjectParameter<Criterion> criterion = new ObjectParameter<>(
         new Name("Criterion"),
         Criterion.BELOW, Criterion.values(),
         "How to test values");

   final FloatParameter threshold = new FloatParameter(
         new Name("Threshold"),
         -70, Unit.DB,
         "Threshold value");

   final FloatParameter replacement = new FloatParameter(
         new Name("Replacement"),
         -120, Unit.DB,
         "Replacement value");

   final OptionalFloatParameter minFrequency = new OptionalFloatParameter(
         new Name("MinFrequency", "Min frequency"),
         Optional.empty(), Unit.KHZ,
         "Minimum frequency for testing against the threshold");

   final OptionalFloatParameter maxFrequency = new OptionalFloatParameter(
         new Name("MaxFrequency", "Max frequency"),
         Optional.empty(), Unit.KHZ,
         "Maximum frequency for testing against the threshold");

   public ThresholdAllChannelsModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            criterion,
            threshold,
            replacement,
            minFrequency,
            maxFrequency
      );
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new ThresholdAllChannelsModuleComputation(this, computationContext, pingSource);
   }
}
