package no.imr.korona.computation.plankton;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.computation.categorization.CategorizationFileService;
import no.imr.korona.computation.categorization.CategorizationModule;
import no.imr.korona.computation.categorization.Category;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.computation.filters.SpikeFilterModule;
import no.imr.korona.computation.offset.TransducerParameterManager;
import no.imr.korona.computation.offset.TransducerParameters;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.computation.plankton.models.BackscatterModel;
import no.imr.korona.computation.plankton.models.FluidBentCylinderModel;
import no.imr.korona.computation.plankton.models.FluidProlateSpheroidModel;
import no.imr.korona.computation.plankton.models.GaseousSphereModel;
import no.imr.korona.computation.plankton.models.HardShelledSphereModel;
import no.imr.korona.computation.plankton.models.SDWBAModel;
import no.imr.korona.computation.plankton.models.SDWBAModelNew;
import no.imr.korona.data.datagrams.Pic0Datagram;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.ButtonParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.DoubleParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.MultiParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.SeparatorParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.xml.XmlUtils;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class PlanktonInversionModule extends GeneralPingModule {
   private static BooleanParameter newCategoryToggleParameter(Name name, boolean initialValue) {
      return new BooleanParameter(name, initialValue, "Perform inversion for category \"" + name.persistentName() + "\"");
   }

   private final HeaderParameter dataSettingsHeader = new HeaderParameter("Data settings");

   public final ObjectParameter<SpikeFilterModule.VerticalUnit> verticalUnit = new ObjectParameter<>(
         new Name("VerticalUnit", "Vertical unit"),
         SpikeFilterModule.VerticalUnit.SAMPLES, SpikeFilterModule.VerticalUnit.values(),
         "Unit for the height of the search window and the search columns");

   public final IntParameter pingsPerBin = new IntParameter(
         new Name("PingsPerBin", "Pings per bin"),
         7, Unit.COUNT, ValueConstraints.gt(0),
         "Pings per bin");

   public final IntParameter depthSamplesPerBin = new IntParameter(
         new Name("DepthSamplesPerBin", "Depth samples per bin"),
         30, Unit.COUNT, ValueConstraints.gt(0),
         "Bin height in number of samples");

   public final FloatParameter depthDurationPerBin = new FloatParameter(
         new Name("DepthDurationPerBin", "Depth duration per bin"),
         4, Unit.MILLISECONDS, ValueConstraints.gt(0f),
         "Bin height in milliseconds");

   public final FloatParameter depthDistancePerBin = new FloatParameter(
         new Name("DepthDistancePerBin", "Depth distance per bin"),
         6, Unit.METER, ValueConstraints.gt(0f),
         "Bin height in meters");

   public final BooleanParameter useNoiseThreshold = new BooleanParameter(
         new Name("Use global noise threshold", "Use global noise threshold"),
         true,
         "Check this if a global noise threshold should be used");

   public final FloatParameter noiseThreshold = new FloatParameter(
         new Name("Noise threshold", "Noise threshold"),
         -90, Unit.DB,
         "Global noise threshold");

   public final BooleanParameter useMinInversionDepth = new BooleanParameter(
         new Name("Use min depth", "Use min depth"),
         false,
         "Check this if min depth should be used");

   public final FloatParameter minInversionDepth = new FloatParameter(
         new Name("Min depth", "Min depth"),
         20, Unit.METER,
         "Min depth used in the processing");

   public final BooleanParameter useMaxInversionDepth = new BooleanParameter(
         new Name("Use max depth", "Use max depth"),
         false,
         "Check this if max depth should be used");

   public final FloatParameter maxInversionDepth = new FloatParameter(
         new Name("Max depth", "Max depth"),
         110, Unit.METER,
         "Max depth used in the processing");

   public final MultiParameter<BooleanParameter> categoriesUsedForInversion = new MultiParameter<>(
         new Name("CategoriesUsedForInversion", "Categories used for inversion"),
         "Which categories to perform inversion for") {
      @Override
      protected Configurable possiblyCreateNewSubConfigurable(String persistentName) {
         return addParameter(newCategoryToggleParameter(new Name(persistentName), false));
      }
   };

   public final BooleanParameter useAllFrequencies = new BooleanParameter(
         new Name("UseAllFrequencies", "Use all frequencies"),
         true,
         "If checked, all frequencies are used in the inversion");

   public final MultiParameter<FrequencyParameter> activeFrequencies = new MultiParameter<>(
         new Name("ActiveFrequencies", "Active frequencies"),
         "Which frequencies to use") {
      @Override
      protected Configurable possiblyCreateNewSubConfigurable(String persistentName) {
         return addParameter(new FrequencyParameter(new Name(persistentName, persistentName + " kHz"), false));
      }

      @Override
      public boolean isEnabled() {
         return !useAllFrequencies.getBooleanValue();
      }
   };

   private final HeaderParameter inversionHeader = new HeaderParameter("Inversion settings");

   public final DoubleParameter minResidualErrorThreshold = new DoubleParameter(
         new Name("MinResidualErrorThreshold", "Min residual error threshold"),
         0.01, Unit.NONE, ValueConstraints.gte(0.0),
         "Threshold for stopping iteration");

   public final DoubleParameter maxResidualErrorThreshold = new DoubleParameter(
         new Name("MaxResidualErrorThreshold", "Max residual error threshold"),
         0.2, Unit.NONE, ValueConstraints.gte(0.0),
         "Threshold for accepting solution");

   public final DoubleParameter levenbergMarquardtFactor = new DoubleParameter(
         new Name("LevenbergMarquardtFactor", "Levenberg-Marquardt factor"),
         5e-4, Unit.NONE, ValueConstraints.gte(0.0),
         "Trade-off between residual error and solution norm");

   public final IntParameter maxIter = new IntParameter(
         new Name("MaxIter", "Max iter"),
         4, Unit.COUNT, ValueConstraints.gte(1),
         "Maximum iteration count for each model");

   private final HeaderParameter planktonModelsHeader = new HeaderParameter("Plankton models");

   // Category number 0 defined in Pic0Datagram to "Uncategorized".
   // Category number 1 defined in Pic0Datagram to "Other".

   private final PlanktonScatterer<HardShelledSphereModel> hardShell = new PlanktonScatterer<>(
         "Hard shelled sphere (Stanton et al. '94)",
         new Pic0Datagram.PlanktonCategory("Hard shelled sphere", "Hard", 2, Color.BLUE),
         new HardShelledSphereModel());  //e.g. gastropods (limacina?)

   private final PlanktonScatterer<GaseousSphereModel> gasSphere = new PlanktonScatterer<>(
         "Gaseous sphere (Stanton et al. '94)",
         new Pic0Datagram.PlanktonCategory("Gaseous sphere", "Gas", 3, Color.GRAY),
         new GaseousSphereModel());  //e.g. siphonophores

   private final PlanktonScatterer<FluidProlateSpheroidModel> fluidSpheroid = new PlanktonScatterer<>(
         "Fluid prolate spheroid (Stanton '89)",
         new Pic0Datagram.PlanktonCategory("Fluid spheriod", "FluidS", 4, Color.MAGENTA),
         new FluidProlateSpheroidModel());  //e.g. copepods

   private final PlanktonScatterer<FluidBentCylinderModel> fluidBentCyl = new PlanktonScatterer<>(
         "Fluid bent cylinder (Stanton et al. '94)",
         new Pic0Datagram.PlanktonCategory("Fluid bent cylinder", "FBCyl", 5, Color.ORANGE),
         new FluidBentCylinderModel());  //e.g. euphausiids

   //Expandes by Rolf 2009.01.23

   private final PlanktonScatterer<FluidBentCylinderModel> fluidBentCyl2 = new PlanktonScatterer<>(
         "Fluid bent cylinder (Stanton et al. '94) (2)",
         new Pic0Datagram.PlanktonCategory("Fluid bent cylinder 2", "FBCyl2", 6, Color.GREEN),
         new FluidBentCylinderModel());  //e.g. euphausiids or copepods in addition to euphausiids in FBCyl above

   private final PlanktonScatterer<SDWBAModelNew> sdwba = new PlanktonScatterer<>(
         "SDWBA (Demer and Conti '05; Conti '06 - Renfree (not published))",
         new Pic0Datagram.PlanktonCategory("SDWBA model", "SDWBA", 7, Color.RED),
         new SDWBAModelNew());

   //Expanded by Rolf 2009.01.23 and 2010.04.28

   private final PlanktonScatterer<SDWBAModelNew> sdwba2 = new PlanktonScatterer<>(
         "SDWBA (Demer and Conti '05; Conti '06 - Renfree (not published)) (2)",
         //new Pic0Datagram.PlanktonCategory("SDWBA model 2", "SDWBA2", 8, Color.black),
         new Pic0Datagram.PlanktonCategory("SDWBA model 2", "SDWBA2", 8, new Color(0xe14b14)),
         new SDWBAModelNew());

   private final PlanktonScatterer<SDWBAModelNew> sdwba3 = new PlanktonScatterer<>(
         "SDWBA (Demer and Conti '05; Conti '06 - Renfree (not published)) (3)",
         new Pic0Datagram.PlanktonCategory("SDWBA model 3", "SDWBA3", 9, Color.PINK),
         new SDWBAModelNew());

   private final PlanktonScatterer<SDWBAModelNew> sdwba4 = new PlanktonScatterer<>(
         "SDWBA (Demer and Conti '05; Conti '06 - Renfree (not published)) (4)",
         new Pic0Datagram.PlanktonCategory("SDWBA model 4", "SDWBA4", 10, Color.BLUE),
         new SDWBAModelNew());

   private final PlanktonScatterer<SDWBAModelNew> sdwba5 = new PlanktonScatterer<>(
         "SDWBA (Demer and Conti '05; Conti '06 - Renfree (not published)) (5)",
         new Pic0Datagram.PlanktonCategory("SDWBA model 5", "SDWBA5", 11, Color.DARK_GRAY),
         new SDWBAModelNew());

   private final PlanktonScatterer<SDWBAModel> sdwbaS = new PlanktonScatterer<>(
         "SDWBA (Demer and Conti '05; Conti '06)",
         new Pic0Datagram.PlanktonCategory("SDWBA model", "SDWBA_orig", 12, Color.YELLOW),
         new SDWBAModel());

   private BooleanParameter newScattererSelectionParameter(Name name, boolean initialValue, String description) {
      BooleanParameter parameter = new BooleanParameter(name, initialValue, description);
      parameter.subscribe(__ -> selectedScatterers = null);
      return parameter;
   }

   private static FloatParameter newScattererModelParameter(Name name, float initialValue, Unit unit, String description,
                                                            Consumer<Float> listener, BooleanParameter use) {
      FloatParameter parameter = new FloatParameter(name, initialValue, unit, description);
      parameter.subscribe(listener);
      use.addListenerAndNotify(parameter::setEnabled);
      return parameter;
   }

   //Hard shelled sphere
   public final BooleanParameter useHardShelled = newScattererSelectionParameter(
         new Name("HardShelled"),
         true,
         "Use hard shelled sphere model");

   public final FloatParameter hardShelledR = newScattererModelParameter(
         new Name("HardShelledR"),
         (float) hardShell.getBackscatterModel().getRFact(), Unit.NONE,
         "Reflection coefficient parameter",
         hardShell.getBackscatterModel()::setRFact,
         useHardShelled);

   private final SeparatorParameter separatorGaseousSphere = SeparatorParameter.line();

   //Gaseous sphere
   public final BooleanParameter useGaseousSphere = newScattererSelectionParameter(
         new Name("GaseousSphere"),
         true,
         "Use gaseous sphere model");

   public final FloatParameter gasSphereG = newScattererModelParameter(
         new Name("GasSphereG"),
         (float) gasSphere.getBackscatterModel().getRelativeDensity(), Unit.NONE,
         "g parameter (relative mass density)",
         gasSphere.getBackscatterModel()::setRelativeDensity,
         useGaseousSphere);

   public final FloatParameter gasSphereH = newScattererModelParameter(
         new Name("GasSphereH"),
         (float) gasSphere.getBackscatterModel().getRelativeSoundSpeed(), Unit.NONE,
         "h parameter (relative sound speed)",
         gasSphere.getBackscatterModel()::setRelativeSoundSpeed,
         useGaseousSphere);

   private final SeparatorParameter separatorFluidSpheroid = SeparatorParameter.line();

   //Fluid spheroid
   public final BooleanParameter useFluidSpheriod = newScattererSelectionParameter(
         new Name("FluidSpheriod"),
         true,
         "Use fluid prolate spheroid model");

   public final FloatParameter fluidSpheriodG = newScattererModelParameter(
         new Name("FluidSpheriodG"),
         (float) fluidSpheroid.getBackscatterModel().getRelativeDensity(), Unit.NONE,
         "g parameter",
         fluidSpheroid.getBackscatterModel()::setRelativeDensity,
         useFluidSpheriod);

   public final FloatParameter fluidSpheriodH = newScattererModelParameter(
         new Name("FluidSpheriodH"),
         (float) fluidSpheroid.getBackscatterModel().getRelativeSoundSpeed(), Unit.NONE,
         "h parameter",
         fluidSpheroid.getBackscatterModel()::setRelativeSoundSpeed,
         useFluidSpheriod);

   public final FloatParameter fluidSpheriodBeta = newScattererModelParameter(
         new Name("FluidSpheriodBeta"),
         (float) fluidSpheroid.getBackscatterModel().getLengthToWidth(), Unit.NONE,
         "Length to width ratio (preferably not less than 5)",
         fluidSpheroid.getBackscatterModel()::setLengthToWidth,
         useFluidSpheriod);

   private final SeparatorParameter separatorFluidBent = SeparatorParameter.line();

   //Fluid bent cylinder
   public final BooleanParameter useFluidBent = newScattererSelectionParameter(
         new Name("FluidBent"),
         true,
         "Use fluid bent cylinder model");

   public final FloatParameter fluidBentR = newScattererModelParameter(
         new Name("FluidBentR"),
         (float) fluidBentCyl.getBackscatterModel().getRFact(), Unit.NONE,
         "Reflection coefficient parameter",
         fluidBentCyl.getBackscatterModel()::setRFact,
         useFluidBent);

   public final FloatParameter fluidBentS = newScattererModelParameter(
         new Name("FluidBentS"),
         (float) fluidBentCyl.getBackscatterModel().getStdDevLength(), Unit.NONE,
         "Standard deviation of length",
         fluidBentCyl.getBackscatterModel()::setStdDevLength,
         useFluidBent);

   public final FloatParameter fluidBentBeta = newScattererModelParameter(
         new Name("FluidBentBeta"),
         (float) fluidBentCyl.getBackscatterModel().getLengthToWidth(), Unit.NONE,
         "Length to width ratio",
         fluidBentCyl.getBackscatterModel()::setLengthToWidth,
         useFluidBent);

   private final SeparatorParameter separatorFluidBent2 = SeparatorParameter.line();

   //Fluid bent cylinder 2
   public final BooleanParameter useFluidBent2 = newScattererSelectionParameter(
         new Name("FluidBent2"),
         false,
         "Use fluid bent cylinder model");

   public final FloatParameter fluidBentR2 = newScattererModelParameter(
         new Name("FluidBentR2"),
         (float) fluidBentCyl2.getBackscatterModel().getRFact(), Unit.NONE,
         "Reflection coefficient parameter",
         fluidBentCyl2.getBackscatterModel()::setRFact,
         useFluidBent2);

   public final FloatParameter fluidBentS2 = newScattererModelParameter(
         new Name("FluidBentS2"),
         (float) fluidBentCyl2.getBackscatterModel().getStdDevLength(), Unit.NONE,
         "Standard deviation of length",
         fluidBentCyl2.getBackscatterModel()::setStdDevLength,
         useFluidBent2);

   public final FloatParameter fluidBentBeta2 = newScattererModelParameter(
         new Name("FluidBentBeta 2", "FluidBentBeta 2"),
         (float) fluidBentCyl2.getBackscatterModel().getLengthToWidth(), Unit.NONE,
         "Length to width ratio",
         fluidBentCyl2.getBackscatterModel()::setLengthToWidth,
         useFluidBent2);

   private final SeparatorParameter separatorSDWBA = SeparatorParameter.line();

   private static <T> ObjectParameter<T> newSDWBAParameter(Name name, T initialValue, T[] allowedValues, String description,
                                                           Consumer<T> listener, BooleanParameter use) {
      ObjectParameter<T> parameter = new ObjectParameter<>(name, initialValue, allowedValues, description);
      parameter.subscribe(listener);
      use.addListenerAndNotify(parameter::setEnabled);
      return parameter;
   }

   //SDWBA
   public final BooleanParameter useSDWBA = newScattererSelectionParameter(
         new Name("SDWBA"),
         false,
         "Use simplified SDWBA model (Renfree parametrisation)");

   public final ObjectParameter<SDWBAModelNew.SDWBAParameterSetName> sdwbaCoeff = newSDWBAParameter(
         new Name("SDWBA coeff", "SDWBA coeff"),
         sdwba.getBackscatterModel().getParameterSetName(), SDWBAModelNew.SDWBAParameterSetName.values(),
         "SDWBA coefficient set",
         sdwba.getBackscatterModel()::setParameterSetName,
         useSDWBA);

   //public SeparatorParameter s6 = new SeparatorParameter();

   //SDWBA 2
   public final BooleanParameter useSDWBA2 = newScattererSelectionParameter(
         new Name("SDWBA2"),
         false,
         "Use simplified SDWBA model (Renfree parametrisation)");

   public final ObjectParameter<SDWBAModelNew.SDWBAParameterSetName> sdwbaCoeff2 = newSDWBAParameter(
         new Name("SDWBA 2 coeff", "SDWBA 2 coeff"),
         sdwba2.getBackscatterModel().getParameterSetName(), SDWBAModelNew.SDWBAParameterSetName.values(),
         "SDWBA coefficient set",
         sdwba2.getBackscatterModel()::setParameterSetName,
         useSDWBA2);

   //public SeparatorParameter s62 = new SeparatorParameter();

   //SDWBA 3
   public final BooleanParameter useSDWBA3 = newScattererSelectionParameter(
         new Name("SDWBA3"),
         false,
         "Use simplified SDWBA model (Renfree parametrisation)");

   public final ObjectParameter<SDWBAModelNew.SDWBAParameterSetName> sdwbaCoeff3 = newSDWBAParameter(
         new Name("SDWBA 3 coeff", "SDWBA 3 coeff"),
         sdwba3.getBackscatterModel().getParameterSetName(), SDWBAModelNew.SDWBAParameterSetName.values(),
         "SDWBA coefficient set",
         sdwba3.getBackscatterModel()::setParameterSetName,
         useSDWBA3);

   //public SeparatorParameter s63 = new SeparatorParameter();

   //SDWBA 4
   public final BooleanParameter useSDWBA4 = newScattererSelectionParameter(
         new Name("SDWBA4"),
         false,
         "Use simplified SDWBA model (Renfree parametrisation)");

   public final ObjectParameter<SDWBAModelNew.SDWBAParameterSetName> sdwbaCoeff4 = newSDWBAParameter(
         new Name("SDWBA 4 coeff", "SDWBA 4 coeff"),
         sdwba4.getBackscatterModel().getParameterSetName(), SDWBAModelNew.SDWBAParameterSetName.values(),
         "SDWBA coefficient set",
         sdwba4.getBackscatterModel()::setParameterSetName,
         useSDWBA4);

   //public final SeparatorParameter s64 = new SeparatorParameter();

   //SDWBA 5
   public final BooleanParameter useSDWBA5 = newScattererSelectionParameter(
         new Name("SDWBA5"),
         false,
         "Use simplified SDWBA model (Renfree parametrisation)");

   public final ObjectParameter<SDWBAModelNew.SDWBAParameterSetName> sdwbaCoeff5 = newSDWBAParameter(
         new Name("SDWBA 5 coeff", "SDWBA 5 coeff"),
         sdwba5.getBackscatterModel().getParameterSetName(), SDWBAModelNew.SDWBAParameterSetName.values(),
         "SDWBA coefficient set",
         sdwba5.getBackscatterModel()::setParameterSetName,
         useSDWBA5);

   private final SeparatorParameter separatorSDWBAS = SeparatorParameter.line();

   //SDWBA orig
   public final BooleanParameter useSDWBAS = newScattererSelectionParameter(
         new Name("SDWBAS"),
         false,
         "Use simplified SDWBA model (Conti parametrisation)");

   public final ObjectParameter<SDWBAModel.SDWBAParameterSetName> sdwbaCoeffS = newSDWBAParameter(
         new Name("SDWBA smooth coeff", "SDWBA smooth coeff"),
         sdwbaS.getBackscatterModel().getParameterSetName(), SDWBAModel.SDWBAParameterSetName.values(),
         "SDWBA coefficient set",
         sdwbaS.getBackscatterModel()::setParameterSetName,
         useSDWBAS);

   private final HeaderParameter visualizationHeader = new HeaderParameter("Visualization");

   public final ButtonParameter showPlot = new ButtonParameter(
         new Name("ShowPlot", "Show backscatter curves..."),
         "Shows relative target strength for the selected backscatter models",
         () -> new PlanktonCurvePlotter(this));

   public final DoubleParameter plotResolution = new DoubleParameter(
         new Name("PlotResolution", "Plot resolution"),
         0.01, Unit.NONE, ValueConstraints.gt(0.0),
         "kA resolution of curves");

   public final DoubleParameter plotMaxRange = new DoubleParameter(
         new Name("PlotMaxRange", "Plot max range"),
         5, Unit.NONE, ValueConstraints.gt(0.0),
         "Max range in plot");

   private @Nullable List<PlanktonScatterer<? extends BackscatterModel>> selectedScatterers;

   public PlanktonInversionModule() {
      verticalUnit.addListenerAndNotify(unit -> {
         depthSamplesPerBin.setVisible(unit == SpikeFilterModule.VerticalUnit.SAMPLES);
         depthDurationPerBin.setVisible(unit == SpikeFilterModule.VerticalUnit.DURATION);
         depthDistancePerBin.setVisible(unit == SpikeFilterModule.VerticalUnit.DISTANCE);
      });
      useNoiseThreshold.addListenerAndNotify(noiseThreshold::setEnabled);
      useMinInversionDepth.addListenerAndNotify(minInversionDepth::setEnabled);
      useMaxInversionDepth.addListenerAndNotify(maxInversionDepth::setEnabled);

      updateScattererSelection();
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            dataSettingsHeader,
            verticalUnit,
            pingsPerBin,
            depthSamplesPerBin,
            depthDurationPerBin,
            depthDistancePerBin,
            useNoiseThreshold,
            noiseThreshold,
            useMinInversionDepth,
            minInversionDepth,
            useMaxInversionDepth,
            maxInversionDepth,
            categoriesUsedForInversion,
            useAllFrequencies,
            activeFrequencies,
            //---
            inversionHeader,
            minResidualErrorThreshold,
            maxResidualErrorThreshold,
            levenbergMarquardtFactor,
            maxIter,
            //---
            planktonModelsHeader,
            useHardShelled,
            hardShelledR,
            //---
            separatorGaseousSphere,
            useGaseousSphere,
            gasSphereG,
            gasSphereH,
            //---
            separatorFluidSpheroid,
            useFluidSpheriod,
            fluidSpheriodG,
            fluidSpheriodH,
            fluidSpheriodBeta,
            //---
            separatorFluidBent,
            useFluidBent,
            fluidBentR,
            fluidBentS,
            fluidBentBeta,
            //---
            separatorFluidBent2,
            useFluidBent2,
            fluidBentR2,
            fluidBentS2,
            fluidBentBeta2,
            //---
            separatorSDWBA,
            useSDWBA,
            sdwbaCoeff,
            useSDWBA2,
            sdwbaCoeff2,
            useSDWBA3,
            sdwbaCoeff3,
            useSDWBA4,
            sdwbaCoeff4,
            useSDWBA5,
            sdwbaCoeff5,
            //---
            separatorSDWBAS,
            useSDWBAS,
            sdwbaCoeffS,
            //---
            visualizationHeader,
            showPlot,
            plotResolution,
            plotMaxRange
      );
   }

   @Override
   public List<Name> getRequiredConfigFileServiceNames() {
      return List.of(PlanktonFileService.NAME, CategorizationFileService.NAME);
   }

   @Override
   public List<Name> getOptionalConfigFileServiceNames() {
      return List.of(TransducerRangesFileService.NAME);
   }

   void updateParameters(@Nullable RawFileConfiguration rawFileConfiguration) {
      updateCategoriesUsedForInversion();
      updateActiveFrequencies(rawFileConfiguration);
   }

   private void updateCategoriesUsedForInversion() {
      Configurator configurator = new Configurator(getModuleContainer().getConfigFileSettings(), null);
      Set<String> selectedCategories = CategorizationModule.getActiveNames(categoriesUsedForInversion);
      boolean firstTime = categoriesUsedForInversion.getParameters().isEmpty();
      categoriesUsedForInversion.clear();
      for (Category category : configurator.getEnabledCategories()) {
         String categoryName = category.getName();
         if (categoryName.equals(Configurator.UNCATEGORIZED_NAME)) {
            continue;
         }

         boolean selected = firstTime ? categoryName.equals(Configurator.UNKNOWN_CATEGORY_NAME) : selectedCategories.contains(categoryName);
         BooleanParameter categoryToggle = newCategoryToggleParameter(new Name(categoryName), selected);
         categoriesUsedForInversion.addParameter(categoryToggle);
      }
   }

   private void updateActiveFrequencies(@Nullable RawFileConfiguration rawFileConfiguration) {
      Set<String> selectedFrequencies = CategorizationModule.getActiveNames(activeFrequencies);
      activeFrequencies.clear();
      for (Integer kHz : getAvailableKHz(rawFileConfiguration)) {
         Name name = new Name(kHz.toString(), kHz + " kHz");
         FrequencyParameter parameter = new FrequencyParameter(name, selectedFrequencies.contains(name.persistentName()));
         activeFrequencies.addParameter(parameter);
      }
   }

   private NavigableSet<Integer> getAvailableKHz(@Nullable RawFileConfiguration rawFileConfiguration) {
      NavigableSet<Integer> kHz = new TreeSet<>();

      Path file = getOptionalConfigFile(TransducerRangesFileService.NAME);
      if (file != null) {
         try {
            TransducerParameterManager transducerParameterManager = new TransducerParameterManager(TransducerParameters.ParameterType.VERTICAL, XmlUtils.readDocument(file));
            kHz.addAll(transducerParameterManager.getKHzs());
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error reading " + file, e);
         }
      }

      if (rawFileConfiguration != null) {
         for (RawFileTransducer transducer : rawFileConfiguration.getTransducers()) {
            kHz.add(transducer.getKHz());
         }
      }

      return kHz;
   }

   public List<PlanktonScatterer<? extends BackscatterModel>> getSelectedScatterers() {
      List<PlanktonScatterer<? extends BackscatterModel>> scatterers = selectedScatterers;
      if (scatterers == null) {
         scatterers = updateScattererSelection();
         selectedScatterers = scatterers;
      }
      return scatterers;
   }

   private List<PlanktonScatterer<? extends BackscatterModel>> updateScattererSelection() {
      List<PlanktonScatterer<? extends BackscatterModel>> scatterers = new ArrayList<>();
      if (useHardShelled.getBooleanValue()) {
         scatterers.add(hardShell);
      }
      if (useGaseousSphere.getBooleanValue()) {
         scatterers.add(gasSphere);
      }
      if (useFluidSpheriod.getBooleanValue()) {
         scatterers.add(fluidSpheroid);
      }
      if (useFluidBent.getBooleanValue()) {
         scatterers.add(fluidBentCyl);
      }
      if (useFluidBent2.getBooleanValue()) {
         scatterers.add(fluidBentCyl2);
      }
      if (useSDWBA.getBooleanValue()) {
         scatterers.add(sdwba);
      }
      if (useSDWBA2.getBooleanValue()) {
         scatterers.add(sdwba2);
      }
      if (useSDWBA3.getBooleanValue()) {
         scatterers.add(sdwba3);
      }
      if (useSDWBA4.getBooleanValue()) {
         scatterers.add(sdwba4);
      }
      if (useSDWBA5.getBooleanValue()) {
         scatterers.add(sdwba5);
      }
      if (useSDWBAS.getBooleanValue()) {
         scatterers.add(sdwbaS);
      }
      return List.copyOf(scatterers);
   }

   @Override
   protected void configureWithoutData() {
      updateParameters(null);
   }

   @Override
   public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new PlanktonInversionModuleComputation(this, computationContext, pingSource);
   }

   static final class FrequencyParameter extends BooleanParameter {
      private FrequencyParameter(Name name, boolean initialValue) {
         super(name, initialValue, "Use " + name.displayName() + " in inversion");
      }

      int getKHz() {
         return Integer.parseInt(getPersistentName());
      }
   }
}
