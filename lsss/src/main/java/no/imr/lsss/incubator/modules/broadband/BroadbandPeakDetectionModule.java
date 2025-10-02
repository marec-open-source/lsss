package no.imr.lsss.incubator.modules.broadband;

import no.imr.korona.computation.broadband.BroadbandSvByFrequency;
import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFilterModuleConfig;
import no.imr.korona.computation.broadband.notchfilter.BroadbandTemporalNotchFilterConfig;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.region.Region;
import no.imr.lsss.incubator.LsssIncubatorFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.broadband.sv.BroadbandSvData;
import no.imr.lsss.modules.broadband.sv.BroadbandSvModule;
import no.imr.lsss.util.FrequencyPlotMarker;
import no.imr.tools.ProgressHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.math.OnlineAverageAndVariance;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.RangeParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.Graph;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.Plotter;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class BroadbandPeakDetectionModule extends BaseViewModule {

   private final ViewHolder<BroadbandPeakDetectionModuleView> viewHolder = new ViewHolder<>(() -> new BroadbandPeakDetectionModuleView(this));

   public final FloatParameter peakHeightThreshold = new FloatParameter(
         new Name("PeakHeightThreshold", "Peak height threshold"),
         4, Unit.DIMENSIONLESS, ValueConstraints.gt(0f),
         "Peak height threshold in units of standard deviations");

   public final FloatParameter peakWidthRelativeHeight = new FloatParameter(
         new Name("PeakWidthRelativeHeight", "Peak width relative height"),
         0.5f, Unit.DIMENSIONLESS, ValueConstraints.gtLt(0f, 1f),
         "Peak with is calculated at this relative height. A larger value implies a wider peak.");

   public final BooleanParameter useWidthScaling = new BooleanParameter(
         new Name("UsePeakWidthScaling", "Use peak width scaling"),
         true,
         "Use peak width scaling");

   public final FloatParameter peakWidthScalingHeight = new FloatParameter(
         new Name("peakWidthRelativeScalingHeight", "Peak width relative scaling height"),
         0.9f, Unit.DIMENSIONLESS, ValueConstraints.gtLt(0f, 1f),
         "Peak width is scaled to this relative height assuming a Gaussian shape");

   public final RangeParameter peakWidthLimit = new RangeParameter(
         new Name("PeakWidthLimit", "Peak width limit"),
         0, 10f, Unit.KHZ, ValueConstraints.gte(0f),
         "Peak width limit");

   public final IntParameter pingAveraging = new IntParameter(
         new Name("pingAveraging", "Ping averaging"),
         1, Unit.DIMENSIONLESS, ValueConstraints.gte(1),
         "Ping averaging");

   public final FloatParameter prominenceThreshold = new FloatParameter(
         new Name("prominenceThreshold", "Prominence threshold"),
         0, Unit.DB, ValueConstraints.gte(0.0f),
         "Prominence threshold");

   private final Supplier<BroadbandSvModule> broadbandSvModule = moduleSupplier(BroadbandSvModule.class);
   private final Map<Integer, JFreeChart> channelToChart = new ConcurrentHashMap<>();
   private final FrequencyPlotMarker frequencyPlotMarker;
   private final Map<Integer, FrequencyPeakDetector.DetectionInfo> channelToFrequencyResponse = new ConcurrentHashMap<>();

   public BroadbandPeakDetectionModule(ModuleInfo<LsssIncubatorFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      frequencyPlotMarker = new FrequencyPlotMarker(getLSSS());

      useWidthScaling.addListenerAndNotify(peakWidthScalingHeight::setEnabled);
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            peakHeightThreshold,
            peakWidthRelativeHeight,
            useWidthScaling,
            peakWidthScalingHeight,
            peakWidthLimit,
            pingAveraging,
            prominenceThreshold
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getInterpretationSettings().getChannelChangeManager(),
            viewHolder.coalescingListener(BroadbandPeakDetectionModuleView::updateChart));

      registry.add(getInterpretationSettings().mouseover().kHz(), newCoalescingExecListener(frequencyPlotMarker::updateMarker));
   }

   @Override
   protected void onDisable() {
      channelToChart.clear();
      channelToFrequencyResponse.clear();
   }

   FrequencyPlotMarker getFrequencyPlotMarker() {
      return frequencyPlotMarker;
   }

   JFreeChart getChart() {
      JFreeChart chart = channelToChart.get(getInterpretationSettings().getChannel());
      return chart != null ? chart : PlotUtils.newEmptyChart();
   }

   private List<BroadbandTemporalNotchFilterConfig> channelPeakDetection(OnlineAverageAndVariance averageAndVariance,
                                                                         float firstFrequency, float deltaFrequency, int channel) {
      float[] var = averageAndVariance.getVar();
      double sumStd = 0;
      float[] std = new float[var.length];
      int j = 0;
      for (float v : var) {
         double s = Math.sqrt(v);
         sumStd += s;
         std[j] = (float) s;
         j++;
      }
      float meanStd = (float) (sumStd / var.length);

      FrequencyPeakDetector detector = new FrequencyPeakDetector();
      detector.detect(averageAndVariance.getMeans(), std, peakHeightThreshold.getValue(), peakWidthLimit.getValue(),
            peakWidthRelativeHeight.getValue(), useWidthScaling.getBooleanValue(), firstFrequency, deltaFrequency, peakWidthScalingHeight.getValue(),
            prominenceThreshold.getValue());

      channelToChart.put(channel, computePlot(averageAndVariance, firstFrequency, deltaFrequency, meanStd, std, detector.getPeakPlotInfos()));
      channelToFrequencyResponse.put(channel, new FrequencyPeakDetector.DetectionInfo(averageAndVariance, detector.getPeakPlotInfos(), firstFrequency, deltaFrequency));
      return detector.getResult();
   }

   private JFreeChart computePlot(OnlineAverageAndVariance averageAndVariance, float firstFrequency, float deltaFrequency, float meanStd, float[] std, List<FrequencyPeakDetector.PeakPlotInfo> peakPlotInfos) {
      Graph sv = new Graph("sv")
            .setColor(Color.RED);
      Graph svPlusStdDev = new Graph("sv + stdDev")
            .setColor(Color.GRAY);
      Graph svMinusStdDev = new Graph("sv - stdDev")
            .setColor(Color.GRAY);
      Graph svPlusMeanStdDev = new Graph("sv + mean stdDev")
            .setColor(Color.PINK);
      Graph svMinusMeanStdDev = new Graph("sv - mean stdDev")
            .setColor(Color.PINK);
      Graph peaks = new Graph("peaks")
            .setColor(Color.GREEN)
            .setLineWidth(3);
      float[] means = averageAndVariance.getMeans();
      for (int i = 0; i < means.length; i++) {
         float mean = means[i];
         float kHz = (firstFrequency + i * deltaFrequency) / 1000;
         sv.addPoint(kHz, mean);
         svPlusStdDev.addPoint(kHz, mean + std[i]);
         svMinusStdDev.addPoint(kHz, mean - std[i]);
         svPlusMeanStdDev.addPoint(kHz, mean + meanStd);
         svMinusMeanStdDev.addPoint(kHz, mean - meanStd);
      }
      for (FrequencyPeakDetector.PeakPlotInfo peakPlotInfo : peakPlotInfos) {
         int i = peakPlotInfo.peakIndex();
         float kHz = (firstFrequency + i * deltaFrequency) / 1000;
         float mean = means[i];
         float minY = mean - peakPlotInfo.prominenceData().prominence();
         peaks.addSeparator();
         peaks.addPoint(kHz, mean);
         peaks.addPoint(kHz, minY);
         peaks.addSeparator();
         peaks.addPoint((firstFrequency + peakPlotInfo.prominenceData().leftBase() * deltaFrequency) / 1000, minY);
         peaks.addPoint((firstFrequency + peakPlotInfo.prominenceData().rightBase() * deltaFrequency) / 1000, minY);
      }
      List<Graph> graphs = List.of(peaks, sv, svMinusStdDev, svPlusStdDev, svMinusMeanStdDev, svPlusMeanStdDev);
      JFreeChart chart = new Plotter(graphs)
            .xAxis("Frequency [kHz]")
            .createChart();
      XYPlot plot = chart.getXYPlot();
      for (FrequencyPeakDetector.PeakPlotInfo peakPlotInfo : peakPlotInfos) {
         float kHz = (firstFrequency + peakPlotInfo.peakIndex() * deltaFrequency) / 1000;
         plot.addDomainMarker(new ValueMarker(kHz, Color.PINK, GuiUtils.STROKE_1));
      }
      frequencyPlotMarker.addMarker(plot);
      return chart;
   }

   private static BroadbandSvData averageBBData(List<BroadbandSvData> svData) {
      int maxSize = svData.stream().mapToInt(value -> value.sv().length).max().orElse(0);
      double minRange = svData.stream().mapToDouble(value -> value.depthRange().min()).min().orElse(0);
      double maxRange = svData.stream().mapToDouble(value -> value.depthRange().max()).max().orElse(Double.POSITIVE_INFINITY);

      float[] sv = new float[maxSize];
      int[] count = new int[maxSize];
      for (BroadbandSvData data : svData) {
         for (int i = 0; i < data.sv().length; i++) {
            float v = data.sv()[i];
            sv[i] += v;
            count[i] += 1;
         }
      }
      for (int i = 0; i < maxSize; i++) {
         if (count[i] > 0) {
            sv[i] /= count[i];
         }
      }
      return new BroadbandSvData(FloatRange.of(minRange, maxRange), sv);
   }

   private List<BroadbandTemporalNotchFilterConfig> computeNotchFilterConfigs(AsyncHandle asyncHandle, ProgressHandler progressHandler) {
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      int transducerCount = dataFileSet.getRawFileConfiguration().getTransducerCount();
      float deltaFrequency = broadbandSvModule.get().frequencyResolution.getFloatValue() * 1000;
      Map<Integer, List<BroadbandSvData>> channelData = new HashMap<>();
      Map<Integer, FloatRange> channelFrequencyRange = new HashMap<>();
      for (int channel = 1; channel <= transducerCount; channel++) {
         channelData.put(channel, new ArrayList<>());
      }

      List<Region> regions = getRegionManager().getSelectedRegions();
      long totalCount = regions.stream()
            .map(Region::getPingRange)
            .mapToLong(PingRange::getPingCount)
            .sum();
      Listener progressListener = progressHandler.asCountingListener(totalCount);

      for (Region region : regions) {
         for (PingIndex pingIndex : dataFileSet.getPingIndices(region.getPingRange())) {
            if (asyncHandle.isCancelled()) {
               return List.of();
            }
            progressListener.listen();
            Ping ping = dataFileSet.getPing(pingIndex);

            for (int channel = 1; channel <= transducerCount; channel++) {
               BroadbandData broadbandData = ping.getBroadbandData(channel);
               if (broadbandData == null) {
                  continue;
               }
               BroadbandSvByFrequency svByFrequency = new BroadbandSvByFrequency(broadbandData);
               svByFrequency.setUseTVG(broadbandSvModule.get().useTVG.getBooleanValue());
               FloatRange frequencyRange = broadbandData.getFrequencyRange()
                     .shrinkByFraction(broadbandSvModule.get().frequencyWindowing.getFloatValue() / 100)
                     .intersection(svByFrequency.getMaxFrequencyRange())
                     .shrinkToMultipleOf(deltaFrequency);
               channelFrequencyRange.putIfAbsent(channel, frequencyRange);
               FloatRangeSet depthRanges = region.getRegionManager().getDepthRangesForChannel(region, ping, channel);
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
               // compute average over all depth ranges
               channelData.get(channel).add(averageBBData(svData));
               //channelData.get(channel).addAll(svData);
            }
         }
      }
      channelData = averageChannelData(transducerCount, deltaFrequency, channelData, channelFrequencyRange, pingAveraging.getIntValue());

      List<BroadbandTemporalNotchFilterConfig> configs = new ArrayList<>();
      for (int channel = 1; channel <= transducerCount; channel++) {
         FloatRange frequencyRange = channelFrequencyRange.get(channel);
         if (frequencyRange == null) {
            continue;
         }
         int n = Math.round(frequencyRange.getSize() / deltaFrequency) + 1;
         OnlineAverageAndVariance accumulator = new OnlineAverageAndVariance(n);
         channelData.get(channel).forEach(svData -> {
            if (svData.sv().length > 0) {
               accumulator.update(svData.sv(), svData.depthRange().getSize());
            }
         });
         if (!accumulator.hasMeans()) {
            continue;
         }
         configs.addAll(channelPeakDetection(accumulator, frequencyRange.min(), deltaFrequency, channel));
      }
      return configs;
   }

   private static Map<Integer, List<BroadbandSvData>> averageChannelData(int transducerCount, float deltaFrequency,
                                                                         Map<Integer, List<BroadbandSvData>> channelData,
                                                                         Map<Integer, FloatRange> channelFrequencyRange, int pingCount) {
      if (pingCount == 1) {
         return channelData;
      }
      Map<Integer, List<BroadbandSvData>> averagedChannelData = new HashMap<>();
      for (int channel = 1; channel <= transducerCount; channel++) {
         averagedChannelData.put(channel, new ArrayList<>());
      }
      for (int channel = 1; channel <= transducerCount; channel++) {
         FloatRange frequencyRange = channelFrequencyRange.get(channel);
         if (frequencyRange == null) {
            continue;
         }
         // average over m pings
         int n = Math.round(frequencyRange.getSize() / deltaFrequency) + 1;
         OnlineAverageAndVariance accumulator = new OnlineAverageAndVariance(n);
         FloatRange depthRange = FloatRange.EMPTY_RANGE;
         int i = 0;
         for (BroadbandSvData svData : channelData.get(channel)) {
            accumulator.update(svData.sv(), svData.depthRange().getSize());
            depthRange = depthRange.intersection(svData.depthRange());
            i += 1;
            if (i == pingCount) {
               averagedChannelData.get(channel).add(new BroadbandSvData(svData.depthRange(), accumulator.getMeans()));
               accumulator = new OnlineAverageAndVariance(n);
               depthRange = FloatRange.EMPTY_RANGE;
               i = 0;
            }
         }
         // add the average of the last pings even if there are still not m of them
         if (i != 0) {
            averagedChannelData.get(channel).add(new BroadbandSvData(depthRange, accumulator.getMeans()));
         }
      }
      return averagedChannelData;
   }

   void updateNotchFilterModuleConfig(AsyncHandle asyncHandle, ProgressHandler progressHandler) {
      channelToChart.clear();
      List<BroadbandTemporalNotchFilterConfig> notchFilterConfigs = computeNotchFilterConfigs(asyncHandle, progressHandler);
      BroadbandNotchFilterModuleConfig broadbandNotchFilterModuleConfig = new BroadbandNotchFilterModuleConfig();
      broadbandNotchFilterModuleConfig.getBroadbandTemporalNotchFilterConfigs().addAll(notchFilterConfigs);
      setBroadbandNotchFilterModuleConfig(broadbandNotchFilterModuleConfig);
      viewHolder.ifView(BroadbandPeakDetectionModuleView::updateChart);
   }

   private static Element frequencyToXml(float frequency, float value) {
      return DocumentHelper.createElement("entry")
            .addAttribute("frequency", Utils.toString(frequency))
            .addAttribute("value", Utils.toString(value));
   }

   private static Element spikeToXML(float kHz, float prominence, float peakValue, float sigma) {
      return DocumentHelper.createElement("spike")
            .addAttribute("frequency", Utils.toString(kHz))
            .addAttribute("prominence", Utils.toString(prominence))
            .addAttribute("peakValue", Utils.toString(peakValue))
            .addAttribute("sigma", Utils.toString(sigma));
   }

   Element specterToXml() {
      Element element = DocumentHelper.createElement("NoiseSpecter");
      for (Map.Entry<Integer, FrequencyPeakDetector.DetectionInfo> integerDetectionInfoEntry : channelToFrequencyResponse.entrySet()) {
         int channel = integerDetectionInfoEntry.getKey();
         Element channelElement = DocumentHelper.createElement("channel");
         channelElement.addAttribute("channelIndex", Utils.toString(channel));
         element.add(channelElement);
         float firstFrequency = integerDetectionInfoEntry.getValue().firstFrequency();
         float deltaFrequency = integerDetectionInfoEntry.getValue().deltaFrequency();
         OnlineAverageAndVariance value = integerDetectionInfoEntry.getValue().averageAndVariance();
         float[] means = Arrays.copyOf(value.getMeans(), value.getMeans().length);
         float[] var = value.getVar();
         float[] std = new float[var.length];
         int j = 0;
         for (float v : var) {
            std[j] = (float) Math.sqrt(v);
            j++;
         }
         // store the spikes (frequency, peak value, height(prominence), width (converted to sigma in normal distribution))
         for (FrequencyPeakDetector.PeakPlotInfo peakPlotInfo : integerDetectionInfoEntry.getValue().peakPlotInfos()) {
            int i = peakPlotInfo.peakIndex();
            float kHz = (firstFrequency + i * deltaFrequency) / 1000;
            float peakValue = means[i] + std[i]; // adding std
            float prominence = peakPlotInfo.prominenceData().prominence();
            float width = peakPlotInfo.widthKHz();
            float sigma = 0.5f * width / (float) Math.sqrt(-2 * Math.log(1 - peakWidthScalingHeight.getFloatValue()));
            channelElement.add(spikeToXML(kHz, prominence, peakValue, sigma));
         }

         //ArrayMath.map(means, PowerData::logSvToSv);
         int i = 0;
         for (float mean : means) {
            // mean + standard deviation implies approx 85% of the noise values are below this value
            float kHz = (firstFrequency + i * deltaFrequency) / 1000;
            channelElement.add(frequencyToXml(kHz, mean));
            i++;
         }
      }

      return element;
   }

   BroadbandNotchFilterModuleConfig getBroadbandNotchFilterModuleConfig() {
      return getInterpretationSettings().getBroadbandNotchFilterModuleConfig();
   }

   void setBroadbandNotchFilterModuleConfig(BroadbandNotchFilterModuleConfig config) {
      getInterpretationSettings().setBroadbandNotchFilterModuleConfig(config);
   }
}
