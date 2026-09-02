package no.imr.korona.computation.categorization;

import com.google.common.base.Strings;
import no.imr.korona.computation.categorization.apriori.CategoryAPriori;
import no.imr.korona.computation.feature.CollectiveFeatureComputation;
import no.imr.korona.computation.feature.EchogramWindow;
import no.imr.korona.computation.feature.FeatureRequirement;
import no.imr.korona.computation.feature.FeatureRequirementFactory;
import no.imr.korona.data.datagrams.DiscreteCategory;
import no.imr.tools.ProgressHandler;
import no.imr.tools.RandomUtils;
import no.imr.tools.Utils;
import no.imr.tools.compile.CompileException;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.time.TimeUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Level;

/**
 * Information about a category.
 */
public final class Category implements DiscreteCategory {
   public enum DistributionLevel {
      PIXEL(""), CELL("_cell"), SCHOOL("_school");

      private final String baseNameSuffix;

      DistributionLevel(String baseNameSuffix) {
         this.baseNameSuffix = baseNameSuffix;
      }

      private String getBaseNameSuffix() {
         return baseNameSuffix;
      }
   }

   public record NeighborhoodInitialization(EchogramWindow echogramWindow, CollectiveFeatureComputation collectiveFeatureComputation) {

      private String getDirName() {
         Path rawFile = echogramWindow.getRawFile();
         String raw = rawFile != null ? rawFile.getFileName().toString() : "NoFile";
         String time = timeToString(echogramWindow.getPings().getFirst().getInstant());
         float depth = echogramWindow.getMinDepth();
         return raw + "-" + time + "-" + depth;
      }

      private static String timeToString(Instant time) {
         return TimeUtils.createUTCDateTimeFormatter("yyyyMMddHHmmss").format(time);
      }
   }

   public static final class NeighborhoodData {
      private final CategoryNeighborhood categoryNeighborhood;
      private final DistributionLevel distributionLevel;
      private @Nullable Neighborhood neighborhood;
      private SoftReference<@Nullable Neighborhood> neighborhoodOnFile = new SoftReference<>(null);
      private @Nullable GaussDistribution gaussDistribution;

      private NeighborhoodData(CategoryNeighborhood categoryNeighborhood, DistributionLevel distributionLevel) {
         this.categoryNeighborhood = categoryNeighborhood;
         this.distributionLevel = distributionLevel;
      }

      public DistributionLevel getDistributionLevel() {
         return distributionLevel;
      }

      public GaussDistribution getGaussDistribution() {
         if (gaussDistribution == null) {
            gaussDistribution = createGaussDistribution();
         }
         return gaussDistribution;
      }

      private GaussDistribution createGaussDistribution() {
         if (neighborhood == null) {
            try {
               Document document = XmlUtils.readDocumentIfExists(gaussFile());
               if (document != null) {
                  return new GaussDistribution(document.getRootElement());
               }
            } catch (IOException e) {
               Log.global.log(Level.WARNING, e.getMessage(), e);
            }
         }
         return new GaussDistribution(getNeighborhood());
      }

      private boolean hasStrongReference() {
         return neighborhood != null;
      }

      public Neighborhood getNeighborhood() {
         // Do we have a strong reference:
         if (neighborhood != null) {
            return neighborhood;
         }

         // If not, do we have a soft reference:
         Neighborhood softNeighborhood = neighborhoodOnFile.get();
         if (softNeighborhood != null) {
            return softNeighborhood;
         }

         // If not, read from file and create a soft reference:
         softNeighborhood = readNeighborhood(scatterFile());
         neighborhoodOnFile = new SoftReference<>(softNeighborhood);
         return softNeighborhood;
      }

      private void setNeighborhood(@Nullable Neighborhood neighborhood) {
         this.neighborhood = neighborhood;
         neighborhoodOnFile = new SoftReference<>(neighborhood);
         gaussDistribution = null; // recomputed when needed
      }

