package no.imr.lsss.modules.misc;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.geometry.depth.PerPingDepthTransform;
import no.imr.korona.region.Region;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.korona.viewer.variables.BaseVariable;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.korona.viewer.variables.raw.SvVariable;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.InterpretationZSettings;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.modules.thresholdresponse.ThresholdResponseModule;
import no.imr.tools.Utils;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.Plotter;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeSet;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class PingPlotModule extends BaseViewModule implements PojoDataContainer {
   private final IntParameter pingRadius = new IntParameter(
         new Name("PingRadius", "Ping radius"),
         0, Unit.COUNT, ValueConstraints.gte(0),
         "Number of pings on either side to use");

   private final BooleanParameter useThresholdRange = new BooleanParameter(
         new Name("UseThresholdRange", "Use threshold range"),
         false,
         "Use value range between lower and upper threshold");

   private final BooleanParameter onlySelectedRegions = new BooleanParameter(
         new Name("OnlySelectedRegions", "Only selected regions"),
         true);

   private final BooleanParameter sameVariableAsEchogram = new BooleanParameter(
         new Name("SameVariableAsEchogram", "Same variable as echogram"),
         true);

   private final ObjectParameter<ContinuousVariable> variable = new ObjectParameter<>(
         new Name("Variable"),
         new SvVariable());

   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));

   private final Listener recomputeListener = newCoalescingExecListener(this::recompute);
   private ValueMarker zMarker = new ValueMarker(Double.NaN);
   private InterpretationZSettings zSettings;
   private JFreeChart chart = PlotUtils.newEmptyChart();

   public PingPlotModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      sameVariableAsEchogram.addListenerAndNotify(same -> variable.setEnabled(!same));
      zSettings = getInterpretationSettings().getPelagicZSettings();

      updateAvailableVariables();
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            pingRadius,
            useThresholdRange,
            onlySelectedRegions,
            sameVariableAsEchogram,
            variable
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
      registry.add(getInterpretationSettings().getColorConverterContainer().getChangeManager(),
            this::updateAvailableVariables);
      registry.add(recomputeListener, List.of(
            getInterpretationSettings().mouseover().pingIndex(),
            getInterpretationSettings().mouseover().depth(),
            getInterpretationSettings().getChannelChangeManager(),
            getInterpretationSettings().getColorConverterContainer().getChangeManager(),
            getRegionManager().selectedRegions(),
            getRegionManager().getRegionDefinitionChangeManager()
      ));
      registry.add(getParameters(), recomputeListener);
      getModuleManager().getModules(EchogramModule.class).forEach(echogramModule -> {
         registry.add(echogramModule.getZSettings().getZoomedChangeManager(), recomputeListener);
         registry.add(echogramModule.mousePosition(), newCoalescingExecListener(mousePosition -> {
            InterpretationZSettings newZSettings = !echogramModule.isPelagic() && mousePosition.isPresent()
                  ? getInterpretationSettings().getBottomZSettings()
                  : getInterpretationSettings().getPelagicZSettings();
            if (zSettings != newZSettings) {
               zSettings = newZSettings;
               recomputeListener.listen();
            }
         }));
      });

      //---

      updateAvailableVariables();
      recomputeListener.listen();
   }

   @Override
   protected void onDisable() {
      plot(List.of());
   }

   private void updateAvailableVariables() {
      ColorConverterContainer colorConverterContainer = getInterpretationSettings().getColorConverterContainer();
      List<ContinuousVariable> variables = colorConverterContainer.getContinuousVariables().stream()
            .filter(BaseVariable::isUsableInContext)
            .toList();
      variable.setAllowedValuesAndPossiblyValue(variables, colorConverterContainer.getSV());
      recomputeListener.listen();
   }

   private void recompute() {
      PingIndex pingIndex = getInterpretationSettings().mouseover().getPingIndex();
      if (pingIndex != null) {
         plot(createGraphs(pingIndex));
         Float depth = getInterpretationSettings().mouseover().getDepth();
         zMarker.setValue(depth != null ? zSettings.depthToZ(depth, pingIndex) : Double.NaN);
      } else {
         plot(List.of());
         zMarker.setValue(Double.NaN);
      }
   }

   private ContinuousVariable getVariable() {
      if (sameVariableAsEchogram.getBooleanValue()) {
         ColorConverterContainer colorConverterContainer = getInterpretationSettings().getColorConverterContainer();
         ContinuousVariable continuousVariable = colorConverterContainer.getColorConverter().getContinuousVariable();
         return continuousVariable != null ? continuousVariable : colorConverterContainer.getSV();
      } else {
         return variable.getValue();
      }
   }

   private void plot(List<Graph> graphs) {
      ContinuousVariable variable = getVariable();
      FloatRange valueRange = useThresholdRange.getBooleanValue() ? variable.getSettings().getRange() : variable.getSettings().getMaxRange();
      FloatRange zRange = zSettings.getZoomedZRange();
      if (!useThresholdRange.getBooleanValue()) {
         graphs = new ArrayList<>(graphs);
         graphs.addAll(ThresholdResponseModule.createThresholdsGraph(variable, zRange));
      }
      JFreeChart chart = new Plotter(graphs)
            .xAxis(Utils.nameAndUnit(variable.getDisplayName(), variable.getUnit()))
            .xRange(valueRange)
            .yAxis(zSettings.isPelagic() ? "Depth [m]" : "z [m]")
            .yRange(zRange)
            .createChart();
      // Create new marker to avoid memory leak, #1423.
      zMarker = new ValueMarker(Double.NaN, Color.DARK_GRAY, GuiUtils.STROKE_1);
      chart.getXYPlot().addRangeMarker(zMarker);
      chart.getXYPlot().getRangeAxis().setInverted(true);
      this.chart = chart;
      viewHolder.ifViewDelayed(this, View::updateChart);
   }

   private List<Graph> createGraphs(PingIndex pingIndex) {
      int n = pingRadius.getIntValue();
      List<Graph> graphs = new ArrayList<>(2 * n + 1);

      // add current graph on top
      Graph graphCurrent = createGraph(pingIndex);
      if (graphCurrent == null) {
         return List.of();
      }
      graphCurrent.setLineWidth(2);
      graphCurrent.setColor(Color.RED);
      graphs.add(graphCurrent);

      for (int i = 1; i <= n; i++) {
         PingIndex pingIndexPast = getInterpretationSettings().getDataFileSet().getPingIndexOrNull(pingIndex.getPingNumber() - i);
         Graph graphPast = createGraph(pingIndexPast);
         if (graphPast != null) {
            graphPast.setColor(getColor(-i, n));
            graphs.add(graphPast);
         }

         PingIndex pingIndexFuture = getInterpretationSettings().getDataFileSet().getPingIndexOrNull(pingIndex.getPingNumber() + i);
         Graph graphFuture = createGraph(pingIndexFuture);
         if (graphFuture != null) {
            graphFuture.setColor(getColor(i, n));
            graphs.add(graphFuture);
         }
      }

      return graphs;
   }

   private @Nullable Graph createGraph(@Nullable PingIndex pingIndex) {
      if (pingIndex == null || !getInterpretationSettings().getPingRange().contains(pingIndex)) {
         return null;
      }

      Ping ping = getInterpretationSettings().getDataFileSet().getPing(pingIndex);

      ContinuousVariable variable = getVariable();
      ContinuousVariableResult result = variable.evaluate(getInterpretationSettings().getChannel(), ping);

      PerPingDepthTransform perPingDepthTransform = zSettings.getDepthTransform().forPing(pingIndex);
      FloatRange zoomedDepthRange = perPingDepthTransform.zToDepth(zSettings.getZoomedZRange());
      int minBegin = Math.clamp(result.depthToIndex(zoomedDepthRange.min()), 0, result.floatData().length);
      int maxEnd = Math.clamp(result.depthToIndex(zoomedDepthRange.max()), 0, result.floatData().length);
      RangeSet<Integer> indexes = new ArrayRangeSet<>();
      if (onlySelectedRegions.getBooleanValue()) {
         for (Region region : getRegionManager().getSelectedRegions()) {
            for (FloatRange depthRange : getRegionManager().getDepthRangesForChannel(region, ping, getInterpretationSettings().getChannel())) {
               int iBegin = Math.clamp(result.depthToIndex(depthRange.min()), minBegin, maxEnd);
               int iEnd = Math.clamp(result.depthToIndex(depthRange.max()), minBegin, maxEnd);
               indexes.add(iBegin, iEnd);
            }
         }
      } else {
         indexes.add(minBegin, maxEnd);
      }

      Graph graph = new Graph(variable.getDisplayName())
            .setXYInfo(new XYInfo(
                  new ParameterExport(variable.getName().persistentName(), variable.getUnit(), variable.getExportTransform()),
                  new ParameterExport(zSettings.isPelagic() ? "depth" : "z", Unit.METER, ExportRounding.depth())));
      float[] values = result.floatData();
      for (Range<Integer> indexRange : indexes) {
         graph.addSeparator();
         for (int i = indexRange.begin(); i < indexRange.end(); i++) {
            graph.addPoint(values[i], perPingDepthTransform.depthToZ(result.indexToDepth(i)));
         }
      }
      return graph;
   }

   public static Color getColor(int i, int n) {
      if (i < 0) {
         return Color.getHSBColor(0.69f, (2 * n + i) / (float) (2 * n), 1 - (n + i) / (float) (2 * n));
      }
      if (i > 0) {
         return Color.getHSBColor(0.31f, (2 * n - i) / (float) (2 * n), 1 - (n - i) / (float) (2 * n));
      }
      return Color.RED;
   }

   @Override
   public PojoData getPojoData() {
      return PojoData.newBuilder(getPersistentName())
            .withDatasets(chart.getXYPlot())
            .build();
   }

   private static final class View extends BaseView {
      private final PingPlotModule module;
      private final ChartPanel chartPanel;

      private View(PingPlotModule module) {
         super(module);

         this.module = module;
         chartPanel = PlotUtils.newChartPanel(module.chart);
      }

      @Override
      public JComponent getComponent() {
         return chartPanel;
      }

      private void updateChart() {
         chartPanel.setChart(module.chart);
      }
   }
}
