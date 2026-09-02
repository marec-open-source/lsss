package no.imr.lsss.modules.echogramplot;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.incubator.KoronaIncubatorFeatureToggles;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.survey.data.DataSetManager;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.echogramplot.functions.AdcpFunction;
import no.imr.lsss.modules.echogramplot.functions.BooleanFunction;
import no.imr.lsss.modules.echogramplot.functions.ChannelDataParameterFunction;
import no.imr.lsss.modules.echogramplot.functions.ChannelDataSampleFunction;
import no.imr.lsss.modules.echogramplot.functions.ComplexChannelDataParameterFunction;
import no.imr.lsss.modules.echogramplot.functions.ExperimentationFunction;
import no.imr.lsss.modules.echogramplot.functions.InterpretationFunction;
import no.imr.lsss.modules.echogramplot.functions.NmeaFunction;
import no.imr.lsss.modules.echogramplot.functions.NoiseXmlFunction;
import no.imr.lsss.modules.echogramplot.functions.Nqp0DatagramFunction;
import no.imr.lsss.modules.echogramplot.functions.PingFunction;
import no.imr.lsss.modules.echogramplot.functions.PlotParameterPingFunction;
import no.imr.lsss.modules.echogramplot.functions.RelativeFrequencyResponseFunction;
import no.imr.lsss.modules.echogramplot.functions.RollTransmitToReceive;
import no.imr.lsss.modules.echogramplot.functions.SaFunction;
import no.imr.lsss.modules.echogramplot.functions.SimplePingFunction;
import no.imr.lsss.modules.echogramplot.functions.TsRelativeMainFrequencyFunction;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.modules.pojodata.PojoDataUtils;
import no.imr.tools.Utils;
import no.imr.tools.listening.ListenableProperty;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.ArrayKernel;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.DynamicListParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.SeparatorParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueConverters;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.UiUtils;
import no.imr.tools.swing.ViewHolder;
import no.marec.lsss.api.util.observing.Subscription;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Displays functions defined along the echogram.
 */
public final class EchogramPlotModule extends BaseViewModule implements PojoDataContainer {
   public final BooleanParameter plotAllChannels = new BooleanParameter(
         new Name("PlotAllChannels", "Plot all channels"),
         false,
         "If unchecked, only the current channel is plotted for channel-dependent functions, "
               + "such as sample interval, transmit power, etc.");

   public final BooleanParameter autoAdjustYAxis = new BooleanParameter(
         new Name("AutoAdjustYAxis", "Auto-adjust y-axis"),
         true,
         "Automatically adjust the y-axis when the plot is updated");

   public final OptionalFloatParameter gaussianSmoothing = new OptionalFloatParameter(
         new Name("GaussianSmoothing", "Gaussian smoothing"),
         Optional.empty(), Unit.NONE, ValueConstraints.gte(0f),
         "Gaussian kernel standard deviation measured in plot points");

   private final ViewHolder<EchogramPlotView> viewHolder = new ViewHolder<>(() -> new EchogramPlotView(this));

   private ValueMarker echogramPositionMarker = new ValueMarker(Double.NaN);

   private final Listener recomputeListener = newCoalescingExecListener(this::recompute);
   private final Listener updateSelectionListener = newCoalescingExecListener(this::updateSelectedPingFunctions);

   private final PlotParameterExtractor plotParameterExtractor = new PlotParameterExtractor();

   private final DynamicListParameter<String> nmea = new DynamicListParameter<>(
         new Name("NMEA"),
         List.of(), Unit.NONE, NmeaFunction.NmeaParameter.CONSTRAINT, ValueConverters.STRING) {
      @Override
      public ValueParameter<Optional<String>> newOptionalParameter(int index, String persistentName) {
         NmeaFunction.NmeaParameter parameter = new NmeaFunction.NmeaParameter(getInterpretationSettings(), persistentName);
         parameter.setProperty(KEY_COMBINE_INPUT_AND_DESCRIPTION, true);
         return parameter;
      }
   };
   private List<NmeaFunction> nmeaPingFunctions = List.of();

