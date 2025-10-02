package no.imr.lsss.framework.config.survey.misc;

import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.incubator.KoronaIncubatorFeatureToggles;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.korona.viewer.variables.categorization.ProbabilityVariable;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.lsss.framework.config.survey.misc.ices.IcesConf;
import no.imr.tools.listening.Listener;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.ButtonParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntCsvListParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.SeparatorParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.List;

/**
 * Various survey-specific configuration.
 */
public final class SurveyMiscConf extends ConfigurationUnit {
   public final BooleanParameter pelagicMode = new BooleanParameter(
         new Name("PelagicMode", "Pelagic mode"),
         false,
         "(Not selected = bottom mode)");

   public final ObjectParameter<PingMapping> pingMapping = new ObjectParameter<>(
         new Name("PingMapping", "Ping mapping"),
         PingMapping.DISTANCE, PingMapping.values());

   private final SeparatorParameter separatorSeabedMounted = SeparatorParameter.space();

   public final BooleanParameter seabedMounted = new BooleanParameter(
         new Name("SeabedMounted", "Seabed mounted echosounder"),
         false);

   public final FloatParameter seabedMountedDistanceToSeabed = new FloatParameter(
         new Name("SeabedMountedDistanceToSeabed", "Seabed mounted echosounder: Distance to seabed"),
         0, Unit.METER, ValueConstraints.gte(0f));

   public final FloatParameter seabedMountedDistanceToSurface = new FloatParameter(
         new Name("SeabedMountedDistanceToSurface", "Seabed mounted echosounder: Distance to surface"),
         500, Unit.METER, ValueConstraints.gte(0f));

   private final SeparatorParameter separatorMainFrequency = SeparatorParameter.space();

   public final FloatParameter mainFrequency = new FloatParameter(
         new Name("MainFrequency", "Main frequency (Used in Map, FR, FileList)"),
         38000, Unit.HZ, ValueConstraints.gt(0f));

   private final SeparatorParameter separatorOffsets = SeparatorParameter.space();

   public final FloatParameter topBoundaryOffset = new FloatParameter(
         new Name("TopBoundaryOffset", "Depth of initial top boundary"),
         15, Unit.METER, ValueConstraints.gte(0f));

   public final FloatParameter bottomBoundaryOffset = new FloatParameter(
         new Name("BottomBoundaryOffset", "Offset from lower integration line to \"detected\" bottom"),
         0.5f, Unit.METER, ValueConstraints.gte(0f));

   private final SeparatorParameter separatorCoordinatedBottom = SeparatorParameter.space();

   public final FloatParameter minFrequencyForBottom = new FloatParameter(
         new Name("MinFrequencyForBottom", "Min frequency for finding coordinated bottom"),
         10000, Unit.HZ, ValueConstraints.gt(0f));

   public final FloatParameter maxFrequencyForBottom = new FloatParameter(
         new Name("MaxFrequencyForBottom", "Max frequency for finding coordinated bottom"),
         200000, Unit.HZ, ValueConstraints.gt(0f));

   public final FloatParameter minimumDepthThresholdFactor = new FloatParameter(
         new Name("MinimumDepthThresholdFactor", "Shallowest acceptable depth relative to max depth"),
         0.99f, Unit.DIMENSIONLESS, ValueConstraints.gt(0f));

   private final SeparatorParameter separatorMinimumThreshold = SeparatorParameter.space();

   public final IntParameter preferredUpperThreshold = new IntParameter(
         new Name("PreferredUpperThreshold", "Preferred upper threshold"),
         -42, Unit.DB);

   public final IntParameter preferredLowerThreshold = new IntParameter(
         new Name("PreferredLowerThreshold", "Preferred lower threshold"),
         -82, Unit.DB);

   public final IntCsvListParameter additionalLowerThresholds = new IntCsvListParameter(
         new Name("AdditionalLowerThresholds", "Additional lower thresholds"),
         List.of(-60), Unit.DB,
         "Comma-separated list");

   private final SeparatorParameter separatorShowWarning = SeparatorParameter.space();

   public final BooleanParameter showWarningIfNotUsingPreferredLowerThreshold = new BooleanParameter(
         new Name("ShowWarningIfNotUsingPreferredLowerThreshold", "Show warning if not using preferred lower threshold"),
         false,
         "If the echogram is not using only the preferred lower threshold,"
               + " a warning is shown next to the \"Store\" button in the Interpretation module");

   public final BooleanParameter showWarningIfVerticallyZoomed = new BooleanParameter(
         new Name("ShowWarningIfVerticallyZoomed", "Show warning if vertically zoomed"),
         false,
         "If the echogram is vertically zoomed,"
               + " a warning is shown next to the \"Store\" button in the Interpretation module");