      private void save() throws IOException {
         saveGaussDistribution(getGaussDistribution(), gaussFile());
         saveNeighborhood(neighborhood, scatterFile());
         neighborhoodOnFile = new SoftReference<>(neighborhood);
         neighborhood = null;
      }

      private Path scatterFile() {
         return getFile("scatter", ".xml");
      }

      private Path gaussFile() {
         return getFile("gauss", ".xml");
      }

      private Path getFile(String baseName, String suffix) {
         return categoryNeighborhood.getDirectory().resolve(baseName + distributionLevel.getBaseNameSuffix() + suffix);
      }
   }

   /**
    * Represents one set of extracted points stored on a file.
    */
   public static final class CategoryNeighborhood {
      private final Category category;
      private final Path directory;

      private final Map<DistributionLevel, NeighborhoodData> neighborhoodDataMap = new EnumMap<>(DistributionLevel.class);

      private boolean deleted = false;
      private @Nullable Element echogramWindowInfo;
      private @Nullable Document moduleConfiguration;
      private @Nullable BufferedImage echogramWindowImage;

      private CategoryNeighborhood(Category category, Path directory, @Nullable NeighborhoodInitialization neighborhoodInitialization) {
         this.category = category;
         this.directory = directory;

         for (DistributionLevel distributionLevel : DistributionLevel.values()) {
            neighborhoodDataMap.put(distributionLevel, new NeighborhoodData(this, distributionLevel));
         }

         if (neighborhoodInitialization != null) {
            setNeighborhoodData(neighborhoodInitialization);

            EchogramWindow echogramWindow = neighborhoodInitialization.echogramWindow();
            echogramWindowInfo = echogramWindow.infoAsXml();
            echogramWindowImage = echogramWindow.getImage();
            moduleConfiguration = echogramWindow.getConfigDocument();
         }
      }

      private CategoryNeighborhood(Category category, Path directory) {
         this(category, directory, null);
      }

      private CategoryNeighborhood(Category category, NeighborhoodInitialization neighborhoodInitialization) {
         this(category, category.historyDirectory().resolve(neighborhoodInitialization.getDirName()), neighborhoodInitialization);
      }

      public Category getCategory() {
         return category;
      }

      /**
       * Returns the directory where the files of this CategoryNeighborhood are stored.
       *
       * @return the directory where the files of this CategoryNeighborhood are stored
       */
      public Path getDirectory() {
         return directory;
      }

      public NeighborhoodData getPixelNeighborhoodData() {
         return getNeighborhoodData(DistributionLevel.PIXEL);
      }

      public NeighborhoodData getCellNeighborhoodData() {
         return getNeighborhoodData(DistributionLevel.CELL);
      }

      public NeighborhoodData getSchoolNeighborhoodData() {
         return getNeighborhoodData(DistributionLevel.SCHOOL);
      }

      public NeighborhoodData getNeighborhoodData(DistributionLevel distributionLevel) {
         return neighborhoodDataMap.get(distributionLevel);
      }

      private Collection<NeighborhoodData> getNeighborhoodDatas() {
         return neighborhoodDataMap.values();
      }

      /**
       * Sets the neighborhood of this CategoryNeighborhood.
       *
       * @param neighborhoodInitialization initialization data
       */
      public void setNeighborhood(NeighborhoodInitialization neighborhoodInitialization) {
         setNeighborhoodData(neighborhoodInitialization);
         category.recompute();
      }

      private void setNeighborhoodData(NeighborhoodInitialization neighborhoodInitialization) {
         getPixelNeighborhoodData().setNeighborhood(neighborhoodInitialization.echogramWindow().toNeighborhood());
         getCellNeighborhoodData().setNeighborhood(neighborhoodInitialization.collectiveFeatureComputation().getCellNeighborhood());
         getSchoolNeighborhoodData().setNeighborhood(neighborhoodInitialization.collectiveFeatureComputation().getAllNeighborhood());
      }