   private final DynamicListParameter<String> adcp = new DynamicListParameter<>(
         new Name("ADCP"),
         List.of(), Unit.NONE, ValueConverters.STRING) {
      @Override
      public ValueParameter<Optional<String>> newOptionalParameter(int index, String persistentName) {
         AdcpFunction.AdcpParameter parameter = new AdcpFunction.AdcpParameter(persistentName);
         parameter.setProperty(KEY_COMBINE_INPUT_AND_DESCRIPTION, true);
         return parameter;
      }
   };
   private List<AdcpFunction> adcpPingFunctions = List.of();

   private final List<PingFunction> allPingFunctions = new ArrayList<>();

   private List<PingFunction> selectedPingFunctions = List.of();
   private List<Subscription> selectedPingFunctionsListenerConnections = List.of();

   final ListenableProperty<Optional<Point>> mousePosition = new ListenableProperty<>(Optional.empty());
   private JFreeChart chart = PlotUtils.newEmptyChart();

   private volatile @Nullable EchogramPlotStatisticsDialog statisticsDialog;

   public EchogramPlotModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      autoAdjustYAxis.setPersistable(false);

      if (ExperimentationFunction.USE) {
         addFunction(new ExperimentationFunction(getLSSS(), 1));
         addFunction(new ExperimentationFunction(getLSSS(), 2));
      }

      addFunction(SimplePingFunction.bottomDepth());
      addFunction(SimplePingFunction.bottomDepthRelativeMainFrequency(getLSSS()));
      addFunction(TsRelativeMainFrequencyFunction.alongshipAngle(getLSSS()));
      addFunction(TsRelativeMainFrequencyFunction.athwartshipAngle(getLSSS()));
      addFunction(TsRelativeMainFrequencyFunction.depth(getLSSS()));
      addFunction(TsRelativeMainFrequencyFunction.alongshipCoordinate(getLSSS()));
      addFunction(TsRelativeMainFrequencyFunction.athwartshipCoordinate(getLSSS()));
      addFunction(TsRelativeMainFrequencyFunction.verticalCoordinate(getLSSS()));
      addFunction(TsRelativeMainFrequencyFunction.tsc(getLSSS()));
      addFunction(TsRelativeMainFrequencyFunction.tsu(getLSSS()));
      addFunction(new RelativeFrequencyResponseFunction(getLSSS()));
      addFunction(new SaFunction(getLSSS()));
      addFunction(SimplePingFunction.timeBetweenPings());
      addFunction(SimplePingFunction.distanceBetweenPings());
      addFunction(SimplePingFunction.vesselDistance());
      addFunction(SimplePingFunction.vesselSpeed());
      addFunction(SimplePingFunction.longitude());
      addFunction(SimplePingFunction.latitude());
      addFunction(SimplePingFunction.heading());

      addFunction(Nqp0DatagramFunction.average());
      addFunction(Nqp0DatagramFunction.upperLimit());
      addFunction(Nqp0DatagramFunction.quality());
      addFunction(NoiseXmlFunction.average());
      addFunction(NoiseXmlFunction.upperLimit());
      addFunction(NoiseXmlFunction.quality());

      addFunction(ChannelDataSampleFunction.sv());
      addFunction(ChannelDataSampleFunction.tsc());
      addFunction(ChannelDataSampleFunction.tsu());

