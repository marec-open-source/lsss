package no.imr.lsss.modules.ts;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.region.Region;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.SeparatorParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.PlotChartPanel;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.Plotter;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.Range;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Displays target positions detected by {@link BaseTsModule}.
 */
public final class TsPositionsModule extends BaseViewModule implements PojoDataContainer {
   private final BooleanParameter cross = new BooleanParameter(
         new Name("Cross"),
         true);

   private final BooleanParameter angularLines = new BooleanParameter(
         new Name("AngularLines", "Angular lines"),
         true);

   private final BooleanParameter angularText = new BooleanParameter(
         new Name("AngularText", "Angular text"),
         true);

   private final BooleanParameter cartesianAxesLabels = new BooleanParameter(
         new Name("CartesianAxesLabels", "Cartesian axes labels"),
         true);

   private final BooleanParameter cartesianGridLines = new BooleanParameter(
         new Name("CartesianGridLines", "Cartesian grid lines"),
         false);

   private final BooleanParameter cartesianGridText = new BooleanParameter(
         new Name("CartesianGridText", "Cartesian grid text"),
         false);

   private final SeparatorParameter separator = SeparatorParameter.line();

   private final BooleanParameter mousePosition = new BooleanParameter(
         new Name("MousePosition", "Mouse position"),
         false,
         "Show angles at mouse position");