      /**
       * Returns whether this CategoryNeighborhood will be deleted at next call to
       * {@link Category#save}.
       *
       * @return {@code true} if this CategoryNeighborhood will be deleted
       */
      public boolean isDeleted() {
         return deleted;
      }

      /**
       * Sets whether this CategoryNeighborhood will be deleted at next call to
       * {@link Category#save}.
       *
       * @param deleted whether this CategoryNeighborhood will be deleted
       */
      public void setDeleted(boolean deleted) {
         this.deleted = deleted;
         category.recompute();
      }

      private void save() throws IOException {
         if (deleted) {
            for (NeighborhoodData neighborhoodData : getNeighborhoodDatas()) {
               neighborhoodData.setNeighborhood(null);
            }
            FileUtils.deleteRecursively(directory);
         } else if (getPixelNeighborhoodData().hasStrongReference()) {
            FileUtils.createDirectories(directory);

            for (NeighborhoodData neighborhoodData : getNeighborhoodDatas()) {
               neighborhoodData.save();
            }

            if (moduleConfiguration != null) {
               Path file = directory.resolve("modules.cds");
               XmlUtils.writeDocument(moduleConfiguration, file);
            }

            if (echogramWindowInfo != null) {
               Path file = directory.resolve("info.xml");
               XmlUtils.writeDocument(echogramWindowInfo, file);
            }

            if (echogramWindowImage != null) {
               Path file = directory.resolve("echogramWindow.png");
               ImageIO.write(echogramWindowImage, "png", file.toFile());
            }
         }
      }
   }

   public static final class CategoryDistribution {
      private final Category category;
      private final DistributionLevel distributionLevel;

      private @Nullable GaussDistribution gaussDistribution;
      private @Nullable Neighborhood neighborhood;

      private CategoryDistribution(Category category, DistributionLevel distributionLevel) {
         this.category = category;
         this.distributionLevel = distributionLevel;
      }

      public GaussDistribution getGaussDistribution() {
         if (gaussDistribution == null) {
            try {
               Document document = XmlUtils.readDocumentIfExists(gaussFile());
               if (document != null) {
                  gaussDistribution = new GaussDistribution(document.getRootElement());
               }
            } catch (IOException e) {
               Log.global.log(Level.WARNING, e.getMessage(), e);
            }
            if (gaussDistribution == null) {
               category.recompute();
            }
         }
         return gaussDistribution;
      }

      public Neighborhood getNeighborhood() {
         if (neighborhood == null) {
            neighborhood = readNeighborhood(scatterFile());
            if (neighborhood.getNeighbors().isEmpty()) {
               category.recompute();
            }
         }
         return neighborhood;
      }

      private Path scatterFile() {
         return getFile("scatter", ".xml");
      }

      private Path gaussFile() {
         return getFile("gauss", ".xml");
      }

      private Path getFile(String baseName, String suffix) {
         return category.categoryDirectory().resolve(baseName + distributionLevel.getBaseNameSuffix() + suffix);
      }

      public void save() throws IOException {
         if (neighborhood != null) {
            saveNeighborhood(neighborhood, scatterFile());
         }

         if (gaussDistribution != null) {
            saveGaussDistribution(gaussDistribution, gaussFile());
         }
      }