      addFunction(ChannelDataParameterFunction.absorption());
      addFunction(ChannelDataParameterFunction.frequencyStart());
      addFunction(ChannelDataParameterFunction.frequencyEnd());
      addFunction(ChannelDataParameterFunction.gain());
      addFunction(ChannelDataParameterFunction.heave());
      addFunction(ChannelDataParameterFunction.pitch());
      addFunction(ChannelDataParameterFunction.pulseDuration());
      addFunction(ChannelDataParameterFunction.roll());
      addFunction(ChannelDataParameterFunction.sampleInterval());
      addFunction(ChannelDataParameterFunction.soundVelocity());
      addFunction(ChannelDataParameterFunction.transducerDepth());
      addFunction(ChannelDataParameterFunction.transmitPower());

      addFunction(ComplexChannelDataParameterFunction.slope());

      addFunction(ComplexChannelDataParameterFunction.transducerImpedance(0));
      addFunction(ComplexChannelDataParameterFunction.transducerImpedance(1));
      addFunction(ComplexChannelDataParameterFunction.transducerImpedance(2));
      addFunction(ComplexChannelDataParameterFunction.transducerImpedance(3));

      addFunction(ComplexChannelDataParameterFunction.transducerImpedancePhase(0));
      addFunction(ComplexChannelDataParameterFunction.transducerImpedancePhase(1));
      addFunction(ComplexChannelDataParameterFunction.transducerImpedancePhase(2));
      addFunction(ComplexChannelDataParameterFunction.transducerImpedancePhase(3));

      addFunction(new RollTransmitToReceive());

      addFunction(InterpretationFunction.bubbleCorrection(getLSSS()));
      addFunction(InterpretationFunction.lowerLayerBoundary(getLSSS()));
      addFunction(InterpretationFunction.lowerThreshold(getLSSS()));
      addFunction(InterpretationFunction.quality(getLSSS()));

