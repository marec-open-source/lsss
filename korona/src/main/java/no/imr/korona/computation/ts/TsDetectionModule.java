package no.imr.korona.computation.ts;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.util.ts.PeakTSDetector;
import no.imr.korona.util.ts.SedTSDetector;
import no.imr.korona.util.ts.TSDetector;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ObjectParameterValue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class TsDetectionModule extends ConcurrentPingModule {
   public final ObjectParameter<DetectorType> detectorType = new ObjectParameter<>(
         new Name("DetectorType", "Detector type"),
         DetectorType.SED, DetectorType.values(),
         "Type of TS detector");

   public final FloatParameter minTS = new FloatParameter(
         new Name("MinTS", "Min TS"),
         -66, Unit.DB,
         "Minimum TS value to be detected");

   public final FloatParameter pulseLengthDeterminationLevel = new FloatParameter(
         new Name("PulseLengthDeterminationLevel", "Pulse length determination level"),
         6, Unit.DB,
         "Pulse length determination level");

   public final FloatParameter minEchoLength = new FloatParameter(
         new Name("MinEchoLength", "Min echo length"),
         0.01f, Unit.DIMENSIONLESS,
         "Minimum echo length (relative to pulse length).   NB: may be << 1 for pulse compressed data!");

   public final FloatParameter maxEchoLength = new FloatParameter(
         new Name("MaxEchoLength", "Max echo length"),
         1.8f, Unit.DIMENSIONLESS,
         "Maximum echo length (relative to pulse length).   NB: may be << 1 for pulse compressed data!");

   public final FloatParameter maxGainCompensation = new FloatParameter(
         new Name("MaxGainCompensation", "Max gain compensation"),
         6, Unit.DB,
         "Maximum (one way) gain compensation");

   public final BooleanParameter doPhaseDeviationCheck = new BooleanParameter(
         new Name("DoPhaseDeviationCheck", "Do phase deviation check"),
         true,
         "Check this if phase deviation check should be performed");

   public final FloatParameter maxPhaseDevPhaseSteps = new FloatParameter(
         new Name("MaxPhaseDevSteps", "Max phase deviation"),
         8, new Unit("phase steps"),
         "Max phase deviation");

   public final OptionalFloatParameter maxDepth = new OptionalFloatParameter(
         new Name("MaxDepth", "Max depth"),
         Optional.empty(), Unit.METER,
         "Maximal depth of targets to detect");

   public TsDetectionModule() {
      doPhaseDeviationCheck.addListenerAndNotify(maxPhaseDevPhaseSteps::setEnabled);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            detectorType,
            minTS,
            pulseLengthDeterminationLevel,
            minEchoLength,
            maxEchoLength,
            maxGainCompensation,
            doPhaseDeviationCheck,
            maxPhaseDevPhaseSteps,
            maxDepth
      );
   }

   @Override
   public List<Name> getOptionalConfigFileServiceNames() {
      return List.of(TransducerRangesFileService.NAME);
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new TsDetectionModuleComputation(this, computationContext, pingSource);
   }

   TSDetector createTSDetector() {
      return switch (detectorType.getValue()) {
         case SED -> {
            yield new SedTSDetector(minTS.getFloatValue(), maxGainCompensation.getFloatValue(), pulseLengthDeterminationLevel.getFloatValue(),
                  minEchoLength.getFloatValue(), maxEchoLength.getFloatValue(), doPhaseDeviationCheck.getBooleanValue(), maxPhaseDevPhaseSteps.getFloatValue(),
                  maxDepth.getValue().orElse(Float.POSITIVE_INFINITY));
         }
         case PEAK -> {
            yield new PeakTSDetector(minTS.getFloatValue(), maxGainCompensation.getFloatValue(), pulseLengthDeterminationLevel.getFloatValue(),
                  minEchoLength.getFloatValue(), maxEchoLength.getFloatValue(), doPhaseDeviationCheck.getBooleanValue(), maxPhaseDevPhaseSteps.getFloatValue(),
                  maxDepth.getValue().orElse(Float.POSITIVE_INFINITY));
         }
      };
   }

   public enum DetectorType implements ObjectParameterValue {
      SED("SED", "Single echo detection"),
      PEAK("Peak", "Detection using peaks");

      private final String label;
      private final String tooltip;

      DetectorType(String label, String tooltip) {
         this.label = label;
         this.tooltip = tooltip;
      }

      @Override
      public String getDisplayLabel() {
         return label;
      }

      @Override
      public String getTooltip() {
         return tooltip;
      }
   }
}
