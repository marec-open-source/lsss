package no.imr.lsss.modules.thresholdresponse;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.region.Region;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.viewer.util.FrequencySelectionButton;
import no.imr.korona.viewer.util.FrequencySelectionPanel;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.tools.Utils;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.Plotter;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.ViewHolder;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays the threshold response.
 * <p>
 * The threshold response curve, t = t(dB), is defined as
 * <blockquote><code>
 * t(dB) = s<sub>A</sub>(-inf, dB)
 * </code></blockquote>
 * so that
 * <blockquote><code>
 * s<sub>A</sub>(dB<sub>low</sub>, dB<sub>high</sub>) = t(dB<sub>high</sub>) - t(dB<sub>low</sub>)
 * </code></blockquote>
 */
public final class ThresholdResponseModule extends BaseViewModule implements PojoDataContainer {
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private final Listener refreshListener = newCoalescingExecListener(this::refresh);
   private final FrequencySelectionPanel frequencySelectionPanel = new FrequencySelectionPanel(refreshListener);

   private final Map<Region, RegionCache> regionMap = new HashMap<>();

   private float[][] selectedRegionsIntegratedSv = new float[0][Histogram.CELL_COUNT];

   private JFreeChart chart = PlotUtils.newEmptyChart();

   public ThresholdResponseModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(newCoalescingExecListener(this::recompute), List.of(
            getConfigurationManager().getGridConf().horizontalGridUnit,
            getInterpretationSettings().getPingRangeChangeManager()
      ));

      registry.add(getRegionManager().getThresholdManager().getChangeManager(), refreshListener);

      registry.add(getInterpretationSettings().getDataFileChangeManager(), newCoalescingExecListener(this::dataFilesChanged));

      registry.add(getInterpretationSettings().getChannelChangeManager(), newCoalescingExecListener(channel -> {
         frequencySelectionPanel.setHighlighted(channel);
         refreshListener.listen();
      }));

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
               regionCache.getPingMap().subMap(pingRange.begin(), pingRange.end()).clear();
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
      int transducerCount = rawFileConfiguration.getTransducerCount();

      if (transducerCount != selectedRegionsIntegratedSv.length) {
         selectedRegionsIntegratedSv = new float[transducerCount][Histogram.CELL_COUNT];
      }

