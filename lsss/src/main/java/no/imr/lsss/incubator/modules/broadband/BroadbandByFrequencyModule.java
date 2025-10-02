package no.imr.lsss.incubator.modules.broadband;

import no.imr.korona.color.Colormap;
import no.imr.korona.color.Colormaps;
import no.imr.korona.computation.broadband.BroadbandSvByFrequency;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.TransmitMode;
import no.imr.korona.region.Region;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.viewer.coloring.SingleValueColorConverter;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.korona.viewer.variables.raw.RawVariableFactory;
import no.imr.lsss.incubator.LsssIncubatorFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.broadband.sv.BroadbandSvData;
import no.imr.lsss.modules.broadband.sv.BroadbandSvModule;
import no.imr.lsss.util.ColorConverterPaintScale;
import no.imr.tools.ProgressHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.Listener;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.math.OnlineAverageAndVariance;
import no.imr.tools.math.WelfordsMethod;
import no.imr.tools.math.linalg.Vec2;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.RangeParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.PointListDataset;
import no.imr.tools.plot.RegularYXToZDataset;
import no.imr.tools.plot.XYZInfo;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.SuffixFileFilter;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.WrappingFlowLayout;
import no.imr.tools.xml.XmlUtils;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.apache.commons.statistics.descriptive.Quantile;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.RectangleAnchor;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

public final class BroadbandByFrequencyModule extends BaseViewModule {

   private final RangeParameter colorRangeLimits = new RangeParameter(
         new Name("colorRangeLimits", "Color range limits"),
         -160, -130, Unit.DB,
         "Min/max limits for the color range");

   private final RangeParameter colorRangeThresholds = new RangeParameter(
         new Name("colorRangeThresholds", "Color range thresholds"),
         -155, -135, Unit.DB,
         "Thresholds for the color range");

   private final BooleanParameter autoRange = new BooleanParameter(
         new Name("autoRange", "Automatic color range"),
         false,
         "Automatically detect reasonable color range");

   private final IntParameter pingAveraging = new IntParameter(
         new Name("pingAveraging", "Ping averaging"),
         1, Unit.DIMENSIONLESS, ValueConstraints.gte(1));

   private List<PlotData> datasets = List.of();
   private final Supplier<BroadbandSvModule> broadbandSvModule = moduleSupplier(BroadbandSvModule.class);
   private final Supplier<BroadbandPeakDetectionModule> broadbandPeakDetectionModule = moduleSupplier(BroadbandPeakDetectionModule.class);
   private JFreeChart chart = PlotUtils.newEmptyChart();
   private final ViewHolder<BroadbandByFrequencyModule.View> viewHolder = new ViewHolder<>(() -> new BroadbandByFrequencyModule.View(this));
   private final Listener replotListener = newCoalescingExecListener(this::plot);
   private boolean showPeaks = true;

