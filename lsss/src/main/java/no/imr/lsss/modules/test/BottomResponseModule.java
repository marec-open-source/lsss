package no.imr.lsss.modules.test;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.viewer.util.FrequencySelectionButton;
import no.imr.korona.viewer.util.FrequencySelectionPanel;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.logging.Log;
import no.imr.tools.math.Median;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.RangeParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.Plotter;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.ViewHolder;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BottomResponseModule extends BaseViewModule implements PojoDataContainer {
   private static final int PING_AVERAGE_COUNT = 5;

   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private final Listener recomputeListener = newCoalescingExecListener(this::recompute);
   private final FrequencySelectionPanel frequencySelectionPanel = new FrequencySelectionPanel(recomputeListener);

   private final RangeParameter depthAxisRange = new RangeParameter(
         new Name("DepthRange", "Depth range"),
         -10, 30, Unit.METER, ValueConstraints.gteLte(-10f, 30f),
         "Depth range around bottom");

   private final RangeParameter svAxisRange = new RangeParameter(
         new Name("SvAxisRange", "Sv-axis range"),
         -100, 20, Unit.DB, ValueConstraints.gteLte(-100f, 20f),
         "Range of log(Sv) axis");

   private JFreeChart chart = PlotUtils.newEmptyChart();

   public BottomResponseModule(ModuleInfo<TestPlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            depthAxisRange,
            svAxisRange
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getInterpretationSettings().mouseover().pingIndex(), recomputeListener);

      registry.add(getInterpretationSettings().getDataFileChangeManager(), newCoalescingExecListener(this::dataFilesChanged));

      registry.add(getInterpretationSettings().getChannelChangeManager(), newCoalescingExecListener(channel -> {
         frequencySelectionPanel.setHighlighted(channel);
         recomputeListener.listen();
      }));

      //---

      dataFilesChanged();
      recompute();
   }

   private void dataFilesChanged() {
      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
      frequencySelectionPanel.update(rawFileConfiguration, getInterpretationSettings().getChannel());
   }

   private void recompute() {
      PingIndex pingIndex = getInterpretationSettings().mouseover().getPingIndex();
      chart = pingIndex != null
            ? createChart(pingIndex, getInterpretationSettings().getBottomZSettings().getDepthTransform())
            : createChart(List.of());
      viewHolder.ifViewDelayed(this, View::updateChart);
   }

   private JFreeChart createChart(PingIndex centerIndex, DepthTransform depthTransform) {
      Map<Integer, Graph> graphs = new HashMap<>();

      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      PingRange totalRange = dataFileSet.getTotalRange();
      long startPingNumber = Math.max(centerIndex.getPingNumber() - PING_AVERAGE_COUNT / 2, totalRange.begin().getPingNumber());
      long endPingNumber = Math.min(startPingNumber + PING_AVERAGE_COUNT, totalRange.end().getPingNumber());

      FloatRange depthRange = depthAxisRange.getValue();

      Map<Integer, Float> sampleDistanceMap = new HashMap<>();
      Map<Integer, Integer> samplesAboveMap = new HashMap<>();
      Map<Integer, Integer> samplesBelowMap = new HashMap<>();
      Map<Integer, List<List<Float>>> sampleValueMap = new HashMap<>();
      for (long pingNumber = startPingNumber; pingNumber < endPingNumber; pingNumber++) {
         PingIndex pingIndex = dataFileSet.getPingIndex(pingNumber);
         Ping ping = dataFileSet.getPing(pingIndex);
         for (int transducerIndex = 0; transducerIndex < dataFileSet.getTransducerCount(); transducerIndex++) {
            if (!(transducerIndex + 1 == getInterpretationSettings().getChannel() || frequencySelectionPanel.isChannelSelected(transducerIndex + 1))) {
               continue;
            }
            PowerData powerData = ping.getPowerData(transducerIndex + 1);
            if (powerData == null) {
               continue;
            }
            float sampleDistance = powerData.getSampleDistance();
            if (!sampleDistanceMap.containsKey(transducerIndex)) {
               sampleDistanceMap.put(transducerIndex, sampleDistance);
               int samplesAbove = (int) (-depthRange.min() / sampleDistance);
               samplesAboveMap.put(transducerIndex, samplesAbove);
               int samplesBelow = (int) (depthRange.max() / sampleDistance);
               samplesBelowMap.put(transducerIndex, samplesBelow);
               List<List<Float>> sampleValues = new ArrayList<>();
               for (int i = 0; i < samplesAbove + samplesBelow + 1; i++) {
                  sampleValues.add(new ArrayList<>());
               }
               sampleValueMap.put(transducerIndex, sampleValues);
               Graph graph = new Graph(ping.getRawFileConfiguration().getTransducers().get(transducerIndex).getKHz() + " kHz")
                     .setXYInfo(new XYInfo(
                           new ParameterExport("distanceBelowBottom", Unit.METER, ExportRounding.depth()),
                           new ParameterExport("sv", Unit.DB, ExportRounding.db())));
               graphs.put(transducerIndex, graph);
            } else if (sampleDistanceMap.get(transducerIndex) != sampleDistance) {
               Log.global.warning("Sample distance changed, ignoring ping");
               continue;
            }
            float bottomDepth = depthTransform.zToDepth(0, ping.getPingIndex());
            int bottomSample = powerData.depthToSampleIndex(bottomDepth);
            if (bottomSample >= powerData.getCount()) {
               continue;
            }
            int samplesAbove = samplesAboveMap.get(transducerIndex);
            int samplesBelow = samplesBelowMap.get(transducerIndex);

            List<List<Float>> sampleValues = sampleValueMap.get(transducerIndex);
            float[] logSv = powerData.getLogSv();
            for (int i = bottomSample - samplesAbove; i < bottomSample + samplesBelow; i++) {
               if (i >= powerData.getCount() || i < 0) {
                  break;
               }
               sampleValues.get(i - bottomSample + samplesAbove).add(logSv[i]);
            }
         }
      }

      for (int transducerIndex = 0; transducerIndex < dataFileSet.getTransducerCount(); transducerIndex++) {
         Graph graph = graphs.get(transducerIndex);
         if (graph == null) {
            continue;
         }
         graph.setColor(FrequencySelectionButton.channelIndexToColor(transducerIndex));
         List<List<Float>> values = sampleValueMap.get(transducerIndex);
         int samplesAbove = samplesAboveMap.get(transducerIndex);
         float sampleDistance = sampleDistanceMap.get(transducerIndex);
         for (int i = 0; i < values.size(); i++) {
            List<Float> samples = values.get(i);
            if (samples.isEmpty()) {
               continue;
            }
            float[] vals = new float[samples.size()];
            int j = 0;
            for (Float sample : samples) {
               vals[j] = sample;
               j++;
            }
            float dist = (i - samplesAbove) * sampleDistance;
            float median = Median.quickSelect(vals);
            graph.addPoint(dist, median);
         }
      }

      return createChart(new ArrayList<>(graphs.values()));
   }

   private JFreeChart createChart(List<Graph> graphs) {
      return new Plotter(graphs)
            .title("Bottom response")
            .xAxis("Distance below bottom [m]")
            .xRange(depthAxisRange.getValue())
            .yAxis("Sv [dB]")
            .yRange(svAxisRange.getValue())
            .createChart();
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   @Override
   public PojoData getPojoData() {
      return PojoData.newBuilder(getPersistentName())
            .withDatasets(chart.getXYPlot())
            .build();
   }

   private static final class View extends BaseView {
      private final BottomResponseModule module;
      private final JPanel mainPanel = new JPanel(new BorderLayout());
      private final ChartPanel chartPanel;

      private View(BottomResponseModule module) {
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
