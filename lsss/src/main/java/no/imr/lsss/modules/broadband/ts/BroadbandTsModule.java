package no.imr.lsss.modules.broadband.ts;

import com.google.common.collect.ImmutableSortedMap;
import no.imr.korona.computation.feature.FrequencyMapping;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.region.Region;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.util.ts.PeakTSDetector;
import no.imr.korona.util.ts.TSDetector;
import no.imr.korona.viewer.util.FrequencySelectionPanel;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.broadband.BroadbandModuleUtils;
import no.imr.lsss.modules.broadband.BroadbandRegionCache;
import no.imr.lsss.modules.misc.PingPlotModule;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.modules.ts.BaseTsModule;
import no.imr.lsss.modules.ts.TSModule;
import no.imr.lsss.util.FrequencyPlotMarker;
import no.imr.tools.Utils;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.Plotter;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.Layer;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class BroadbandTsModule extends BaseViewModule implements BaseTsModule, PojoDataContainer {
   private final HeaderParameter targetDetectorHeader = new HeaderParameter("Target detector settings");

   public final FloatParameter minTS = new FloatParameter(
         new Name("MinTS", "Min TS"),
         -80, Unit.DB,
         "Minimum TS value to be detected");

   public final FloatParameter pulseLengthDeterminationLevel = new FloatParameter(
         new Name("PulseLengthDeterminationLevel", "Pulse length determination level"),
         6, Unit.DB,
         "Pulse length determination level");

   public final FloatParameter minEchoLength = new FloatParameter(
         new Name("MinEchoLength", "Min echo length"),
         0, Unit.DIMENSIONLESS,
         "Minimum echo length (relative to pulse length).   NB: may be << 1 for pulse compressed data!");

   public final FloatParameter maxEchoLength = new FloatParameter(
         new Name("MaxEchoLength", "Max echo length"),
         1, Unit.DIMENSIONLESS,
         "Maximum echo length (relative to pulse length).   NB: may be << 1 for pulse compressed data!");

   public final FloatParameter maxGainCompensation = new FloatParameter(
         new Name("MaxGainCompensation", "Max gain compensation"),
         6, Unit.DB,
         "Maximum (one way) gain compensation");

   public final BooleanParameter doPhaseDeviationCheck = new BooleanParameter(
         new Name("DoPhaseDeviationCheck", "Do phase deviation check"),
         false,
         "Check this if phase deviation check should be performed (not recommended)");

   public final FloatParameter maxPhaseDevPhaseSteps = new FloatParameter(
         new Name("MaxPhaseDevSteps", "Max phase deviation"),
         8, new Unit("phase steps"),
         "Max phase deviation");

   private final HeaderParameter spectrumExtractionHeader = new HeaderParameter("Spectrum extraction settings");

   public final ObjectParameter<TargetExtentMode> targetExtentMode = new ObjectParameter<>(
         new Name("TargetExtentMode", "Target extent for FFT"),
         TargetExtentMode.AUTOMATIC, TargetExtentMode.values());

   public final BooleanParameter manualTargetExtentSymmetrical = new BooleanParameter(
         new Name("ManualTargetExtentSymmetrical", "Manual target extent symmetrical"),
         true,
         "Manually specified extent symmetrical around peek");

   public final FloatParameter manualTargetExtent = new FloatParameter(
         new Name("ManualTargetExtent", "Manual target extent"),
         0.5f, Unit.METER, ValueConstraints.gt(0f),
         "Manually specified extent of the FFT centered around peek");

   public final FloatParameter manualTargetExtentAbove = new FloatParameter(
         new Name("ManualTargetExtentAbove", "Manual target extent above"),
         0.25f, Unit.METER, ValueConstraints.gt(0f),
         "Manually specified extent of the FFT above peek");

   public final FloatParameter manualTargetExtentBelow = new FloatParameter(
         new Name("ManualTargetExtentBelow", "Manual target extent below"),
         0.25f, Unit.METER, ValueConstraints.gt(0f),
         "Manually specified extent of the FFT below peek");

   private final HeaderParameter displayHeader = new HeaderParameter("Display settings");

   public final IntParameter pingRadius = new IntParameter(
         new Name("PingRadius", "Ping radius"),
         1, Unit.COUNT, ValueConstraints.gte(0),
         "Number of pings on either side to use");

   public final FloatParameter frequencyWindowing = new FloatParameter(
         new Name("FrequencyWindowing", "Frequency windowing"),
         5, Unit.PERCENT, ValueConstraints.gteLte(0f, 50f),
         "Percentage of extremal frequencies to be discarded (two-sided)");

   public final FloatParameter frequencyResolution = new FloatParameter(
         new Name("FrequencyResolution", "Frequency resolution"),
         0.1f, Unit.KHZ, ValueConstraints.gt(0f),
         "Frequency resolution in display");

   public final BooleanParameter plotNarrowband = new BooleanParameter(
         new Name("PlotNarrowband", "Plot narrowband"),
         false,
         "Plot frequencies with narrowband data");

   public final ObjectParameter<FrequencyMapping> xAxis = new ObjectParameter<>(
         new Name("XAxis", "X-axis"),
         FrequencyMapping.SQRT, FrequencyMapping.values(),
         "Mapping of frequencies along x-axis");

   public final BooleanParameter autoAdjustAxes = new BooleanParameter(
         new Name("AutoAdjustAxes", "Auto-adjust axes"),
         true,
         "Automatically adjust the x-axis and y-axis when the plot is updated");

   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private final FrequencyPlotMarker frequencyPlotMarker;
   private JFreeChart chart = PlotUtils.newEmptyChart();

   private final Listener refreshListener = newCoalescingExecListener(this::refresh);
   private final FrequencySelectionPanel frequencySelectionPanel = new FrequencySelectionPanel(refreshListener);
   private final Listener updatePlotListener = newCoalescingExecListener(this::updatePlot);

   private final Map<Region, BroadbandRegionCache<BroadbandTsPingCache>> regionMap = new ConcurrentHashMap<>();

   private final ChangeManager tsDetectionChangeManager = new ChangeManager();

   private final Supplier<TSModule> tsModule = moduleSupplier(TSModule.class);

   public BroadbandTsModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      doPhaseDeviationCheck.addListenerAndNotify(maxPhaseDevPhaseSteps::setEnabled);

      targetExtentMode.addListenerAndNotify(mode -> {
         boolean manual = mode == TargetExtentMode.MANUAL;
         manualTargetExtentSymmetrical.setEnabled(manual);
         manualTargetExtent.setEnabled(manual);
         manualTargetExtentAbove.setEnabled(manual);
         manualTargetExtentBelow.setEnabled(manual);
      });
      manualTargetExtentSymmetrical.addListenerAndNotify(symmetrical -> {
         manualTargetExtent.setVisible(symmetrical);
         manualTargetExtentAbove.setVisible(!symmetrical);
         manualTargetExtentBelow.setVisible(!symmetrical);
      });

      autoAdjustAxes.setPersistable(false);

      frequencyPlotMarker = new FrequencyPlotMarker(getLSSS());
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            targetDetectorHeader,
            minTS,
            pulseLengthDeterminationLevel,
            minEchoLength,
            maxEchoLength,
            maxGainCompensation,
            doPhaseDeviationCheck,
            maxPhaseDevPhaseSteps,
            //---
            spectrumExtractionHeader,
            targetExtentMode,
            manualTargetExtentSymmetrical,
            manualTargetExtent,
            manualTargetExtentAbove,
            manualTargetExtentBelow,
            //---
            displayHeader,
            pingRadius,
            frequencyWindowing,
            frequencyResolution,
            plotNarrowband,
            xAxis,
            autoAdjustAxes
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getInterpretationSettings().getDataFileChangeManager(), newCoalescingExecListener(this::dataFilesUpdated));

      registry.add(getInterpretationSettings().getChannelChangeManager(), newCoalescingExecListener(channel -> {
         frequencySelectionPanel.setHighlighted(channel);
         refreshListener.listen();
      }));

      registry.add(getInterpretationSettings().mouseover().echogramPoint(), updatePlotListener);
      registry.add(getInterpretationSettings().mouseover().kHz(), newCoalescingExecListener(frequencyPlotMarker::updateMarker));

      registry.add(tsModule.get().getTSDetectionChangeManager(), __ -> {
         if (plotNarrowband.getBooleanValue()) {
            updatePlotListener.listen();
         }
      });

      Listener recomputeListener = newCoalescingExecListener(this::recompute);
      registry.add(getInterpretationSettings().getPingRangeChangeManager(), newCoalescingExecListener(pingRange -> {
         regionMap.values().forEach(cache -> cache.retainPingRange(pingRange));
         refreshListener.listen();
      }));

      Set<BaseParameter<?>> plotParameters = Set.of(pingRadius, plotNarrowband, xAxis, autoAdjustAxes);
      registry.add(plotParameters, updatePlotListener);

      Set<BaseParameter<?>> recomputeParameters = new HashSet<>(getParameters());
      recomputeParameters.removeAll(plotParameters);
      registry.add(recomputeParameters, recomputeListener);

      registry.add(getInterpretationSettings().getPingSampler().getNewPingsChangeManager(), refreshListener);

      registry.add(getRegionManager().selectedRegions(), refreshListener);
      registry.add(getRegionManager().getRegionDeletedChangeManager(), newExecListener(regions -> {
         regionMap.keySet().removeAll(regions);
         refreshListener.listen();
      }));
      registry.add(getRegionManager().getRegionDefinitionChangeManager(), newExecListener(regionEvent -> {
         PingRange pingRange = regionEvent.pingRange();
         for (Region region : regionEvent.regions()) {
            BroadbandRegionCache<BroadbandTsPingCache> regionCache = regionMap.get(region);
            if (regionCache != null) {
               regionCache.clearPingRange(pingRange);
            }
         }
         refreshListener.listen();
      }));

      //---

      dataFilesUpdated();
      recompute();
   }

   @Override
   protected void onDisable() {
      regionMap.clear();
      setChart(PlotUtils.newEmptyChart());
      tsDetectionChangeManager.notifyListeners();
   }

   private void dataFilesUpdated() {
      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
      frequencySelectionPanel.update(rawFileConfiguration, getInterpretationSettings().getChannel());
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   @Override
   public ChangeManager getTSDetectionChangeManager() {
      return tsDetectionChangeManager;
   }

   private void processPings(List<Ping> pings) {
      TSDetector tsDetector = createTSDetector();
      PingRange pingRange = getInterpretationSettings().getPingRange();

      boolean didComputeSomething = false;
      long maxTime = System.currentTimeMillis() + 2000;

      regionLoop:
      for (Region region : getRegionManager().getSelectedRegions()) {
         PingRange visibleRegionPingRange = region.getPingRange().intersection(pingRange);
         if (visibleRegionPingRange.isEmpty()) {
            continue;
         }

         BroadbandRegionCache<BroadbandTsPingCache> regionCache = regionMap.get(region);
         if (regionCache == null) {
            regionCache = new BroadbandRegionCache<>();
            regionMap.put(region, regionCache);
         }

         for (Ping ping : pings) {
            if (didComputeSomething && System.currentTimeMillis() > maxTime) {
               refreshListener.listen();
               break regionLoop;
            }

            PingIndex pingIndex = ping.getPingIndex();

            if (!visibleRegionPingRange.contains(pingIndex)) {
               continue;
            }

            BroadbandTsPingCache pingCache = regionCache.getPing(pingIndex);
            if (pingCache == null) {
               didComputeSomething = true;
               pingCache = new BroadbandTsPingCache(getRegionManager(), ping, region, tsDetector, this);
               regionCache.putPing(pingIndex, pingCache);
            }
         }
      }

      updatePlotListener.listen();

      tsDetectionChangeManager.notifyListeners();
   }

   private void updatePlot() {
      List<Region> selectedRegions = getRegionManager().getSelectedRegions();
      if (selectedRegions.isEmpty()) {
         plot(List.of(), "No regions selected");
         return;
      }
      plot(createGraphs(selectedRegions), "");
   }

   private void plot(List<Graph> graphs, String noDataMessage) {
      JFreeChart chart = new Plotter(graphs)
            .xAxis(() -> BroadbandModuleUtils.kHzAxis(graphs, xAxis.getValue()))
            .yAxis("TS [dB]")
            .createChart();
      XYPlot plot = chart.getXYPlot();
      plot.setNoDataMessage(noDataMessage);
      for (RawFileTransducer transducer : getInterpretationSettings().getDataFileSet().getRawFileConfiguration().getTransducers()) {
         plot.addDomainMarker(new ValueMarker(transducer.getKHz(), Color.LIGHT_GRAY, GuiUtils.STROKE_1), Layer.BACKGROUND);
      }
      frequencyPlotMarker.addMarker(plot);
      setChart(chart);
   }

   private void setChart(JFreeChart chart) {
      if (!autoAdjustAxes.getBooleanValue()) {
         PlotUtils.preserveAxisRanges(this.chart, chart);
      }
      this.chart = chart;
      viewHolder.ifViewDelayed(this, View::updateChart);
   }

   private List<Graph> createGraphs(List<Region> regions) {
      EchogramPoint echogramPoint = getInterpretationSettings().mouseover().getEchogramPoint();
      if (echogramPoint == null) {
         return List.of();
      }
      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();

      Set<BroadbandTsData> highlightedTargets = getTargetsForPoint(regions, echogramPoint, getInterpretationSettings().getChannel()).values().stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

      List<Graph> graphs = new ArrayList<>();
      for (int channel = 1; channel <= rawFileConfiguration.getTransducerCount(); channel++) {
         if (channel == getInterpretationSettings().getChannel() || frequencySelectionPanel.isChannelSelected(channel)) {
            graphs.addAll(createGraphs(regions, echogramPoint.pingIndex(), channel, highlightedTargets));
         }
      }
      return graphs;
   }

   FloatRange getManualTargetDepthRange(float peakDepth) {
      if (manualTargetExtentSymmetrical.getBooleanValue()) {
         return FloatRange.ofCenterAndSize(peakDepth, manualTargetExtent.getFloatValue());
      } else {
         return FloatRange.of(peakDepth - manualTargetExtentAbove.getFloatValue(), peakDepth + manualTargetExtentBelow.getFloatValue());
      }
   }

   @Override
   public Map<PingIndex, List<BroadbandTsData>> getTargetsForPoint(List<Region> regions, EchogramPoint echogramPoint, int channel) {
      List<BroadbandTsData> targets = regions.stream()
            .map(regionMap::get)
            .filter(Objects::nonNull)
            .map(regionCache -> regionCache.getPing(echogramPoint.pingIndex()))
            .filter(Objects::nonNull)
            .flatMap(pingCache -> pingCache.getTsData(channel).stream())
            .filter(tsData -> tsData.depthRange().contains(echogramPoint.depth()))
            .toList();
      return Map.of(echogramPoint.pingIndex(), targets);
   }

   private List<Graph> createGraphs(List<Region> regions, PingIndex pingIndex, int channel, Set<BroadbandTsData> highlightedTargets) {
      List<Graph> graphs = new ArrayList<>();

      // Add current graph on top.
      if (highlightedTargets.isEmpty()) {
         createGraph("Current ping", regions, pingIndex, channel, x -> true).ifPresent(graphCurrent -> {
            graphCurrent.setLineWidth(3);
            graphCurrent.setColor(Color.RED);
            graphs.add(graphCurrent);
         });
      } else {
         Predicate<BroadbandTsData> predicate = highlightedTargets::contains;
         createGraph("Current ping (highlighted)", regions, pingIndex, channel, predicate).ifPresent(graphCurrent -> {
            graphCurrent.setLineWidth(2);
            graphCurrent.setColor(Color.RED);
            graphs.add(graphCurrent);

            Graph backgroundGraph = new Graph("Current ping (highlighted, background)")
                  .setXYInfo(XYInfo.getEmpty())
                  .setLineWidth(4)
                  .setColor(Color.BLACK);
            backgroundGraph.getPoints().addAll(graphCurrent.getPoints());
            graphs.add(backgroundGraph);
         });
         createGraph("Current ping", regions, pingIndex, channel, predicate.negate()).ifPresent(graphCurrent -> {
            graphCurrent.setColor(ColorUtils.INDIANRED);
            graphs.add(graphCurrent);
         });
      }

      int n = pingRadius.getIntValue();
      IntStream.rangeClosed(1, n).forEach(i -> {
         PingIndex pingIndexPast = getInterpretationSettings().getDataFileSet().getPingIndexOrNull(pingIndex.getPingNumber() - i);
         createGraph("Current ping - " + i, regions, pingIndexPast, channel, x -> true).ifPresent(graphPast -> {
            graphPast.setColor(PingPlotModule.getColor(-i, n));
            graphs.add(graphPast);
         });

         PingIndex pingIndexFuture = getInterpretationSettings().getDataFileSet().getPingIndexOrNull(pingIndex.getPingNumber() + i);
         createGraph("Current ping + " + i, regions, pingIndexFuture, channel, x -> true).ifPresent(graphFuture -> {
            graphFuture.setColor(PingPlotModule.getColor(i, n));
            graphs.add(graphFuture);
         });
      });

      return graphs;
   }

   private Optional<Graph> createGraph(String name, List<Region> regions, @Nullable PingIndex pingIndex, int channel, Predicate<BroadbandTsData> predicate) {
      if (pingIndex == null) {
         return Optional.empty();
      }

      Graph graph = new Graph(name)
            .setXYInfo(new XYInfo(
                  new ParameterExport("frequency", Unit.KHZ, ExportRounding.kHz()),
                  new ParameterExport("tsc", Unit.DB, ExportRounding.db())));

      regions.stream()
            .map(regionMap::get)
            .filter(Objects::nonNull)
            .map(regionCache -> regionCache.getPing(pingIndex))
            .filter(Objects::nonNull)
            .flatMap(pingCache -> pingCache.getTsData(channel).stream())
            .filter(predicate)
            .limit(100) // For avoiding too many curves making GUI slow
            .forEach(target -> {
               graph.addSeparator();
               addToGraph(target, graph);
            });

      if (!graph.getPoints().isEmpty()) {
         return Optional.of(graph);
      }

      if (plotNarrowband.getBooleanValue() && !isBroadband(pingIndex, channel)) {
         int kHz = Utils.hzToKHz(getInterpretationSettings().getDataFileSet().getFrequency(channel));
         regions.stream()
               .flatMap(region -> tsModule.get().getTSData(region, pingIndex, channel).stream())
               .forEach(tsData -> {
                  graph.addSeparator();
                  graph.addPoint(kHz - 1, tsData.tsc());
                  graph.addPoint(kHz + 1, tsData.tsc());
               });

         if (!graph.getPoints().isEmpty()) {
            return Optional.of(graph);
         }
      }

      return Optional.empty();
   }

   private boolean isBroadband(PingIndex pingIndex, int channel) {
      return regionMap.values().stream()
            .map(regionCache -> regionCache.getPing(pingIndex))
            .filter(Objects::nonNull)
            .anyMatch(pingCache -> pingCache.getChannelCache(channel).isBroadband());
   }

   static void addToGraph(BroadbandTsData target, Graph graph) {
      float minKHz = target.frequencyRange().min() / 1000;
      float deltaKHz = target.getDeltaFrequency() / 1000;
      float[] values = target.values();
      for (int i = 0; i < values.length; i++) {
         graph.addPoint(minKHz + i * deltaKHz, values[i]);
      }
   }

   TSDetector createTSDetector() {
      float maxDepth = getInterpretationSettings().getPelagicZSettings().getMaxZRange().max();
      return new PeakTSDetector(minTS.getFloatValue(), maxGainCompensation.getFloatValue(), pulseLengthDeterminationLevel.getFloatValue(),
            minEchoLength.getFloatValue(), maxEchoLength.getFloatValue(),
            doPhaseDeviationCheck.getBooleanValue(), maxPhaseDevPhaseSteps.getFloatValue(), maxDepth
      );
   }

   @Override
   public NavigableMap<PingIndex, BroadbandTsPingCache> getTSData(Region region) {
      BroadbandRegionCache<BroadbandTsPingCache> regionCache = regionMap.get(region);
      if (regionCache == null) {
         return ImmutableSortedMap.of();
      }
      return regionCache.getPingMap();
   }

   private void recompute() {
      regionMap.clear();
      refreshListener.listen();
   }

   private void refresh() {
      processPings(getInterpretationSettings().getPingSampler().getAvailablePings());
   }

   @Override
   public PojoData getPojoData() {
      return PojoData.newBuilder(getPersistentName())
            .withDatasets(chart.getXYPlot())
            .build();
   }

   private static final class View extends BaseView {
      private final BroadbandTsModule module;
      private final JPanel mainPanel = new JPanel(new BorderLayout());
      private final ChartPanel chartPanel;

      private View(BroadbandTsModule module) {
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