   public BroadbandByFrequencyModule(ModuleInfo<LsssIncubatorFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            colorRangeLimits,
            colorRangeThresholds,
            autoRange,
            pingAveraging
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   private void compute(AsyncHandle asyncHandle, ProgressHandler progressHandler) {
      datasets = computePlotData(asyncHandle, progressHandler);
   }

   private List<PlotData> computePlotData(AsyncHandle asyncHandle, ProgressHandler progressHandler) {
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      float deltaFrequency = broadbandSvModule.get().frequencyResolution.getFloatValue() * 1000;

      int currentChannel = getInterpretationSettings().getChannel();

      List<TimeFrequencyData> byFrequencyData = new ArrayList<>();
      List<Region> regions = getRegionManager().getSelectedRegions();
      long totalCount = regions.stream()
            .map(Region::getPingRange)
            .mapToLong(PingRange::getPingCount)
            .sum();
      Listener progressListener = progressHandler.asCountingListener(totalCount);
      FloatRange firstFrequencyRange = FloatRange.EMPTY_RANGE;
      int maxLength = pingAveraging.getIntValue();
      Rolling rollingCentered = new Rolling(maxLength);
      Deque<PingIndex> lastPingIndices = new ArrayDeque<>(maxLength);
      float firstDepth = Float.NaN;
      int transmitMode = -1;
      for (Region region : regions) {
         for (PingIndex pingIndex : dataFileSet.getPingIndices(region.getPingRange())) {
            if (asyncHandle.isCancelled()) {
               return List.of();
            }
            progressListener.listen();
            Ping ping = dataFileSet.getPing(pingIndex);
            BroadbandData broadbandData = ping.getBroadbandData(currentChannel);
            if (broadbandData == null) {
               continue;
            }
            transmitMode = broadbandData.getTransmitMode();
            BroadbandSvByFrequency svByFrequency = new BroadbandSvByFrequency(broadbandData);
            FloatRange frequencyRange = broadbandData.getFrequencyRange()
                  .shrinkByFraction(broadbandSvModule.get().frequencyWindowing.getFloatValue() / 100)
                  .intersection(svByFrequency.getMaxFrequencyRange())
                  .shrinkToMultipleOf(deltaFrequency);
            if (firstFrequencyRange.isEmpty()) {
               firstFrequencyRange = frequencyRange;
            }
            int n = Math.round(frequencyRange.getSize() / deltaFrequency) + 1;
            OnlineAverageAndVariance accumulator = new OnlineAverageAndVariance(n);
            svByFrequency.setUseTVG(broadbandSvModule.get().useTVG.getBooleanValue());
            FloatRangeSet depthRanges = region.getRegionManager().getDepthRangesForChannel(region, ping, currentChannel);
            if (Float.isNaN(firstDepth)) {
               firstDepth = depthRanges.getBoundingRange().min();
            }
            List<BroadbandSvData> svData = depthRanges.getFloatRanges().stream()
                  .flatMap(depthRange -> svByFrequency.windowDepthRanges(depthRange,
                        broadbandSvModule.get().depthResolution.getFloatValue(),
                        broadbandSvModule.get().depthMargin.getFloatValue(),
                        broadbandSvModule.get().fftWindowSize.getFloatValue()).stream())
                  .map(depthRange -> {
                     float[] values = svByFrequency.calculate(depthRange, frequencyRange);
                     values = ArrayMath.resample(values, Math.round(frequencyRange.getSize() / deltaFrequency) + 1);
                     if (broadbandSvModule.get().useDb.getBooleanValue()) {
                        ArrayMath.map(values, PowerData::svToLogSv);
                     }
                     return new BroadbandSvData(depthRange, values);
                  })
                  .toList();
            for (BroadbandSvData data : svData) {
               accumulator.update(data.sv(), data.depthRange().getSize());
            }
            rollingCentered.add(accumulator.getMeans());
            lastPingIndices.addLast(pingIndex);
            if (lastPingIndices.size() > maxLength) {
               lastPingIndices.removeFirst();
            }
            if (rollingCentered.length >= Math.round(rollingCentered.maxLength / 2.0f)) {
               PingIndex centerPingIndex = (PingIndex) lastPingIndices.toArray()[rollingCentered.length - Math.round(rollingCentered.maxLength / 2.0f)];
               byFrequencyData.add(new TimeFrequencyData(centerPingIndex, rollingCentered.mean(), rollingCentered.stdDev()));
            }
         }
         includeLastPingIndices(dataFileSet, byFrequencyData, rollingCentered, lastPingIndices);
      }

      if (byFrequencyData.isEmpty()) {
         return List.of();
      }
      String name = Utils.hzToKHz(getInterpretationSettings().getFrequency()) + " kHz";
      PingIndex beginPingIndex = byFrequencyData.getFirst().pingIndex;
      int n = byFrequencyData.size();
      float deltaN = 1.0f;
      float[][] data = new float[n][];
      Graph peaks = new Graph("peaks")
            .setRenderer(() -> PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.LINES))
            .setColor(Color.RED)
            .setStroke(GuiUtils.STROKE_3);
      PeakInfo peakInfo = new PeakInfo();
      int j = 0;
      for (TimeFrequencyData timeFrequencyData : byFrequencyData) {
         data[j] = timeFrequencyData.svMeans;
         float[] stdDev = timeFrequencyData.stdDevs;
         FrequencyPeakDetector detector = new FrequencyPeakDetector();
         detector.detect(data[j], stdDev, broadbandPeakDetectionModule.get().peakHeightThreshold.getFloatValue(),
               broadbandPeakDetectionModule.get().peakWidthLimit.getValue(), broadbandPeakDetectionModule.get().peakWidthRelativeHeight.getFloatValue(),
               broadbandPeakDetectionModule.get().useWidthScaling.getBooleanValue(), firstFrequencyRange.min(), deltaFrequency,
               broadbandPeakDetectionModule.get().peakWidthScalingHeight.getValue(), broadbandPeakDetectionModule.get().prominenceThreshold.getValue());
         List<FrequencyPeakDetector.PeakPlotInfo> peakPlotInfos = detector.getPeakPlotInfos();
         if (!peakPlotInfos.isEmpty()) {
            for (FrequencyPeakDetector.PeakPlotInfo peakPlotInfo : peakPlotInfos) {
               peaks.addSeparator();
               peaks.addPoint(j * deltaN + beginPingIndex.getPingNumber(), peakPlotInfo.peakIndex() * deltaFrequency / 1000 + firstFrequencyRange.min() / 1000);
               peaks.addPoint((j + 1) * deltaN + beginPingIndex.getPingNumber(), peakPlotInfo.peakIndex() * deltaFrequency / 1000 + firstFrequencyRange.min() / 1000);
               peakInfo.add(new PeakData(new Date(timeFrequencyData.pingIndex().getTimeInMillis()).toString(), timeFrequencyData.pingIndex().getPingNumber(),
                     peakPlotInfo.peakIndex() * deltaFrequency / 1000 + firstFrequencyRange.min() / 1000,
                     peakPlotInfo.prominenceData().prominence(), peakPlotInfo.std()));
            }
         }
         j++;
      }
      data = transposeArray(data);
      XYZInfo xyzInfo = new XYZInfo(
            new ParameterExport("ping number", Unit.DIMENSIONLESS, ExportTransform.identity()),
            new ParameterExport("frequency", Unit.KHZ, ExportRounding.kHz()),
            new ParameterExport(broadbandSvModule.get().useTVG.getBooleanValue() ? "sv" : "noise", Unit.DB, ExportRounding.db()));
      if (autoRange.getBooleanValue()) {
         float maxThreshold = Float.NEGATIVE_INFINITY;
         float minThreshold = Float.POSITIVE_INFINITY;
         double[] flattenedData = Utils.toDoubles(flatten(data));
         maxThreshold = (float) Math.max(Quantile.withDefaults().evaluate(flattenedData, 0.99), maxThreshold);
         minThreshold = (float) Math.min(Quantile.withDefaults().evaluate(flattenedData, 0.01), minThreshold);
         colorRangeLimits.setValue(FloatRange.of(minThreshold, maxThreshold));
         colorRangeThresholds.setValue(FloatRange.of(minThreshold, maxThreshold));
      }
      Info info = new Info(dataFileSet.getDataFile(beginPingIndex).getSegmentHandle().getBaseName(), new Date(beginPingIndex.getTimeInMillis()).toString(),
            n, firstDepth, getInterpretationSettings().getFrequency(), transmitMode);

      return List.of(new PlotData(info, new RegularYXToZDataset(name,
            beginPingIndex.getPingNumber(), 1.0,
            firstFrequencyRange.min() / 1000, deltaFrequency / 1000,
            data,
            xyzInfo), peaks, peakInfo));
   }

