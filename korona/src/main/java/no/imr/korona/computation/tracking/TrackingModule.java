package no.imr.korona.computation.tracking;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.computation.tracking.impl.AlphaBetaEstimator;
import no.imr.korona.computation.tracking.impl.SimpleGateFunction;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.ButtonParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.MultiParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ObjectParameterValue;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class TrackingModule extends GeneralPingModule {
   private static final float INITIAL_VALUE_TS_MIN = -66;
   private static final float INITIAL_VALUE_PULSE_DET_SED = 6;
   private static final float INITIAL_VALUE_PULSE_DET_PEAK = 6;
   private static final float INITIAL_VALUE_MIN_ECHO_SED = 0.01f;
   private static final float INITIAL_VALUE_MIN_ECHO_PEAK = 0;
   private static final float INITIAL_VALUE_MAX_ECHO_SED = 1.8f;
   private static final float INITIAL_VALUE_MAX_ECHO_PEAK = 1;
   private static final float INITIAL_VALUE_MAX_GAIN_COMP = 6;
   private static final boolean INITIAL_VALUE_DO_PHASE_CHECK_SED = true;
   private static final boolean INITIAL_VALUE_DO_PHASE_CHECK_PEAK = false;
   private static final float INITIAL_VALUE_MAX_PHASE_DEV = 8;

   public final ObjectParameter<TrackerType> targetTrackerType = new ObjectParameter<>(
         new Name("TrackerType", "Tracker type"),
         TrackerType.AGGREGATION, TrackerType.values(),
         "Type of tracker");

   public final IntParameter kHz = new IntParameter(
         new Name("kHz", "Frequency"),
         38, Unit.KHZ);

   private final HeaderParameter platformHeader = new HeaderParameter("Platform settings");

   public final ObjectParameter<PlatformMotionType> platformMotionType = new ObjectParameter<>(
         new Name("PlatformMotionType", "Platform motion type"),
         PlatformMotionType.FLOATING, PlatformMotionType.values(),
         "Type of platform motion");

   private final HeaderParameter targetDetectorHeader = new HeaderParameter("Target detector settings");

   public final FloatParameter minTS = new FloatParameter(
         new Name("MinTS", "Min TS"),
         INITIAL_VALUE_TS_MIN, Unit.DB,
         "Minimum TS value to be detected (i.e. minimum corrected TS)");

   public final FloatParameter pulseLengthDeterminationLevel = new FloatParameter(
         new Name("PulseLengthDeterminationLevel", "Pulse length determination level"),
         INITIAL_VALUE_PULSE_DET_SED, Unit.DB,
         "Pulse length determination level");

   public final FloatParameter minEchoLength = new FloatParameter(
         new Name("MinEchoLength", "Min echo length"),
         INITIAL_VALUE_MIN_ECHO_SED, Unit.DIMENSIONLESS,
         "Minimum echo length (relative to nominal length).   NB: may be << 1 for pulse compressed data!");

   public final FloatParameter maxEchoLength = new FloatParameter(
         new Name("MaxEchoLength", "Max echo length"),
         INITIAL_VALUE_MAX_ECHO_SED, Unit.DIMENSIONLESS,
         "Maximum echo length (relative to nominal length).   NB: may be << 1 for pulse compressed data!");

   public final FloatParameter maxGainCompensation = new FloatParameter(
         new Name("MaxGainCompensation", "Max gain compensation"),
         INITIAL_VALUE_MAX_GAIN_COMP, Unit.DB,
         "Maximum (one way) gain compensation");

   public final BooleanParameter doPhaseDeviationCheck = new BooleanParameter(
         new Name("DoPhaseDeviationCheck", "Do phase deviation check"),
         INITIAL_VALUE_DO_PHASE_CHECK_SED,
         "Check this if phase deviation check should be performed");

   public final FloatParameter maxPhaseDevPhaseSteps = new FloatParameter(
         new Name("MaxPhaseDevSteps", "Max phase deviation"),
         INITIAL_VALUE_MAX_PHASE_DEV, new Unit("phase steps"),
         "Max phase deviation");

   public final ButtonParameter presetButton = new ButtonParameter(
         new Name("SetPresets", "Set presets"),
         "Update parameters with presets for chosen tracker type",
         () -> {
            minTS.setValue(INITIAL_VALUE_TS_MIN);
            maxGainCompensation.setValue(INITIAL_VALUE_MAX_GAIN_COMP);
            maxPhaseDevPhaseSteps.setValue(INITIAL_VALUE_MAX_PHASE_DEV);

            switch (targetTrackerType.getValue()) {
               case AGGREGATION, TS_MODULE -> {
               }
               case SED -> {
                  pulseLengthDeterminationLevel.setValue(INITIAL_VALUE_PULSE_DET_SED);
                  minEchoLength.setValue(INITIAL_VALUE_MIN_ECHO_SED);
                  maxEchoLength.setValue(INITIAL_VALUE_MAX_ECHO_SED);
                  doPhaseDeviationCheck.setValue(INITIAL_VALUE_DO_PHASE_CHECK_SED);
               }
               case PEAK -> {
                  pulseLengthDeterminationLevel.setValue(INITIAL_VALUE_PULSE_DET_PEAK);
                  minEchoLength.setValue(INITIAL_VALUE_MIN_ECHO_PEAK);
                  maxEchoLength.setValue(INITIAL_VALUE_MAX_ECHO_PEAK);
                  doPhaseDeviationCheck.setValue(INITIAL_VALUE_DO_PHASE_CHECK_PEAK);
               }
            }
         });

   private final HeaderParameter targetFilteringHeader = new HeaderParameter("Target filtering settings");

   public final FloatParameter maxTS = new FloatParameter(
         new Name("MaxTS", "Max TS"),
         0, Unit.DB,
         "Maximum TS value to be tracked (i.e. maximum corrected TS)");

   public final OptionalFloatParameter maxDepth = new OptionalFloatParameter(
         new Name("MaxDepth", "Max depth"),
         Optional.empty(), Unit.METER,
         "Maximal depth of targets to detect");

   public final FloatParameter maxAlongshipAngle = new FloatParameter(
         new Name("MaxAlongshipAngle", "Max alongship angle"),
         10, Unit.DEGREES,
         "Maximum alongship angle for tracked targets");

   public final FloatParameter maxAthwartshipAngle = new FloatParameter(
         new Name("MaxAthwartshipAngle", "Max athwartship angle"),
         10, Unit.DEGREES,
         "Maximum athwartship angle for tracked targets");

   private final HeaderParameter trackInitializationHeader = new HeaderParameter("Track initialisation settings");

   public final GateFunctionParameter initiationGateFunction = new GateFunctionParameter(
         new Name("InitiationGateFunction", "Initiation gate function"), "0");

   public final IntParameter initiationMinLength = new IntParameter(
         new Name("InitiationMinLength", "Initiation min length"),
         1, Unit.COUNT, ValueConstraints.gte(1),
         "<html>N<sub>0</sub> - Minimum number of samples for starting a new track");

   private final HeaderParameter trackAssociationHeader = new HeaderParameter("Track association settings");

   public final GateFunctionParameter gateFunction = new GateFunctionParameter(
         new Name("GateFunction", "Gate function"), "G");
   public final AlphaBetaEstimatorParameter alphaBetaEstimator = new AlphaBetaEstimatorParameter(
         new Name("AlphaBetaEstimator", "Alpha beta estimator"));

   public final IntParameter maxMissingPings = new IntParameter(
         new Name("MaxMissingPings", "Max missing pings"),
         4, Unit.COUNT, ValueConstraints.gte(0),
         "<html>N<sub>m</sub> - Maximum number of subsequent missing pings in a track");

   public final IntParameter maxMissingSamples = new IntParameter(
         new Name("MaxMissingSamples", "Max missing samples"),
         2, Unit.COUNT, ValueConstraints.gte(0),
         "<html>N<sub>e</sub> - Maximum number of missing samples in one ping of a track");

   private final HeaderParameter trackAcceptanceHeader = new HeaderParameter("Track acceptance settings");

   public final FloatParameter maxMissingPingsFraction = new FloatParameter(
         new Name("MaxMissingPingsFraction", "Max missing pings fraction"),
         0.5f, Unit.DIMENSIONLESS, ValueConstraints.gte(0f),
         "MN - Maximum acceptable ratio of missing pings to number of pings in track");

   public final IntParameter minTrackLength = new IntParameter(
         new Name("MinTrackLength", "Min track length"),
         8, Unit.COUNT, ValueConstraints.gte(1),
         "TL - Minimum acceptable total number of pings in track");

   public final FloatParameter minSampleToLengthFraction = new FloatParameter(
         new Name("MinSampleToLengthFraction", "Min sample to length fraction"),
         2, Unit.DIMENSIONLESS, ValueConstraints.gte(0f),
         "NL - Minimum acceptable ratio of total number of samples to number of pings in track");

   public TrackingModule() {
      doPhaseDeviationCheck.addListenerAndNotify(maxPhaseDevPhaseSteps::setEnabled);
      targetTrackerType.addListenerAndNotify(this::updateParameterVisibility);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            targetTrackerType,
            kHz,
            //---
            platformHeader,
            platformMotionType,
            //---
            targetDetectorHeader,
            minTS,
            pulseLengthDeterminationLevel,
            minEchoLength,
            maxEchoLength,
            maxGainCompensation,
            doPhaseDeviationCheck,
            maxPhaseDevPhaseSteps,
            presetButton,
            //---
            targetFilteringHeader,
            maxTS,
            maxDepth,
            maxAlongshipAngle,
            maxAthwartshipAngle,
            //---
            trackInitializationHeader,
            initiationGateFunction,
            initiationMinLength,
            //---
            trackAssociationHeader,
            gateFunction,
            alphaBetaEstimator,
            maxMissingPings,
            maxMissingSamples,
            //---
            trackAcceptanceHeader,
            maxMissingPingsFraction,
            minTrackLength,
            minSampleToLengthFraction
      );
   }

   private void updateParameterVisibility(TrackerType trackerType) {
      boolean aggregation = trackerType == TrackerType.AGGREGATION;
      boolean tsDetector = trackerType == TrackerType.SED || trackerType == TrackerType.PEAK;
      boolean tsModule = trackerType == TrackerType.TS_MODULE;

      // Target detector:
      targetDetectorHeader.setVisible(!tsModule);
      minTS.setVisible(!tsModule);
      pulseLengthDeterminationLevel.setVisible(tsDetector);
      minEchoLength.setVisible(tsDetector);
      maxEchoLength.setVisible(tsDetector);
      maxGainCompensation.setVisible(!tsModule);
      doPhaseDeviationCheck.setVisible(tsDetector);
      maxPhaseDevPhaseSteps.setVisible(tsDetector);
      presetButton.setVisible(!tsModule);

      // Target filtering:
      targetFilteringHeader.setVisible(!tsModule);
      maxTS.setVisible(!tsModule);
      maxDepth.setVisible(!tsModule);
      maxAlongshipAngle.setVisible(!tsModule);
      maxAthwartshipAngle.setVisible(!tsModule);

      // Track initialization:
      trackInitializationHeader.setVisible(aggregation);
      initiationGateFunction.setVisible(aggregation);
      initiationMinLength.setVisible(aggregation);

      // Track association settings:
      maxMissingSamples.setVisible(aggregation);
   }

   @Override
   public List<Name> getRequiredConfigFileServiceNames() {
      return List.of(TransducerRangesFileService.NAME);
   }

   @Override
   public @Nullable GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      RawFileConfiguration rawFileConfiguration = pingConfiguration.getRawFileConfiguration();
      int channel = rawFileConfiguration.lastChannelWithKHz(kHz.getIntValue());
      if (channel <= 0) {
         Log.global.warning("Frequency not available: " + kHz.getIntValue() + " kHz");
         return null;
      }
      return new TrackingModuleComputation(this, computationContext, pingSource, channel);
   }

   enum TrackerType implements ObjectParameterValue {
      AGGREGATION("Aggregation", "Aggregation", "Aggregating samples stronger than MinTS"),
      SED("SED", "SED", "Single echo detection"),
      PEAK("Peak", "Peak", "Peak detection"),
      TS_MODULE("TSModule", "TS detection module (KORONA)", "Use results from the TS detection module");

      private final String id;
      private final String label;
      private final String tooltip;

      TrackerType(String id, String label, String tooltip) {
         this.id = id;
         this.label = label;
         this.tooltip = tooltip;
      }

      @Override
      public String toString() {
         return id;
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

   enum PlatformMotionType implements ObjectParameterValue {
      STATIONARY("Stationary", "Tracking is done in the transducer coordinate system"),
      FLOATING("Floating", "Correction for heading, pitch, roll and heave"),
      MOVING("Moving", "Correction for heading, pitch, roll and heave, and also geographical position");

      private final String id;
      private final String tooltip;

      PlatformMotionType(String id, String tooltip) {
         this.id = id;
         this.tooltip = tooltip;
      }

      @Override
      public String toString() {
         return id;
      }

      @Override
      public String getTooltip() {
         return tooltip;
      }
   }

   static final class GateFunctionParameter extends MultiParameter<FloatParameter> {
      private final FloatParameter alpha;
      private final FloatParameter beta;
      private final FloatParameter range;
      private final FloatParameter ts;

      private GateFunctionParameter(Name name, String subScript) {
         super(name);

         String sub = "<sub>" + subScript + "</sub>";
         alpha = new FloatParameter(new Name("Alpha", "<html>&alpha;" + sub),
               2.8f, Unit.DEGREES);

         beta = new FloatParameter(new Name("Beta", "<html>&beta;" + sub),
               2.8f, Unit.DEGREES);

         range = new FloatParameter(new Name("Range", "<html>r" + sub),
               0.1f, Unit.METER);

         ts = new FloatParameter(new Name("TS", "<html>I" + sub),
               20, Unit.DB,
               "dB");

         addParameter(alpha);
         addParameter(beta);
         addParameter(range);
         addParameter(ts);
      }

      GateFunction create() {
         return new SimpleGateFunction(range.getFloatValue(), (float) Math.toRadians(alpha.getFloatValue()),
               (float) Math.toRadians(beta.getFloatValue()), ts.getFloatValue());
      }
   }

   static final class AlphaBetaEstimatorParameter extends MultiParameter<FloatParameter> {
      private final FloatParameter alpha = new FloatParameter(new Name("Alpha"),
            0.5f, Unit.DIMENSIONLESS, ValueConstraints.gteLte(0f, 1f));

      private final FloatParameter beta = new FloatParameter(new Name("Beta"),
            0.5f, Unit.DIMENSIONLESS, ValueConstraints.gteLte(0f, 1f));

      private AlphaBetaEstimatorParameter(Name name) {
         super(name);

         addParameter(alpha);
         addParameter(beta);
      }

      Estimator create() {
         return new AlphaBetaEstimator(alpha.getFloatValue(), beta.getFloatValue());
      }
   }
}
