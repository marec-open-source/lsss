package no.imr.lsss.modules.ts;

import com.google.common.collect.ImmutableSortedMap;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.region.Region;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.korona.util.ts.PeakTSDetector;
import no.imr.korona.util.ts.SedTSDetector;
import no.imr.korona.viewer.util.FrequencySelectionButton;
import no.imr.korona.viewer.util.FrequencySelectionPanel;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.tools.Utils;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.math.NiceNumber;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.Plotter;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.icons.MiscIcons;
import no.marec.lsss.api.util.parameters.ObjectParameterValue;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.axis.Tick;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TSModule extends BaseViewModule implements BaseTsModule, PojoDataContainer {
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private final Listener refreshListener = newCoalescingExecListener(this::refresh);
   private final FrequencySelectionPanel frequencySelectionPanel = new FrequencySelectionPanel(refreshListener);

   private final Map<Region, RegionCache> regionMap = new ConcurrentHashMap<>();
   private Set<TSData> selectedTSData = Set.of();

   private final ChangeManager tsDetectionChangeManager = new ChangeManager();
   private final ChangeManager tsSelectionChangeManager = new ChangeManager();

   private int histogramBinCount;
   private FloatRange histogramRange = FloatRange.EMPTY_RANGE;

   private final HeaderParameter targetDetectorHeader = new HeaderParameter("Target detector settings");

   private final ObjectParameter<DetectorType> detectorType = new ObjectParameter<>(
         new Name("DetectorType", "Detector type"),
         DetectorType.SED, DetectorType.values(),
         "Type of TS detector") {
      @Override
      public @Nullable DetectorType unknownStringToValue(String unknownString) {
         if (unknownString.equals("Narrowband")) {
            return DetectorType.PEAK;
         }
         return null;
      }
   };
   final FloatParameter minTS = new FloatParameter(
         new Name("MinTS", "Min TS"),
         -66, Unit.DB,
         "Minimum TS value to be detected");

   final FloatParameter pulseLengthDeterminationLevel = new FloatParameter(
         new Name("PulseLengthDeterminationLevel", "Pulse length determination level"),
         6, Unit.DB,
         "Pulse length determination level");

   final FloatParameter minEchoLength = new FloatParameter(
         new Name("MinEchoLength", "Min echo length"),
         0.01f, Unit.DIMENSIONLESS,
         "Minimum echo length (relative to pulse length).   NB: may be << 1 for pulse compressed data!");

   final FloatParameter maxEchoLength = new FloatParameter(
         new Name("MaxEchoLength", "Max echo length"),
         1.8f, Unit.DIMENSIONLESS,
         "Maximum echo length (relative to pulse length).   NB: may be << 1 for pulse compressed data!");

   final FloatParameter maxGainCompensation = new FloatParameter(
         new Name("MaxGainCompensation", "Max gain compensation"),
         6, Unit.DB,
         "Maximum (one way) gain compensation");

   final BooleanParameter doPhaseDeviationCheck = new BooleanParameter(
         new Name("DoPhaseDeviationCheck", "Do phase deviation check"),
         true,
         "Check this if phase deviation check should be performed");

   final FloatParameter maxPhaseDevPhaseSteps = new FloatParameter(
         new Name("MaxPhaseDevSteps", "Max phase deviation"),
         8, new Unit("phase steps"),
         "Max phase deviation");

   private final OptionalFloatParameter maxDepth = new OptionalFloatParameter(
         new Name("MaxDepth", "Max depth"),
         Optional.empty(), Unit.METER,
         "Maximal depth of targets to detect");

   private final HeaderParameter displayHeader = new HeaderParameter("Display settings");

   private final FloatParameter deltaTS = new FloatParameter(
         new Name("DeltaTS", "Delta TS"),
         1, Unit.DB, ValueConstraints.gt(0f),
         "Histogram bin size");

   private final FloatParameter displayMinTS = new FloatParameter(
         new Name("DisplayMinTS", "Min TS"),
         minTS.getFloatValue(), Unit.DB,
         "Minimum TS in histogram");

   private final FloatParameter maxTS = new FloatParameter(
         new Name("MaxTS", "Max TS"),
         -20, Unit.DB,
         "Maximum TS in histogram");

   private JFreeChart chart = PlotUtils.newEmptyChart();

   public TSModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      doPhaseDeviationCheck.addListenerAndNotify(maxPhaseDevPhaseSteps::setEnabled);
      minTS.subscribe(value -> {
         maxTS.setAtLeastTo(value + 1);
         displayMinTS.setValue(value);
      });
      maxTS.subscribe(value -> {
         minTS.setAtMostTo(value - 1);
      });
      detectorType.subscribe(type -> {
         boolean korona = type == DetectorType.KORONA;
         displayMinTS.setVisible(korona);
         displayMinTS.setValue(minTS.getFloatValue());

         // Detector settings are hidden for KORONA:
         minTS.setVisible(!korona);
         pulseLengthDeterminationLevel.setVisible(!korona);
         minEchoLength.setVisible(!korona);
         maxEchoLength.setVisible(!korona);
         maxGainCompensation.setVisible(!korona);
         doPhaseDeviationCheck.setVisible(!korona);
         maxPhaseDevPhaseSteps.setVisible(!korona);
         maxDepth.setVisible(!korona);
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            targetDetectorHeader,
            detectorType,
            minTS,
            pulseLengthDeterminationLevel,
            minEchoLength,
            maxEchoLength,
            maxGainCompensation,
            doPhaseDeviationCheck,
            maxPhaseDevPhaseSteps,
            maxDepth,
            //---
            displayHeader,
            deltaTS,
            displayMinTS,
            maxTS
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

      Listener recomputeListener = newCoalescingExecListener(this::recompute);
      registry.add(getInterpretationSettings().getPingRangeChangeManager(), recomputeListener);
      registry.add(getParameters(), recomputeListener);

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
      tsDetectionChangeManager.notifyListeners();
      setSelectedTSData(Set.of());
   }

   private void dataFilesUpdated() {
      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
      frequencySelectionPanel.update(rawFileConfiguration, getInterpretationSettings().getChannel());
   }

   private void initBins() {
      float delta = deltaTS.getFloatValue();
      histogramRange = FloatRange.of(displayMinTS.getFloatValue(), maxTS.getFloatValue())
            .expandToMultipleOf(delta);
      histogramBinCount = Math.round(histogramRange.getSize() / delta);
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   @Override
   public ChangeManager getTSDetectionChangeManager() {
      return tsDetectionChangeManager;
   }

   public ChangeManager getTSSelectionChangeManager() {
      return tsSelectionChangeManager;
   }

   public Set<TSData> getSelectedTSData() {
      return selectedTSData;
   }

   public void setSelectedTSData(Set<TSData> tsData) {
      selectedTSData = tsData;
      tsSelectionChangeManager.notifyListeners();
   }

   private JFreeChart createChart(List<Graph> graphs, int maxNumberOfTargets, int numberOfDetectionsForActiveChannel) {
      JFreeChart chart = new Plotter(graphs)
            .xAxis(() -> {
               NumberAxis xAxis = new NumberAxis("TS [dB]") {
                  @Override
                  public List<Tick> refreshTicks(Graphics2D g2, AxisState state, Rectangle2D dataArea, RectangleEdge edge) {
                     List<Tick> ticks = new ArrayList<>();
                     long n0 = Math.round(getLowerBound() / deltaTS.getFloatValue());
                     long n1 = Math.round(getUpperBound() / deltaTS.getFloatValue());
                     int dn = Math.max(1, (int) NiceNumber.niceNumber((n1 - n0) * 50 / dataArea.getWidth(), true));
                     for (int i = 0; i <= histogramBinCount; i++) {
                        float ts = histogramRange.min() + i * deltaTS.getFloatValue();
                        int n = Math.round(ts / deltaTS.getFloatValue());
                        String label = n % dn == 0 ? Utils.toString(ts) : "";
                        ticks.add(new NumberTick(ts, label, TextAnchor.TOP_CENTER, TextAnchor.CENTER, 0));
                     }
                     return ticks;
                  }
               };
               xAxis.setAutoRangeIncludesZero(false);
               xAxis.setLowerMargin(0);
               xAxis.setUpperMargin(0);
               return xAxis;
            })
            .xRange(histogramRange)
            .yAxis(() -> {
               NumberAxis yAxis = new NumberAxis();
               yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
               return yAxis;
            })
            .yRange(0, Math.max(maxNumberOfTargets, 1))
            .createChart();
      chart.addSubtitle(PlotUtils.newTextTitle(numberOfDetectionsForActiveChannel + " detections", HorizontalAlignment.RIGHT));
      return chart;
   }

   private void plotGraphs(List<Graph> graphs, int maxNumberOfTargets, int numberOfDetectionsForActiveChannel) {
      JFreeChart chart = createChart(graphs, maxNumberOfTargets, numberOfDetectionsForActiveChannel);
      setChart(chart);
   }

   private void setChart(JFreeChart chart) {
      this.chart = chart;
      viewHolder.ifViewDelayed(this, View::updateChart);
   }

   private void plotEmptyPlot() {
      plotGraphs(List.of(), 10, 0);
   }

   private void processPings(List<Ping> pings) {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         plotEmptyPlot();
         tsDetectionChangeManager.notifyListeners();
         return;
      }

      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
      int channelCount = rawFileConfiguration.getTransducerCount();

      TsDataComputer tsDataComputer = createTSDataComputer();

      int[][] selectedRegionsData = new int[channelCount][histogramBinCount];
      for (Region region : getRegionManager().getSelectedRegions()) {
         PingRange visibleRegionPingRange = region.getPingRange().intersection(pingRange);
         if (visibleRegionPingRange.isEmpty()) {
            continue;
         }

         RegionCache regionCache = regionMap.get(region);
         if (regionCache == null) {
            regionCache = new RegionCache(channelCount, histogramBinCount);
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
               pingCache = new PingCache(getRegionManager(), ping, region, channelCount, tsDataComputer);
               regionCache.pingMap.put(pingIndex, pingCache);
               regionNeedUpdate = true;
            }
         }

         if (regionNeedUpdate) {
            regionCache.update(histogramRange.min(), deltaTS.getFloatValue());
         }

         for (int channelIndex = 0; channelIndex < selectedRegionsData.length; channelIndex++) {
            ArrayMath.add(selectedRegionsData[channelIndex], regionCache.tsHistogram[channelIndex]);
         }
      }

      List<Graph> graphs = new ArrayList<>(selectedRegionsData.length + 2);
      int highestColumn = 0;
      int numberOfDetectionsForCurrentChannel = 0;
      for (int channelIndex = 0; channelIndex < selectedRegionsData.length; channelIndex++) {
         int channel = channelIndex + 1;
         int kHz = rawFileConfiguration.getTransducers().get(channelIndex).getKHz();

         if (channel == getInterpretationSettings().getChannel() || frequencySelectionPanel.isChannelSelected(channel)) {
            Graph graph = new Graph(kHz + " kHz", histogramBinCount)
                  .setXYInfo(new XYInfo(
                        new ParameterExport("tsc", Unit.DB, ExportRounding.db()),
                        new ParameterExport("count", Unit.COUNT, ExportTransform.identity())))
                  .setColor(FrequencySelectionButton.channelIndexToColor(channelIndex));
            if (getInterpretationSettings().getChannel() == channel) {
               graph.setLineWidth(3);
            }
            graphs.add(graph);

            int[] countForChannel = selectedRegionsData[channelIndex];
            float maxNumber = 0;
            for (int i = 0; i < histogramBinCount; i++) {
               graph.addPoint(histogramRange.min() + i * deltaTS.getFloatValue(), countForChannel[i]);
               if (getInterpretationSettings().getChannel() == channel) {
                  graph.addPoint(histogramRange.min() + (i + 1) * deltaTS.getFloatValue(), countForChannel[i]);
               }
               if (maxNumber < countForChannel[i]) {
                  maxNumber = countForChannel[i];
               }
               if (getInterpretationSettings().getChannel() == channel) {
                  numberOfDetectionsForCurrentChannel += countForChannel[i];
               }
            }
            highestColumn = (int) Math.max(highestColumn, maxNumber);
         }
      }
      plotGraphs(graphs, highestColumn, numberOfDetectionsForCurrentChannel);

      tsDetectionChangeManager.notifyListeners();
   }

   TsDataComputer createTSDataComputer() {
      return switch (detectorType.getValue()) {
         case SED -> {
            yield TsDataComputer.detector(new SedTSDetector(minTS.getFloatValue(), maxGainCompensation.getFloatValue(), pulseLengthDeterminationLevel.getFloatValue(),
                  minEchoLength.getFloatValue(), maxEchoLength.getFloatValue(), doPhaseDeviationCheck.getBooleanValue(), maxPhaseDevPhaseSteps.getFloatValue(),
                  maxDepth.getValue().orElse(Float.POSITIVE_INFINITY)));
         }
         case PEAK -> {
            yield TsDataComputer.detector(new PeakTSDetector(minTS.getFloatValue(), maxGainCompensation.getFloatValue(), pulseLengthDeterminationLevel.getFloatValue(),
                  minEchoLength.getFloatValue(), maxEchoLength.getFloatValue(), doPhaseDeviationCheck.getBooleanValue(), maxPhaseDevPhaseSteps.getFloatValue(),
                  maxDepth.getValue().orElse(Float.POSITIVE_INFINITY)));
         }
         case KORONA -> {
            yield TsDataComputer.korona();
         }
      };
   }

   @Override
   public NavigableMap<PingIndex, PingCache> getTSData(Region region) {
      RegionCache regionCache = regionMap.get(region);
      if (regionCache == null) {
         return ImmutableSortedMap.of();
      }
      return regionCache.pingMap;
   }

   public List<TSData> getTSData(Region region, PingIndex pingIndex, int channel) {
      RegionCache regionCache = regionMap.get(region);
      if (regionCache == null) {
         return List.of();
      }
      PingCache pingCache = regionCache.pingMap.get(pingIndex);
      if (pingCache == null) {
         return List.of();
      }
      return pingCache.getTsData(channel);
   }

   @Override
   public Map<PingIndex, List<TSData>> getTargetsForPoint(List<Region> regions, EchogramPoint echogramPoint, int channel) {
      float radius = 5;
      for (EchogramModule echogramModule : getModuleManager().getModules(EchogramModule.class).toList()) {
         Point mousePosition = echogramModule.getMousePosition();
         if (mousePosition != null) {
            EchogramZSettings zSettings = echogramModule.getZSettings();
            EchogramPingSettings pingSettings = getInterpretationSettings().getPingSettings();
            float x1 = pingSettings.pingIndexToX(echogramPoint.pingIndex());
            float x2 = getX2(x1, echogramPoint.pingIndex());

            Map<PingIndex, List<TSData>> targets = new HashMap<>();
            for (Region region : regions) {
               RegionCache regionCache = regionMap.get(region);
               if (regionCache == null) {
                  continue;
               }
               regionCache.pingMap.forEach((pingIndex, pingCache) -> {
                  float tsDataX1 = pingSettings.pingIndexToX(pingIndex);
                  if (tsDataX1 > x2 + radius) {
                     return;
                  }
                  float tsDataX2 = getX2(tsDataX1, pingIndex);
                  if (tsDataX2 < x1 - radius) {
                     return;
                  }
                  List<TSData> list = new ArrayList<>();
                  pingCache.getTsData(channel).forEach(tsData -> {
                     float tsDataY = zSettings.depthToY(tsData.depth(), pingIndex);
                     if (tsDataY < mousePosition.y - radius || tsDataY > mousePosition.y + radius) {
                        return;
                     }
                     if (Line2D.ptSegDistSq(tsDataX1, tsDataY, tsDataX2, tsDataY, mousePosition.x, mousePosition.y) <= radius * radius) {
                        list.add(tsData);
                     }
                  });
                  if (!list.isEmpty()) {
                     targets.put(pingIndex, list);
                  }
               });
            }
            return targets;
         }
      }
      return Map.of();
   }

   private float getX2(float x1, PingIndex pingIndex) {
      PingIndex nextPingIndex = getInterpretationSettings().getDataFileSet().nextOrNull(pingIndex);
      return nextPingIndex != null ? Math.max(x1, getInterpretationSettings().getPingSettings().pingIndexToX(nextPingIndex) - 1) : x1;
   }

   private void recompute() {
      initBins();
      regionMap.clear();
      refreshListener.listen();
   }

   private void refresh() {
      setSelectedTSData(Set.of());
      processPings(getInterpretationSettings().getPingSampler().getAvailablePings());
   }

   @Override
   public PojoData getPojoData() {
      return PojoData.newBuilder(getPersistentName())
            .withDatasets(chart.getXYPlot())
            .build();
   }

   private static final class View extends BaseView {
      private final TSModule module;
      private final JPanel mainPanel = new JPanel(new BorderLayout());
      private final ChartPanel chartPanel;

      private View(TSModule module) {
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

      @Override
      public void addToFloatableModuleMenu(JPopupMenu popupMenu) {
         List<TSEchogramOverlay> tsEchogramOverlays = module.getModuleManager().getModules(TSEchogramOverlay.class).toList();
         boolean selected = tsEchogramOverlays.stream().anyMatch(o -> o.getEchogramModule().isPelagic() && o.isEnabledByUser());
         MiscIcons.checkBox(selected).on(popupMenu.add("Show TS locations in echogram")).addActionListener(e -> {
            tsEchogramOverlays.forEach(o -> o.setEnabledByUser(!selected));
         });
         MiscIcons.SCATTER_PLOT.on(popupMenu.add("Visualizer dialog...")).addActionListener(e -> {
            new TSVisualizerDialog(module, mainPanel);
         });
      }

      private void updateChart() {
         chartPanel.setChart(module.chart);
      }
   }

   private enum DetectorType implements ObjectParameterValue {
      SED("SED", "Single echo detection"),
      PEAK("Peak", "Detection using peaks"),
      KORONA("KORONA", "Use results from the TS detection module in KORONA");

      private final String label;
      private final String tooltip;

      DetectorType(String label, String tooltip) {
         this.label = label;
         this.tooltip = tooltip;
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
}