   private static void includeLastPingIndices(DataFileSet dataFileSet, List<TimeFrequencyData> byFrequencyData, Rolling rollingCentered, Deque<PingIndex> lastPingIndices) {
      if (byFrequencyData.isEmpty()) {
         return;
      }
      PingIndex lastPingIndex = byFrequencyData.getLast().pingIndex;
      while (rollingCentered.length > Math.round(rollingCentered.maxLength / 2.0f)) {
         rollingCentered.removeLeft();
         lastPingIndices.removeFirst();
         PingIndex centerPingIndex = dataFileSet.getPingIndexOrNull(lastPingIndex.getPingNumber() + 1);
         if (centerPingIndex == null) {
            break;
         }
         lastPingIndex = centerPingIndex;
         byFrequencyData.add(new TimeFrequencyData(centerPingIndex, rollingCentered.mean(), rollingCentered.stdDev()));
      }
   }

   private static float[] flatten(float[][] array) {
      int m = array.length;
      int n = array[0].length;
      float[] result = new float[n * m];
      for (int x = 0; x < m; x++) {
         for (int y = 0; y < n; y++) {
            result[y * m + x] = array[x][y];
         }
      }
      return result;
   }

   private static float[][] transposeArray(float[][] array) {
      int m = array.length;
      int n = array[0].length;

      float[][] transposedArray = new float[n][m];

      for (int x = 0; x < n; x++) {
         for (int y = 0; y < m; y++) {
            transposedArray[x][y] = array[y][x];
         }
      }

      return transposedArray;
   }