      frequencySelectionPanel.update(rawFileConfiguration, getInterpretationSettings().getChannel());
   }

   private void recompute() {
      regionMap.clear();
      refreshListener.listen();
   }

   private void refresh() {
      processPings(getInterpretationSettings().getPingSampler().getAvailablePings());
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   private void processPings(List<Ping> pings) {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         plotOnlyThresholds();
         return;
      }

      PingMapping pingMapping = getConfigurationManager().getGridConf().horizontalGridUnit.getValue();

      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
      int channelCount = rawFileConfiguration.getTransducerCount();

      Utils.fill(selectedRegionsIntegratedSv, 0);

      PingRange selectedVisibleRegionPingRange = PingRange.EMPTY_RANGE;

      for (Region region : getRegionManager().getSelectedRegions()) {
         PingRange visibleRegionPingRange = region.getPingRange().intersection(pingRange);
         if (visibleRegionPingRange.isEmpty()) {
            continue;
         }

         selectedVisibleRegionPingRange = selectedVisibleRegionPingRange.union(visibleRegionPingRange);

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

            PingCache pingCache = regionCache.getPingMap().get(pingIndex);
            if (pingCache == null) {
               pingCache = new PingCache(getRegionManager(), ping, region, channelCount);
               regionCache.getPingMap().put(pingIndex, pingCache);
               regionNeedUpdate = true;
            }
         }

         if (regionNeedUpdate) {
            regionCache.update(visibleRegionPingRange, pingMapping);
         }

         for (int channelIndex = 0; channelIndex < selectedRegionsIntegratedSv.length; channelIndex++) {
            float[] selectedIntegratedSv = selectedRegionsIntegratedSv[channelIndex];
            float[] horizontallyIntegratedSv = regionCache.getHorizontallyIntegratedSv(channelIndex);

            for (int i = 0; i < Histogram.CELL_COUNT; i++) {
               selectedIntegratedSv[i] += horizontallyIntegratedSv[i];
            }
         }
      }

      if (selectedVisibleRegionPingRange.isEmpty()) {
         plotOnlyThresholds();
         return;
      }

      double selectedDistance = pingMapping.distance(selectedVisibleRegionPingRange);
      if (selectedDistance == 0) {
         plotOnlyThresholds();
         return;
      }

      double maxSa = 0;

      List<Graph> graphs = new ArrayList<>(selectedRegionsIntegratedSv.length + 2);
      XYInfo xyInfo = new XYInfo(
            new ParameterExport("sv", Unit.DB, ExportRounding.db()),
            new ParameterExport("sa", Unit.SA, ExportRounding.sa()));

      for (int channelIndex = 0; channelIndex < selectedRegionsIntegratedSv.length; channelIndex++) {
         int channel = channelIndex + 1;
         int kHz = rawFileConfiguration.getTransducers().get(channelIndex).getKHz();

         if (channel == getInterpretationSettings().getChannel() || frequencySelectionPanel.isChannelSelected(channel)) {
            Graph graph = new Graph(kHz + " kHz", Histogram.CELL_COUNT)
                  .setXYInfo(xyInfo)
                  .setColor(FrequencySelectionButton.channelIndexToColor(channelIndex));
            if (getInterpretationSettings().getChannel() == channel) {
               graph.setLineWidth(3);
            }
            graphs.add(graph);
            float[] selectedIntegratedSv = selectedRegionsIntegratedSv[channelIndex];
            double sa = 0;
            for (int i = Histogram.CELL_COUNT - 1; i > 0; i--) {
               sa += selectedIntegratedSv[i] / selectedDistance;
               graph.addPoint(Histogram.getLowerCellBoundary(i), sa);
            }
            maxSa = Math.max(maxSa, sa);
         }
      }

      plotGraphs(graphs, (float) maxSa);
   }

   private void plotOnlyThresholds() {
      plotGraphs(new ArrayList<>(2), 1);
   }

   private JFreeChart createChart(List<Graph> graphs, FloatRange yRange) {
      graphs.addAll(createThresholdsGraph(getInterpretationSettings().getColorConverterContainer().getSV(), yRange));

      return new Plotter(graphs)
            .xAxis("Sv [dB]")
            .yAxis("sA")
            .yRange(yRange)
            .createChart();
   }

   private void plotGraphs(List<Graph> graphs, float maxY) {
      JFreeChart chart = createChart(graphs, FloatRange.of(0, maxY));
      setChart(chart);
   }

   private void setChart(JFreeChart chart) {
      this.chart = chart;
      viewHolder.ifViewDelayed(this, View::updateChart);
   }

   public static List<Graph> createThresholdsGraph(ContinuousVariable continuousVariable, FloatRange yRange) {
      ContinuousVariableSettings settings = continuousVariable.getSettings();
      FloatRange range = settings.getRange();

      XYInfo xyInfo = XYInfo.getEmpty();

      Graph minGraph = new Graph("Lower threshold", 2)
            .setXYInfo(xyInfo)
            .setColor(Color.BLACK);
      minGraph.addPoint(range.min(), yRange.min());
      minGraph.addPoint(range.min(), yRange.max());

      Graph maxGraph = new Graph("Upper threshold", 2)
            .setXYInfo(xyInfo)
            .setColor(Color.BLACK);
      maxGraph.addPoint(range.max(), yRange.min());
      maxGraph.addPoint(range.max(), yRange.max());
      if (!settings.isClipAbove()) {
         maxGraph.setDashed();
      }

      return List.of(minGraph, maxGraph);
   }

   float getSa(Region region, FloatRange logSvRange) {
      RegionCache regionCache = regionMap.get(region);
      float[] horizontallyIntegratedSv = regionCache.getHorizontallyIntegratedSv(getInterpretationSettings().getChannel() - 1);
      int i0 = Histogram.logSvToIndex(logSvRange.min());
      int i1 = Histogram.logSvToIndex(logSvRange.max());
      double sa = 0;
      for (int i = i0; i < i1; i++) {
         sa += horizontallyIntegratedSv[i];
      }
      if (logSvRange.max() > Histogram.MAX_LOG_SV) {
         sa += horizontallyIntegratedSv[Histogram.CELL_COUNT - 1];
      }
      PingMapping pingMapping = getConfigurationManager().getGridConf().horizontalGridUnit.getValue();
      double distance = pingMapping.distance(region.getPingRange());
      return (float) (sa / distance);
   }

   @Override
   public PojoData getPojoData() {
      return PojoData.newBuilder(getPersistentName())
            .withDatasets(chart.getXYPlot())
            .build();
   }

   private static final class View extends BaseView {
      private final ThresholdResponseModule module;
      private final JPanel mainPanel = new JPanel(new BorderLayout());
      private final ChartPanel chartPanel;

      private View(ThresholdResponseModule module) {
         super(module);

         this.module = module;
         chartPanel = PlotUtils.newChartPanel(module.chart);
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
