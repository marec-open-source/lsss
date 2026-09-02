package no.imr.lsss.modules.sv;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.region.Region;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.viewer.util.FrequencySelectionButton;
import no.imr.korona.viewer.util.FrequencySelectionPanel;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.modules.thresholdresponse.ThresholdResponseModule;
import no.imr.tools.Utils;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.Plotter;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.ViewHolder;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ui.HorizontalAlignment;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SvDistributionModule extends BaseViewModule implements PojoDataContainer {

   static final float MIN_LOG_SV = -220;
   private static final float MAX_LOG_SV = -5;
   static final float DELTA_LOG_SV = 1;
   private static final float RANGE_LOG_SV = MAX_LOG_SV - MIN_LOG_SV;
   static final int N_LOG_SV = (int) (RANGE_LOG_SV / DELTA_LOG_SV) + 1;
   private static final float[] I_LOG_SV_TO_LOG_SV = new float[N_LOG_SV];

   private final FloatParameter minLogSv = new FloatParameter(
         new Name("MinSv", "Min Sv"),
         -125, Unit.DB,
         "Minimum Sv value in plot");

   private final FloatParameter maxLogSv = new FloatParameter(
         new Name("MaxSv", "Max Sv"),
         -25, Unit.DB,
         "Maximum Sv value in plot");

   private final FloatParameter maxRange = new FloatParameter(
         new Name("MaxRange", "Max range"),
         -1, Unit.COUNT,
         "Maximum count (-1 to automatically find range)");

   static {
      for (int i = 0; i < N_LOG_SV; i++) {
         I_LOG_SV_TO_LOG_SV[i] = MIN_LOG_SV + i * DELTA_LOG_SV;
      }
   }

   private final BooleanParameter useTVG = new BooleanParameter(
         new Name("UseTVG", "Use TVG"),
         true,
         "Use gain compensation");

   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private JFreeChart chart = PlotUtils.newEmptyChart();
   private final Listener refreshListener = newCoalescingExecListener(this::refresh);
   private final FrequencySelectionPanel frequencySelectionPanel = new FrequencySelectionPanel(refreshListener);

   private final Map<Region, RegionCache> regionMap = new HashMap<>();

   public SvDistributionModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            minLogSv,
            maxLogSv,
            maxRange,
            useTVG
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      Listener recomputeListener = newCoalescingExecListener(this::recompute);

      registry.add(getParameters(), recomputeListener);

      registry.add(getInterpretationSettings().getPingRangeChangeManager(), recomputeListener);
      registry.add(getInterpretationSettings().getPingSampler().getNewPingsChangeManager(), newExecListener(this::processPings));
      registry.add(getInterpretationSettings().getDataFileChangeManager(), newCoalescingExecListener(this::updateCheckBoxes));
      registry.add(getInterpretationSettings().getChannelChangeManager(), newCoalescingExecListener(channel -> {
         frequencySelectionPanel.setHighlighted(channel);
         refreshListener.listen();
      }));

      registry.add(getRegionManager().getThresholdManager().getChangeManager(), refreshListener);
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

      updateCheckBoxes();
      recompute();
   }

   @Override
   protected void onDisable() {
      regionMap.clear();
   }

   private void updateCheckBoxes() {
      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
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

      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
      int channelCount = rawFileConfiguration.getTransducerCount();

      PerFrequencyData[] selectedRegionsData = PerFrequencyData.newArray(channelCount);

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

            PingCache pingCache = regionCache.pingMap.get(pingIndex);
            if (pingCache == null) {
               pingCache = new PingCache(getRegionManager(), ping, region, channelCount, useTVG.getValue());
               regionCache.pingMap.put(pingIndex, pingCache);
               regionNeedUpdate = true;
            }
         }

         if (regionNeedUpdate) {
            regionCache.update();
         }

         for (int channelIndex = 0; channelIndex < selectedRegionsData.length; channelIndex++) {
            selectedRegionsData[channelIndex].accumulate(regionCache.perFrequencyData[channelIndex]);
         }
      }

      if (selectedVisibleRegionPingRange.isEmpty()) {
         plotOnlyThresholds();
         return;
      }

      int maxCount = 0;

      List<Graph> graphs = new ArrayList<>(selectedRegionsData.length + 2);
      XYInfo xyInfo = new XYInfo(
            new ParameterExport(useTVG.getBooleanValue() ? "sv" : "noise", Unit.DB, ExportRounding.db()),
            new ParameterExport("count", Unit.COUNT, ExportTransform.identity()));

      for (int channelIndex = 0; channelIndex < selectedRegionsData.length; channelIndex++) {
         int channel = channelIndex + 1;
         int kHz = rawFileConfiguration.getTransducers().get(channelIndex).getKHz();

         if (channel == getInterpretationSettings().getChannel() || frequencySelectionPanel.isChannelSelected(channel)) {
            Graph graph = new Graph(kHz + " kHz", N_LOG_SV)
                  .setXYInfo(xyInfo)
                  .setColor(FrequencySelectionButton.channelIndexToColor(channelIndex));
            if (getInterpretationSettings().getChannel() == channel) {
               graph.setLineWidth(3);
            }
            graphs.add(graph);
            int[] selectedHistogram = selectedRegionsData[channelIndex].histogram;
            for (int i = 0; i < N_LOG_SV; i++) {
               int count = selectedHistogram[i];
               graph.addPoint(I_LOG_SV_TO_LOG_SV[i], count);
               if (maxCount < count) {
                  maxCount = count;
               }
            }
         }
      }

      float meanLogSv = PowerData.svToLogSv(selectedRegionsData[getInterpretationSettings().getChannel() - 1].getMeanSv());

      float maxY = maxRange.getFloatValue() > 0 ? maxRange.getFloatValue() : maxCount * 1.05f;
      plotGraphs(graphs, maxY, "Mean Sv: " + Utils.format("%.1f", meanLogSv));
   }

   private void plotOnlyThresholds() {
      plotGraphs(new ArrayList<>(2), 1, "");
   }

   private void plotGraphs(List<Graph> graphs, float maxY, String meanText) {
      if (useTVG.getValue()) {
         graphs.addAll(ThresholdResponseModule.createThresholdsGraph(getInterpretationSettings().getColorConverterContainer().getSV(), FloatRange.of(0, maxY)));
      }
      chart = new Plotter(graphs)
            .xAxis(useTVG.getValue() ? "Sv [dB]" : "Noise [dB] (Sv without TVG)")
            .xRange(minLogSv.getFloatValue(), maxLogSv.getFloatValue())
            .yAxis(() -> {
               NumberAxis yAxis = PlotUtils.newNumberAxis("Count");
               yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
               return yAxis;
            })
            .yRange(0, maxY)
            .createChart();
      chart.addSubtitle(PlotUtils.newTextTitle(meanText, HorizontalAlignment.RIGHT));
      viewHolder.ifViewDelayed(this, View::updateChart);
   }

   @Override
   public PojoData getPojoData() {
      return PojoData.newBuilder(getPersistentName())
            .withDatasets(chart.getXYPlot())
            .build();
   }

   private static final class View extends BaseView {
      private final SvDistributionModule module;
      private final JPanel mainPanel = new JPanel(new BorderLayout());
      private final ChartPanel chartPanel;

      private View(SvDistributionModule module) {
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