      private void recompute(ProgressHandler progressHandler, AsyncHandle asyncHandle, int thinnedScatterSize,
                             Collection<CategoryNeighborhood> categoryNeighborhoods) {
         neighborhood = new Neighborhood();
         gaussDistribution = new GaussDistribution(neighborhood);

         int newSize = (int) Math.ceil((double) thinnedScatterSize / (double) categoryNeighborhoods.size());

         Collection<Neighborhood> neighborhoods = new ArrayList<>();
         List<Neighbor> selectedNeighbors = new ArrayList<>();
         for (CategoryNeighborhood categoryNeighborhood : categoryNeighborhoods) {
            if (asyncHandle.isCancelled()) {
               return;
            }
            Neighborhood additionalNeighborhood = categoryNeighborhood.getNeighborhoodData(distributionLevel).getNeighborhood();
            neighborhoods.add(additionalNeighborhood);
            RandomUtils.stream(new Random(), additionalNeighborhood.getNeighbors(), newSize)
                  .forEach(selectedNeighbors::add);
            progressHandler.setProgress(0.9 * neighborhoods.size() / categoryNeighborhoods.size());
         }
         neighborhood = new Neighborhood(selectedNeighbors);
         gaussDistribution = new GaussDistribution(neighborhoods, asyncHandle, progressHandler.subHandlerForRange(0.9, 1));
      }
   }

   public enum Type {
      Aggregation, Track
   }

   private byte number = -1;
   private boolean enabled = true;
   private boolean active = true;
   private String name;
   private @Nullable String newName; // used when renaming a category, which is actually done on save
   private String legend;
   private @Nullable String longLegend;
   private @Nullable String comment;
   private Color color;
   private Type type = Type.Aggregation;
   private float apriori = 1;
   private Optional<Float> schoolApriori = Optional.empty();
   private Optional<Float> minLogSv38 = Optional.empty();
   private Optional<Float> maxLogSv38 = Optional.empty();
   private String featureRequirementExpression = "";
   private FeatureRequirement featureRequirement = FeatureRequirement.ALWAYS_TRUE;

   private CategoryAPriori categoryAPriori = new CategoryAPriori();

   private final Configurator configurator;
   private final Map<DistributionLevel, CategoryDistribution> categoryDistributionMap = new EnumMap<>(DistributionLevel.class);
   private final Collection<CategoryNeighborhood> categoryNeighborhoods = new ArrayList<>();

   /**
    * Creates a category given name and color.
    *
    * @param configurator a configurator
    * @param name         the name
    * @param color        the color
    */
   public Category(Configurator configurator, String name, Color color) {
      this.configurator = configurator;
      this.name = name;
      legend = name.substring(0, Math.min(name.length(), Configurator.CATEGORY_LEGEND_MAX_LENGTH));
      this.color = color;
      init();
   }

   /**
    * Creates a category from XML.
    *
    * @param configurator a configurator
    * @param element      an XML element
    */
   public Category(Configurator configurator, Element element) {
      this.configurator = configurator;

      enabled = Boolean.parseBoolean(element.attributeValue(Configurator.XML.ENABLED));
      active = enabled;
      name = element.attributeValue(Configurator.XML.NAME, "");
      legend = element.attributeValue(Configurator.XML.LEGEND, "");
      longLegend = element.attributeValue(Configurator.XML.LONG_LEGEND);
      Element commentElement = element.element(Configurator.XML.COMMENT);
      if (commentElement != null) {
         comment = Strings.emptyToNull(commentElement.getText().trim());
      }
      String colorName = element.attributeValue(Configurator.XML.COLOR, "");
      Color parsedColor = ColorUtils.parseColor(colorName);
      if (parsedColor != null) {
         color = parsedColor;
      } else {
         Log.global.log(Level.WARNING, "Illegal color: " + colorName);
         color = Color.BLACK;
      }

      String parsedType = element.attributeValue(Configurator.XML.TYPE);
      if (parsedType != null) {
         type = Type.valueOf(parsedType);
      }

      String parsedAPriori = element.attributeValue(Configurator.XML.APRIORI);
      if (parsedAPriori != null) {
         apriori = Float.parseFloat(parsedAPriori);
      }

      schoolApriori = parseOptionalFloat(element.attributeValue(Configurator.XML.SCHOOL_APRIORI));
      minLogSv38 = parseOptionalFloat(element.attributeValue(Configurator.XML.MIN_SV_38));
      maxLogSv38 = parseOptionalFloat(element.attributeValue(Configurator.XML.MAX_SV_38));

      String requirement = element.attributeValue(Configurator.XML.REQUIREMENT);
      if (requirement != null) {
         try {
            setFeatureRequirementExpression(requirement);
         } catch (CompileException e) {
            Log.global.log(Level.WARNING, "Could not compile requirement for category " + name + ": " + requirement, e);
         }
      }

      Element aPrioriElement = element.element(CategoryAPriori.XML_A_PRIORI);
      if (aPrioriElement != null) {
         categoryAPriori = new CategoryAPriori(aPrioriElement);
      }

      init();
   }

