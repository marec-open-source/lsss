package no.imr.korona.computation.categorization;

import no.imr.korona.computation.BaseModule;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.computation.feature.FeatureExtractor;
import no.imr.korona.computation.offset.TransducerParameterManager;
import no.imr.korona.computation.offset.TransducerParameters;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.DiscreteCategory;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;
import no.imr.tools.xml.XmlUtils;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.logging.Level;

/**
 * Used by CategorizationSubModules for initial configuration.
 */
public final class Configurator implements ParameterContainer {
   /**
    * Name of the configuration file.
    */
   public static final String CATEGORIZATION_FILE = "categorization.xml";

   public static final String CATEGORIES_SUB_DIR = "categories";

   public static final String UNKNOWN_CATEGORY_NAME = "unknown";
   public static final String BOTTOM_CATEGORY_NAME = "bottom";
   public static final String BLIND_ZONE_CATEGORY_NAME = "blindzone";
   public static final String NOISE_CATEGORY_NAME = "noise";
   public static final String UNCATEGORIZED_NAME = "uncategorized";
   public static final List<String> SPECIAL_CATEGORIES = List.of(
         UNKNOWN_CATEGORY_NAME,
         BOTTOM_CATEGORY_NAME,
         BLIND_ZONE_CATEGORY_NAME,
         NOISE_CATEGORY_NAME,
         UNCATEGORIZED_NAME
   );

   public static final int MAX_CATEGORY_NUMBER = Byte.MAX_VALUE;

   /**
    * A category name must match this regular expression.
    */
   public static final String CATEGORY_NAME_REG_EXP = "[a-zæøåA-ZÆØÅ0-9_\\-]+";

   /**
    * Maximum number of characters in a category legend.
    */
   public static final int CATEGORY_LEGEND_MAX_LENGTH = 5;

   /**
    * Names of XML tags categorization files.
    */
   public static final class XML {
      public static final String CATEGORIZATION = "categorization";
      private static final String CATEGORIES = "categories";
      public static final String CATEGORY = "category";
      public static final String ENABLED = "enabled";
      public static final String NAME = "name";
      public static final String LEGEND = "legend";
      public static final String LONG_LEGEND = "longLegend";
      public static final String COMMENT = "comment";
      public static final String COLOR = "color";
      public static final String TYPE = "type";
      public static final String APRIORI = "apriori";
      public static final String SCHOOL_APRIORI = "schoolApriori";
      public static final String MIN_SV_38 = "minSv38";
      public static final String MAX_SV_38 = "maxSv38";
      public static final String REQUIREMENT = "requirement";
      private static final String COST = "cost";
      private static final String COST_MATRIX_ENTRY = "costMatrixEntry";
      private static final String ACTUAL = "actual";
      private static final String ESTIMATED = "estimated";
      private static final String VALUE = "value";
      private static final String FEATURES = "features";
      public static final String FEATURE = "feature";
      public static final String NEIGHBORHOOD = "neighborhood";
      public static final String NEIGHBOR = "neighbor";
      public static final String GAUSS = "gauss";
      public static final String MEAN = "mean";
      public static final String COVARIANCE = "covariance";

      private XML() {
      }
   }

   public final FloatParameter outlierFraction = new FloatParameter(
         new Name("OutlierFraction", "Outlier fraction"),
         0.01f, Unit.DIMENSIONLESS, ValueConstraints.gteLte(0f, 1f),
         "Determines a threshold for accepted class conditional probability");

   public final FloatParameter icmBeta = new FloatParameter(
         new Name("ICMBeta", "ICM beta"),
         0.1f, Unit.DIMENSIONLESS,
         "Used when updating a priori probability, during contextual iterations");

   public final ObjectParameter<Metric> nearestNeighborMetric = new ObjectParameter<>(
         new Name("NearestNeighborMetric", "Nearest neighbor metric"),
         Metric.EUCLIDEAN, Metric.values(),
         "The metric used for calculating distances");

   public final FloatParameter nearestNeighborDistanceThreshold = new FloatParameter(
         new Name("NearestNeighborDistanceThreshold", "Nearest neighbor distance threshold"),
         0, Unit.DIMENSIONLESS,
         "The maximum accepted distance");

   public final FloatParameter defaultCostDiagonalValue = new FloatParameter(
         new Name("DefaultCostDiagonalValue", "Default cost diagonal value"),
         -1, Unit.DIMENSIONLESS,
         "Default diagonal value");

