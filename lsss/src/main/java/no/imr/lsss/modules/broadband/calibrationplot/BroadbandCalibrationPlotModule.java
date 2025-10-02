package no.imr.lsss.modules.broadband.calibrationplot;

import no.imr.korona.data.formats.ek60.calibration.BroadbandFunction;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.util.ExportRounding;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.util.FrequencyPlotMarker;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.Function1D;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.Plotter;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.ViewHolder;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class BroadbandCalibrationPlotModule extends BaseViewModule implements PojoDataContainer {
   private final ViewHolder<BroadbandCalibrationPlotView> viewHolder = new ViewHolder<>(() -> new BroadbandCalibrationPlotView(this));
   private final FrequencyPlotMarker frequencyPlotMarker;
   private JFreeChart chart = PlotUtils.newEmptyChart();

   private final Listener recomputeListener = newCoalescingExecListener(this::recompute);
   private final Set<CalibrationPlotParameter> parameters = EnumSet.of(CalibrationPlotParameter.GAIN);

   public BroadbandCalibrationPlotModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      frequencyPlotMarker = new FrequencyPlotMarker(getLSSS());
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getInterpretationSettings().mouseover().pingIndex(), recomputeListener);
      registry.add(getInterpretationSettings().mouseover().kHz(), newCoalescingExecListener(frequencyPlotMarker::updateMarker));

      //---

      recompute();
   }

   @Override
   protected void onDisable() {
      setChart(PlotUtils.newEmptyChart());
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   JFreeChart getChart() {
      return chart;
   }

   FrequencyPlotMarker getFrequencyPlotMarker() {
      return frequencyPlotMarker;
   }

   boolean isSelected(CalibrationPlotParameter parameter) {
      return parameters.contains(parameter);
   }

   void setSelected(CalibrationPlotParameter parameter, boolean selected) {
      executeIfEnabled(() -> {
         if (selected) {
            parameters.add(parameter);
         } else {
            parameters.remove(parameter);
         }
         recomputeListener.listen();
      });
   }

   private void recompute() {
      PingIndex pingIndex = getInterpretationSettings().mouseover().getPingIndex();
      if (pingIndex == null) {
         plot(List.of(), "");
         return;
      }
      Ping ping = getInterpretationSettings().getDataFileSet().getPing(pingIndex);
      plot(createGraphs(ping), parameters.isEmpty() ? "No parameters selected" : "No broadband data");
   }

   private void plot(List<Graph> graphs, String noDataMessage) {
      String yLabel = parameters.stream()
            .map(parameter -> parameter.unit)
            .distinct()
            .map(unit -> '[' + unit.text() + ']')
            .collect(Collectors.joining(", "));
      JFreeChart chart = new Plotter(graphs)
            .xAxis("Frequency [kHz]")
            .yAxis(yLabel)
            .createChart();
      XYPlot plot = chart.getXYPlot();
      plot.setNoDataMessage(noDataMessage);
      frequencyPlotMarker.addMarker(plot);
      setChart(chart);
   }

   private void setChart(JFreeChart chart) {
      this.chart = chart;
      viewHolder.ifViewDelayed(this, BroadbandCalibrationPlotView::updateChart);
   }

   private List<Graph> createGraphs(Ping ping) {
      List<Graph> graphs = new ArrayList<>();

      ping.getNonNullChannelDatas(BroadbandData.class).forEach(broadbandData -> {
         parameters.forEach(parameter -> {
            FloatRange frequencyRange = broadbandData.getFrequencyRange();
            graphs.add(makeDefaultGraph(parameter, parameter.uncalibrated.apply(broadbandData), frequencyRange));
            parameter.calibrated.apply(broadbandData.getCalibration()).ifPresent(f -> {
               graphs.addAll(makeCalibrationGraph(parameter, f, frequencyRange));
            });
         });
      });

      return graphs;
   }

   private static XYInfo createXYInfo(CalibrationPlotParameter parameter) {
      return new XYInfo(
            new ParameterExport("frequency", Unit.KHZ, ExportRounding.kHz()),
            new ParameterExport(parameter.exportName, parameter.unit, parameter.exportTransform));
   }

   private static Graph makeDefaultGraph(CalibrationPlotParameter parameter, Function1D function, FloatRange frequencyRange) {
      int n = 20;
      float frequencyStep = frequencyRange.getSize() / (n - 1);
      Graph graph = new Graph(parameter.fullName + " (default)")
            .setXYInfo(createXYInfo(parameter))
            .setColor(Color.GRAY);
      for (int i = 0; i < n; i++) {
         float hz = frequencyRange.min() + i * frequencyStep;
         graph.addPoint(hz / 1000, function.eval(hz));
      }
      return graph;
   }

   private static List<Graph> makeCalibrationGraph(CalibrationPlotParameter parameter, BroadbandFunction function, FloatRange frequencyRange) {
      double[] hz = function.getHz();
      double[] values = function.getValues();
      Graph graph = new Graph(parameter.fullName)
            .setXYInfo(createXYInfo(parameter))
            .setColor(Color.BLUE);
      for (int i = 0; i < hz.length; i++) {
         graph.addPoint(hz[i] / 1000, values[i]);
      }
      List<Graph> graphs = new ArrayList<>(3);
      makeExtrapolationGraph(parameter, frequencyRange.min(), hz[0], values[0]).ifPresent(graphs::add);
      makeExtrapolationGraph(parameter, hz[hz.length - 1], frequencyRange.max(), values[hz.length - 1]).ifPresent(graphs::add);
      graphs.add(graph);
      return graphs;
   }

   private static Optional<Graph> makeExtrapolationGraph(CalibrationPlotParameter parameter, double minHz, double maxHz, double value) {
      if (minHz >= maxHz) {
         return Optional.empty();
      }
      Graph graph = new Graph(parameter.fullName + " (extrapolation)")
            .setXYInfo(createXYInfo(parameter))
            .setColor(Color.RED)
            .setLineWidth(3);
      graph.addPoint(minHz / 1000, value);
      graph.addPoint(maxHz / 1000, value);
      return Optional.of(graph);
   }

   @Override
   public PojoData getPojoData() {
      return PojoData.newBuilder(getPersistentName())
            .withDatasets(chart.getXYPlot())
            .build();
   }
}