      addFunction(BooleanFunction.active());
      addFunction(BooleanFunction.broadband());
   }

   private void addFunction(PingFunction pingFunction) {
      pingFunction.selected.getChangeManager().addListener(updateSelectionListener);
      allPingFunctions.add(pingFunction);
   }

   private void removeFunction(PingFunction pingFunction) {
      pingFunction.selected.getChangeManager().removeListener(updateSelectionListener);
      allPingFunctions.remove(pingFunction);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      List<BaseParameter<?>> parameters = new ArrayList<>();
      parameters.add(plotAllChannels);
      parameters.add(autoAdjustYAxis);
      parameters.add(gaussianSmoothing);
      parameters.add(SeparatorParameter.line());
      allPingFunctions.stream()
            .filter(f -> !(f instanceof NmeaFunction) && !(f instanceof AdcpFunction))
            .map(f -> f.selected)
            .forEach(parameters::add);
      parameters.add(nmea);
      if (KoronaIncubatorFeatureToggles.ADCP_NETCDF) {
         parameters.add(adcp);
      }
      return parameters;
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(recomputeListener, List.of(
            plotAllChannels,
            gaussianSmoothing,
            getInterpretationSettings().getPingRangeChangeManager(),
            getInterpretationSettings().getPingMappingChangeManager(),
            getInterpretationSettings().getChannelChangeManager(),
            getInterpretationSettings().getPingSampler().getNewPingsChangeManager()
      ));

      registry.add(nmea, newCoalescingExecListener(this::updateNmeaPingFunctions));
      registry.add(adcp, newCoalescingExecListener(this::updateAdcpPingFunctions));

      registry.add(getConfigurationManager().getAppMiscConf().verticalScrollBar, this::updateScrollBarMargin);

      registry.add(newCoalescingExecListener(this::updateEchogramPositionMarker), List.of(
            getInterpretationSettings().mouseover().pingIndex(),
            getInterpretationSettings().mouseover().frozen(),
            getInterpretationSettings().getPingMappingChangeManager()
      ));

      Listener updateMouseEchogramPointListener = newCoalescingExecListener(this::updateMouseEchogramPoint);
      registry.add(mousePosition, updateMouseEchogramPointListener);
      registry.add(getInterpretationSettings().mouseover().frozen(), _ -> {
         if (mousePosition.getValue().isPresent()) {
            updateMouseEchogramPointListener.listen();
         }
      });

      DataSetManager dataSetManager = getConfigurationManager().getDataConf().getDataSetManager();
      registry.add(newCoalescingExecListener(this::updatePlotParameterExtractor), List.of(
            dataSetManager.getDataManager().getDataFileSetChangeManager(),
            dataSetManager.getOtherDataManager().getDataFileSetChangeManager()
      ));

      //---

      updateNmeaPingFunctions();
      updateAdcpPingFunctions();
      updatePlotParameterExtractor();
      updateScrollBarMargin();
      updateEchogramPositionMarker();
      updateSelectionListener.listen();
   }

   @Override
   protected void onDisable() {
      setSelectedPingFunctions(List.of());
      plot(List.of());
   }

   private void updateNmeaPingFunctions() {
      allPingFunctions.removeAll(nmeaPingFunctions);
      nmeaPingFunctions = nmea.getValue().stream()
            .map(NmeaFunction.NmeaParameter::stringToFunction)
            .toList();
      allPingFunctions.addAll(nmeaPingFunctions);
      updateSelectionListener.listen();
   }

   private void updateAdcpPingFunctions() {
      if (!KoronaIncubatorFeatureToggles.ADCP_NETCDF) {
         return;
      }
      allPingFunctions.removeAll(adcpPingFunctions);
      adcpPingFunctions = adcp.getValue().stream()
            .map(AdcpFunction.AdcpParameter::stringToFunction)
            .toList();
      allPingFunctions.addAll(adcpPingFunctions);
      updateSelectionListener.listen();
   }

   private void updatePlotParameterExtractor() {
      DataSetManager dataSetManager = getConfigurationManager().getDataConf().getDataSetManager();
      plotParameterExtractor.update(this, dataSetManager, PlotParameterPingFunction::of);
   }

   private void updateScrollBarMargin() {
      viewHolder.ifView(EchogramPlotView::updateBorder);
   }

   public void add(PingFunction pingFunction) {
      executeAlways(() -> {
         addFunction(pingFunction);
         updateSelectionListener.listen();
      });
   }

   public void remove(PingFunction pingFunction) {
      executeAlways(() -> {
         removeFunction(pingFunction);
         updateSelectionListener.listen();
      });
   }

   private void updateSelectedPingFunctions() {
      List<PingFunction> pingFunctions = allPingFunctions.stream()
            .filter(f -> f.selected.getBooleanValue())
            .toList();
      setSelectedPingFunctions(pingFunctions);
   }

   JFreeChart getChart() {
      return chart;
   }

   List<PingFunction> getAllPingFunctions() {
      return allPingFunctions;
   }

   List<PingFunction> getSelectedPingFunctions() {
      return selectedPingFunctions;
   }

   private void setSelectedPingFunctions(List<PingFunction> pingFunctions) {
      selectedPingFunctionsListenerConnections.forEach(Subscription::unsubscribe);
      selectedPingFunctions = pingFunctions;
      ListenerRegistry listenerRegistry = new ListenerRegistry();
      selectedPingFunctions.forEach(pingFunction -> {
         pingFunction.addListeners(getLSSS(), listenerRegistry);
         listenerRegistry.add(pingFunction.getChangeManager(), recomputeListener);
      });
      selectedPingFunctionsListenerConnections = listenerRegistry.build();
      recomputeListener.listen();
   }

   private void updateMouseEchogramPoint() {
      Point mousePoint = mousePosition.getValue().orElse(null);
      PingIndex pingIndex = mousePoint != null
            ? getInterpretationSettings().getPingSettings().xToContainingPingIndex(mousePoint.x)
            : null;
      getInterpretationSettings().mouseover().setPos(pingIndex);
   }

   private void updateEchogramPositionMarker() {
      double value;
      if (mousePosition.getValue().isPresent() && !getInterpretationSettings().mouseover().isFrozen()) {
         value = Double.NaN;
      } else {
         PingIndex pingIndex = getInterpretationSettings().mouseover().getPingIndex();
         value = pingIndex != null ? getInterpretationSettings().getPingMapping().valueOf(pingIndex) : Double.NaN;
      }
      echogramPositionMarker.setValue(value);
   }

   private void recompute() {
      plot(compute());
   }

   private List<EchogramPlotDataset> compute() {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return List.of();
      }

      List<Ping> pings = getInterpretationSettings().getPingSampler().getAvailablePings();
      if (pings.isEmpty()) {
         return List.of();
      }

      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      int currentChannel = getInterpretationSettings().getChannel();

      PingMapping pingMapping = getInterpretationSettings().getPingMapping();
      double[] x = new double[pings.size() + 1];
      Instant[] instants = new Instant[pings.size() + 1];
      float[] bottom = new float[pings.size() + 1];
      for (int i = 0; i < pings.size(); i++) {
         Ping ping = pings.get(i);
         x[i] = pingMapping.valueOf(ping);
         instants[i] = ping.getInstant();
         bottom[i] = dataFileSet.getCoordinatedDepth(ping.getPingIndex());
      }
      PingIndex endPingIndex = pingRange.end();
      x[x.length - 1] = pingMapping.valueOf(endPingIndex);
      instants[instants.length - 1] = endPingIndex.getInstant();
      bottom[bottom.length - 1] = dataFileSet.getCoordinatedDepth(endPingIndex);
      Ping endPing = dataFileSet.getTotalRange().end().equals(endPingIndex) ? null : dataFileSet.getPing(endPingIndex);

      float smoothingStdDev = gaussianSmoothing.getValue().orElse(0f);
      ArrayKernel smootherKernel = smoothingStdDev > 0 ? ArrayKernel.createGaussian(smoothingStdDev) : null;

      List<EchogramPlotDataset> dataSets = new ArrayList<>(selectedPingFunctions.size());
      for (PingFunction pingFunction : selectedPingFunctions) {
         XYInfo xyInfo = new XYInfo(
               PojoDataUtils.getParameterExport(pingMapping),
               pingFunction.getParameterExport());
         if (plotAllChannels.getBooleanValue() && pingFunction.isChannelDependent()) {
            List<RawFileTransducer> transducers = dataFileSet.getRawFileConfiguration().getTransducers();
            int transducerCount = transducers.size();
            for (int channel = 1; channel <= transducerCount; channel++) {
               float[] y = evaluatePingFunction(pingFunction, dataFileSet, pings, channel, endPing, instants, bottom);
               if (smootherKernel != null) {
                  y = smootherKernel.smooth(y);
               }
               int kHz = transducers.get(channel - 1).getKHz();
               String nameAndUnit = Utils.nameAndUnit(pingFunction.getName().displayName() + ", " + kHz + " kHz", pingFunction.getUnit());
               dataSets.add(new EchogramPlotDataset(nameAndUnit, x, y, xyInfo));
            }
         } else {
            float[] y = evaluatePingFunction(pingFunction, dataFileSet, pings, currentChannel, endPing, instants, bottom);
            if (smootherKernel != null) {
               y = smootherKernel.smooth(y);
            }
            String nameAndUnit = Utils.nameAndUnit(pingFunction.getName().displayName(), pingFunction.getUnit());
            dataSets.add(new EchogramPlotDataset(nameAndUnit, x, y, xyInfo));
         }
      }
      EchogramPlotStatisticsDialog statisticsDialog = this.statisticsDialog;
      if (statisticsDialog != null) {
         statisticsDialog.update(dataSets);
      }
      return dataSets;
   }

   private static float[] evaluatePingFunction(PingFunction pingFunction, DataFileSet dataFileSet, List<Ping> pings, int channel,
                                               @Nullable Ping endPing, Instant[] instants, float[] bottom) {
      float[] y = new float[pings.size() + 1];
      for (int i = 0; i < pings.size(); i++) {
         Ping ping = pings.get(i);
         y[i] = (float) pingFunction.compute(dataFileSet, ping, channel);
      }
      y[y.length - 1] = endPing != null ? (float) pingFunction.compute(dataFileSet, endPing, channel) : y[y.length - 2];
      return pingFunction.postprocess(y, instants, bottom);
   }

   private void plot(List<EchogramPlotDataset> dataSets) {
      NumberAxis xAxis = PlotUtils.newNumberAxis(null);
      xAxis.setVisible(false);
      xAxis.setLowerMargin(0);
      xAxis.setUpperMargin(0);
      if (!dataSets.isEmpty()) {
         EchogramPlotDataset dataset = dataSets.getFirst();
         double xMin = dataset.getXValue(0, 0);
         double xMax = dataset.getXValue(0, dataset.getItemCount(0) - 1);
         if (xMin < xMax) {
            xAxis.setRange(xMin, xMax);
         }
      }

      NumberAxis yAxis = PlotUtils.newNumberAxis(null);
      yAxis.setTickLabelInsets(new RectangleInsets(0, -55, 0, 0));

      XYPlot plot = new XYPlot(null, xAxis, yAxis, null);
      plot.setDomainGridlinesVisible(false);
      plot.setRangePannable(true);
      plot.setOutlineVisible(false);
      plot.setInsets(new RectangleInsets(0, 0, 0, 0));

      LegendTitle legendTitle = new LegendTitle(plot);
      legendTitle.setItemFont(UiUtils.labelFont().deriveFont(Font.PLAIN, 10));
      legendTitle.setBackgroundPaint(new Color(0, 0, 0, 32));
      legendTitle.setMargin(0, 0, 5, 5);
      plot.addAnnotation(new XYTitleAnnotation(1, 0, legendTitle, RectangleAnchor.BOTTOM_RIGHT));

      // Create new marker to avoid memory leak, #1423.
      echogramPositionMarker = new ValueMarker(Double.NaN, Color.DARK_GRAY, GuiUtils.STROKE_1);
      updateEchogramPositionMarker();
      plot.addDomainMarker(echogramPositionMarker);
      for (PingFunction pingFunction : selectedPingFunctions) {
         pingFunction.addMarkers(plot);
      }

      DefaultDrawingSupplier drawingSupplier = new DefaultDrawingSupplier();
      for (int i = 0; i < dataSets.size(); i++) {
         EchogramPlotDataset dataset = dataSets.get(i);
         plot.setDataset(i, dataset);
         StandardXYItemRenderer renderer;
         if (dataset.containsIsolatedValues()) {
            renderer = PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.SHAPES_AND_LINES);
            renderer.setSeriesShape(0, new Rectangle2D.Float(-0.5f, -0.5f, 1, 1));
         } else {
            renderer = PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.LINES);
         }
         renderer.setSeriesPaint(0, drawingSupplier.getNextPaint());
         plot.setRenderer(i, renderer);
      }

      JFreeChart newChart = PlotUtils.newChart(null, plot, false);
      newChart.setBorderVisible(false);
      newChart.setPadding(new RectangleInsets(0, 0, 0, 0));
      newChart.setBackgroundPaint(Color.WHITE);
      if (!autoAdjustYAxis.getBooleanValue()) {
         PlotUtils.preserveRangeAxisRange(chart, newChart);
      }
      chart = newChart;

      viewHolder.ifViewDelayed(this, EchogramPlotView::updateChart);
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

   @Nullable
   EchogramPlotStatisticsDialog getStatisticsDialog() {
      return statisticsDialog;
   }

   void setStatisticsDialog(@Nullable EchogramPlotStatisticsDialog statisticsDialog) {
      this.statisticsDialog = statisticsDialog;
      if (statisticsDialog != null) {
         recomputeListener.listen();
      }
   }
}
