package no.imr.lsss.modules.misc;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.region.Region;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.viewer.util.FrequencySelectionPanel;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.tools.Utils;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
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

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CorrelationModule extends BaseViewModule implements PojoDataContainer {
   private static final float MIN_UPPER_THRESHOLD = -100;
   private static final int N = 200;
   private static final float DELTA = 0.5f;
   private static final float MIN_DB_SPAN = 10.0f * DELTA;

   private final FloatParameter delta = new FloatParameter(new Name("Delta"),
         0.5f, Unit.DB,
         "The difference in upper threshold (resolution: " + DELTA + ")");

   private final FloatParameter dbSpan = new FloatParameter(new Name("Span"),
         15.0f, Unit.DB,
         "The difference between upper and lower threshold (minimum " + MIN_DB_SPAN + ")");

   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private JFreeChart chart = PlotUtils.newEmptyChart();

   private final Listener recomputeListener = newCoalescingExecListener(this::recompute);
   private final FrequencySelectionPanel frequencySelectionPanel = new FrequencySelectionPanel(recomputeListener);

   private float[][] sums = new float[0][N];
   private int[][] counts = new int[0][N];
   private final Set<PingIndex> pingIndexes = new HashSet<>();

   public CorrelationModule(ModuleInfo<?> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            delta,
            dbSpan
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getInterpretationSettings().getDataFileChangeManager(), newCoalescingExecListener(this::dataFileChanged));
      registry.add(getParameters(), recomputeListener);
      registry.add(getInterpretationSettings().getChannelChangeManager(), newCoalescingExecListener(channel -> {
         frequencySelectionPanel.setHighlighted(channel);
         recomputeListener.listen();
      }));
      registry.add(recomputeListener, List.of(
            getInterpretationSettings().getPingRangeChangeManager(),
            getRegionManager().getThresholdManager().getChangeManager(),
            getRegionManager().selectedRegions(),
            getRegionManager().getRegionDefinitionChangeManager()
      ));
      registry.add(getInterpretationSettings().getPingSampler().getNewPingsChangeManager(), newExecListener(this::processPings));

      //---

      dataFileChanged();
   }

   private void dataFileChanged() {
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      RawFileConfiguration rawFileConfiguration = dataFileSet.getRawFileConfiguration();
      if (rawFileConfiguration.getTransducerCount() != sums.length) {
         sums = new float[rawFileConfiguration.getTransducerCount()][N];
         counts = new int[rawFileConfiguration.getTransducerCount()][N];
      }
      frequencySelectionPanel.update(rawFileConfiguration, getInterpretationSettings().getChannel());
      recompute();
   }

   private void recompute() {
      Utils.fill(sums, 0);
      Utils.fill(counts, 0);
      pingIndexes.clear();
      processPings(getInterpretationSettings().getPingSampler().getAvailablePings());
   }

   private void processPings(List<Ping> pings) {
      PingRange pingRange = getInterpretationSettings().getPingRange();

      for (Region region : getRegionManager().getSelectedRegions()) {
         PingRange visibleRegionPingRange = region.getPingRange().intersection(pingRange);
         if (visibleRegionPingRange.isEmpty()) {
            continue;
         }
         int dBSpan = (int) Math.ceil(Math.max(dbSpan.getFloatValue(), MIN_DB_SPAN));

         for (Ping ping : pings) {
            PingIndex pingIndex = ping.getPingIndex();
            if (!visibleRegionPingRange.contains(pingIndex)) {
               continue;
            }
            if (!pingIndexes.add(pingIndex)) {
               continue;
            }

            FloatRange svRange = getRegionManager().getThresholdManager().getLinearSvRange(pingIndex);
            float lowerThreshold = svRange.min();

            for (ChannelData channelData : ping.getChannelDatas()) {
               if (channelData == null) {
                  continue;
               }
               PowerData powerData = channelData.getPowerData();
               int channel = powerData.getChannel();
               if (!(channel == getInterpretationSettings().getChannel() || frequencySelectionPanel.isChannelSelected(channel))) {
                  continue;
               }

               float[] svArray = powerData.getSv();
               float[] channelSums = sums[channel - 1];
               int[] channelCounts = counts[channel - 1];

               for (FloatRange depthRange : getRegionManager().getDepthRangesForChannel(region, ping, channel)) {
                  int iBegin = powerData.depthToClampedSampleIndex(depthRange.min());
                  int iEnd = powerData.depthToClampedSampleIndex(depthRange.max());

                  for (int iSv = iBegin; iSv < iEnd; iSv++) {
                     float sv = svArray[iSv];
                     if (sv < lowerThreshold) {
                        continue;
                     }
                     float logSv = PowerData.svToLogSv(sv);
                     int upperThresholdIndex = Math.max(0, (int) Math.ceil((logSv - MIN_UPPER_THRESHOLD) / DELTA));
                     int lowerThresholdIndex = Math.min(channelSums.length, upperThresholdIndex + dBSpan);
                     for (int i = upperThresholdIndex; i < lowerThresholdIndex; i++) {
                        channelSums[i] += sv;
                        channelCounts[i]++;
                     }
                  }
               }
            }
         }
      }

      updatePlot();
   }

   private void updatePlot() {
      Graph graph = new Graph("Correlation")
            .setXYInfo(new XYInfo(
                  new ParameterExport("sv", Unit.DB, ExportRounding.db()),
                  new ParameterExport("correlation", Unit.NONE, ExportTransform.identity())));
      int step = Math.round(delta.getFloatValue() / DELTA);
      for (int i = 0; i < N; i++) {
         int j = i + step;
         if (j < 0 || j >= N) {
            continue;
         }
         float x = MIN_UPPER_THRESHOLD + i * DELTA;
         float y = (float) correlation(i, j);
         graph.addPoint(x, y);
      }
      chart = new Plotter(List.of(graph))
            .xAxis("Sv [dB]")
            .yAxis("Correlation")
            .createChart();
      viewHolder.ifViewDelayed(this, View::updateChart);
   }

   private double correlation(int index1, int index2) {
      double sumX = 0;
      double sumY = 0;
      double sumSqX = 0;
      double sumSqY = 0;
      double sumXY = 0;
      int n = 0;
      for (int channelIndex = 0; channelIndex < sums.length; channelIndex++) {
         int nx = counts[channelIndex][index1];
         int ny = counts[channelIndex][index2];
         if (nx == 0 || ny == 0) {
            continue;
         }
         double x = (double) sums[channelIndex][index1] / nx;
         double y = (double) sums[channelIndex][index2] / ny;
         sumX += x;
         sumY += y;
         sumSqX += x * x;
         sumSqY += y * y;
         sumXY += x * y;
         n++;
      }
      if (n <= 1) {
         return Double.NaN;
      }
      return (n * sumXY - sumX * sumY) / Math.sqrt((n * sumSqX - sumX * sumX) * (n * sumSqY - sumY * sumY));
   }

   @Override
   public PojoData getPojoData() {
      return PojoData.newBuilder(getPersistentName())
            .withDatasets(chart.getXYPlot())
            .build();
   }

   private static final class View extends BaseView {
      private final CorrelationModule module;
      private final JPanel mainPanel = new JPanel(new BorderLayout());
      private final ChartPanel chartPanel;

      private View(CorrelationModule module) {
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