   private void setChart(JFreeChart chart) {
      this.chart = chart;
      viewHolder.ifViewDelayed(this, View::updateChart);
   }

   private static NumberAxis createAxis(String label) {
      NumberAxis axis = PlotUtils.newNumberAxis(label);
      axis.setLowerMargin(0);
      axis.setUpperMargin(0);
      return axis;
   }

   private void setShowPeaks(boolean showPeaks) {
      this.showPeaks = showPeaks;
      executeIfEnabled(replotListener::listen);
   }

   private void plot() {
      NumberAxis xAxis = createAxis("Ping number [-]");
      NumberAxis yAxis = createAxis("Frequency [kHz]");
      XYPlot plot = new XYPlot(null, xAxis, yAxis, null);
      plot.setDomainGridlinesVisible(false);
      plot.setRangeGridlinesVisible(false);
      plot.setDomainPannable(true);
      plot.setRangePannable(true);

      if (!datasets.isEmpty()) {
         plotData(plot);
      } else {
         plot.setNoDataMessage("No broadband data");
      }

      setChart(PlotUtils.newChart(null, plot, false));
   }

   private void plotData(XYPlot plot) {
      XYBlockRenderer renderer = new XYBlockRenderer();
      renderer.setBlockHeight(broadbandSvModule.get().frequencyResolution.getFloatValue());
      renderer.setBlockWidth(datasets.getFirst().dataset().getSeriesCount());
      renderer.setBlockAnchor(RectangleAnchor.CENTER);
      Colormap colormap = getInterpretationSettings().getColorConverterContainer().getColorConverter().getColormap();
      ContinuousVariable noiseVariable = new NoiseVariable(colorRangeLimits.getValue(), colorRangeThresholds.getValue());
      SingleValueColorConverter colorConverter = new SingleValueColorConverter(noiseVariable, colormap != null ? colormap : Colormaps.COMBINED);
      renderer.setPaintScale(new ColorConverterPaintScale(colorConverter));
      for (PlotData dataset : datasets) {
         int i = PlotUtils.nextDatasetIndex(plot);
         plot.setDataset(i, dataset.dataset());
         plot.setRenderer(i, renderer);
      }
      if (showPeaks) {
         for (PlotData dataset : datasets) {
            int i = PlotUtils.nextDatasetIndex(plot);
            Graph graph = dataset.peaks();
            XYItemRenderer graphRenderer = graph.getRenderer().get();
            graphRenderer.setSeriesPaint(0, graph.getColor());
            graphRenderer.setSeriesStroke(0, graph.getStroke());
            plot.setDataset(i, new PointListDataset(graph));
            plot.setRenderer(i, graphRenderer);
         }
         plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
      }
   }

   private String defaultExportFileName() {
      StringBuilder baseName = new StringBuilder("Noise");
      for (PlotData dataset : datasets) {
         baseName.append('_').append(dataset.info.firstDate);
      }
      baseName.append(".xml");
      String s = baseName.toString();
      return s.replace(':', '_');
   }

