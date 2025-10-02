package no.imr.korona.computation.datareduction;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.Unit;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Removes data below configured transducer range.
 */
public final class DataReductionModule extends ConcurrentPingModule {
   private final HeaderParameter upperLimitHeader = new HeaderParameter("Upper limit");

   public final BooleanParameter useTransducerBlindZone = new BooleanParameter(
         new Name("BlindZone", "Blind zone"),
         false,
         "Use the blind zone configured in the Transducer ranges file");

   public final OptionalFloatParameter minRange = new OptionalFloatParameter(
         new Name("MinRange", "Min range"),
         Optional.empty(), Unit.METER);

   public final OptionalFloatParameter minDepth = new OptionalFloatParameter(
         new Name("MinDepth", "Min depth"),
         Optional.empty(), Unit.METER);

   private final HeaderParameter lowerLimitHeader = new HeaderParameter("Lower limit");

   public final BooleanParameter useTransducerRange = new BooleanParameter(
         new Name("TransducerRange", "Transducer range"),
         true,
         "Use the range configured in the Transducer ranges file");

   public final OptionalFloatParameter maxRange = new OptionalFloatParameter(
         new Name("MaxRange", "Max range"),
         Optional.empty(), Unit.METER);

   public final OptionalFloatParameter maxDepth = new OptionalFloatParameter(
         new Name("MaxDepth", "Max depth"),
         Optional.empty(), Unit.METER);

   public final OptionalFloatParameter maxBelowBottom = new OptionalFloatParameter(
         new Name("MaxBelowBottom", "Max below bottom"),
         Optional.empty(), Unit.METER,
         "Distance below the detected coordinated bottom");

   public DataReductionModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            upperLimitHeader,
            useTransducerBlindZone,
            minRange,
            minDepth,
            //---
            lowerLimitHeader,
            useTransducerRange,
            maxRange,
            maxDepth,
            maxBelowBottom
      );
   }

   @Override
   public List<Name> getOptionalConfigFileServiceNames() {
      return List.of(TransducerRangesFileService.NAME);
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new DataReductionModuleComputation(this, computationContext, pingSource);
   }
}
