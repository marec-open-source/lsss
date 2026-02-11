package no.imr.korona.computation.noise;

import no.imr.korona.computation.BaseModuleComputation;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.computation.display.PlayboxModule;
import no.imr.korona.data.datagrams.Nqp0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.viewer.KoronaPlaybox;
import no.imr.korona.viewer.util.FrequencySelectionButton;
import no.imr.korona.viewer.util.FrequencySelectionPanel;
import no.imr.tools.Utils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.Plotter;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MultiSplitPane;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jspecify.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Visualizes noise parameters in a separate window.
 */
public final class NoiseVisualizationModule extends PlayboxModule {
   private @Nullable NoiseVisualizationView view;
   private final FrequencySelectionPanel frequencySelectionPanel = new FrequencySelectionPanel(this::updatePlotVisibility);
   private boolean showNE = true;
   private boolean showNH = true;
   private boolean showNMedian = true;

   private @Nullable NoiseVisualizationModuleComputation computation;

   public final IntParameter timeSeriesLength = new IntParameter(
         new Name("TimeSeriesLength", "Time series length"),
         200, Unit.SECONDS, ValueConstraints.gte(1),
         "Number of seconds in the time series");

   public final FloatParameter histogramSpan = new FloatParameter(
         new Name("HistogramSpan", "Histogram span"),
         3, Unit.DIMENSIONLESS, ValueConstraints.gt(0f),
         "Upper limit of plotted histogram domain in multiples of N_H");

   public final IntParameter medianSampleCount = new IntParameter(
         new Name("MedianSampleCount", "Median sample count"),
         500, Unit.COUNT, ValueConstraints.gte(0),
         "Number of equidistant samples used for finding median noise");

