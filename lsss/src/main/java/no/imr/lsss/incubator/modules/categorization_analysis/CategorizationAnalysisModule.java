package no.imr.lsss.incubator.modules.categorization_analysis;

import no.imr.korona.color.Colormaps;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.Cad0Datagram;
import no.imr.korona.data.datagrams.Cas0Datagram;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.region.ChannelInterpretation;
import no.imr.korona.region.Region;
import no.imr.korona.region.School;
import no.imr.korona.viewer.coloring.CategoryColorConverter;
import no.imr.korona.viewer.coloring.ColorConverter;
import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.korona.viewer.coloring.SingleValueColorConverter;
import no.imr.korona.viewer.variables.categorization.CategoryVariable;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.framework.NavigationHistory;
import no.imr.lsss.framework.config.ConfigurationManager;
import no.imr.lsss.framework.config.survey.data.DataConf;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.incubator.LsssIncubatorFeaturePlugin;
import no.imr.lsss.incubator.modules.categorization_analysis.pojo.AnalysisResult;
import no.imr.lsss.incubator.modules.categorization_analysis.pojo.BackscatterValues;
import no.imr.lsss.incubator.modules.categorization_analysis.pojo.CategoryResult;
import no.imr.lsss.incubator.modules.categorization_analysis.pojo.OverallResult;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.echogram.PelagicEchogramModule;
import no.imr.lsss.modules.frequencyresponse.FrequencyResponseModule;
import no.imr.lsss.modules.korona.region.KoronaRegionLSSS;
import no.imr.lsss.modules.korona.region.KoronaRegionModule;
import no.imr.tools.ResourceUtils;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.ButtonParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.SeparatorParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeBuilder;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.WorkerDialog;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class CategorizationAnalysisModule extends BaseViewModule {
   private static final String ANALYSIS_RESULT_JSON = "analysisResult.json";

   private final Map<String, FloatParameter> meanTS = new LinkedHashMap<>();

   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private final Supplier<KoronaRegionModule> koronaRegionModule = moduleSupplier(KoronaRegionModule.class);

   public CategorizationAnalysisModule(ModuleInfo<LsssIncubatorFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      setTSParameters(loadDefaultTSValues());
   }

   private void analyzeSchools(Component referenceComponent) {
      new WorkerDialog(referenceComponent, "Analyzing...")
            .start(this::analyze);
   }

   private void analyseAllFiles(Component referenceComponent) {
      getInterpretationSettings().doNonInteractively(() -> {
         DataConf dataConf = getConfigurationManager().getDataConf();
         List<SegmentHandle> segmentHandles = dataConf.getAllOriginalSegmentHandles();
         ProgressView progressView = new ProgressView("Analyzing...", segmentHandles.size());
         new WorkerDialog(referenceComponent, progressView.getComponent())
               .start(asyncHandle -> {
                  for (SegmentHandle segmentHandle : segmentHandles) {
                     if (asyncHandle.isCancelled()) {
                        return;
                     }
                     progressView.incrementMainProgress("");
                     dataConf.selectSegmentHandles(List.of(segmentHandle));
                     GuiUtils.invokeNowOrWait(getConfigurationManager()::ok);
                     selectAllSchools();
                     getInterpretationSettings().waitUntilFinished();
                     Utils.sleep(500);
                     analyze(asyncHandle);
                  }
               });
      });
   }

   private void selectAllSchools() {
      getRegionManager().replaceSelectedRegions(getRegionManager().getSchoolManager().getSchools());
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      List<BaseParameter<?>> parameters = new ArrayList<>();
      parameters.addAll(meanTS.values());
      parameters.add(SeparatorParameter.space());
      parameters.add(new ButtonParameter(new Name("RestoreTS", "Restore default TS"),
            "Restore mean TS values to default",
            () -> setTSParameters(loadDefaultTSValues())));
      return parameters;
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   private static Map<String, Float> loadDefaultTSValues() {
      BackscatterValues defaultBackscatterValues = ResourceUtils.getJson("no/imr/lsss/resources/incubator/modules/categorization_analysis/defaultBackscatterValues.json", BackscatterValues.class);
      return defaultBackscatterValues.backscatterValues;
   }

   private void setTSParameters(Map<String, Float> tsValues) {
      tsValues.forEach((category, ts) -> {
         FloatParameter parameter = meanTS.get(category);
         if (parameter != null) {
            parameter.setFloatValue(ts);
         } else {
            meanTS.put(category, new FloatParameter(new Name(category, "Mean TS for " + category), ts, Unit.DB));
         }
      });
   }

   private void analyze(AsyncHandle asyncHandle) {
      ColorConverter colorConverter = getInterpretationSettings().getColorConverterContainer().getColorConverter();
      getInterpretationSettings().getNavigationHistory().doWithNoAddCheckPoint(() -> {
         NavigationHistory.NavigationState currentState = getInterpretationSettings().getNavigationHistory().getCurrentState();
         List<Region> selectedRegions = getRegionManager().getSelectedRegions();
         for (Region region : selectedRegions) {
            if (asyncHandle.isCancelled()) {
               break;
            }
            if (!(region instanceof School)) {
               continue;
            }
            try {
               analyze(region);
            } catch (IOException e) {
               Log.global.log(Level.WARNING, e.getMessage(), e);
            }
         }
         getRegionManager().replaceSelectedRegions(selectedRegions);
         currentState.apply();
      });
      getInterpretationSettings().getColorConverterContainer().setColorConverter(colorConverter);
   }

   private @Nullable Path getOutputDirectory() {
      Path exportDir = getConfigurationManager().getDataConf().getDir(DataConfLSSS.EXPORT_SUB_DIR).getFile();
      return exportDir != null ? exportDir.resolve("categorizationAnalysis") : null;
   }

   private void analyze(Region region) throws IOException {
      getRegionManager().replaceSelectedRegions(region);
      PingRange pingRange = region.getPingRange();
      long margin = Math.max(1, Math.round(0.05 * pingRange.getPingCount()));
      PingIndex begin = getInterpretationSettings().getDataFileSet().getPingIndexClamped(pingRange.begin().getPingNumber() - margin);
      PingIndex end = getInterpretationSettings().getDataFileSet().getPingIndexClamped(pingRange.end().getPingNumber() + margin);
      getInterpretationSettings().setPingRange(PingRange.of(begin, end));

      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      Cac0Datagram cac0 = dataFileSet.getConfigurationItem(Cac0Datagram.class);
      if (cac0 == null) {
         Log.global.warning("No CAC0 datagram");
         return;
      }

      int channel = getInterpretationSettings().getChannel();
      CategoryVariable categoryVariable = getInterpretationSettings().getColorConverterContainer().getDiscreteVariable(CategoryVariable.class);

      int maxCategoryNumber = 0;
      for (Cac0Datagram.Category category : cac0.getCategories()) {
         maxCategoryNumber = Math.max(maxCategoryNumber, category.getNumber());
      }
      @Nullable CategorySum[] categorySums = new CategorySum[maxCategoryNumber + 1];
      for (Cac0Datagram.Category category : cac0.getCategories()) {
         categorySums[category.getNumber()] = new CategorySum(null, category.getName());
      }

      getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticToCategory().getAcousticCategoryToKoronaCategories().forEach((acousticCategory, names) -> {
         CategorySum categorySum = new CategorySum(acousticCategory, names.stream().sorted().collect(Collectors.joining(", ")));
         FloatParameter meanTsParameter = meanTS.get(getConfigurationManager().getLanguageUtils().getAcCatInitials(acousticCategory));
         if (meanTsParameter != null) {
            categorySum.meanTS = meanTsParameter.getValue();
         }
         names.stream()
               .map(cac0::nameToCategory)
               .filter(Objects::nonNull)
               .forEach(category -> categorySums[category.getNumber()] = categorySum);
      });

      FloatRangeBuilder depthRangeBuilder = new FloatRangeBuilder();
      for (PingIndex pingIndex : dataFileSet.getPingIndices(pingRange)) {
         Ping ping = dataFileSet.getPing(pingIndex);
         PowerData powerData = ping.getPowerData(channel);
         if (powerData == null) {
            continue;
         }
         float[] sv = powerData.getSv();

         Cad0Datagram cad0 = ping.getPingItem(Cad0Datagram.class);
         if (cad0 == null) {
            continue;
         }

         int[] schoolCategory = new int[powerData.getCount()];
         Arrays.fill(schoolCategory, -1);
         for (KoronaRegionLSSS koronaRegionLSSS : koronaRegionModule.get().getKoronaRegions()) {
            Cas0Datagram cas0 = koronaRegionLSSS.getCas0Datagram();
            if (cas0 == null) {
               continue;
            }
            FloatRangeSet depthRangeSet = koronaRegionLSSS.getMask().get(pingIndex);
            if (depthRangeSet == null) {
               continue;
            }
            for (FloatRange depthRange : depthRangeSet) {
               int iBegin = powerData.depthToClampedSampleIndex(depthRange.min());
               int iEnd = powerData.depthToClampedSampleIndex(depthRange.max());
               Arrays.fill(schoolCategory, iBegin, iEnd, cas0.getBestCategory());
            }
         }

         double dx = DataUtils.getPingWidthMeters(dataFileSet, pingIndex);
         double dy = powerData.getSampleDistance();
         double sampleArea = dx * dy;

         for (FloatRange depthRange : getRegionManager().getDepthRangesForChannel(region, ping, channel)) {
            depthRangeBuilder.expand(depthRange);

            int iBegin = Math.max(0, powerData.depthToSampleIndex(depthRange.min()));
            int iEnd = Math.min(powerData.getCount(), powerData.depthToSampleIndex(depthRange.max()));

            for (int i = iBegin; i < iEnd; i++) {
               int category = schoolCategory[i];
               if (category == -1) {
                  float depth = powerData.getSampleDepth(i);
                  category = cad0.getBestCategory(cad0.depthToIndex(depth), categoryVariable, cac0.getUnknownCategory().getNumber());
               }
               CategorySum categorySum = categorySums[category];
               if (categorySum != null) {
                  categorySum.accumulatePixel(sv[i], sampleArea);
               }
            }
         }
      }
      FloatRange depthRange = depthRangeBuilder.toFloatRange();
      getInterpretationSettings().getPelagicZSettings().setZ(depthRange.zoom(1.5f));
      getInterpretationSettings().waitUntilFinished();

      export(pingRange, categorySums, region.getChannelInterpretation(channel));
   }

   private void export(PingRange pingRange, @Nullable CategorySum[] categorySums, ChannelInterpretation channelInterpretation) throws IOException {
      DateTimeFormatter dateFormat = Utils.createUTCDateTimeFormatter("yyyy-MM-dd_HHmm");
      String subfolder = dateFormat.format(pingRange.begin().getInstant())
            + "___" + dateFormat.format(pingRange.end().getInstant());
      Path outputDirectory = getOutputDirectory();
      if (outputDirectory == null) {
         return;
      }
      Path dir = outputDirectory.resolve(subfolder);
      FileUtils.createDirectories(dir);
      Log.global.info("Exporting to " + dir);

      double totalAbundance = 0;
      AnalysisResult analysisResult = new AnalysisResult();
      analysisResult.name = subfolder;
      analysisResult.dir = subfolder;
      analysisResult.beginTime = pingRange.begin().getTimeInMillis();
      analysisResult.endTime = pingRange.end().getTimeInMillis();
      for (CategorySum categorySum : new LinkedHashSet<>(Arrays.asList(categorySums))) {
         if (categorySum == null) {
            continue;
         }
         if (Configurator.SPECIAL_CATEGORIES.contains(categorySum.categoryName)) {
            continue;
         }
         float ts = categorySum.meanTS;
         double meanBackscatterSigma = ts != 0 ? 10 * Math.pow(10, ts / 10) : 0;
         double abundance = meanBackscatterSigma != 0 ? categorySum.areaSum * categorySum.getMeanSv() / meanBackscatterSigma : 0;
         totalAbundance += abundance;
         CategoryResult categoryResult = new CategoryResult();
         categoryResult.name = categorySum.categoryName;
         categoryResult.pixelCount = categorySum.pixelCount;
         categoryResult.area = categorySum.areaSum;
         categoryResult.meanSv = categorySum.getMeanSv();
         categoryResult.abundance = abundance;
         categoryResult.backscatterTS = ts;
         categoryResult.backscatterSigma = meanBackscatterSigma;
         categoryResult.assignment = categorySum.acousticCategory != null
               ? channelInterpretation.getAssignment(categorySum.acousticCategory.getCompId().getAcousticCategory())
               : 0;
         analysisResult.categories.add(categoryResult);
      }
      double fitSum = 0;
      int fitN = 0;
      double weightedFit = 0;
      for (CategoryResult categoryResult : analysisResult.categories) {
         categoryResult.abundanceFraction = totalAbundance == 0 ? 0 : categoryResult.abundance / totalAbundance;
         categoryResult.fit = 1 - Math.abs(categoryResult.abundanceFraction - categoryResult.assignment);
         categoryResult.weightedFit = categoryResult.fit * categoryResult.assignment;
         if (categoryResult.assignment > 0) {
            fitSum += categoryResult.fit;
            fitN++;
         }
         weightedFit += categoryResult.weightedFit;
      }
      analysisResult.fit = fitN == 0 ? 0 : fitSum / fitN;
      analysisResult.weightedFit = weightedFit;
      JsonUtils.writeValuePrettily(dir.resolve(ANALYSIS_RESULT_JSON), analysisResult);

      ColorConverterContainer colorConverterContainer = getInterpretationSettings().getColorConverterContainer();
      colorConverterContainer.setColorConverter(new SingleValueColorConverter(colorConverterContainer.getSV(), Colormaps.COMBINED));
      saveEchogram(dir.resolve("echogram.png"));

      colorConverterContainer.setColorConverter(new CategoryColorConverter(colorConverterContainer.getDiscreteVariable(CategoryVariable.class)));
      saveEchogram(dir.resolve("categorization.png"));

      BufferedImage image = GuiUtils.getNowOrWait(() -> GuiUtils.toImage(getModuleManager().getModule(FrequencyResponseModule.class).getChartPanel()));
      ImageIO.write(image, "png", dir.resolve("frequencyResponse.png").toFile());
   }

   private void saveEchogram(Path file) throws IOException {
      PelagicEchogramModule echogramModule = getModuleManager().getModule(PelagicEchogramModule.class);
      getInterpretationSettings().waitUntilFinished();
      Utils.sleep(500);
      BufferedImage image = GuiUtils.getNowOrWait(() -> GuiUtils.toImage(echogramModule.getViewHolder().getView().getApiComponent()));
      ImageIO.write(image, "png", file.toFile());
   }

   private void makeReport() {
      try {
         Path outputDirectory = getOutputDirectory();
         if (outputDirectory == null) {
            return;
         }
         FileUtils.createDirectories(outputDirectory);
         List<AnalysisResult> analysisResults = new ArrayList<>();
         for (Path directory : FileUtils.listFiles(outputDirectory, Files::isDirectory)) {
            analysisResults.add(JsonUtils.JSON_MAPPER.readValue(directory.resolve(ANALYSIS_RESULT_JSON), AnalysisResult.class));
         }
         analysisResults.sort(Comparator.comparing(analysisResult -> analysisResult.name));
         double fitSum = 0;
         double weightedFitSum = 0;
         for (AnalysisResult analysisResult : analysisResults) {
            fitSum += analysisResult.fit;
            weightedFitSum += analysisResult.weightedFit;
         }
         OverallResult overallResult = new OverallResult();
         overallResult.analysisResults = analysisResults;
         overallResult.fit = fitSum == 0 ? 0 : fitSum / analysisResults.size();
         overallResult.weightedFit = weightedFitSum == 0 ? 0 : weightedFitSum / analysisResults.size();
         String js = "var categorizationAnalysisOverallResult = " + JsonUtils.PRETTY_PRINTER.writeValueAsString(overallResult) + ';';
         Files.writeString(outputDirectory.resolve("categorizationAnalysisOverallResult.js"), js, Utils.UTF_8);
         FileUtils.copy(ResourceUtils.getUrl("no/imr/lsss/resources/incubator/modules/categorization_analysis/index.html"), outputDirectory.resolve("index.html"));
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error making report", e);
      }
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   private static final class CategorySum {
      private final @Nullable AcousticCategory acousticCategory;
      private final String categoryName;
      private float meanTS;
      private int pixelCount;
      private double areaSum;
      private double svTimesAreaSum;

      private CategorySum(@Nullable AcousticCategory acousticCategory, String categoryName) {
         this.acousticCategory = acousticCategory;
         this.categoryName = categoryName;
      }

      private double getMeanSv() {
         return areaSum == 0 ? 0 : svTimesAreaSum / areaSum;
      }

      private void accumulatePixel(double sv, double area) {
         pixelCount++;
         areaSum += area;
         svTimesAreaSum += sv * area;
      }
   }

   private static final class View extends BaseView {
      private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

      private View(CategorizationAnalysisModule module) {
         super(module);

         JPanel buttonsPanel = new JPanel(new GridLayout(0, 1));
         buttonsPanel.add(makeButton("Select next file", _ -> {
            ConfigurationManager configurationManager = module.getLSSS().getConfigurationManager();
            configurationManager.getDataConf().selectNextFiles();
            configurationManager.ok();
            module.selectAllSchools();
         }));
         buttonsPanel.add(makeButton("Select all schools", _ -> module.selectAllSchools()));
         buttonsPanel.add(makeButton("Analyze schools", _ -> module.analyzeSchools(panel)));
         buttonsPanel.add(makeButton("Analyze all files", _ -> module.analyseAllFiles(panel)));
         buttonsPanel.add(makeButton("Make report", _ -> module.makeReport()));
         buttonsPanel.add(makeButton("Open browser", _ -> openBrowser(module)));

         panel.setBackground(Color.WHITE);
         panel.add(buttonsPanel);
      }

      @Override
      public JComponent getComponent() {
         return panel;
      }

      private static JButton makeButton(String text, ActionListener actionListener) {
         JButton button = new JButton(text);
         button.addActionListener(actionListener);
         return button;
      }

      private void openBrowser(CategorizationAnalysisModule module) {
         Path outputDirectory = module.getOutputDirectory();
         if (outputDirectory == null) {
            JOptionPane.showMessageDialog(panel, "No output directory configured");
            return;
         }
         GuiUtils.desktopBrowse(outputDirectory.resolve("index.html").toUri(), panel);
      }
   }
}
