package no.imr.korona.computation.misc;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.SimplePingModule;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.IntCsvListParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Bottom depth detection module.
 */
public final class DepthModule extends SimplePingModule {
   public enum Algorithm {
      EK500, Gradient, Threshold, Pelagic
   }

   public final ObjectParameter<Algorithm> algorithm = new ObjectParameter<>(
         new Name("Algorithm"),
         Algorithm.EK500, Algorithm.values(),
         "The algorithm to be used in the bottom calculation");

   public final FloatParameter minDepthLimit = new FloatParameter(
         new Name("MinDepthLimit", "Min depth limit"),
         10, Unit.METER, ValueConstraints.gte(0f),
         "The maximal distance above bottom accepted as the minimal bottom distance");

   public final FloatParameter minDepthValueFraction = new FloatParameter(
         new Name("MinDepthValueFraction", "Min depth value fraction = \"Backstep\""),
         0.001f, Unit.DIMENSIONLESS, ValueConstraints.gt(0f),
         "A fraction of the maximal bottom echo strength. Used in the minimal bottom calculation");

   public final FloatParameter signalStrengthThreshold = new FloatParameter(
         new Name("SignalStrengthThreshold", "Signal strength threshold"),
         -31, Unit.DB,
         "The detected bottom must have signal strength larger than this value");

   public final FloatParameter minimumDepthThresholdFactor = new FloatParameter(
         new Name("MinimumDepthThresholdFactor", "Minimum depth threshold factor"),
         0.99f, Unit.DIMENSIONLESS, ValueConstraints.gte(0f),
         "Shallowest acceptable depth relative to max depth");

   public final FloatParameter maxRangeFactor = new FloatParameter(
         new Name("MaxRangeFactor", "Max range factor"),
         1.5f, Unit.DIMENSIONLESS, ValueConstraints.gt(0f),
         "Factor to multiply with given transducer ranges for ranges used in depth detection");

   public final BooleanParameter forceDetection = new BooleanParameter(
         new Name("AlwaysDetectBottom", "Always detect bottom"),
         true,
         "Always try to detect bottom even if echosounder has zero bottom at all frequencies");

   public final BooleanParameter keepBottomDeeperThanData = new BooleanParameter(
         new Name("KeepBottomDeeperThanData", "Keep bottom deeper than data"),
         false,
         "Keep echosounder bottom if it is deeper than available data");

   public final FloatParameter minBottomDepth = new FloatParameter(
         new Name("MinBottomDepth", "Minimum bottom depth"),
         0, Unit.METER, ValueConstraints.gte(0f),
         "The detected bottom must be minimum this value");

   public final FloatParameter maxBottomDepth = new FloatParameter(
         new Name("MaxBottomDepth", "Maximum bottom depth"),
         9999, Unit.METER, ValueConstraints.gte(0f),
         "The detected bottom must be maximum this value");

   private final HeaderParameter coordinatedBottomHeader = new HeaderParameter(
         "To be used in subsequent KORONA modules to find coordinated bottom");

   public final OptionalFloatParameter preferredKHz = new OptionalFloatParameter(
         new Name("PreferredKHz", "Preferred kHz"),
         Optional.empty(), Unit.KHZ,
         "Preferred frequency for finding coordinated bottom");

   public final FloatParameter minKHz = new FloatParameter(
         new Name("MinKHz", "Minimum kHz"),
         0, Unit.KHZ,
         "Only use channels with at least this frequency");

   public final FloatParameter maxKHz = new FloatParameter(
         new Name("MaxKHz", "Maximum kHz"),
         9999, Unit.KHZ,
         "Only use channels with at most this frequency");

   public final IntCsvListParameter doNotUseKHz = new IntCsvListParameter(
         new Name("DoNotUseKHz", "Do not use kHz"),
         List.of(), Unit.KHZ,
         "Comma-separated list of frequencies to not use");

   public DepthModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            algorithm,
            minDepthLimit,
            minDepthValueFraction,
            signalStrengthThreshold,
            minimumDepthThresholdFactor,
            maxRangeFactor,
            forceDetection,
            keepBottomDeeperThanData,
            minBottomDepth,
            maxBottomDepth,
            //---
            coordinatedBottomHeader,
            preferredKHz,
            minKHz,
            maxKHz,
            doNotUseKHz
      );
   }

   @Override
   public List<Name> getRequiredConfigFileServiceNames() {
      return List.of(TransducerRangesFileService.NAME);
   }

   @Override
   public SimplePingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new DepthModuleComputation(this, computationContext, pingSource);
   }
}