   public NoiseVisualizationModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            timeSeriesLength,
            histogramSpan,
            medianSampleCount
      );
   }

   private void updatePlotVisibility() {
      if (computation != null) {
         computation.updatePlotVisibility();
      }
   }

   private synchronized NoiseVisualizationView view() {
      NoiseVisualizationView view = this.view;
      if (view == null) {
         view = GuiUtils.getNowOrWait(() -> new NoiseVisualizationView(this));
         this.view = view;
      }
      return view;
   }

   @Override
   protected void init(KoronaPlaybox koronaPlaybox) {
      view().frame.setVisible(true);
   }

   @Override
   protected void exit() {
      view().frame.dispose();
   }

   @Override
   protected SimplePingModuleComputation createPlayboxComputation(ComputationContext computationContext, PingSource pingSource) throws ModuleConfigurationException {
      computation = new NoiseVisualizationModuleComputation(this, computationContext, pingSource);
      return computation;
   }

   private static final class NoiseVisualizationModuleComputation extends SimplePingModuleComputation {
      private final NoiseVisualizationModule module;
      private final NoiseVisualizationView view;
      private final RawFileConfiguration rawFileConfiguration;
      private final NoiseQuantificationModuleComputation noiseQuantificationComputation;
      private final int transducerCount;
      private final TimeSeries[] neTimeSeries;
      private final TimeSeries[] nhTimeSeries;
      private final TimeSeries[] nMedianTimeSeries;
      private List<TimeSeriesUpdate> pendingTimeSeriesUpdates = new ArrayList<>();
      private volatile boolean updateInProgress;

      private NoiseVisualizationModuleComputation(NoiseVisualizationModule module, ComputationContext computationContext, PingSource pingSource) throws ModuleConfigurationException {
         super(module, computationContext, pingSource);

         this.module = module;
         view = module.view();
         rawFileConfiguration = pingSource.getPingConfiguration().getRawFileConfiguration();
         transducerCount = rawFileConfiguration.getTransducerCount();
         neTimeSeries = new TimeSeries[transducerCount];
         nhTimeSeries = new TimeSeries[transducerCount];
         nMedianTimeSeries = new TimeSeries[transducerCount];

         noiseQuantificationComputation = findNoiseQuantificationModuleComputation(pingSource);

         GuiUtils.invokeNowOrWait(this::configure);
      }

      private NoiseQuantificationModuleComputation findNoiseQuantificationModuleComputation(PingSource pingSource) throws ModuleConfigurationException {
         while (true) {
            switch (pingSource) {
               case NoiseQuantificationModuleComputation noiseQuantificationModuleComputation -> {
                  return noiseQuantificationModuleComputation;
               }
               case BaseModuleComputation moduleComputation -> {
                  pingSource = moduleComputation.getPingSource();
               }
               default -> {
                  throw new ModuleConfigurationException(module, "No NoiseQuantificationModule");
               }
            }
         }
      }

      @Override
      protected void processPing(Ping ping) {
         addTimeSeriesUpdates(pendingTimeSeriesUpdates, ping);

         if (!updateInProgress) {
            doUpdate();
         }
      }

      @Override
      protected void endOfInput() {
         doUpdate();
      }

      private void doUpdate() {
         HistogramPlotData histogramPlotData = createHistogramGraphs();
         List<TimeSeriesUpdate> timeSeriesUpdates = pendingTimeSeriesUpdates;
         pendingTimeSeriesUpdates = new ArrayList<>();
         updateInProgress = true;
         SwingUtilities.invokeLater(() -> {
            updatePlot(histogramPlotData, timeSeriesUpdates);
            updateInProgress = false;
         });
      }

      private List<Graph> createEmptyHistogramGraphs() {
         Ellipse2D.Double dot = new Ellipse2D.Double(-3, -3, 6, 6);
         Ellipse2D.Double empty = new Ellipse2D.Double(0, 0, 0, 0);
         Supplier<XYItemRenderer> markerRenderer = () -> {
            return new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES_AND_LINES, PlotUtils.newStandardXYToolTipGenerator()) {
               @Override
               public Shape getItemShape(int row, int column) {
                  return column == 0 ? empty : dot;
               }
            };
         };
         List<Graph> graphs = new ArrayList<>(transducerCount);
         for (int i = 0; i < transducerCount; i++) {
            int kHz = rawFileConfiguration.getTransducers().get(i).getKHz();
            Color color = FrequencySelectionButton.channelIndexToColor(i);

            graphs.add(new Graph("NE " + kHz + " kHz")
                  .setRenderer(markerRenderer)
                  .setColor(color));

            graphs.add(new Graph("NH " + kHz + " kHz")
                  .setRenderer(markerRenderer)
                  .setColor(color));

            graphs.add(new Graph("NHH " + kHz + " kHz")
                  .setRenderer(markerRenderer)
                  .setColor(color));

            graphs.add(new Graph(kHz + " kHz")
                  .setColor(color));
         }
         return graphs;
      }

      private HistogramPlotData createHistogramGraphs() {
         List<Graph> graphs = createEmptyHistogramGraphs();

         StringBuilder infoText = new StringBuilder("""
               <html>
               <style>
               td, th {
                  white-space: nowrap;
                  margin: 1px 5px;
                  text-align: right;
               }
               tr {
                  border-bottom: 1px solid #d3d3d3;
               }
               </style>
               <table cellpadding=0 cellspacing=0>
               <tr>
                  <th>kHz</th>
                  <th>Samples</th>
               </tr>
               """);

         for (int i = 0; i < transducerCount; i++) {
            Graph neGraph = graphs.get(4 * i);
            Graph nhGraph = graphs.get(4 * i + 1);
            Graph nhhGraph = graphs.get(4 * i + 2);
            Graph graph = graphs.get(4 * i + 3);

            BaseHistogram histogram = noiseQuantificationComputation.getHistogram(i + 1);
            NoiseQuantificationModule.NQP nqp = NoiseQuantificationModule.NQP.create(histogram,
                  noiseQuantificationComputation.getSdevMasking(), noiseQuantificationComputation.getSdev(),
                  noiseQuantificationComputation.getSNMasking(), noiseQuantificationComputation.getSNRatio());
            if (nqp != null) {
               float ne = nqp.getNE();
               neGraph.addPoint(ne, 0);
               neGraph.addPoint(ne, nqp.getProbNE());

               float nh = nqp.getNH();
               nhGraph.addPoint(nh, 0);
               nhGraph.addPoint(nh, nqp.getProbNH());

               float nhh = nqp.getNHH();
               nhhGraph.addPoint(nhh, 0);
               nhhGraph.addPoint(nhh, nqp.getProbNHH());

               float xMax = module.histogramSpan.getFloatValue() * nh;
               float[] x = histogram.getLimits();
               float[] y = histogram.getProbabilities();
               for (int j = 0; j < y.length; j++) {
                  float xMid = (x[j] + x[j + 1]) / 2;
                  if (xMid > xMax) {
                     break;
                  }
                  graph.addPoint(xMid, y[j]);
               }
            }

            infoText.append("<tr style=\"color: " + ColorUtils.colorToHex(FrequencySelectionButton.channelIndexToColor(i)) + ";\">")
                  .append("<td>" + rawFileConfiguration.getTransducers().get(i).getKHz() + "</td>")
                  .append("<td>" + histogram.getSampleCount() + "</td>")
                  .append("</tr>");
         }
         return new HistogramPlotData(graphs, infoText.toString());
      }

      private void addTimeSeriesUpdates(List<TimeSeriesUpdate> updates, Ping ping) {
         Millisecond millisecond = new Millisecond(new Date(ping.getTimeInMillis()));
         for (PingItem pingItem : ping.getPingItems()) {
            switch (pingItem) {
               case Nqp0Datagram nqp -> {
                  updates.add(new TimeSeriesUpdate(millisecond, neTimeSeries[nqp.getChannel() - 1], PowerData.svToLogSv(nqp.getAverage())));
                  updates.add(new TimeSeriesUpdate(millisecond, nhTimeSeries[nqp.getChannel() - 1], PowerData.svToLogSv(nqp.getUpperLimit())));
               }
               case ChannelData channelData -> {
                  PowerData powerData = channelData.getPowerData();
                  int index = channelData.getChannel() - 1;
                  if (index < transducerCount) {
                     float noise = NoiseUtils.medianNoise(powerData, module.medianSampleCount.getIntValue());
                     if (noise > 0) {
                        updates.add(new TimeSeriesUpdate(millisecond, nMedianTimeSeries[index], PowerData.svToLogSv(noise)));
                     }
                  }
               }
               default -> {
               }
            }
         }
      }

      private void updatePlot(HistogramPlotData histogramPlotData, List<TimeSeriesUpdate> timeSeriesUpdates) {
         new Plotter(histogramPlotData.graphs())
               .title("Histogram")
               .xAxis("N")
               .yAxis("Probability density")
               .update(view.histogramChartPanel);

         view.infoText.setText(histogramPlotData.infoText());

         timeSeriesUpdates.forEach(TimeSeriesUpdate::apply);
         Utils.getAllOfType(view.timeSeriesChart.getXYPlot().getDatasets().values(), TimeSeriesCollection.class)
               .flatMap(timeSeriesCollection -> IntStream.range(0, timeSeriesCollection.getSeriesCount()).mapToObj(timeSeriesCollection::getSeries))
               .forEach(TimeSeries::fireSeriesChanged);

         updatePlotVisibility();
      }

      private void updatePlotVisibility() {
         XYPlot timeSeriesPlot = view.timeSeriesChart.getXYPlot();
         XYPlot histogramPlot = view.histogramChartPanel.getChart().getXYPlot();

         for (int i = 0; i < transducerCount; i++) {
            boolean channelVisible = module.frequencySelectionPanel.isChannelSelected(i + 1);

            XYItemRenderer timeSeriesRenderer = timeSeriesPlot.getRenderer(i);
            setSeriesVisible(timeSeriesRenderer, 0, channelVisible && module.showNE);
            setSeriesVisible(timeSeriesRenderer, 1, channelVisible && module.showNH);
            setSeriesVisible(timeSeriesRenderer, 2, channelVisible && module.showNMedian);

            setSeriesVisible(histogramPlot.getRenderer(4 * i), 0, channelVisible);
            setSeriesVisible(histogramPlot.getRenderer(4 * i + 1), 0, channelVisible);
            setSeriesVisible(histogramPlot.getRenderer(4 * i + 2), 0, channelVisible);
            setSeriesVisible(histogramPlot.getRenderer(4 * i + 3), 0, channelVisible);
         }
      }

      private static void setSeriesVisible(XYItemRenderer renderer, int series, boolean visible) {
         if (renderer.isSeriesVisible(series) != visible) {
            renderer.setSeriesVisible(series, visible);
         }
      }

      private void configure() {
         module.frequencySelectionPanel.update(rawFileConfiguration, 0);

         XYPlot timeSeriesPlot = view.timeSeriesChart.getXYPlot();

         for (int i = 0; i < transducerCount; i++) {
            TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection();
            timeSeriesPlot.setDataset(i, timeSeriesCollection);

            XYLineAndShapeRenderer timeSeriesRenderer = new XYLineAndShapeRenderer(true, false);
            timeSeriesRenderer.setDefaultToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());
            timeSeriesPlot.setRenderer(i, timeSeriesRenderer);

            int kHz = rawFileConfiguration.getTransducers().get(i).getKHz();
            Color color = FrequencySelectionButton.channelIndexToColor(i);

            TimeSeries ne = new TimeSeries("NE " + kHz);
            ne.setMaximumItemAge(module.timeSeriesLength.getIntValue() * 1000L);
            timeSeriesCollection.addSeries(ne);
            timeSeriesRenderer.setSeriesPaint(0, color);
            neTimeSeries[i] = ne;

            TimeSeries nh = new TimeSeries("NH " + kHz);
            nh.setMaximumItemAge(module.timeSeriesLength.getIntValue() * 1000L);
            timeSeriesCollection.addSeries(nh);
            timeSeriesRenderer.setSeriesPaint(1, color);
            nhTimeSeries[i] = nh;

            TimeSeries nMedian = new TimeSeries("NMedian " + kHz);
            nMedian.setMaximumItemAge(module.timeSeriesLength.getIntValue() * 1000L);
            timeSeriesCollection.addSeries(nMedian);
            timeSeriesRenderer.setSeriesPaint(2, color);
            nMedianTimeSeries[i] = nMedian;
         }

         int datasetCount = timeSeriesPlot.getDatasetCount();
         for (int i = transducerCount; i < datasetCount; i++) {
            timeSeriesPlot.setDataset(i, null);
         }

         updatePlot(new HistogramPlotData(createEmptyHistogramGraphs(), ""), List.of());

         view.updateFrameTitle();
         view.setVisible(true);
      }

      private record HistogramPlotData(List<Graph> graphs, String infoText) {
      }

      private record TimeSeriesUpdate(Millisecond millisecond, TimeSeries timeSeries, float value) {
         private void apply() {
            int count = timeSeries.getItemCount();
            if (count == 0 || millisecond.compareTo(timeSeries.getTimePeriod(count - 1)) > 0) {
               timeSeries.add(millisecond, value, false);
            } else {
               timeSeries.addOrUpdate(millisecond, value);
            }
         }
      }
   }

   private static final class NoiseVisualizationView {
      private final NoiseVisualizationModule module;
      private final JFrame frame;
      private final JFreeChart timeSeriesChart;
      private final ChartPanel histogramChartPanel;
      private final JTextPane infoText = GuiUtils.readonlyHtmlTextPane("");

      private NoiseVisualizationView(NoiseVisualizationModule module) {
         this.module = module;
         histogramChartPanel = PlotUtils.newChartPanel(null);
         timeSeriesChart = createTimeSeriesChart();

         JPanel timeSeriesPanel = new JPanel(new BorderLayout());
         timeSeriesPanel.add(PlotUtils.newChartPanel(timeSeriesChart));
         timeSeriesPanel.add(createTimeSeriesSelectionPanel(), BorderLayout.SOUTH);

         JPanel histogramPanel = new JPanel(new BorderLayout());
         histogramPanel.add(histogramChartPanel);
         histogramPanel.add(module.frequencySelectionPanel.getComponent(), BorderLayout.SOUTH);

         JPanel numericalInfoPanel = new JPanel(new BorderLayout());
         numericalInfoPanel.setBackground(Color.WHITE);
         numericalInfoPanel.add(new JLabel("<html><h2>Histogram info<h2>", JLabel.CENTER), BorderLayout.NORTH);
         numericalInfoPanel.add(new JScrollPane(infoText));

         MultiSplitPane multiSplitPane = new MultiSplitPane(JSplitPane.VERTICAL_SPLIT);
         multiSplitPane.add(timeSeriesPanel);
         multiSplitPane.add(histogramPanel);
         multiSplitPane.add(numericalInfoPanel);

         frame = new JFrame();
         frame.getContentPane().removeAll();
         frame.getContentPane().add(multiSplitPane.getPanel());
         frame.setSize(600, 800);

         module.active.subscribe(GuiListeners.coalescingLater(this::setVisible));
      }

      private void updateFrameTitle() {
         String title = "Noise visualization";
         String comment = module.comment.getValue();
         if (!comment.isEmpty()) {
            title = title + " - " + comment;
         }
         frame.setTitle(title);
      }

      private void setVisible(boolean visible) {
         if (module.getKoronaPlaybox() != null) {
            frame.setVisible(visible);
         }
      }

      private static JFreeChart createTimeSeriesChart() {
         DateAxis xAxis = new DateAxis("Time");
         xAxis.setLowerMargin(0);
         xAxis.setUpperMargin(0);

         NumberAxis yAxis = PlotUtils.newNumberAxis("N [dB]");

         XYPlot plot = new XYPlot(null, xAxis, yAxis, null);
         plot.setDomainPannable(true);
         plot.setRangePannable(true);

         return PlotUtils.newChart("Time series", plot, false);
      }

      private JPanel createTimeSeriesSelectionPanel() {
         JCheckBox ne = new JCheckBox("NE", module.showNE);
         ne.setBackground(Color.WHITE);
         ne.addItemListener(_ -> {
            module.showNE = ne.isSelected();
            module.updatePlotVisibility();
         });

         JCheckBox nh = new JCheckBox("NH", module.showNH);
         nh.setBackground(Color.WHITE);
         nh.addItemListener(_ -> {
            module.showNH = nh.isSelected();
            module.updatePlotVisibility();
         });

         JCheckBox nMedian = new JCheckBox("NMedian", module.showNMedian);
         nMedian.setBackground(Color.WHITE);
         nMedian.addItemListener(_ -> {
            module.showNMedian = nMedian.isSelected();
            module.updatePlotVisibility();
         });

         JPanel checkBoxPanel = new JPanel();
         checkBoxPanel.setBackground(Color.WHITE);
         checkBoxPanel.add(ne);
         checkBoxPanel.add(nh);
         checkBoxPanel.add(nMedian);
         return checkBoxPanel;
      }
   }
}
