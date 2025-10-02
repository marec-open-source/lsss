package no.imr.lsss.modules.broadband.sv;

import no.imr.korona.computation.broadband.notchfilter.BroadbandTemporalNotchFilterConfig;
import no.imr.korona.computation.feature.FrequencyMapping;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.region.Region;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.viewer.util.FrequencySelectionButton;
import no.imr.korona.viewer.util.FrequencySelectionPanel;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.incubator.LsssIncubatorFeatureToggles;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.broadband.BroadbandModuleUtils;
import no.imr.lsss.modules.broadband.BroadbandRegionCache;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.util.FrequencyPlotMarker;
import no.imr.tools.Max;
import no.imr.tools.Min;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.OnlineAverageAndVariance;
import no.imr.tools.math.WelfordsMethod;
import no.imr.tools.misc.FloatUnaryOperator;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.HeaderParameter;
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
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.Layer;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class BroadbandSvModule extends BaseViewModule implements PojoDataContainer {
   private final HeaderParameter fftHeader = new HeaderParameter("FFT settings");

   public final FloatParameter frequencyWindowing = new FloatParameter(
         new Name("FrequencyWindowing", "Frequency windowing"),
         5, Unit.PERCENT, ValueConstraints.gteLte(0f, 50f),
         "Percentage of extremal frequencies to be discarded (two-sided)");

   public final FloatParameter frequencyResolution = new FloatParameter(
         new Name("FrequencyResolution", "Frequency resolution"),
         0.1f, Unit.KHZ, ValueConstraints.gt(0f),
         "Frequency resolution in display");

   public final FloatParameter depthResolution = new FloatParameter(
         new Name("DepthResolution", "Depth resolution"),
         1, Unit.METER, ValueConstraints.gt(0f),
         "Vertical distance between the centers of the FFT windows");

   public final FloatParameter depthMargin = new FloatParameter(
         new Name("DepthMargin", "Depth margin"),
         1, Unit.METER, ValueConstraints.gte(0f),
         "The minimal vertical distance from the region boundary to the center of the FFT window");

   public final FloatParameter fftWindowSize = new FloatParameter(
         new Name("FftWindowSize", "FFT window size"),
         2, Unit.NONE, ValueConstraints.gt(0f),
         "Size of the FFT window in units of pulse length");

   private final HeaderParameter displayHeader = new HeaderParameter("Display settings");

   public final BooleanParameter plotStdErr = new BooleanParameter(
         new Name("PlotStdErr", "Plot std. err."),
         true,
         "Plot standard error");

   public final BooleanParameter plotNarrowband = new BooleanParameter(
         new Name("PlotNarrowband", "Plot narrowband"),
         false,
         "Plot frequencies with narrowband data");

   public final ObjectParameter<FrequencyMapping> xAxis = new ObjectParameter<>(
         new Name("XAxis", "X-axis"),
         FrequencyMapping.SQRT, FrequencyMapping.values(),
         "Mapping of frequencies along x-axis");

   public final BooleanParameter useDb = new BooleanParameter(
         new Name("UseDb", "Use dB"),
         true,
         "Use logarithmic Sv values in dB");

   public final BooleanParameter useTVG = new BooleanParameter(
         new Name("UseTVG", "Use TVG"),
         true,
         "Use gain compensation");

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

   private final Map<Region, BroadbandRegionCache<BroadbandSvPingCache>> regionMap = new HashMap<>();

   public BroadbandSvModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      autoAdjustAxes.setPersistable(false);
      frequencyPlotMarker = new FrequencyPlotMarker(getLSSS());
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            fftHeader,
            frequencyWindowing,
            frequencyResolution,
            depthResolution,
            depthMargin,
            fftWindowSize,
            displayHeader,
            plotStdErr,
            plotNarrowband,
            xAxis,
            useDb,
            useTVG,
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

      registry.add(getInterpretationSettings().mouseover().kHz(), newCoalescingExecListener(frequencyPlotMarker::updateMarker));

      Listener recomputeListener = newCoalescingExecListener(this::recompute);
      registry.add(getInterpretationSettings().getPingRangeChangeManager(), newCoalescingExecListener(pingRange -> {
         regionMap.values().forEach(cache -> cache.retainPingRange(pingRange));
         refreshListener.listen();
      }));

      Set<BaseParameter<?>> plotParameters = Set.of(plotStdErr, plotNarrowband, xAxis, useDb, autoAdjustAxes);
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
            BroadbandRegionCache<BroadbandSvPingCache> regionCache = regionMap.get(region);
            if (regionCache != null) {
               regionCache.clearPingRange(pingRange);
            }
         }
         refreshListener.listen();
      }));

      registry.add(getInterpretationSettings().getBroadbandNotchFilterModuleConfigArgChangeManager(), updatePlotListener);

      //---

      dataFilesUpdated();
      recompute();
   }

   @Override
   protected void onDisable() {
      regionMap.clear();
      setChart(PlotUtils.newEmptyChart());
   }

   private void dataFilesUpdated() {
      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
      frequencySelectionPanel.update(rawFileConfiguration, getInterpretationSettings().getChannel());
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   private void processPings(List<Ping> pings) {
      PingRange pingRange = getInterpretationSettings().getPingRange();

      boolean didComputeSomething = false;
      long maxTime = System.currentTimeMillis() + 2000;

      regionLoop:
      for (Region region : getRegionManager().getSelectedRegions()) {
         PingRange visibleRegionPingRange = region.getPingRange().intersection(pingRange);
         if (visibleRegionPingRange.isEmpty()) {
            continue;
         }

         BroadbandRegionCache<BroadbandSvPingCache> regionCache = regionMap.get(region);
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

            BroadbandSvPingCache pingCache = regionCache.getPing(pingIndex);
            if (pingCache == null) {
               didComputeSomething = true;
               pingCache = new BroadbandSvPingCache(getRegionManager(), ping, region, this);
               regionCache.putPing(pingIndex, pingCache);
            }
         }
      }

      updatePlotListener.listen();
   }

   private void updatePlot() {
      List<Region> selectedRegions = getRegionManager().getSelectedRegions();
      if (selectedRegions.isEmpty()) {
         plot(List.of(), "No regions selected");
         return;
      }
      plot(createGraphs(selectedRegions), "No valid depth ranges");
   }

   private void plot(List<Graph> graphs, String noDataMessage) {
      String svUnit = (useDb.getBooleanValue() ? Unit.DB : Unit.SV).text();
      JFreeChart chart = new Plotter(graphs)
            .xAxis(() -> BroadbandModuleUtils.kHzAxis(graphs, xAxis.getValue()))
            .yAxis(useTVG.getBooleanValue() ? "Sv [" + svUnit + "]" : "Noise [" + svUnit + "] (Sv without TVG)")
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
      List<BroadbandRegionCache<BroadbandSvPingCache>> regionCaches = regions.stream()
            .map(regionMap::get)
            .filter(Objects::nonNull)
            .toList();
      if (regionCaches.isEmpty()) {
         return List.of();
      }
      FloatUnaryOperator svMapping = useDb.getBooleanValue() ? PowerData::svToLogSv : FloatUnaryOperator.identity();
      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
      List<Graph> graphs = new ArrayList<>();
      for (int channel = 1; channel <= rawFileConfiguration.getTransducerCount(); channel++) {
         if (channel == getInterpretationSettings().getChannel() || frequencySelectionPanel.isChannelSelected(channel)) {
            int kHz = rawFileConfiguration.getTransducers().get(channel - 1).getKHz();
            String label = kHz + " kHz";
            graphs.addAll(createBroadbandGraphs(regionCaches, channel, label, svMapping));
            if (plotNarrowband.getBooleanValue()) {
               graphs.addAll(createNarrowbandGraphs(regionCaches, kHz, channel, label, svMapping));
            }
         }
      }
      return graphs;
   }

   private List<Graph> createBroadbandGraphs(List<BroadbandRegionCache<BroadbandSvPingCache>> regionCaches, int channel, String label, FloatUnaryOperator svMapping) {
      FloatRange firstFrequencyRange = regionCaches.stream()
            .flatMap(regionCache -> regionCache.getPingMap().values().stream())
            .map(pingCache -> pingCache.getChannelCache(channel))
            .map(BroadbandSvChannelCache::frequencyRange)
            .filter(Predicate.not(FloatRange::isEmpty))
            .findFirst()
            .orElse(null);
      if (firstFrequencyRange == null) {
         return List.of();
      }

      float deltaFrequency = frequencyResolution.getFloatValue() * 1000;
      int n = Math.round(firstFrequencyRange.getSize() / deltaFrequency) + 1;

      OnlineAverageAndVariance accumulator = new OnlineAverageAndVariance(n);
      regionCaches.stream()
            .flatMap(regionCache -> regionCache.getPingMap().values().stream())
            .flatMap(pingCache -> pingCache.getChannelCache(channel).svData().stream())
            .forEach(svData -> accumulator.update(svData.sv(), svData.depthRange().getSize()));
      if (!accumulator.hasMeans()) {
         return List.of();
      }

      List<Graph> graphs = new ArrayList<>();

      float minKHz = firstFrequencyRange.min() / 1000;
      float deltaKHz = frequencyResolution.getFloatValue();

      XYInfo xyInfo = getXyInfo();

      Graph meanGraph = new Graph(label)
            .setXYInfo(xyInfo)
            .setColor(FrequencySelectionButton.channelIndexToColor(channel - 1));
      if (getInterpretationSettings().getChannel() == channel) {
         meanGraph.setLineWidth(3);
      }
      float[] means = accumulator.getMeans();
      for (int i = 0; i < means.length; i++) {
         meanGraph.addPoint(minKHz + i * deltaKHz, svMapping.applyAsFloat(means[i]));
      }
      graphs.add(meanGraph);

      if (LsssIncubatorFeatureToggles.BROADBAND_PEAK_DETECTION) {
         Graph peakGraph = new Graph(label + " peaks")
               .setXYInfo(xyInfo)
               .setColor(Color.RED);

         for (BroadbandTemporalNotchFilterConfig broadbandTemporalNotchFilterConfig : getInterpretationSettings().getBroadbandNotchFilterModuleConfig().getBroadbandTemporalNotchFilterConfigs()) {
            float freq = broadbandTemporalNotchFilterConfig.getBroadbandNotchFilterConfig().rejectionFrequency() / 1000;
            int meanIndex = Math.round((freq - minKHz) / deltaKHz);
            if (meanIndex >= 0 && meanIndex < means.length) {
               float bw = broadbandTemporalNotchFilterConfig.getBroadbandNotchFilterConfig().bandwidth() / 1000;
               int bwIndexWidth = Max.of(1, (int) (bw / deltaKHz));
               float localMin = Min.of(means, Max.of(0, meanIndex - bwIndexWidth * 2), Min.of(meanIndex + bwIndexWidth * 2, means.length));
               if (localMin < Float.POSITIVE_INFINITY) {
                  peakGraph.addSeparator();
                  peakGraph.addPoint(freq, 2 * svMapping.applyAsFloat(localMin) - svMapping.applyAsFloat(means[meanIndex]));
                  peakGraph.addPoint(freq, svMapping.applyAsFloat(means[meanIndex]));
                  peakGraph.addSeparator();
                  peakGraph.addPoint(freq - bw / 2, svMapping.applyAsFloat(localMin));
                  peakGraph.addPoint(freq + bw / 2, svMapping.applyAsFloat(localMin));
               }
            }
         }
         if (!peakGraph.getPoints().isEmpty()) {
            graphs.add(peakGraph);
         }
      }

      if (plotStdErr.getBooleanValue() && accumulator.hasVar()) {
         Graph upperGraph = new Graph(label + " (+ std err)")
               .setXYInfo(xyInfo)
               .setDashed()
               .setColor(Color.BLACK);

         Graph lowerGraph = new Graph(label + " (- std err)")
               .setXYInfo(xyInfo)
               .setDashed()
               .setColor(Color.BLACK);

         float[] stdErr = accumulator.getStdErr();
         for (int i = 0; i < means.length; i++) {
            float kHz = minKHz + i * deltaKHz;
            upperGraph.addPoint(kHz, svMapping.applyAsFloat(means[i] + stdErr[i]));
            lowerGraph.addPoint(kHz, svMapping.applyAsFloat(Math.max(0, means[i] - stdErr[i])));
         }

         graphs.add(upperGraph);
         graphs.add(lowerGraph);
      }

      return graphs;
   }

   private XYInfo getXyInfo() {
      return new XYInfo(
            new ParameterExport("frequency", Unit.KHZ, ExportRounding.kHz()),
            new ParameterExport(
                  useTVG.getBooleanValue() ? "sv" : "noise",
                  useDb.getBooleanValue() ? Unit.DB : Unit.SV,
                  useDb.getBooleanValue() ? ExportRounding.db() : ExportTransform.identity()));
   }

   private List<Graph> createNarrowbandGraphs(List<BroadbandRegionCache<BroadbandSvPingCache>> regionCaches, int kHz, int channel, String label, FloatUnaryOperator svMapping) {
      WelfordsMethod welfordsMethod = new WelfordsMethod();
      regionCaches.stream()
            .flatMap(regionCache -> regionCache.getPingMap().values().stream())
            .mapToDouble(pingCache -> pingCache.getChannelCache(channel).narrowbandSv())
            .filter(sv -> !Double.isNaN(sv))
            .forEach(welfordsMethod::update);

      long n = welfordsMethod.getCount();
      if (n == 0) {
         return List.of();
      }

      XYInfo xyInfo = getXyInfo();

      Graph meanGraph = new Graph(label)
            .setXYInfo(xyInfo)
            .setColor(FrequencySelectionButton.channelIndexToColor(channel - 1))
            .setRenderer(narrowbandRenderer(getInterpretationSettings().getChannel() == channel ? 3 : 1));
      float mean = (float) welfordsMethod.getMean();
      meanGraph.addPoint(kHz, svMapping.applyAsFloat(mean));

      if (!plotStdErr.getBooleanValue() || n <= 1) {
         return List.of(meanGraph);
      }

      Graph upperGraph = new Graph(label + " (+ std err)")
            .setXYInfo(xyInfo)
            .setColor(Color.BLACK)
            .setRenderer(narrowbandRenderer(1));

      Graph lowerGraph = new Graph(label + " (- std err)")
            .setXYInfo(xyInfo)
            .setColor(Color.BLACK)
            .setRenderer(narrowbandRenderer(1));

      float stdErr = (float) welfordsMethod.getStdErr();
      upperGraph.addPoint(kHz, svMapping.applyAsFloat(mean + stdErr));
      lowerGraph.addPoint(kHz, svMapping.applyAsFloat(Math.max(0, mean - stdErr)));

      return List.of(meanGraph, upperGraph, lowerGraph);
   }

   private static Supplier<XYItemRenderer> narrowbandRenderer(float height) {
      return () -> {
         StandardXYItemRenderer renderer = PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
         renderer.setSeriesShape(0, new Rectangle2D.Float(-3, -height / 2, 6, height));
         return renderer;
      };
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
      private final BroadbandSvModule module;
      private final JPanel mainPanel = new JPanel(new BorderLayout());
      private final ChartPanel chartPanel;

      private View(BroadbandSvModule module) {
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