   private final SeparatorParameter separatorCategorization = SeparatorParameter.line();

   public final BooleanParameter useSchoolCategorization = new BooleanParameter(
         new Name("UseSchoolCategorization", "Use school categorization in interpretation"),
         true);

   public final BooleanParameter useTrackCategorization = new BooleanParameter(
         new Name("UseTrackCategorization", "Use track categorization in interpretation"),
         true);

   public final FloatParameter probabilityThreshold = new FloatParameter(
         new Name("ProbabilityThreshold", "Minimum value for categorization probability/similarity"),
         0, Unit.DIMENSIONLESS, ValueConstraints.gteLte(0f, 1f));

   private final SeparatorParameter separatorSchools = SeparatorParameter.line();

   public final FloatParameter schoolGrowSmoothing = new FloatParameter(
         new Name("SchoolGrowSmoothing", "School grow smoothing"),
         0, Unit.METER,
         "Schools grown by clicking in echogram are smoothed by scaling up and down with this amount");

   public final BooleanParameter computeSchoolParameters = new BooleanParameter(
         new Name("ComputeSchoolParameters", "Compute school parameters"),
         false);

   public final ButtonParameter recomputeSchoolParameters = new ButtonParameter(
         new Name("RecomputeSchoolParameters", "Recompute"),
         "Recomputes all school parameters for all schools");

   private final IcesConf icesConf;

   public SurveyMiscConf(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("SurveyMiscConf", "Miscellaneous"),
            "Miscellaneous settings for this survey");

      icesConf = addSubConfigurationUnit(new IcesConf(plugin));

      seabedMounted.addListenerAndNotify(value -> {
         seabedMountedDistanceToSeabed.setEnabled(value);
         seabedMountedDistanceToSurface.setEnabled(value);
      });

      preferredUpperThreshold.subscribe(value -> preferredLowerThreshold.setIntValue(Math.min(preferredLowerThreshold.getIntValue(), value)));
      preferredLowerThreshold.subscribe(value -> preferredUpperThreshold.setIntValue(Math.max(preferredUpperThreshold.getIntValue(), value)));

      useTrackCategorization.setVisible(KoronaIncubatorFeatureToggles.USE_TRACK_CATEGORIZATION);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            pelagicMode,
            pingMapping,
            separatorSeabedMounted,
            seabedMounted,
            seabedMountedDistanceToSeabed,
            seabedMountedDistanceToSurface,
            separatorMainFrequency,
            mainFrequency,
            separatorOffsets,
            topBoundaryOffset,
            bottomBoundaryOffset,
            separatorCoordinatedBottom,
            minFrequencyForBottom,
            maxFrequencyForBottom,
            minimumDepthThresholdFactor,
            separatorMinimumThreshold,
            preferredUpperThreshold,
            preferredLowerThreshold,
            additionalLowerThresholds,
            separatorShowWarning,
            showWarningIfNotUsingPreferredLowerThreshold,
            showWarningIfVerticallyZoomed,
            //---
            separatorCategorization,
            useSchoolCategorization,
            useTrackCategorization,
            probabilityThreshold,
            //---
            separatorSchools,
            schoolGrowSmoothing,
            computeSchoolParameters,
            recomputeSchoolParameters
      );
   }

   public IcesConf getIcesConf() {
      return icesConf;
   }

   @Override
   public void setup() {
      super.setup();

      Listener.of(getLSSS().getInterpretationSettings()::recompute).addTo(
            pelagicMode,
            seabedMounted,
            seabedMountedDistanceToSeabed,
            seabedMountedDistanceToSurface
      );

      getLSSS().getInterpretationSettings().getPingMappingChangeManager().addListener(pingMapping);
      pingMapping.subscribe(getLSSS().getInterpretationSettings()::setPingMapping);

      ProbabilityVariable probabilityVariable = getLSSS().getInterpretationSettings().getColorConverterContainer().getContinuousVariable(ProbabilityVariable.class);
      ContinuousVariableSettings probabilitySettings = probabilityVariable.getSettings();
      probabilityThreshold.addListenerAndNotify(probabilitySettings::setMin);
      probabilitySettings.getChangeManager().addListener(() -> probabilityThreshold.setFloatValue(probabilitySettings.getRange().min()));
   }

   @Override
   protected UserProfile getMinimumUserProfileForEditing(BaseParameter<?> parameter) {
      if (parameter == pelagicMode || parameter == pingMapping) {
         return UserProfile.NORMAL_USE;
      }
      return UserProfile.SURVEY_SETUP;
   }
}