   private static Optional<Float> parseOptionalFloat(@Nullable String text) {
      return text == null || text.isBlank() ? Optional.empty() : Optional.of(Float.parseFloat(text));
   }

   /**
    * Common initialization for both constructors.
    */
   private void init() {
      for (DistributionLevel distributionLevel : DistributionLevel.values()) {
         categoryDistributionMap.put(distributionLevel, new CategoryDistribution(this, distributionLevel));
      }

      if (!isSpecial()) {
         readCategoryNeighborhoods();
      }
   }

   public Element toXml() {
      Element categoryElement = DocumentHelper.createElement(Configurator.XML.CATEGORY)
            .addAttribute(Configurator.XML.ENABLED, Boolean.toString(enabled))
            .addAttribute(Configurator.XML.NAME, getName())
            .addAttribute(Configurator.XML.LEGEND, legend)
            .addAttribute(Configurator.XML.LONG_LEGEND, longLegend)
            .addAttribute(Configurator.XML.COLOR, ColorUtils.colorToNameOrHex(color));
      if (comment != null) {
         categoryElement.addElement(Configurator.XML.COMMENT)
               .addText(comment);
      }
      if (!isSpecial()) {
         categoryElement
               .addAttribute(Configurator.XML.TYPE, type.name())
               .addAttribute(Configurator.XML.APRIORI, Utils.toString(apriori))
               .addAttribute(Configurator.XML.SCHOOL_APRIORI, schoolApriori.map(Utils::toString).orElse(""))
               .addAttribute(Configurator.XML.MIN_SV_38, minLogSv38.map(Utils::toString).orElse(""))
               .addAttribute(Configurator.XML.MAX_SV_38, maxLogSv38.map(Utils::toString).orElse(""))
               .addAttribute(Configurator.XML.REQUIREMENT, featureRequirementExpression);
      }
      Element aPrioriElement = categoryAPriori.toXml();
      if (aPrioriElement != null) {
         categoryElement.add(aPrioriElement);
      }
      return categoryElement;
   }

   @Override
   public byte getNumber() {
      return number;
   }

   void setNumber(byte number) {
      this.number = number;
   }

   public boolean isEnabled() {
      return enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
      if (!enabled) {
         active = false;
      }
   }

   public boolean isActive() {
      return active;
   }

   public void setActive(boolean active) {
      if (enabled) {
         this.active = active;
      }
   }

   @Override
   public String getName() {
      return newName != null ? newName : name;
   }

   public void setName(String name) {
      if (this.name.isEmpty()) {
         this.name = name;
      } else {
         newName = name;
      }
   }

   @Override
   public String getLegend() {
      return legend;
   }

   public void setLegend(String legend) {
      this.legend = legend;
   }

   public String getLongLegend() {
      return longLegend != null ? longLegend : name;
   }

   public void setLongLegend(@Nullable String longLegend) {
      this.longLegend = longLegend;
   }

   public @Nullable String getComment() {
      return comment;
   }

   public void setComment(@Nullable String comment) {
      this.comment = comment != null ? Strings.emptyToNull(comment.trim()) : null;
   }

   @Override
   public Color getColor() {
      return color;
   }

   public void setColor(Color color) {
      this.color = color;
   }

   public Type getType() {
      return type;
   }

