package no.imr.lsss.modules.broadband.sv;

import no.imr.korona.color.Colormap;
import no.imr.korona.color.Colormaps;
import no.imr.korona.computation.broadband.BroadbandSvByFrequency;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.viewer.coloring.SingleValueColorConverter;
import no.imr.korona.viewer.util.FrequencySelectionPanel;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.util.ColorConverterPaintScale;
import no.imr.lsss.util.FrequencyPlotMarker;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.RegularYXToZDataset;
import no.imr.tools.plot.XYZInfo;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.ui.RectangleAnchor;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class BroadbandSvByDepthModule extends BaseViewModule implements PojoDataContainer {
   private final HeaderParameter fftHeader = new HeaderParameter("FFT settings");

   private final FloatParameter frequencyResolution = new FloatParameter(
         new Name("FrequencyResolution", "Frequency resolution"),
         1, Unit.KHZ, ValueConstraints.gt(0f),
         "Horizontal grid size");

   private final FloatParameter depthResolution = new FloatParameter(
         new Name("DepthResolution", "Depth resolution"),
         2, Unit.METER, ValueConstraints.gt(0f),
         "Vertical grid size, used when computing Sv(f)");

   private final FloatParameter fftWindowSize = new FloatParameter(
         new Name("FftWindowSize", "FFT window size"),
         2, Unit.NONE, ValueConstraints.gt(0f),
         "Size of the FFT window in units of pulse length");

   private final HeaderParameter displayHeader = new HeaderParameter("Display settings");

   private final BooleanParameter useTVG = new BooleanParameter(
         new Name("UseTVG", "Use TVG"),
         true,
         "Use gain compensation");

   private final BooleanParameter autoAdjustAxes = new BooleanParameter(
         new Name("AutoAdjustAxes", "Auto-adjust axes"),
         true,
         "Automatically adjust the x-axis and y-axis when the plot is updated");

   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));

   private final Listener computeListener = newCoalescingExecListener(this::compute);
   private final FrequencySelectionPanel frequencySelectionPanel = new FrequencySelectionPanel(computeListener);
   private final Listener plotListener = newCoalescingExecListener(this::plot);

   private List<RegularYXToZDataset> datasets = List.of();
   private final FrequencyPlotMarker frequencyPlotMarker;
   private JFreeChart chart = PlotUtils.newEmptyChart();

   public BroadbandSvByDepthModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      autoAdjustAxes.setPersistable(false);

      frequencyPlotMarker = new FrequencyPlotMarker(getLSSS());
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            fftHeader,
            frequencyResolution,
            depthResolution,
            fftWindowSize,
            //---
            displayHeader,
            useTVG,
            autoAdjustAxes
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getParameters(), computeListener);

      registry.add(computeListener, List.of(
            getInterpretationSettings().mouseover().pingIndex(),
            getInterpretationSettings().mouseover().depth()
      ));
      registry.add(getInterpretationSettings().mouseover().kHz(), newCoalescingExecListener(frequencyPlotMarker::updateMarker));

      registry.add(getInterpretationSettings().getDataFileChangeManager(), newCoalescingExecListener(this::dataFilesUpdated));

      registry.add(getInterpretationSettings().getChannelChangeManager(), newCoalescingExecListener(channel -> {
         frequencySelectionPanel.setHighlighted(channel);
         computeListener.listen();
      }));

      registry.add(getInterpretationSettings().getColorConverterContainer().getChangeManager(), plotListener);

      //---

      dataFilesUpdated();
   }

   @Override
   protected void onDisable() {
      datasets = List.of();
      setChart(PlotUtils.newEmptyChart());
   }

   private void dataFilesUpdated() {
      RawFileConfiguration rawFileConfiguration = getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
      frequencySelectionPanel.update(rawFileConfiguration, getInterpretationSettings().getChannel());
      computeListener.listen();
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   private void compute() {
      datasets = computePlotData();
      plotListener.listen();
   }

   private List<RegularYXToZDataset> computePlotData() {
      PingIndex mousePingIndex = getInterpretationSettings().mouseover().getPingIndex();
      if (mousePingIndex == null) {
         return List.of();
      }
      Ping ping = getInterpretationSettings().getDataFileSet().getPing(mousePingIndex);
      List<RegularYXToZDataset> newDatasets = new ArrayList<>();
      XYZInfo xyzInfo = new XYZInfo(
            new ParameterExport("frequency", Unit.KHZ, ExportRounding.kHz()),
            new ParameterExport("depth", Unit.METER, ExportRounding.depth()),
            new ParameterExport(useTVG.getBooleanValue() ? "sv" : "noise", Unit.DB, ExportRounding.db()));
      for (int channel : frequencySelectionPanel.getChannels()) {
         if (!(channel == getInterpretationSettings().getChannel() || frequencySelectionPanel.isChannelSelected(channel))) {
            continue;
         }
         BroadbandData broadbandData = ping.getBroadbandData(channel);
         if (broadbandData == null) {
            continue;
         }
         float deltaDepth = depthResolution.getFloatValue();

         float pulseLength = broadbandData.getSoundVelocity() * broadbandData.getPulseDuration();
         float fftWindowRadius = fftWindowSize.getFloatValue() * pulseLength / 2;

         FloatRange totalDepthRange = broadbandData.getDepthRange()
               .shrink(fftWindowRadius - deltaDepth / 2) // So that the center of each depth cell sees +-fftWindowRadius.
               .shrinkToMultipleOf(deltaDepth);
         int nDepth = Math.round(totalDepthRange.getSize() / deltaDepth);

         FloatRange frequencyRange = broadbandData.getFrequencyRange();
         float deltaFrequency = frequencyResolution.getFloatValue() * 1000;
         int nFrequency = Math.round(frequencyRange.getSize() / deltaFrequency) + 1;

         float[][] depthAndFrequencyToSv = new float[nDepth][];
         BroadbandSvByFrequency broadbandSvByFrequency = new BroadbandSvByFrequency(broadbandData);
         broadbandSvByFrequency.setUseTVG(useTVG.getBooleanValue());
         for (int i = 0; i < nDepth; i++) {
            float centerDepth = totalDepthRange.min() + (i + 0.5f) * deltaDepth;
            FloatRange depthRange = FloatRange.ofCenterAndRadius(centerDepth, fftWindowRadius);
            float[] sv = broadbandSvByFrequency.calculate(depthRange, frequencyRange);
            sv = ArrayMath.resample(sv, nFrequency);
            ArrayMath.map(sv, PowerData::svToLogSv);
            depthAndFrequencyToSv[i] = sv;
         }
         String name = broadbandData.getTransducer().getKHz() + " kHz";
         RegularYXToZDataset dataset = new RegularYXToZDataset(name,
               frequencyRange.min() / 1000, deltaFrequency / 1000, // kHz
               totalDepthRange.min(), deltaDepth,
               depthAndFrequencyToSv,
               xyzInfo);
         newDatasets.add(dataset);
      }

      return newDatasets;
   }

   private void plot() {
      NumberAxis xAxis = createAxis("Frequency [kHz]");
      NumberAxis yAxis = createAxis("Depth [m]");
      yAxis.setInverted(true);
      XYPlot plot = new XYPlot(null, xAxis, yAxis, null);
      plot.setDomainGridlinesVisible(false);
      plot.setRangeGridlinesVisible(false);
      plot.setDomainPannable(true);
      plot.setRangePannable(true);

      plotDataSets(plot);

      if (getInterpretationSettings().mouseover().getPingIndex() != null) {
         Float depth = getInterpretationSettings().mouseover().getDepth();
         if (depth != null) {
            plot.addRangeMarker(new ValueMarker(depth, Color.DARK_GRAY, GuiUtils.STROKE_1));
         }
         plot.setNoDataMessage("No broadband data");
      }

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

   private void plotDataSets(XYPlot plot) {
      XYBlockRenderer renderer = new XYBlockRenderer();
      renderer.setBlockWidth(frequencyResolution.getFloatValue());
      renderer.setBlockHeight(depthResolution.getFloatValue());
      renderer.setBlockAnchor(RectangleAnchor.CENTER);
      Colormap colormap = getInterpretationSettings().getColorConverterContainer().getColorConverter().getColormap();
      ContinuousVariable svVariable = getInterpretationSettings().getColorConverterContainer().getSV();
      SingleValueColorConverter colorConverter = new SingleValueColorConverter(svVariable, colormap != null ? colormap : Colormaps.COMBINED);
      renderer.setPaintScale(new ColorConverterPaintScale(colorConverter));
      for (RegularYXToZDataset dataset : datasets) {
         int i = PlotUtils.nextDatasetIndex(plot);
         plot.setDataset(i, dataset);
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

   private static final class View extends BaseView {
      private final BroadbandSvByDepthModule module;
      private final JPanel mainPanel = new JPanel(new BorderLayout());
      private final ChartPanel chartPanel;

      private View(BroadbandSvByDepthModule module) {
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
