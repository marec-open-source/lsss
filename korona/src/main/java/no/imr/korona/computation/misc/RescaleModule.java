package no.imr.korona.computation.misc;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.SimplePingModule;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.io.IOException;
import java.util.List;

/**
 * Rescales some channels to fit into the logarithmic Sv range.
 */
public final class RescaleModule extends SimplePingModule {
   static final String RESCALE_ALL = "all";
   static final String RESCALE_HALF = "half";
   static final String RESCALE_LAST = "last";
   static final String RESCALE_NONE = "none";
   private static final List<String> RESCALE_OPTIONS = List.of(
         RESCALE_ALL, RESCALE_HALF, RESCALE_LAST, RESCALE_NONE);

   public final ObjectParameter<String> rescaleChannels = new ObjectParameter<>(
         new Name("RescaleChannels", "Rescale channels"),
         RESCALE_ALL, RESCALE_OPTIONS,
         "Which channels to rescale");

   public final FloatParameter desiredMinimum = new FloatParameter(
         new Name("DesiredMinimum", "Desired minimum"),
         -85, Unit.DB,
         "Desired minimum");

   public final FloatParameter desiredMaximum = new FloatParameter(
         new Name("DesiredMaximum", "Desired maximum"),
         -30, Unit.DB,
         "Desired maximum");

   public final FloatParameter minMaxFraction = new FloatParameter(
         new Name("MinMaxFraction", "Min max fraction"),
         0.05f, Unit.DIMENSIONLESS, ValueConstraints.gteLte(0f, 1f),
         "Discard this fraction of lower and upper values when deciding the rescaling");

   public final IntParameter initializationDatagramCount = new IntParameter(
         new Name("InitializationDatagramCount", "Initialization datagram count"),
         10, Unit.COUNT, ValueConstraints.gte(1),
         "Number of datagrams from each channel used to determine rescaling factors");

   public final IntParameter initializationMaxPingCount = new IntParameter(
         new Name("InitializationMaxPingCount", "Initialization max ping count"),
         100, Unit.COUNT, ValueConstraints.gte(1),
         "Maximum number of pings to buffer up when finding rescaling factors");

   public RescaleModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            rescaleChannels,
            desiredMinimum,
            desiredMaximum,
            minMaxFraction,
            initializationDatagramCount,
            initializationMaxPingCount
      );
   }

   @Override
   public SimplePingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new RescaleModuleComputation(this, computationContext, pingSource);
   }
}
