package no.imr.korona.computation.categorization;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.computation.feature.FeatureExtractor;
import no.imr.korona.computation.feature.FeatureRequirementSmoke;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.compile.CompileException;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.MultiParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Module for categorization.
 */
public final class CategorizationModule extends GeneralPingModule {
   public final IntParameter categoryCount = new IntParameter(
         new Name("CategoryCount", "Category count"),
         3, Unit.COUNT, ValueConstraints.gte(1),
         "Number of categories per pixel to put in the categorization datagrams");

   public final MultiParameter<BooleanParameter> activeFeatures = new MultiParameter<>(
         new Name("ActiveFeatures", "Active features"),
         "Which features to use") {
      @Override
      protected Configurable possiblyCreateNewSubConfigurable(String persistentName) {
         return addParameter(new BooleanParameter(new Name(persistentName), false));
      }
   };

   public final MultiParameter<BooleanParameter> activeCategories = new MultiParameter<>(
         new Name("ActiveCategories", "Active categories"),
         "Which categories to use") {
      @Override
      protected Configurable possiblyCreateNewSubConfigurable(String persistentName) {
         return addParameter(new BooleanParameter(new Name(persistentName), false));
      }
   };

   public void applyFeatureSettings(CategorizationModuleComputation computation) {
      updateFeatureSettings(computation.getConfigurator());
      computation.getFeatureExtractSubModule().configure(computation.getConfigurator());
   }

   private void updateCategorySettings(Configurator configurator) {
      Set<String> activeCategoryNames = getActiveNames(activeCategories);
      boolean firstTime = activeCategories.getParameters().isEmpty();
      activeCategories.clear();

      for (Category category : configurator.getEnabledCategories()) {
         String categoryName = category.getName();
         if (categoryName.equals(Configurator.UNKNOWN_CATEGORY_NAME) || categoryName.equals(Configurator.UNCATEGORIZED_NAME)) {
            continue;
         }

         boolean active;
         if (firstTime) {
            active = category.isSpecial();
         } else {
            active = activeCategoryNames.contains(categoryName);
         }
         BooleanParameter parameter = new BooleanParameter(new Name(categoryName),
               active,
               "Use category \"" + categoryName + "\" in categorization");
         activeCategories.addParameter(parameter);
         category.setActive(active);
      }
   }

   private void updateFeatureSettings(Configurator configurator) {
      Set<String> activeFeatureNames = getActiveNames(activeFeatures);
      boolean firstTime = activeFeatures.getParameters().isEmpty();
      activeFeatures.clear();

      for (FeatureExtractor featureExtractor : configurator.getEnabledFeatureExtractors()) {
         String featureName = featureExtractor.getFeatureName();
         boolean active;
         if (firstTime) {
            active = !featureExtractor.isAdditional() || featureName.equals(FeatureExtractor.ADDITIONAL_FEATURE_SV38);
         } else {
            active = activeFeatureNames.contains(featureName);
         }
         BooleanParameter parameter = new BooleanParameter(new Name(featureName),
               active,
               "Use feature \"" + featureName + "\" in categorization");
         activeFeatures.addParameter(parameter);
         featureExtractor.setActive(active);
      }
   }

   public static Set<String> getActiveNames(MultiParameter<? extends BooleanParameter> multiParameter) {
      Set<String> activeNames = new HashSet<>();
      for (BooleanParameter parameter : multiParameter.getParameters()) {
         if (parameter.getBooleanValue()) {
            activeNames.add(parameter.getPersistentName());
         }
      }
      return activeNames;
   }

   void updateSettings(Configurator configurator) {
      updateCategorySettings(configurator);
      updateFeatureSettings(configurator);
   }

   @FunctionalInterface
   interface SubModuleFactory {
      CategorizationSubModule makeSubModule(CategorizationSubModule previousSubModule, Configurator configurator);
   }

