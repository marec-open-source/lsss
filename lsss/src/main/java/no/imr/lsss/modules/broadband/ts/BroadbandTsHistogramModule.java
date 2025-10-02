package no.imr.lsss.modules.broadband.ts;

import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.viewer.util.FrequencySelectionPanel;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.util.FrequencyPlotMarker;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.Histogram2D;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.Histogram2DDataset;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.PointListDataset;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.plot.XYZInfo;
import no.imr.tools.range.FloatRangeBuilder;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.ui.RectangleAnchor;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class BroadbandTsHistogramModule extends BaseViewModule implements PojoDataContainer {
   public final FloatParameter tsResolution = new FloatParameter(
         new Name("TSResolution", "TS resolution"),
         1, Unit.DB, ValueConstraints.gt(0f));

   public final BooleanParameter autoAdjustAxes = new BooleanParameter(
         new Name("AutoAdjustAxes", "Auto-adjust axes"),
         true,
         "Automatically adjust the x-axis and y-axis when the plot is updated");

   private final Supplier<BroadbandTsModule> broadbandTsModule = moduleSupplier(BroadbandTsModule.class);

   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));

   private final Listener computeListener = newCoalescingExecListener(this::compute);
   private final FrequencySelectionPanel frequencySelectionPanel = new FrequencySelectionPanel(computeListener);
   private final Listener plotListener = newCoalescingExecListener(this::plot);

   private List<Histogram2DDataset> histogramDatasets = List.of();
   private int maxCount = 1;
   private Graph highlightedGraph = new Graph();
   private final FrequencyPlotMarker frequencyPlotMarker;
   private JFreeChart chart = PlotUtils.newEmptyChart();

   public BroadbandTsHistogramModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      autoAdjustAxes.setPersistable(false);

      frequencyPlotMarker = new FrequencyPlotMarker(getLSSS());
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            tsResolution,
            autoAdjustAxes
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(computeListener, List.of(
            tsResolution,
            autoAdjustAxes,
            broadbandTsModule.get().frequencyResolution,
            broadbandTsModule.get().getTSDetectionChangeManager()
      ));

      registry.add(getInterpretationSettings().getDataFileChangeManager(), newCoalescingExecListener(this::dataFilesUpdated));

      registry.add(getInterpretationSettings().getChannelChangeManager(), newCoalescingExecListener(channel -> {
         frequencySelectionPanel.setHighlighted(channel);
         computeListener.listen();
      }));

      registry.add(getInterpretationSettings().mouseover().echogramPoint(), newCoalescingExecListener(this::computeHighlighted));
      registry.add(getInterpretationSettings().mouseover().kHz(), newCoalescingExecListener(frequencyPlotMarker::updateMarker));

      //---

      dataFilesUpdated();
      computeListener.listen();
   }

   @Override
   protected void onDisable() {
      histogramDatasets = List.of();
      maxCount = 1;
      highlightedGraph = new Graph();
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

   private void computeHighlighted() {
      Graph graph = new Graph("Highlighted")
            .setXYInfo(new XYInfo(
                  new ParameterExport("frequency", Unit.KHZ, ExportRounding.kHz()),
                  new ParameterExport("tsc", Unit.DB, ExportRounding.db())));
      EchogramPoint echogramPoint = getInterpretationSettings().mouseover().getEchogramPoint();
      if (echogramPoint != null) {
         graph.setColor(Color.RED);
         int channel = getInterpretationSettings().getChannel();
         broadbandTsModule.get().getTargetsForPoint(getRegionManager().getSelectedRegions(), echogramPoint, channel).values().stream()
               .flatMap(Collection::stream)
               .forEach(target -> {
                  graph.addSeparator();
                  BroadbandTsModule.addToGraph(target, graph);
               });
      }
      highlightedGraph = graph;
      plotListener.listen();
   }

   private void compute() {
      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
      List<RawFileTransducer> transducers = rawFileConfiguration.getTransducers();
      List<Histogram2D> histograms = new ArrayList<>();
      List<Histogram2DDataset> newHistogramDatasets = new ArrayList<>();
      XYZInfo xyzInfo = new XYZInfo(
            new ParameterExport("frequency", Unit.KHZ, ExportRounding.kHz()),
            new ParameterExport("tsc", Unit.DB, ExportRounding.db()),
            new ParameterExport("count", Unit.COUNT, ExportTransform.identity()));
      for (int channel : frequencySelectionPanel.getChannels()) {
         if (!(channel == getInterpretationSettings().getChannel() || frequencySelectionPanel.isChannelSelected(channel))) {
            continue;
         }

         FloatRangeBuilder frequencyRangeBuilder = new FloatRangeBuilder();
         FloatRangeBuilder tsRangeBuilder = new FloatRangeBuilder();
         forEachTarget(channel, target -> {
            frequencyRangeBuilder.expand(target.frequencyRange());
            tsRangeBuilder.expand(target.values());
         });

         Histogram2D histogram = new Histogram2D(frequencyRangeBuilder.toFloatRange().multiply(0.001f), broadbandTsModule.get().frequencyResolution.getFloatValue(),
               tsRangeBuilder.toFloatRange(), tsResolution.getFloatValue());
         histograms.add(histogram);
         String name = channel <= transducers.size() ? transducers.get(channel - 1).getKHz() + " kHz" : "";
         newHistogramDatasets.add(new Histogram2DDataset(name, histogram, xyzInfo));

         forEachTarget(channel, target -> {
            float minKHz = target.frequencyRange().min() / 1000;
            float deltaKHz = target.getDeltaFrequency() / 1000;
            float[] values = target.values();
            int i0 = histogram.xToI(minKHz);
            int j0 = histogram.yToJ(values[0]);
            histogram.addByIndex(i0, j0);
            for (int index = 1; index < values.length; index++) {
               int i1 = histogram.xToI(minKHz + index * deltaKHz);
               if (i1 == i0) {
                  continue;
               }
               int j1 = histogram.yToJ(values[index]);
               float djdi = (float) (j1 - j0) / (i1 - i0);
               float j = j0;
               for (int i = i0 + 1; i <= i1; i++) {
                  j += djdi;
                  histogram.addByIndex(i, Math.round(j));
               }
               i0 = i1;
               j0 = j1;
            }
         });
      }
      histogramDatasets = newHistogramDatasets;

      maxCount = histograms.stream()
            .flatMap(histogram -> Stream.of(histogram.getCounts()))
            .flatMapToInt(IntStream::of)
            .max()
            .orElse(1);

      computeHighlighted();

      plotListener.listen();
   }

   private void forEachTarget(int channel, Consumer<BroadbandTsData> consumer) {
      getRegionManager().getSelectedRegions().stream()
            .flatMap(region -> broadbandTsModule.get().getTSData(region).values().stream())
            .flatMap(pingCache -> pingCache.getTsData(channel).stream())
            .forEach(consumer);
   }

   private void plot() {
      NumberAxis xAxis = createAxis("Frequency [kHz]");
      NumberAxis yAxis = createAxis("TS [dB]");
      XYPlot plot = new XYPlot(null, xAxis, yAxis, null);
      plot.setDomainGridlinesVisible(false);
      plot.setRangeGridlinesVisible(false);
      plot.setDomainPannable(true);
      plot.setRangePannable(true);

      plotHighlighted(plot);
      plotHistogram(plot);

      frequencyPlotMarker.addMarker(plot);

      setChart(PlotUtils.newChart(null, plot, false));
   }

   private void setChart(JFreeChart chart) {
      if (!autoAdjustAxes.getBooleanValue()) {
         PlotUtils.preserveAxisRanges(this.chart, chart);
      }
      this.chart = chart;
      viewHolder.ifViewDelayed(this, View::updateChart);
   }

   private void plotHighlighted(XYPlot plot) {
      StandardXYItemRenderer renderer = PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.LINES);
      renderer.setSeriesStroke(0, GuiUtils.STROKE_3);
      renderer.setSeriesPaint(0, Color.RED);
      int i = PlotUtils.nextDatasetIndex(plot);
      plot.setDataset(i, new PointListDataset(highlightedGraph));
      plot.setRenderer(i, renderer);
   }

   private void plotHistogram(XYPlot plot) {
      XYBlockRenderer renderer = new XYBlockRenderer();
      renderer.setBlockWidth(broadbandTsModule.get().frequencyResolution.getFloatValue());
      renderer.setBlockHeight(tsResolution.getFloatValue());
      renderer.setBlockAnchor(RectangleAnchor.CENTER);
      renderer.setPaintScale(new GreyPaintScale(maxCount));
      for (Histogram2DDataset histogramDataset : histogramDatasets) {
         int i = PlotUtils.nextDatasetIndex(plot);
         plot.setDataset(i, histogramDataset);
         plot.setRenderer(i, renderer);
      }
   }

   private static NumberAxis createAxis(String label) {
      NumberAxis axis = PlotUtils.newNumberAxis(label);
      axis.setLowerMargin(0);
      axis.setUpperMargin(0);
      return axis;
   }

   @Override
   public PojoData getPojoData() {
      return PojoData.newBuilder(getPersistentName())
            .withDatasets(chart.getXYPlot())
            .build();
   }

   private record GreyPaintScale(double max) implements PaintScale {

      @Override
      public double getLowerBound() {
         return 0;
      }

      @Override
      public double getUpperBound() {
         return max;
      }

      @Override
      public Paint getPaint(double value) {
         int g = (int) (255 * (1 - value / max));
         return new Color(g, g, g);
      }
   }

   private static final class View extends BaseView {
      private final BroadbandTsHistogramModule module;
      private final JPanel mainPanel = new JPanel(new BorderLayout());
      private final ChartPanel chartPanel;

      private View(BroadbandTsHistogramModule module) {
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