   public void setType(Type type) {
      this.type = type;
   }

   public float getApriori() {
      return apriori;
   }

   public void setApriori(float apriori) {
      this.apriori = apriori;
   }

   public Optional<Float> getSchoolApriori() {
      return schoolApriori;
   }

   public void setSchoolApriori(Optional<Float> schoolApriori) {
      this.schoolApriori = schoolApriori;
   }

   public Optional<Float> getMinLogSv38() {
      return minLogSv38;
   }

   public void setMinLogSv38(Optional<Float> minLogSv38) {
      this.minLogSv38 = minLogSv38;
   }

   public Optional<Float> getMaxLogSv38() {
      return maxLogSv38;
   }

   public void setMaxLogSv38(Optional<Float> maxLogSv38) {
      this.maxLogSv38 = maxLogSv38;
   }

   public String getFeatureRequirementExpression() {
      return featureRequirementExpression;
   }

   public void setFeatureRequirementExpression(String featureRequirementExpression) throws CompileException {
      featureRequirementExpression = featureRequirementExpression.trim();

      // Assign featureRequirement first in case exception is thrown
      featureRequirement = FeatureRequirementFactory.create(featureRequirementExpression);
      this.featureRequirementExpression = featureRequirementExpression;
   }

   public FeatureRequirement getFeatureRequirement() {
      return featureRequirement;
   }

   public CategoryAPriori getCategoryAPriori() {
      return categoryAPriori;
   }

   /**
    * Returns whether this category is one of the special categories
    * {@link Configurator#SPECIAL_CATEGORIES}.
    *
    * @return {@code true} if this category is special
    */
   public boolean isSpecial() {
      return Configurator.SPECIAL_CATEGORIES.contains(name);
   }

   public CategoryDistribution getPixelCategoryDistribution() {
      return getCategoryDistribution(DistributionLevel.PIXEL);
   }

   public CategoryDistribution getCellCategoryDistribution() {
      return getCategoryDistribution(DistributionLevel.CELL);
   }

   public CategoryDistribution getSchoolCategoryDistribution() {
      return getCategoryDistribution(DistributionLevel.SCHOOL);
   }

   public CategoryDistribution getCategoryDistribution(DistributionLevel distributionLevel) {
      return categoryDistributionMap.get(distributionLevel);
   }

   public Collection<CategoryDistribution> getCategoryDistributions() {
      return categoryDistributionMap.values();
   }

   private static Neighborhood readNeighborhood(Path nonGzipFile) {
      try {
         // First, test for gzip version:
         Path gzipFile = FileUtils.addGzipSuffix(nonGzipFile);
         Document gzipDocument = XmlUtils.readDocumentIfExists(gzipFile);
         if (gzipDocument != null) {
            return new Neighborhood(gzipDocument.getRootElement());
         }
         // Secondly, test for non-gzip version:
         Document nonGzipDocument = XmlUtils.readDocumentIfExists(nonGzipFile);
         if (nonGzipDocument != null) {
            return new Neighborhood(nonGzipDocument.getRootElement());
         }
         return new Neighborhood();
      } catch (IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
         return new Neighborhood();
      }
   }

   private static void saveGaussDistribution(GaussDistribution gaussDistribution, Path file) throws IOException {
      if (gaussDistribution.isEmpty()) {
         Files.deleteIfExists(file);
      } else {
         XmlUtils.writeDocument(gaussDistribution.toXml(), file);
      }
   }

   private static void saveNeighborhood(@Nullable Neighborhood neighborhood, Path nonGzipFile) throws IOException {
      Path gzipFile = FileUtils.addGzipSuffix(nonGzipFile);
      if (neighborhood == null || neighborhood.getNeighbors().isEmpty()) {
         Files.deleteIfExists(gzipFile);
      } else {
         neighborhood.save(gzipFile);
      }
      Files.deleteIfExists(nonGzipFile);
   }