   enum CategorizerType {
      GAUSS("Gauss", (previousSubModule, configurator) -> {
         return new CategorizerSubModule(previousSubModule,
               new GaussCategorizer(Category.DistributionLevel.PIXEL, false, configurator));
      }),
      NEAREST_NEIGHBOR("Nearest neighbor", (previousSubModule, configurator) -> {
         return new CategorizerSubModule(previousSubModule,
               new NearestNeighborCategorizer(Category.DistributionLevel.PIXEL, configurator));
      }),
      COMBINED("Combined", (previousSubModule, configurator) -> {
         return new CategorizerSubModule(previousSubModule, new CombinedCategorizer(List.of(
               new GaussCategorizer(Category.DistributionLevel.PIXEL, false, configurator),
               new NearestNeighborCategorizer(Category.DistributionLevel.PIXEL, configurator)
         )));
      });

      private final String id;
      final SubModuleFactory factory;

      CategorizerType(String id, SubModuleFactory factory) {
         this.id = id;
         this.factory = factory;
      }

      @Override
      public String toString() {
         return id;
      }
   }

   public final ObjectParameter<CategorizerType> categorizer = new ObjectParameter<>(
         new Name("Categorizer"),
         CategorizerType.GAUSS, CategorizerType.values(),
         "Type of classifier");

   enum DiscriminantType {
      A_POSTERIORI("Aposteriori", (previousSubModule, configurator) -> new AposterioriDiscriminant(previousSubModule)),
      COST("Cost", CostDiscriminant::new);

      private final String id;
      final SubModuleFactory factory;

      DiscriminantType(String id, SubModuleFactory factory) {
         this.id = id;
         this.factory = factory;
      }

      @Override
      public String toString() {
         return id;
      }
   }

   public final ObjectParameter<DiscriminantType> discriminant = new ObjectParameter<>(
         new Name("Discriminant"),
         DiscriminantType.A_POSTERIORI, DiscriminantType.values(),
         "Type of discriminant function");

   enum ContextualCorrectionType {
      NONE("None", (previousSubModule, configurator) -> {
         throw new UnsupportedOperationException();
      }),
      ICM("ICM", ICMContextualCorrection::new);

      private final String id;
      final SubModuleFactory factory;

      ContextualCorrectionType(String id, SubModuleFactory factory) {
         this.id = id;
         this.factory = factory;
      }

      @Override
      public String toString() {
         return id;
      }
   }

   public final ObjectParameter<ContextualCorrectionType> contextualCorrection = new ObjectParameter<>(
         new Name("ContextualCorrection", "Contextual correction"),
         ContextualCorrectionType.ICM, ContextualCorrectionType.values(),
         "Type of contextual correction iteration");

   public final IntParameter contextualIterations = new IntParameter(
         new Name("ContextualIterations", "Contextual iterations"),
         3, Unit.COUNT, ValueConstraints.gte(0),
         "Number of contextual correction iterations");

   public final BooleanParameter useMinLogSv = new BooleanParameter(
         new Name("UseMinLogSv", "Use min Sv"),
         true,
         "Enable Sv threshold");

   public final FloatParameter minLogSv = new FloatParameter(
         new Name("MinLogSv", "Min Sv"),
         -100, Unit.DB,
         "Only assign category for samples above a threshold at 38 kHz");

   public CategorizationModule() {
      contextualCorrection.addListenerAndNotify(correction -> {
         contextualIterations.setEnabled(correction != ContextualCorrectionType.NONE);
      });
      useMinLogSv.addListenerAndNotify(minLogSv::setEnabled);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            categoryCount,
            activeFeatures,
            activeCategories,
            categorizer,
            discriminant,
            contextualCorrection,
            contextualIterations,
            useMinLogSv,
            minLogSv
      );
   }

   @Override
   public List<Name> getRequiredConfigFileServiceNames() {
      return List.of(CategorizationFileService.NAME, TransducerRangesFileService.NAME);
   }

   @Override
   public void configureWithoutData() {
      Configurator configurator = new Configurator(getModuleContainer().getConfigFileSettings(), null);
      updateSettings(configurator);
   }

   @Override
   public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new CategorizationModuleComputation(this, computationContext, pingSource);
   }

   @Override
   public void runSmokeTest() throws CompileException {
      new FeatureRequirementSmoke().run();
   }
}