   public final FloatParameter defaultCostOffDiagonalValue = new FloatParameter(
         new Name("DefaultCostOffDiagonalValue", "Default cost off-diagonal value"),
         0, Unit.DIMENSIONLESS,
         "Default off-diagonal value");

   public final IntParameter thinnedScatterSize = new IntParameter(
         new Name("ThinnedScatterSize", "Thinned scatter size"),
         1000, Unit.COUNT, ValueConstraints.gte(1),
         "Maximum size of the thinned point scatter for each category");

   public final FloatParameter deltaMinMaxSv38 = new FloatParameter(
         new Name("DeltaMinMaxSv38", "Delta min max Sv 38"),
         10, Unit.DB, ValueConstraints.gte(0f),
         "Distance outside minSv38 and maxSv38 until the a priori probability is linearly reduced to 0");   //RK 2015.12.20:   5 => 10

   private final @Nullable Path categorizationFile;
   private final @Nullable Path transducerRangesFile;
   private final @Nullable RawFileConfiguration rawFileConfiguration;

   public static final int REFERENCE_KHZ = 38;
   private static final float REFERENCE_FREQUENCY = REFERENCE_KHZ * 1000;
   private static final float REFERENCE_FREQUENCY_SLACK = 1000;

   private final int referenceChannel;

   private final List<Category> allCategories = new ArrayList<>();
   private final List<Category> deletedCategories = new ArrayList<>();
   private final NavigableSet<FeatureExtractor> featureExtractors = new TreeSet<>();

   private Category.Type categoryType = Category.Type.Aggregation;

   private record CostMapKey(Category estimated, Category actual) {
   }

   private final Map<CostMapKey, Float> costMap = new HashMap<>();

   public Configurator(ConfigFileSettings configFileSettings, @Nullable RawFileConfiguration rawFileConfiguration) {
      this(configFileSettings.getFile(CategorizationFileService.NAME), configFileSettings.getFile(TransducerRangesFileService.NAME), rawFileConfiguration);
   }