   private static List<Vec2> stride(List<Vec2> points, int strideLength) {
      List<Vec2> result = new ArrayList<>();
      for (int i = 0; i < points.size(); i += strideLength) {
         Vec2 point = points.get(i);
         result.add(point);
      }
      return result;
   }

   private void export(Path file) throws IOException {
      Element root = DocumentHelper.createElement("peaks");
      for (PlotData dataset : datasets) {
         String transmitMode = switch (dataset.info.transmitMode) {
            case TransmitMode.ACTIVE -> "active";
            case TransmitMode.PASSIVE -> "passive";
            default -> "unknown";
         };
         root.addElement("id")
               .addAttribute("name", dataset.info.filename)
               .addAttribute("startDate", dataset.info.firstDate)
               .addAttribute("nPings", Integer.toString(dataset.info.nPings))
               .addAttribute("minDepth", Float.toString(dataset.info.firstDepth))
               .addAttribute("nominalFrequency", Float.toString(dataset.info.frequency))
               .addAttribute("transmitMode", transmitMode);

         List<PeakData> peakData = dataset.peakInfo().data;
         peakData.sort(Comparator.comparing(PeakData::frequency).thenComparing(PeakData::pingNumber));
         for (PeakData pd : peakData) {
            root.addElement("entry")
                  .addAttribute("index", Float.toString(pd.pingNumber))
                  .addAttribute("frequency", Float.toString(pd.frequency))
                  .addAttribute("time", pd.date)
                  .addAttribute("prominence", Float.toString(pd.prominence))
                  .addAttribute("std", Float.toString(pd.std));
         }
      }
      Document document = DocumentHelper.createDocument(root);
      XmlUtils.writeDocument(document, file);
   }

   private record TimeFrequencyData(PingIndex pingIndex, float[] svMeans, float[] stdDevs) {
   }

   private static final class Rolling {
      private final int maxLength;
      private float[][] array;
      private int length;

      private Rolling(int maxLength) {
         this.maxLength = maxLength;
         array = new float[maxLength][];
      }

      private float[][] shiftLeft() {
         float[][] newArray = new float[maxLength][];
         if (maxLength - 1 >= 0) {
            System.arraycopy(array, 1, newArray, 0, maxLength - 1);
         }
         return newArray;
      }

      private void add(float[] data) {
         if (length == maxLength) {
            array = shiftLeft();
            array[maxLength - 1] = data;
         } else {
            array[length] = data;
            length += 1;
         }
      }

      private void removeLeft() {
         array = shiftLeft();
         length -= 1;
      }

      private float[] mean() {
         int size = array[0].length;
         float[] total = new float[size];
         int count = 0;
         for (int i = 0; i < maxLength; i++) {
            if (array[i] != null && array[i].length == size) {
               for (int j = 0; j < size; j++) {
                  total[j] += array[i][j];
               }
               count++;
            }
         }
         if (count == 0) {
            return Utils.EMPTY_FLOAT_ARRAY;
         }
         ArrayMath.divide(total, count);
         return total;
      }

      private float[] stdDev() {
         int size = array[0].length;
         WelfordsMethod[] welford = new WelfordsMethod[size];
         for (int j = 0; j < size; j++) {
            welford[j] = new WelfordsMethod();
            for (int i = 0; i < maxLength; i++) {
               if (array[i] != null && array[i].length == size) {
                  welford[j].update(array[i][j]);
               }
            }
         }
         float[] std = new float[size];
         for (int j = 0; j < size; j++) {
            std[j] = (float) welford[j].getStdDev();
         }
         return std;
      }
   }

   private static final class NoiseVariable extends ContinuousVariable {

      private NoiseVariable(FloatRange maxRange, FloatRange range) {
         super(RawVariableFactory.RAW_VARIABLE_GROUP, new Name("noise", "Noise"), new ContinuousVariableSettings(maxRange, range, 1, false),
               Unit.DB, ExportRounding.db());
      }

      @Override
      public boolean isUsableInContext() {
         return true;
      }

      @Override
      public ContinuousVariableResult evaluate(int channel, Ping ping) {
         return ContinuousVariableResult.EMPTY;
      }
   }