   private List<BaseTsModule> tsModules = List.of();

   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));

   private final Listener plotListener = newCoalescingExecListener(this::plot);

   private double maxRange = 1;
   private double aspectRatio = 1;
   private Graph targetsGraph = new Graph();
   private Graph tracksGraph = new Graph();
   private Graph mouseGraph = new Graph();
   private Graph highlightedGraph = new Graph();
   private JFreeChart chart = PlotUtils.newEmptyChart();

   public TsPositionsModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            cross,
            angularLines,
            angularText,
            cartesianAxesLabels,
            cartesianGridLines,
            cartesianGridText,
            separator,
            mousePosition
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      tsModules = getModuleManager().getAll(BaseTsModule.class).toList();

      registry.add(getParameters(), plotListener);

      Listener recomputeListener = newCoalescingExecListener(this::recompute);
      registry.add(tsModules.stream().map(BaseTsModule::getTSDetectionChangeManager), recomputeListener);
      registry.add(getInterpretationSettings().getChannelChangeManager(), recomputeListener);
      registry.add(getInterpretationSettings().mouseover().echogramPoint(), newCoalescingExecListener(this::computeHighlightedTargetGraph));

      //---

      recompute();
   }

   @Override
   protected void onDisable() {
      targetsGraph = new Graph();
      tracksGraph = new Graph();
      mouseGraph = new Graph();
      highlightedGraph = new Graph();
      setChart(PlotUtils.newEmptyChart());
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   private void recompute() {
      computeTargetsGraph();
      computeTracksGraph();

      double maxRangeSq = Stream.concat(targetsGraph.getPoints().stream(), tracksGraph.getPoints().stream())
            .filter(p -> !Float.isNaN(p.x()))
            .mapToDouble(p -> p.x() * p.x() + p.y() * p.y())
            .max()
            .orElse(49);
      maxRange = Math.max(1, Math.ceil(Math.sqrt(maxRangeSq)));

      computeHighlightedTargetGraph();
   }

   private void computeTargetsGraph() {
      int channel = getInterpretationSettings().getChannel();

      Graph graph = new Graph("Targets")
            .setXYInfo(createXYInfo())
            .setColor(Color.BLUE)
            .setRenderer(() -> {
               StandardXYItemRenderer renderer = PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
               renderer.setSeriesShape(0, new Ellipse2D.Double(-2, -2, 4, 4));
               return renderer;
            });
      tsModules.forEach(tsModule -> {
         for (Region region : getRegionManager().getSelectedRegions()) {
            tsModule.getTSData(region).forEach((pingIndex, pingCache) -> {
               PowerData powerData = getPowerData(pingIndex, channel);
               List<? extends BaseTsData> targets = pingCache.getTsData(channel);
               addAnglesToGraph(graph, powerData, targets);
            });
         }
      });
      targetsGraph = graph;
   }

   private void computeTracksGraph() {
      int channel = getInterpretationSettings().getChannel();

      Graph graph = new Graph("Tracks")
            .setXYInfo(createXYInfo())
            .setColor(Color.RED)
            .setRenderer(() -> {
               StandardXYItemRenderer renderer = PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.SHAPES_AND_LINES);
               renderer.setSeriesShape(0, new Ellipse2D.Double(-2, -2, 4, 4));
               return renderer;
            });
      tsModules.stream()
            .flatMap(BaseTsModule::getSelectedTracks)
            .forEach(track -> {
               graph.addSeparator();
               track.forEach((pingIndex, target) -> {
                  PowerData powerData = getPowerData(pingIndex, channel);
                  addAnglesToGraph(graph, powerData, List.of(target));
               });
            });
      tracksGraph = graph;
   }

   private static XYInfo createXYInfo() {
      ExportTransform transform = ExportTransform.round(100);
      return new XYInfo(
            new ParameterExport("athwartshipAngle", Unit.DEGREES, transform),
            new ParameterExport("alongshipAngle", Unit.DEGREES, transform));
   }

   private void computeHighlightedTargetGraph() {
      Graph newHighlightedGraph = new Graph("Highlighted")
            .setXYInfo(createXYInfo());
      Graph newMouseGraph = new Graph("Mouse")
            .setXYInfo(createXYInfo());
      EchogramPoint echogramPoint = getInterpretationSettings().mouseover().getEchogramPoint();
      if (echogramPoint != null) {
         newHighlightedGraph.setColor(Color.RED);
         newHighlightedGraph.setRenderer(() -> {
            StandardXYItemRenderer renderer = PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
            renderer.setSeriesShape(0, new Ellipse2D.Double(-4, -4, 8, 8));
            return renderer;
         });
         int channel = getInterpretationSettings().getChannel();
         tsModules.forEach(tsModule -> {
            tsModule.getTargetsForPoint(getRegionManager().getSelectedRegions(), echogramPoint, channel).forEach((pingIndex, targets) -> {
               if (targets.isEmpty()) {
                  return;
               }
               PowerData powerData = getPowerData(pingIndex, channel);
               addAnglesToGraph(newHighlightedGraph, powerData, targets);
            });
         });
         if (mousePosition.getBooleanValue()) {
            newMouseGraph.setColor(Color.GRAY);
            newMouseGraph.setRenderer(() -> {
               StandardXYItemRenderer renderer = PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
               renderer.setSeriesShape(0, new Ellipse2D.Double(-4, -4, 8, 8));
               return renderer;
            });
            PowerData powerData = getPowerData(echogramPoint.pingIndex(), channel);
            if (powerData != null && powerData.getAngleData() != null) {
               addAnglesToGraph(newMouseGraph, powerData, echogramPoint.depth());
            }
         }
      }
      highlightedGraph = newHighlightedGraph;
      mouseGraph = newMouseGraph;
      plotListener.listen();
   }

   private static void addAnglesToGraph(Graph graph, @Nullable PowerData powerData, Collection<? extends BaseTsData> targets) {
      if (powerData == null || powerData.getAngleData() == null) {
         return;
      }
      targets.forEach(target -> {
         addAnglesToGraph(graph, powerData, target.depth());
      });
   }

   private static void addAnglesToGraph(Graph graph, PowerData powerData, float depth) {
      int i = powerData.depthToSampleIndex(depth);
      if (i < 0 || i >= powerData.getCount()) {
         return;
      }
      float alongAngle = powerData.getMechanicalAlongAngle(i);
      float athwartAngle = powerData.getMechanicalAthwartAngle(i);
      graph.addPoint(athwartAngle, alongAngle);
   }

   private @Nullable PowerData getPowerData(PingIndex pingIndex, int channel) {
      Ping ping = getInterpretationSettings().getDataFileSet().getPing(pingIndex);
      return ping.getPowerData(channel);
   }

   private void plot() {
      JFreeChart chart = new Plotter(List.of(highlightedGraph, mouseGraph, tracksGraph, targetsGraph))
            .xAxis("Athwart [deg]")
            .yAxis("Along [deg]")
            .createChart();
      XYPlot plot = chart.getXYPlot();
      if (!cartesianAxesLabels.getBooleanValue()) {
         plot.getDomainAxis().setLabel(null);
         plot.getRangeAxis().setLabel(null);
      }
      if (!cartesianGridLines.getBooleanValue()) {
         plot.setDomainGridlinesVisible(false);
         plot.setRangeGridlinesVisible(false);
      }
      if (!cartesianGridText.getBooleanValue()) {
         plot.getDomainAxis().setTickLabelsVisible(false);
         plot.getDomainAxis().setTickMarksVisible(false);
         plot.getRangeAxis().setTickLabelsVisible(false);
         plot.getRangeAxis().setTickMarksVisible(false);
      }
      if (!cartesianAxesLabels.getBooleanValue() && !cartesianGridLines.getBooleanValue() && !cartesianGridText.getBooleanValue()) {
         plot.getDomainAxis().setVisible(false);
         plot.getRangeAxis().setVisible(false);
         plot.setOutlineVisible(false);
      }

      updateAxesRanges(plot);
      ((NumberAxis) plot.getDomainAxis()).setTickUnit(new NumberTickUnit(1));
      ((NumberAxis) plot.getRangeAxis()).setTickUnit(new NumberTickUnit(1));

      if (cross.getBooleanValue()) {
         plot.getRenderer().addAnnotation(new XYLineAnnotation(-maxRange, 0, maxRange, 0, GuiUtils.STROKE_1, Color.LIGHT_GRAY), Layer.BACKGROUND);
         plot.getRenderer().addAnnotation(new XYLineAnnotation(0, -maxRange, 0, maxRange, GuiUtils.STROKE_1, Color.LIGHT_GRAY), Layer.BACKGROUND);
      }
      for (int i = 1; i <= maxRange; i++) {
         if (angularLines.getBooleanValue()) {
            plot.getRenderer().addAnnotation(new XYShapeAnnotation(new Ellipse2D.Double(-i, -i, 2 * i, 2 * i), GuiUtils.STROKE_1, Color.LIGHT_GRAY), Layer.BACKGROUND);
         }
         if (angularText.getBooleanValue()) {
            XYTextAnnotation textAnnotation = new XYTextAnnotation(Integer.toString(i), i, 0);
            if (cross.getBooleanValue()) {
               textAnnotation.setTextAnchor(TextAnchor.TOP_CENTER);
            }
            plot.getRenderer().addAnnotation(textAnnotation, Layer.BACKGROUND);
         }
      }

      setChart(chart);
   }

   private void updateAxesRanges(XYPlot plot) {
      plot.getDomainAxis().setRange(symmetricRange(maxRange * Math.max(1, aspectRatio)));
      plot.getRangeAxis().setRange(symmetricRange(maxRange * Math.max(1, 1 / aspectRatio)));
   }

   private static Range symmetricRange(double radius) {
      return new Range(-radius, radius);
   }

   private void setChart(JFreeChart chart) {
      this.chart = chart;
      viewHolder.ifViewDelayed(this, View::updateChart);
   }

   @Override
   public PojoData getPojoData() {
      return PojoData.newBuilder(getPersistentName())
            .withDatasets(chart.getXYPlot())
            .build();
   }

   private static final class View extends BaseView {
      private final TsPositionsModule module;
      private final ChartPanel chartPanel;

      private View(TsPositionsModule module) {
         super(module);

         this.module = module;
         chartPanel = new PlotChartPanel(module.chart) {
            @Override
            public void paintComponent(Graphics g) {
               super.paintComponent(g);
               updateAspectRatio();
            }
         };
      }

      @Override
      public JComponent getComponent() {
         return chartPanel;
      }

      private void updateChart() {
         chartPanel.setChart(module.chart);
      }

      private void updateAspectRatio() {
         Rectangle2D area = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
         if (area == null) {
            return;
         }
         double aspectRatio = area.getWidth() / area.getHeight();
         if (module.aspectRatio != aspectRatio) {
            module.aspectRatio = aspectRatio;
            XYPlot xyPlot = chartPanel.getChart().getXYPlot();
            if (xyPlot.getDomainAxis() != null) {
               module.updateAxesRanges(xyPlot);
            }
         }
      }
   }
}
