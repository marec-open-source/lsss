package no.imr.lsss.modules.frequencyresponse;

import no.imr.korona.computation.categorization.Category;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.computation.feature.CategoryVisualizer;
import no.imr.korona.computation.feature.FrequencyMapping;
import no.imr.korona.computation.feature.FrequencyResponseAxis;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.region.Region;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.util.SvSum;
import no.imr.korona.viewer.util.FrequencySelectionPanel;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.interpretation.InterpretationModule;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.util.FrequencyPlotMarker;
import no.imr.tools.UnionList;
import no.imr.tools.Utils;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.logging.Log;
import no.imr.tools.math.Function1D;
import no.imr.tools.math.WelfordsMethod;
import no.imr.tools.math.linalg.Vec2;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.Plotter;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeBuilder;
import no.imr.tools.swing.UiUtils;
import no.imr.tools.swing.ViewHolder;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.ColumnArrangement;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleAnchor;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Displays the frequency response for the currently selected region(s).
 */
public final class FrequencyResponseModule extends BaseViewModule implements PojoDataContainer {
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private final Listener refreshListener = newCoalescingExecListener(this::refresh);
   private final FrequencySelectionPanel frequencySelectionPanel = new FrequencySelectionPanel(refreshListener);

   private final Map<Region, RegionCache> regionMap = new HashMap<>();

   private final ObjectParameter<FrequencyMapping> xAxis = new ObjectParameter<>(
         new Name("XAxis", "X-axis"),
         FrequencyMapping.SQRT, FrequencyMapping.values(),
         "Mapping of frequencies along x-axis");

   private final ObjectParameter<FrequencyResponseAxis> yAxis = new ObjectParameter<>(
         new Name("YAxis", "Y-axis"),
         FrequencyResponseAxis.DYNAMIC, FrequencyResponseAxis.values(),
         "Type of y-axis");

   private final BooleanParameter plotStdErr = new BooleanParameter(
         new Name("PlotStdErr", "Plot std. err."),
         true,
         "Plot standard error");

   private final BooleanParameter showLegends = new BooleanParameter(
         new Name("ShowLegends", "Show legends"),
         false,
         "Show legends for the selected KORONA categories");

   private final BooleanParameter plotInterpretationFunction = new BooleanParameter(
         new Name("PlotInterpretationFunction", "Plot interpretation function"),
         false,
         "Plot the frequency response function configured for the interpretation module");

   private final KoronaCategoriesSelection koronaCategoriesSelection = new KoronaCategoriesSelection();

   private int normalizationChannel;
   private final FrequencyPlotMarker frequencyPlotMarker;
   private JFreeChart chart = PlotUtils.newEmptyChart();

