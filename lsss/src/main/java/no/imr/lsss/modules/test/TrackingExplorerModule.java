package no.imr.lsss.modules.test;

import no.imr.korona.computation.tracking.PositionFunction;
import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.data.StateVector;
import no.imr.korona.computation.tracking.impl.MovingPositionFunction;
import no.imr.korona.computation.tracking.impl.StationaryPositionFunction;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.region.Region;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.Plotter;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeBuilder;
import no.imr.tools.swing.ViewHolder;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class TrackingExplorerModule extends BaseViewModule {
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private @Nullable Measurement mouseMeasurement;
   private DisplayData displayData = createEmptyDisplayData();

   public TrackingExplorerModule(ModuleInfo<TestPlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      Listener recomputeListener = newCoalescingExecListener(this::recompute);
      registry.add(recomputeListener, List.of(
            getRegionManager().getThresholdManager().getChangeManager(),
            getRegionManager().selectedRegions(),
            getRegionManager().getRegionDefinitionChangeManager(),
            getInterpretationSettings().getChannelChangeManager()
      ));

      Consumer<Optional<EchogramPoint>> echogramPointListener = newCoalescingExecListener(echogramPoint -> {
         mouseMeasurement = toMeasurement(echogramPoint.orElse(null));
         recomputeListener.listen();
      });
      registry.add(getInterpretationSettings().mouseover().echogramPoint(), echogramPointListener);
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   private void recompute() {
      displayData = createDisplayData();
      viewHolder.ifViewDelayed(this, view -> {
         view.panel.removeAll();
         displayData.addToPanel(view.panel, mouseMeasurement);
         view.panel.validate();
         view.panel.repaint();
      });
   }

   private static DisplayData createEmptyDisplayData() {
      return new DisplayData(new Graph(), new Graph(), new Graph(),
            new StationaryPositionFunction(), new StationaryPositionFunction(),
            FloatRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE);
   }

   private DisplayData createDisplayData() {
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      PositionFunction pingPositionFunction = new MovingPositionFunction(dataFileSet);
      PositionFunction referencePositionFunction = null;

      Graph alongRangeGraph = createGraph(Color.BLACK);
      Graph athwartRangeGraph = createGraph(Color.BLACK);
      Graph alongAthwartGraph = createGraph(Color.BLACK);
      FloatRangeBuilder alongRange = new FloatRangeBuilder();
      FloatRangeBuilder athwartRange = new FloatRangeBuilder();
      FloatRangeBuilder rangeRange = new FloatRangeBuilder();

      int n = 0;

      for (Region region : getRegionManager().getSelectedRegions()) {
         PingRange pingRange = region.getPingRange();
         pingRange = pingRange.intersection(getInterpretationSettings().getPingRange());
         for (PingIndex pingIndex : dataFileSet.getPingIndices(pingRange)) {
            alongRangeGraph.addSeparator();
            athwartRangeGraph.addSeparator();
            alongAthwartGraph.addSeparator();

            FloatRange logSvRange = getRegionManager().getThresholdManager().getLogSvRange(pingIndex);
            Ping ping = dataFileSet.getPing(pingIndex);
            pingPositionFunction.update(ping);
            if (referencePositionFunction == null) {
               referencePositionFunction = new MovingPositionFunction(dataFileSet);
               referencePositionFunction.update(ping);
            }
            PowerData powerData = ping.getPowerData(getInterpretationSettings().getChannel());
            if (powerData == null || powerData.getAngleData() == null) {
               continue;
            }
            for (FloatRange depthRange : getRegionManager().getDepthRangesForChannel(region, ping, getInterpretationSettings().getChannel())) {
               int begin = powerData.depthToClampedSampleIndex(depthRange.min());
               int end = powerData.depthToClampedSampleIndex(depthRange.max());
               for (int i = begin; i < end; i++) {
                  if (!logSvRange.contains(powerData.getLogSv()[i])) {
                     continue;
                  }
                  if (++n > 1000) {
                     return createEmptyDisplayData();
                  }

                  Vec3 p = pingPositionFunction.toGlobalPosition(new Measurement(powerData, i, 0));
                  Measurement m = referencePositionFunction.toMeasurement(new StateVector(p, Vec3.ZERO, 0));

                  alongRangeGraph.addPoint(Math.toDegrees(m.alongshipAngle()), m.range());
                  athwartRangeGraph.addPoint(Math.toDegrees(m.athwartshipAngle()), m.range());
                  alongAthwartGraph.addPoint(Math.toDegrees(m.alongshipAngle()), Math.toDegrees(m.athwartshipAngle()));

                  rangeRange.expand(m.range());
                  alongRange.expand(m.alongshipAngle());
                  athwartRange.expand(m.athwartshipAngle());
               }
            }
         }
      }

      if (referencePositionFunction == null) {
         return createEmptyDisplayData();
      }
      return new DisplayData(alongRangeGraph, athwartRangeGraph, alongAthwartGraph,
            pingPositionFunction, referencePositionFunction,
            rangeRange.toFloatRange(), alongRange.toFloatRange(), athwartRange.toFloatRange());
   }

   private static Graph createGraph(Color color) {
      return new Graph()
            .setColor(color);
   }

   private @Nullable Measurement toMeasurement(@Nullable EchogramPoint echogramPoint) {
      if (echogramPoint == null) {
         return null;
      }
      Ping ping = getInterpretationSettings().getDataFileSet().getPing(echogramPoint.pingIndex());
      PowerData powerData = ping.getPowerData(getInterpretationSettings().getChannel());
      if (powerData == null || powerData.getAngleData() == null) {
         return null;
      }
      PositionFunction positionFunction = displayData.positionFunction;
      positionFunction.update(ping);
      int i = powerData.depthToSampleIndex(echogramPoint.depth());
      if (i < 0 || i >= powerData.getCount()) {
         return null;
      }
      Vec3 p = positionFunction.toGlobalPosition(new Measurement(powerData, i, 0));
      return displayData.referencePositionFunction.toMeasurement(new StateVector(p, Vec3.ZERO, 0));
   }

   private record DisplayData(
         Graph alongRangeGraph,
         Graph athwartRangeGraph,
         Graph alongAthwartGraph,
         PositionFunction positionFunction,
         PositionFunction referencePositionFunction,
         FloatRange rangeRange,
         FloatRange alongRange,
         FloatRange athwartRange
   ) {
      private void addToPanel(JPanel panel, @Nullable Measurement mouseMeasurement) {
         List<Graph> alongRangeGraphs = new ArrayList<>();
         List<Graph> athwartRangeGraphs = new ArrayList<>();
         List<Graph> alongAthwartGraphs = new ArrayList<>();

         if (mouseMeasurement != null
               && rangeRange.contains(mouseMeasurement.range())
               && alongRange.contains(mouseMeasurement.alongshipAngle())
               && athwartRange.contains(mouseMeasurement.athwartshipAngle())) {
            addMouseGraph(alongRangeGraphs, Math.toDegrees(mouseMeasurement.alongshipAngle()), mouseMeasurement.range());
            addMouseGraph(athwartRangeGraphs, Math.toDegrees(mouseMeasurement.athwartshipAngle()), mouseMeasurement.range());
            addMouseGraph(alongAthwartGraphs, Math.toDegrees(mouseMeasurement.alongshipAngle()), Math.toDegrees(mouseMeasurement.athwartshipAngle()));
         }
         alongRangeGraphs.add(alongRangeGraph);
         athwartRangeGraphs.add(athwartRangeGraph);
         alongAthwartGraphs.add(alongAthwartGraph);

         plot(panel, "Along", "Range", alongRangeGraphs);
         plot(panel, "Athwart", "Range", athwartRangeGraphs);
         plot(panel, "Along", "Athwart", alongAthwartGraphs);
      }

      private static void addMouseGraph(List<Graph> graphs, double x, double y) {
         Graph mouseGraph = createGraph(Color.RED);
         mouseGraph.addPoint(x, y);
         graphs.add(mouseGraph);
      }

      private static void plot(JPanel panel, String xLabel, String yLabel, List<Graph> graphs) {
         for (Graph graph : graphs) {
            graph.setRenderer(() -> PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.SHAPES_AND_LINES));
         }
         ChartPanel chartPanel = new Plotter(graphs)
               .xAxis(xLabel)
               .yAxis(yLabel)
               .createChartPanel();
         panel.add(chartPanel);
      }
   }

   private static final class View extends BaseView {
      private final JPanel panel = new JPanel(new GridLayout(1, 0));

      private View(TrackingExplorerModule module) {
         super(module);
      }

      @Override
      public JComponent getComponent() {
         return panel;
      }
   }
}