   /**
    * Returns all neighborhoods of this category.
    *
    * @return all neighborhoods of this category
    */
   public Collection<CategoryNeighborhood> getCategoryNeighborhoods() {
      return categoryNeighborhoods;
   }

   /**
    * Adds a {@link Neighborhood} extracted from an {@link EchogramWindow} to this category.
    *
    * @param neighborhoodInitialization neighborhood data
    * @return the neighborhood created from the echogram window
    */
   public CategoryNeighborhood addNeighborhood(NeighborhoodInitialization neighborhoodInitialization) {
      CategoryNeighborhood categoryNeighborhood = createNeighborhood(neighborhoodInitialization);
      //categoryNeighborhood.setNeighborhood(Neighborhood.createRandom(configurator.getFeatureExtractors(), 1234));
      categoryNeighborhoods.add(categoryNeighborhood);
      recompute();
      return categoryNeighborhood;
   }

   public CategoryNeighborhood createNeighborhood(NeighborhoodInitialization neighborhoodInitialization) {
      return new CategoryNeighborhood(this, neighborhoodInitialization);
   }

   /**
    * Saves changes to this category.
    *
    * @throws IOException if some IO error occurs
    */
   public void save() throws IOException {
      if (isSpecial()) {
         // nothing to save for special categories.
         return;
      }

      FileUtils.createDirectories(categoryDirectory());
      FileUtils.createDirectories(historyDirectory());

      for (CategoryNeighborhood categoryNeighborhood : categoryNeighborhoods) {
         categoryNeighborhood.save();
      }

      for (CategoryDistribution categoryDistribution : getCategoryDistributions()) {
         categoryDistribution.save();
      }

      if (newName != null && !newName.equals(name)) { // rename directory after all other savings
         Path oldDir = categoryDirectory();
         name = newName;
         newName = null;
         Path newDir = categoryDirectory();
         FileUtils.move(oldDir, newDir);
      }

      readCategoryNeighborhoods(); // must reread in case something has been deleted.

      if (categoryNeighborhoods.isEmpty()) {
         FileUtils.deleteRecursively(historyDirectory());
      }
   }

   private void readCategoryNeighborhoods() {
      categoryNeighborhoods.clear();
      List<FileInfo> fileInfos;
      try {
         fileInfos = FileUtils.listFilesWithAttributes(historyDirectory());
      } catch (IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
         return;
      }
      for (FileInfo fileInfo : fileInfos) {
         if (fileInfo.isDirectory()) {
            categoryNeighborhoods.add(new CategoryNeighborhood(this, fileInfo.file()));
         }
      }
   }

   /**
    * Recomputes Gauss distribution (using all extracted points) and thinned scatter.
    */
   public void recompute() {
      recompute(new AsyncHandle(), ProgressHandler.ignore());
   }

   /**
    * Recomputes Gauss distribution (using all extracted points) and thinned scatter.
    * For each processed category neighborhood the progress is incremented by one, and
    * the progress monitor is notified.
    *
    * @param asyncHandle     async handle
    * @param progressHandler progress handler
    */
   public void recompute(AsyncHandle asyncHandle, ProgressHandler progressHandler) {
      Collection<CategoryDistribution> categoryDistributions = getCategoryDistributions();
      int n = configurator.thinnedScatterSize.getIntValue();
      int i = 0;
      for (CategoryDistribution categoryDistribution : categoryDistributions) {
         categoryDistribution.recompute(progressHandler.subHandlerForPart(i++, categoryDistributions.size()), asyncHandle, n, categoryNeighborhoods);
      }
   }

   Path categoryDirectory() {
      return configurator.getCategoriesDirectory().resolve(name);
   }

   private Path historyDirectory() {
      return categoryDirectory().resolve("history");
   }

   @Override
   public String toString() {
      return number + ", " + getName() + ", " + legend + ", " + ColorUtils.colorToNameOrHex(color);
   }
}