   public Configurator(@Nullable Path categorizationFile, @Nullable Path transducerRangesFile, @Nullable RawFileConfiguration rawFileConfiguration) {
      this.categorizationFile = categorizationFile;
      this.transducerRangesFile = transducerRangesFile;
      this.rawFileConfiguration = rawFileConfiguration;
      referenceChannel = rawFileConfiguration != null ? findReferenceChannel(rawFileConfiguration) : -1;

      init();
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            outlierFraction,
            icmBeta,
            nearestNeighborMetric,
            nearestNeighborDistanceThreshold,
            defaultCostDiagonalValue,
            defaultCostOffDiagonalValue,
            thinnedScatterSize,
            deltaMinMaxSv38
      );
   }

   /**
    * Reinitialization of this configurator.
    */
   public void init() {
      try {
         Document document = categorizationFile != null ? XmlUtils.readDocumentIfExists(categorizationFile) : null;
         Element element = document != null ? document.getRootElement() : null;
         init(element);
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error parsing categorization file " + categorizationFile, e);
      }
   }

   private void init(@Nullable Element element) {
      featureExtractors.clear();
      deletedCategories.clear();
      allCategories.clear();
      costMap.clear();

      if (element != null) {
         parseFeatureExtractors(element.element(XML.FEATURES));
         new ParameterCollection(this).fromXml(element.element(ParameterCollection.XML_PARAMETERS));
         parseCategories(element.element(XML.CATEGORIES)); // nb: after parseParameters
         parseCostMatrix(element.element(XML.COST)); // nb: after parseCategories
      }

      for (String specialCategory : SPECIAL_CATEGORIES) {
         if (getCategory(specialCategory) == null) {
            addCategory(new Category(this, specialCategory, Color.BLACK));
         }
      }

      addAllAvailableFeatureExtractors(); // nb: after parsing configuration on file

      try {
         initRanges();
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error reading " + transducerRangesFile, e);
      }
   }

   private void initRanges() throws IOException {
      Document document = transducerRangesFile != null ? XmlUtils.readDocumentIfExists(transducerRangesFile) : null;
      if (document != null) {
         TransducerParameterManager transducerParameterManager = new TransducerParameterManager(TransducerParameters.ParameterType.RANGE, document);

         Utils.getAllOfType(featureExtractors, FeatureExtractor.FrequencyFeatureExtractor.class).forEach(frequencyFeatureExtractor -> {
            int kHz = frequencyFeatureExtractor.getKHz();
            Optional<Float> blindZone = transducerParameterManager.getBlindZone(kHz);
            Optional<Float> range = transducerParameterManager.getRange(kHz);
            if (blindZone.isPresent() && range.isPresent()) {
               frequencyFeatureExtractor.setRange(FloatRange.of(blindZone.get(), range.get()));
            }
         });
      }
   }

   /**
    * Creates a categorization configuration datagram.
    *
    * @param instant              time
    * @param includeAllCategories if true, also disabled categories are included
    * @return the Cac0Datagram corresponding to the settings in this Configurator
    */
   public Cac0Datagram createCac0Datagram(Instant instant, boolean includeAllCategories) {
      Cac0Datagram cac0Datagram = new Cac0Datagram(instant);
      for (Category category : getCategories()) {
         if (category.isActive() || includeAllCategories) {
            cac0Datagram.addCategory(category);
         }
      }
      return cac0Datagram;
   }

   private Cac0Datagram possiblyCreateNewCac0Datagram(BaseModule module, PingConfiguration pingConfiguration, @Nullable Cac0Datagram cac0Datagram) throws ModuleConfigurationException {
      if (cac0Datagram == null || !containsAllCategories(cac0Datagram.getCategories())) {
         return createCac0Datagram(pingConfiguration.getRawFileConfiguration().getInstant(), false);
      }

      Cac0Datagram copy = null;

      for (Category category : getCategories()) {
         if (!category.isActive()) {
            continue;
         }
         Cac0Datagram.Category definedCategory = cac0Datagram.nameToCategory(category.getName());
         if (definedCategory == null) {
            if (copy == null) {
               copy = PingItem.copy(cac0Datagram);
            }
            copy.addCategory(category);
         } else {
            if (category.getNumber() != definedCategory.getNumber()) {
               throw new ModuleConfigurationException(module, "Incompatible category definitions for " + category.getName());
            }
         }
      }

      return copy != null ? copy : cac0Datagram;
   }

   private boolean containsAllCategories(Collection<? extends DiscreteCategory> categories) {
      return categories.stream().allMatch(otherCategory -> {
         int number = otherCategory.getNumber();
         if (number >= allCategories.size()) {
            return false;
         }
         Category category = allCategories.get(number);
         return category.getName().equals(otherCategory.getName())
               && category.getLegend().equals(otherCategory.getLegend())
               && category.getColor().equals(otherCategory.getColor());
      });
   }

   public PingConfiguration configure(BaseModule module, PingConfiguration pingConfiguration) throws ModuleConfigurationException {
      Cac0Datagram oldCac0Datagram = pingConfiguration.getConfigurationItem(Cac0Datagram.class);
      Cac0Datagram newCac0Datagram = possiblyCreateNewCac0Datagram(module, pingConfiguration, oldCac0Datagram);
      if (oldCac0Datagram == newCac0Datagram) {
         return pingConfiguration;
      }

      PingConfiguration newPingConfiguration = pingConfiguration.createCopy();
      newPingConfiguration.getConfigurationItems().removeIf(Cac0Datagram.class::isInstance);
      newPingConfiguration.getConfigurationItems().add(newCac0Datagram);
      return newPingConfiguration;
   }

   public Category.Type getCategoryType() {
      return categoryType;
   }

   public void setCategoryType(Category.Type categoryType) {
      this.categoryType = categoryType;
   }

   public Collection<Category> getAllCategories() {
      return allCategories;
   }

   /**
    * Returns only the categories relevant for the current {@link Category.Type}.
    *
    * @return categories
    */
   public List<Category> getCategories() {
      return allCategories.stream()
            .filter(category -> category.getType() == categoryType || category.isSpecial())
            .toList();
   }

   public List<Category> getEnabledCategories() {
      List<Category> enabledCategories = new ArrayList<>();

      for (Category category : getCategories()) {
         if (category.isEnabled()) {
            enabledCategories.add(category);
         }
      }

      return enabledCategories;
   }

   /**
    * Returns a collection of the categories that are not special and enabled.
    *
    * @return a collection of the categories that are not special and enabled
    */
   public List<Category> getNonSpecialEnabledCategories() {
      List<Category> categories = new ArrayList<>();
      for (Category category : getCategories()) {
         if (!category.isSpecial() && category.isEnabled()) {
            categories.add(category);
         }
      }
      return categories;
   }

   /**
    * Returns the category for a specified category name.
    *
    * @param name a category name
    * @return the category or {@code null} if non found
    */
   public @Nullable Category getCategory(String name) {
      for (Category category : allCategories) {
         if (category.getName().equals(name)) {
            return category;
         }
      }
      return null;
   }

   public Category getSpecialCategory(String name) {
      for (Category category : allCategories) {
         if (category.getName().equals(name)) {
            return category;
         }
      }
      throw new IllegalArgumentException(name);
   }

   /**
    * Returns the maximum of all category numbers.
    *
    * @return the maximum of all category numbers, or -1 if there are no categories
    */
   public int getMaxCategoryNumber() {
      return allCategories.size() - 1;
   }

   private void addCategory(Category category) {
      category.setNumber((byte) allCategories.size());
      allCategories.add(category);
   }

   void setCategories(Collection<Category> categories) {
      allCategories.removeAll(categories);
      deletedCategories.addAll(allCategories);
      allCategories.clear();
      for (Category category : categories) {
         addCategory(category);
      }
   }

   Path getCategoriesDirectory() {
      if (categorizationFile == null) {
         throw new IllegalStateException("No categorization file");
      }
      return getCategoriesDirectory(categorizationFile);
   }

   public static Path getCategoriesDirectory(Path file) {
      return file.resolveSibling(CATEGORIES_SUB_DIR);
   }

   /**
    * Returns the categorization file.
    *
    * @return the categorization file
    */
   public @Nullable Path getCategorizationFile() {
      return categorizationFile;
   }

   /**
    * Returns the frequency [Hz] of the reference channel.
    *
    * @return the frequency [Hz] of the reference channel
    */
   public float getReferenceFrequency() {
      return REFERENCE_FREQUENCY;
   }

   /**
    * Returns the reference channel.
    *
    * @return the reference channel
    */
   public int getReferenceChannel() {
      return referenceChannel;
   }

   /**
    * Returns the index of a specified frequency.
    *
    * @param kHz the frequency in kHz
    * @return the index, or -1 if the frequency is not present in the current data
    */
   public int getFrequencyIndex(int kHz) {
      if (rawFileConfiguration != null) {
         List<RawFileTransducer> transducers = rawFileConfiguration.getTransducers();
         for (int i = 0; i < transducers.size(); i++) {
            RawFileTransducer transducer = transducers.get(i);
            if (transducer.getKHz() == kHz) {
               return i;
            }
         }
      }
      return -1;
   }

   /**
    * Returns a collection of extractors for all features.
    *
    * @return a collection of extractors for all features
    */
   public Collection<FeatureExtractor> getFeatureExtractors() {
      return featureExtractors;
   }

   /**
    * Returns a {@link FeatureExtractor} for a specified feature name.
    *
    * @param featureName the feature name
    * @return the feature extractor
    */
   public @Nullable FeatureExtractor getFeatureExtractor(String featureName) {
      for (FeatureExtractor featureExtractor : featureExtractors) {
         if (featureExtractor.getFeatureName().equals(featureName)) {
            return featureExtractor;
         }
      }
      return null;
   }

   /**
    * Returns all enabled feature extractors.
    *
    * @return all enabled feature extractors
    */
   public List<FeatureExtractor> getEnabledFeatureExtractors() {
      List<FeatureExtractor> enabledExtractors = new ArrayList<>();
      for (FeatureExtractor featureExtractor : featureExtractors) {
         if (featureExtractor.isEnabled()) {
            enabledExtractors.add(featureExtractor);
         }
      }
      return enabledExtractors;
   }

   /**
    * Returns a collection of extractors for all operational features.
    * Operational means active and that the frequency is present in the current data.
    *
    * @return a collection of extractors for all operational features
    */
   public List<FeatureExtractor> getOperationalFeatureExtractors() {
      List<FeatureExtractor> operationalExtractors = new ArrayList<>();
      for (FeatureExtractor featureExtractor : featureExtractors) {
         if (featureExtractor.isOperational()) {
            operationalExtractors.add(featureExtractor);
         }
      }
      return operationalExtractors;
   }

   public @Nullable RawFileConfiguration getRawFileConfiguration() {
      return rawFileConfiguration;
   }

   /**
    * Returns an entry in the cost matrix.
    *
    * @param estimated the estimated category
    * @param actual    the actual category
    * @return the cost
    */
   public float getCost(Category estimated, Category actual) {
      Float cost = costMap.get(new CostMapKey(estimated, actual));
      if (cost != null) {
         return cost;
      } else if (estimated == actual) {
         return defaultCostDiagonalValue.getFloatValue();
      } else {
         return defaultCostOffDiagonalValue.getFloatValue();
      }
   }

   private void parseFeatureExtractors(Element element) {
      for (Element featureElement : element.elements(XML.FEATURE)) {
         String name = featureElement.attributeValue(XML.NAME);

         FeatureExtractor featureExtractor = FeatureExtractor.createFeatureExtractor(name, this);
         if (featureExtractor == null) {
            Log.global.warning("unknown feature " + name);
         } else {
            boolean enabled = Boolean.parseBoolean(featureElement.attributeValue(XML.ENABLED));
            featureExtractor.setEnabled(enabled);
            featureExtractor.setActive(enabled);
            featureExtractors.add(featureExtractor);
         }
      }
   }

   private void addAllAvailableFeatureExtractors() {
      if (rawFileConfiguration != null) {
         List<RawFileTransducer> transducers = rawFileConfiguration.getTransducers();
         for (int i = 0; i < transducers.size(); i++) {
            RawFileTransducer transducer = transducers.get(i);
            if (i == referenceChannel - 1) {
               continue;
            }
            FeatureExtractor featureExtractor = FeatureExtractor.createFrequencyFeatureExtractor(
                  transducer.getKHz(), this);
            featureExtractors.add(featureExtractor);
         }
      }
      featureExtractors.addAll(FeatureExtractor.createAllAdditionalFeatureExtractors(this));
   }

   private void parseCategories(Element element) {
      for (Element categoryElement : element.elements()) {
         addCategory(new Category(this, categoryElement));
      }
   }

   private void parseCostMatrix(Element element) {
      for (Element costMatrixElement : element.elements()) {
         String estimatedName = costMatrixElement.attributeValue(XML.ESTIMATED);
         String actualName = costMatrixElement.attributeValue(XML.ACTUAL);
         Category estimated = getCategory(estimatedName);
         Category actual = getCategory(actualName);
         Float cost = Float.valueOf(costMatrixElement.attributeValue(XML.VALUE));

         if (estimated == null || actual == null) {
            if (estimated == null) {
               Log.global.warning("no such category: " + estimatedName);
            }
            if (actual == null) {
               Log.global.warning("no such category: " + actualName);
            }
            continue;
         }

         costMap.put(new CostMapKey(estimated, actual), cost);
      }
   }

   public static int findReferenceChannel(RawFileConfiguration rawFileConfiguration) {
      List<RawFileTransducer> transducers = rawFileConfiguration.getTransducers();
      for (int i = 0; i < transducers.size(); i++) {
         float frequency = transducers.get(i).getFrequency();
         if (Math.abs(frequency - REFERENCE_FREQUENCY) < REFERENCE_FREQUENCY_SLACK) {
            return i + 1;
         }
      }
      return -1;
   }

   /**
    * Saves the settings in this configurator to file.
    */
   public void save() {
      if (categorizationFile == null) {
         Log.global.warning("No categorization file");
         return;
      }

      try {
         XmlUtils.writeDocument(toXml(), categorizationFile);

         for (Category category : deletedCategories) {
            Path dir = category.categoryDirectory();
            FileUtils.deleteRecursively(dir);
         }
         deletedCategories.clear();

         for (Category category : allCategories) {
            category.save();
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error saving categorization file " + categorizationFile, e);
      }
   }

   public Element toXml() {
      Element root = DocumentHelper.createElement(XML.CATEGORIZATION);

      Element categoriesElement = root.addElement(XML.CATEGORIES);
      for (Category category : allCategories) {
         categoriesElement.add(category.toXml());
      }

      root.add(new ParameterCollection(this).toXml());

      Element costElement = root.addElement(XML.COST);
      for (Map.Entry<CostMapKey, Float> entry : costMap.entrySet()) {
         CostMapKey key = entry.getKey();
         Category estimated = key.estimated();
         Category actual = key.actual();
         if (!allCategories.contains(estimated) || !allCategories.contains(actual)) {
            continue;
         }
         float cost = entry.getValue();
         if (estimated == actual && cost == defaultCostDiagonalValue.getFloatValue()
               || estimated != actual && cost == defaultCostOffDiagonalValue.getFloatValue()) {
            continue;
         }
         costElement.addElement(XML.COST_MATRIX_ENTRY)
               .addAttribute(XML.ESTIMATED, estimated.getName())
               .addAttribute(XML.ACTUAL, actual.getName())
               .addAttribute(XML.VALUE, Utils.toString(cost));
      }

      Element additionalFeaturesElement = root.addElement(XML.FEATURES);
      for (FeatureExtractor featureExtractor : featureExtractors) {
         additionalFeaturesElement.addElement(XML.FEATURE)
               .addAttribute(XML.ENABLED, Boolean.toString(featureExtractor.isEnabled()))
               .addAttribute(XML.NAME, featureExtractor.getFeatureName());
      }

      return root;
   }
}