   private static final class View extends BaseView {
      private final BroadbandByFrequencyModule module;
      private final JPanel mainPanel = new JPanel(new BorderLayout());
      private final ChartPanel chartPanel;
      private Path lastFile = Utils.getUserHome().resolve("noise.xml");

      private View(BroadbandByFrequencyModule module) {
         super(module);

         this.module = module;
         chartPanel = PlotUtils.newChartPanel(module.chart);
         mainPanel.add(chartPanel);

         mainPanel.add(makeButtonsPanel(), BorderLayout.NORTH);
         mainPanel.add(makeSelectionPanel(), BorderLayout.SOUTH);
      }

      private JComponent makeButtonsPanel() {
         JPanel buttonsPanel = new JPanel(new WrappingFlowLayout(FlowLayout.LEFT));
         buttonsPanel.setBackground(Color.WHITE);

         JButton computePeaks = new JButton("Compute");
         computePeaks.addActionListener(e -> compute());
         buttonsPanel.add(computePeaks);

         JButton saveAs = new JButton("Save as...");
         saveAs.addActionListener(e -> save());
         buttonsPanel.add(saveAs);

         return buttonsPanel;
      }

      private JComponent makeSelectionPanel() {
         JPanel panel = new JPanel(new WrappingFlowLayout());
         panel.setMinimumSize(new Dimension(10, 10));
         panel.setBackground(Color.WHITE);
         JCheckBox checkBox = new JCheckBox("Show peaks", module.showPeaks);
         checkBox.setBackground(Color.WHITE);
         checkBox.addItemListener(e -> module.setShowPeaks(checkBox.isSelected()));
         panel.add(checkBox);
         return panel;
      }

      private void compute() {
         ProgressView progressView = new ProgressView("Computing...", 1000)
               .mainProgressAsPercentage();
         new WorkerDialog(getComponent(), progressView.getComponent())
               .start(asyncHandle -> {
                  module.compute(asyncHandle, progressView.getMainProgressHandler());
                  module.plot();
               });
      }

      private JFileChooser createFileChooser(String defaultFileName) {
         Path surveyFile = module.getLSSS().getSurveyManager().getSurveyFile();
         Path surveyDir = surveyFile != null ? surveyFile.getParent() : null;
         if (surveyDir != null && !FileUtils.isInDir(lastFile, surveyDir)) {
            lastFile = surveyDir.resolve(defaultFileName);
         }
         JFileChooser fileChooser = new JFileChooser();
         fileChooser.setSelectedFile(lastFile.resolveSibling(defaultFileName).toFile());
         fileChooser.setFileFilter(new SuffixFileFilter("Broadband peak detection", ".xml"));
         return fileChooser;
      }

      private void save() {
         JFileChooser fileChooser = createFileChooser(module.defaultExportFileName());
         int returnVal = fileChooser.showSaveDialog(getComponent());
         if (returnVal == JFileChooser.APPROVE_OPTION) {
            Path selectedFile = fileChooser.getSelectedFile().toPath();
            lastFile = selectedFile;
            if (Files.exists(selectedFile)) {
               int confirm = JOptionPane.showConfirmDialog(mainPanel, "Do you want to replace the existing file?",
                     "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
               if (confirm != JOptionPane.YES_OPTION) {
                  return;
               }
            }
            try {
               module.export(selectedFile);
            } catch (IOException e) {
               GuiUtils.showErrorDialog(getComponent(), "Error saving " + selectedFile);
            }
         }
      }

      @Override
      public JComponent getComponent() {
         return mainPanel;
      }

      private void updateChart() {
         chartPanel.setChart(module.chart);
      }
   }

   private static final class PeakInfo {
      private final List<PeakData> data = new ArrayList<>();

      private PeakInfo() {
      }

      private void add(PeakData peakData) {
         data.add(peakData);
      }
   }

   private record PeakData(String date, long pingNumber, float frequency, float prominence, float std) {
   }

   private record Info(String filename, String firstDate, int nPings, float firstDepth, float frequency, int transmitMode) {
   }

   private record PlotData(Info info, RegularYXToZDataset dataset, Graph peaks, PeakInfo peakInfo) {
   }
}