   public FrequencyResponseModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      frequencyPlotMarker = new FrequencyPlotMarker(getLSSS());
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            xAxis,
            yAxis,
            plotStdErr,
            showLegends,
            plotInterpretationFunction,
            koronaCategoriesSelection.plotCategories,
            koronaCategoriesSelection.koronaCategories
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getConfigurationManager().getSurveyMiscConf().mainFrequency, newCoalescingExecListener(this::findNormalizationChannel));

      registry.add(getInterpretationSettings().getDataFileChangeManager(), newCoalescingExecListener(this::dataFilesChanged));

      registry.add(getInterpretationSettings().getChannelChangeManager(), newCoalescingExecListener(channel -> {
         frequencySelectionPanel.setHighlighted(channel);
         refreshListener.listen();
      }));

      registry.add(getInterpretationSettings().mouseover().kHz(), newCoalescingExecListener(frequencyPlotMarker::updateMarker));

      registry.add(newCoalescingExecListener(this::recompute), List.of(
            getInterpretationSettings().getPingRangeChangeManager(),
            getRegionManager().getThresholdManager().getChangeManager()
      ));

      registry.add(getModuleManager().getModule(InterpretationModule.class).frequencyResponseFunction, refreshListener);
      registry.add(getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticCategoryChangeManager(), refreshListener);
      registry.add(getParameters(), refreshListener);

      registry.add(getInterpretationSettings().getPingSampler().getNewPingsChangeManager(), newExecListener(this::processPings));

      registry.add(getRegionManager().selectedRegions(), refreshListener);
      registry.add(getRegionManager().getRegionDeletedChangeManager(), newExecListener(regions -> {
         regionMap.keySet().removeAll(regions);
         refreshListener.listen();
      }));
      registry.add(getRegionManager().getRegionDefinitionChangeManager(), newExecListener(regionEvent -> {
         PingRange pingRange = regionEvent.pingRange();
         for (Region region : regionEvent.regions()) {
            RegionCache regionCache = regionMap.get(region);
            if (regionCache != null) {
               regionCache.pingMap.subMap(pingRange.begin(), pingRange.end()).clear();
            }
         }
         refreshListener.listen();
      }));

      //---

      dataFilesChanged();
      recompute();
   }

   @Override
   protected void onDisable() {
      regionMap.clear();
   }

   private void dataFilesChanged() {
      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
      findNormalizationChannel();
      frequencySelectionPanel.update(rawFileConfiguration, getInterpretationSettings().getChannel());
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   public JComponent getChartPanel() {
      return viewHolder.getView().chartPanel;
   }

   private void recompute() {
      regionMap.clear();
      refreshListener.listen();
   }

   private void refresh() {
      processPings(getInterpretationSettings().getPingSampler().getAvailablePings());
   }

   private void findNormalizationChannel() {
      float mainFrequency = getConfigurationManager().getSurveyMiscConf().mainFrequency.getFloatValue();
      normalizationChannel = getInterpretationSettings().getDataFileSet().firstChannelClosestTo(mainFrequency);
      if (normalizationChannel <= 0 && !getInterpretationSettings().getDataFileSet().isEmpty()) {
         Log.global.info("Could not find normalization frequency " + mainFrequency);
      }
      refreshListener.listen();
   }

   private void processPings(List<Ping> pings) {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         plotNoGraphs();
         return;
      }
      List<Region> selectedRegions = getRegionManager().getSelectedRegions();
      if (selectedRegions.isEmpty()) {
         plotNoGraphs();
         return;
      }
      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
      int channelCount = rawFileConfiguration.getTransducerCount();
      SvSum svSum = new SvSum(channelCount);

      for (Region region : selectedRegions) {
         PingRange visibleRegionPingRange = region.getPingRange().intersection(pingRange);
         if (visibleRegionPingRange.isEmpty()) {
            continue;
         }

         RegionCache regionCache = regionMap.get(region);
         if (regionCache == null) {
            regionCache = new RegionCache(channelCount);
            regionMap.put(region, regionCache);
         }

         boolean regionNeedUpdate = false;

         for (Ping ping : pings) {
            PingIndex pingIndex = ping.getPingIndex();

            if (!visibleRegionPingRange.contains(pingIndex)) {
               continue;
            }

            PingCache pingCache = regionCache.pingMap.get(pingIndex);
            if (pingCache == null) {
               FloatRange svRange = getRegionManager().getThresholdManager().getLinearSvRange(pingIndex);
               pingCache = new PingCache(getRegionManager(), region, ping, channelCount, svRange);
               regionCache.pingMap.put(pingIndex, pingCache);
               regionNeedUpdate = true;
            }
         }

         if (regionNeedUpdate) {
            regionCache.update();
         }

         svSum.accumulate(regionCache.svSum);
      }

      float[] svAverage = new float[channelCount];
      float[] svStdErr = new float[channelCount];
      for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
         // Frequency check box selection mechanism
         int channel = channelIndex + 1;
         if (channel == getInterpretationSettings().getChannel() || frequencySelectionPanel.isChannelSelected(channel) || channel == normalizationChannel) {
            WelfordsMethod welfordsMethod = svSum.getWelfordsMethod()[channelIndex];
            long n = welfordsMethod.getCount();
            if (n <= 1) {
               svAverage[channelIndex] = 0;
               svStdErr[channelIndex] = 0;
               continue;
            }
            svAverage[channelIndex] = (float) welfordsMethod.getMean();
            svStdErr[channelIndex] = (float) welfordsMethod.getStdErr();
         }
      }

      float normalizingSv;
      if (normalizationChannel > 0) {
         normalizingSv = svAverage[normalizationChannel - 1];
         if (normalizingSv <= 0) {
            normalizingSv = 1;
         }
      } else {
         normalizingSv = 1;
      }

      XYInfo xyInfo = createXYInfo();

      Graph graphAverage = new Graph("Frequency response", channelCount)
            .setXYInfo(xyInfo)
            .setRenderer(() -> PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.SHAPES_AND_LINES))
            .setColor(Color.RED);

      Graph graphStdErrAbove = new Graph("Frequency response + std err", channelCount)
            .setXYInfo(xyInfo)
            .setDashed()
            .setColor(Color.BLACK);

      Graph graphStdErrBelow = new Graph("Frequency response - std err", channelCount)
            .setXYInfo(xyInfo)
            .setDashed()
            .setColor(Color.BLACK);

      float maxY = 0;
      for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
         // Frequency check box selection mechanism
         int channel = channelIndex + 1;
         if (channel == getInterpretationSettings().getChannel() || frequencySelectionPanel.isChannelSelected(channel)) {
            float x = rawFileConfiguration.getTransducers().get(channelIndex).getKHz();
            float average = svAverage[channelIndex] / normalizingSv;
            float stdErr = svStdErr[channelIndex] / normalizingSv;

            graphAverage.addPoint(x, average);
            graphStdErrAbove.addPoint(x, average + stdErr);
            graphStdErrBelow.addPoint(x, average - stdErr);

            float y = plotStdErr.getBooleanValue() ? average + stdErr : average;
            maxY = Math.max(maxY, y);
         }
      }

      List<Graph> graphs = new ArrayList<>(3);
      graphs.add(graphAverage);
      if (plotStdErr.getBooleanValue()) {
         graphs.add(graphStdErrAbove);
         graphs.add(graphStdErrBelow);
      }

      plotGraphs(graphs);
   }

   private static XYInfo createXYInfo() {
      return new XYInfo(
            new ParameterExport("frequency", Unit.KHZ, ExportRounding.kHz()),
            new ParameterExport("response", Unit.NONE, ExportTransform.round(1000)));
   }

   private void plotNoGraphs() {
      plotGraphs(List.of());
   }

   private JFreeChart createChart(List<Graph> graphs) {
      int legendBeginIndex = graphs.size();

      Set<Integer> kHzTicks = new TreeSet<>();
      FloatRangeBuilder kHzRangeBuilder = new FloatRangeBuilder();
      for (int channel : frequencySelectionPanel.getChannels()) {
         if (!(channel == getInterpretationSettings().getChannel() || frequencySelectionPanel.isChannelSelected(channel))) {
            continue;
         }
         int kHz = Utils.hzToKHz(getInterpretationSettings().getDataFileSet().getFrequency(channel));
         kHzRangeBuilder.expand(kHz);
         kHzTicks.add(kHz);
      }
      FloatRange kHzRange = kHzRangeBuilder.toFloatRange();

      if (koronaCategoriesSelection.plotCategories.getBooleanValue()) {
         graphs = new UnionList<>(graphs, createCategoryGraphs(kHzRange));
      }
      if (plotInterpretationFunction.getBooleanValue()) {
         graphs = new UnionList<>(graphs, List.of(createInterpretationGraph(kHzRange)));
      }

      graphs.forEach(Graph::sortPointsByX);

      FloatRange yRange = getYRange(graphs);

      JFreeChart newChart = new Plotter(graphs)
            .xAxis(() -> {
               return xAxis.getValue().axis(kHzRange, kHzTicks);
            })
            .yAxis(() -> {
               NumberAxis axis = yAxis.getValue().axis(yRange);
               if (yRange.isEmpty() && axis.isAutoRange()) {
                  axis.setRange(0.5, 1.5);
               }
               return axis;
            })
            .createChart();

      if (showLegends.getBooleanValue()) {
         addLegends(newChart.getXYPlot(), legendBeginIndex);
      }

      return newChart;
   }

   private static void addLegends(XYPlot plot, int legendBeginIndex) {
      LegendItemCollection legendItems = new LegendItemCollection();
      for (int i = 0, n = plot.getDatasetCount(); i < n; i++) {
         XYItemRenderer renderer = plot.getRenderer(i);
         LegendItem item = renderer.getLegendItem(i, 0);
         // Must call getLegendItem on all renderers, or the plotted shape might change.
         if (i >= legendBeginIndex) {
            legendItems.add(item);
         }
      }
      plot.setFixedLegendItems(legendItems);

      LegendTitle legendTitle = new LegendTitle(plot, new ColumnArrangement(), new ColumnArrangement());
      legendTitle.setItemFont(UiUtils.labelFont().deriveFont(Font.PLAIN, 10));
      legendTitle.setBackgroundPaint(new Color(0, 0, 0, 32));
      legendTitle.setMargin(0, 5, 5, 0);
      plot.addAnnotation(new XYTitleAnnotation(0, 0, legendTitle, RectangleAnchor.BOTTOM_LEFT));
   }

   private void plotGraphs(List<Graph> graphs) {
      chart = createChart(graphs);
      frequencyPlotMarker.addMarker(chart.getXYPlot());
      viewHolder.ifViewDelayed(this, View::updateChart);
   }

   private List<Graph> createCategoryGraphs(FloatRange kHzRange) {
      ConfigFileSettings configFileSettings;
      try {
         configFileSettings = getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().getMainSetup().createConfigFileSettings();
      } catch (IOException e) {
         return List.of();
      }
      Configurator configurator = new Configurator(configFileSettings, getInterpretationSettings().getDataFileSet().getRawFileConfiguration());
      List<Category> categories = configurator.getNonSpecialEnabledCategories();
      koronaCategoriesSelection.update(categories);

      XYInfo xyInfo = createXYInfo();
      List<Graph> graphs = new ArrayList<>();
      for (Category category : categories) {
         if (koronaCategoriesSelection.isSelected(category)) {
            Category.CategoryDistribution distribution = category.getCategoryDistribution(Category.DistributionLevel.PIXEL);
            graphs.add(CategoryVisualizer.responseGraph(category, distribution.getGaussDistribution(), configurator, kHzRange, false, xyInfo).getFirst());
         }
      }
      return graphs;
   }

   private Graph createInterpretationGraph(FloatRange kHzRange) {
      InterpretationModule interpretationModule = getModuleManager().getModule(InterpretationModule.class);
      Function1D function = interpretationModule.frequencyResponseFunction.getFunction();
      int n = 100;
      Graph graph = new Graph("Interpretation function", n)
            .setXYInfo(createXYInfo())
            .setColor(Color.BLACK);
      if (!kHzRange.isEmpty()) {
         float delta = kHzRange.getSize() / (n - 1);
         for (int i = 0; i < n; i++) {
            float kHz = kHzRange.min() + i * delta;
            graph.addPoint(kHz, function.eval(kHz * 1000));
         }
      }
      return graph;
   }

   private static FloatRange getYRange(List<Graph> graphs) {
      FloatRangeBuilder yRangeBuilder = new FloatRangeBuilder();
      yRangeBuilder.expand(1);
      for (Graph graph : graphs) {
         for (Vec2 point : graph.getPoints()) {
            yRangeBuilder.expand(point.y());
         }
      }
      return yRangeBuilder.toFloatRange();
   }

   @Override
   public PojoData getPojoData() {
      return PojoData.newBuilder(getPersistentName())
            .withDatasets(chart.getXYPlot())
            .build();
   }

   private static final class View extends BaseView {
      private final FrequencyResponseModule module;
      private final JPanel mainPanel = new JPanel(new BorderLayout());
      private final ChartPanel chartPanel;

      private View(FrequencyResponseModule module) {
         super(module);

         this.module = module;
         chartPanel = PlotUtils.newChartPanel(module.chart);
         module.frequencyPlotMarker.addMouseListener(chartPanel);
         mainPanel.add(chartPanel);
         mainPanel.add(module.frequencySelectionPanel.getComponent(), BorderLayout.SOUTH);
      }

      @Override
      public JComponent getComponent() {
         return mainPanel;
      }

      private void updateChart() {
         chartPanel.setChart(module.chart);
      }
   }
}
