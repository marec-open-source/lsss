package no.imr.lsss.modules.broadband.sv;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.region.Region;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.export.ExportFile;
import no.imr.lsss.framework.export.ExportUtils;
import no.imr.lsss.framework.export.StreamingExporter;
import no.imr.lsss.framework.export.pojo.ExportInfo;
import no.imr.lsss.framework.export.pojo.ExportScrutiny;
import no.imr.lsss.modules.broadband.BroadbandChannelInfoAccumulator;
import no.imr.lsss.modules.broadband.pojo.sv.BroadbandSvExportPerChannel;
import no.imr.lsss.modules.broadband.pojo.sv.BroadbandSvExportPerPing;
import no.imr.lsss.modules.broadband.pojo.sv.BroadbandSvExportPerRegion;
import no.imr.tools.ProgressHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.Listener;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.math.MathUtils;
import no.imr.tools.math.OnlineAverageAndVariance;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.range.FloatRange;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class BroadbandSvExporter extends StreamingExporter {
   private final BooleanParameter allFrequencies = new BooleanParameter(
         new Name("AllFrequencies", "All frequencies"),
         false);

   public BroadbandSvExporter(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("BroadbandSv", "BB Sv(f)"), "Broadband Sv(f) for selected regions");
   }

   @Override
   protected List<? extends BaseParameter<?>> getSettingsParameters() {
      return List.of(allFrequencies);
   }

   @Override
   protected void doExport(AsyncHandle asyncHandle, ProgressHandler progressHandler) throws IOException {
      PingRange pingRange = getLSSS().getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return;
      }
      DataFileSet dataFileSet = getLSSS().getInterpretationSettings().getDataFileSet();

      ExportFile exportFile = new ExportFile("BroadbandSv", ".json");
      ExportFile cdsFile = createCdsFileWrapper();
      initExportFiles(pingRange, exportFile, cdsFile);
      writeModuleSetup(cdsFile, dataFileSet, pingRange);

      exportToStream(asyncHandle, progressHandler, exportFile);
   }

   @Override
   public void exportToStream(AsyncHandle asyncHandle, ProgressHandler progressHandler, OutputStream out, ObjectWriter objectWriter) {
      PingRange pingRange = getLSSS().getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return;
      }
      DataFileSet dataFileSet = getLSSS().getInterpretationSettings().getDataFileSet();

      BroadbandSvModule broadbandSvModule = getLSSS().getModuleManager().getModule(BroadbandSvModule.class);

      List<Integer> channels = allFrequencies.getBooleanValue()
            ? IntStream.rangeClosed(1, dataFileSet.getRawFileConfiguration().getTransducerCount()).boxed().toList()
            : List.of(getLSSS().getInterpretationSettings().getChannel());

      try (JsonGenerator json = objectWriter.createGenerator(out)) {
         json.writeStartObject();

         json.writePOJOProperty("info", getExportInfo(broadbandSvModule));

         json.writeName("regions");
         json.writeStartArray();
         BroadbandChannelInfoAccumulator channelInfoAccumulator = new BroadbandChannelInfoAccumulator();
         List<Region> selectedRegions = getLSSS().getRegionManager().getSelectedRegions();
         long pingsToLoad = selectedRegions.stream().mapToLong(region -> getVisiblePingIndexes(List.of(region)).size()).sum();
         Listener progressListener = progressHandler.asCountingListener(pingsToLoad);
         for (Region region : selectedRegions) {
            BroadbandSvExportPerRegion perRegion = makeExport(broadbandSvModule, channels, region, channelInfoAccumulator, asyncHandle, progressListener);
            if (asyncHandle.isCancelled()) {
               return;
            }
            json.writePOJO(perRegion);
         }
         json.writeEndArray();

         json.writePOJOProperty("channelInfo", channelInfoAccumulator.getChannelInfos());

         json.writeEndObject();
      }
   }

   private ExportInfo getExportInfo(BroadbandSvModule broadbandSvModule) {
      ExportInfo exportInfo = new ExportInfo("2.8.0", getName().persistentName());

      exportInfo.parameters.put(allFrequencies.getPersistentName(), allFrequencies.getValue());
      exportInfo.parameters.put(broadbandSvModule.frequencyWindowing.getPersistentName(), broadbandSvModule.frequencyWindowing.getValue());
      exportInfo.parameters.put(broadbandSvModule.frequencyResolution.getPersistentName(), broadbandSvModule.frequencyResolution.getValue() * 1000);

      // exportInfo.units.put(allFrequencies.getPersistentName(), "");
      exportInfo.units.put(broadbandSvModule.frequencyWindowing.getPersistentName(), "%");
      exportInfo.units.put(broadbandSvModule.frequencyResolution.getPersistentName(), "Hz");
      exportInfo.units.put("objectNumber", "1");
      // exportInfo.units.put("labels", "");
      // exportInfo.units.put("scrutiny", "");
      // exportInfo.units.put("assignment", "");
      exportInfo.units.put("number", "1");
      // exportInfo.units.put("time", "");
      // exportInfo.units.put("id", "");
      exportInfo.units.put("nominalFrequency", "Hz");
      exportInfo.units.put("minFrequency", "Hz");
      exportInfo.units.put("maxFrequency", "Hz");
      exportInfo.units.put("numFrequencies", "1");
      exportInfo.units.put("sv", "dB");
      exportInfo.units.put("depth", "m");
      // exportInfo.units.put("error", "");
      BroadbandChannelInfoAccumulator.addUnits(exportInfo.units);

      exportInfo.comments.add("Depth is for the centre.");
      exportInfo.comments.add("If a region spans more than one depth range for a given ping then Sv is computed for each depth range and then averaged.");
      exportInfo.comments.add("If a depth range is too short to compute Sv then the error property is set and that depth range is ignored.");

      return exportInfo;
   }

   private BroadbandSvExportPerRegion makeExport(BroadbandSvModule broadbandSvModule, List<Integer> channels, Region region, BroadbandChannelInfoAccumulator channelInfoAccumulator, AsyncHandle asyncHandle, Listener progressListener) {
      ExportScrutiny scrutiny = ExportUtils.makeScrutiny(getLSSS(), channels, region.getInterpretation());
      BroadbandSvExportPerRegion perRegion = new BroadbandSvExportPerRegion(region.getObjectNumber(), region.getLabels(), scrutiny);

      Map<Integer, Accumulator> accumulators = new HashMap<>();
      DataFileSet dataFileSet = getLSSS().getDataManager().getDataFileSet();
      List<PingIndex> pingIndexes = getVisiblePingIndexes(List.of(region));
      for (PingIndex pingIndex : pingIndexes) {
         progressListener.listen();
         Ping ping = dataFileSet.getPing(pingIndex);
         if (asyncHandle.isCancelled()) {
            break;
         }
         BroadbandSvExportPerPing perPing = new BroadbandSvExportPerPing(pingIndex);
         perRegion.pings.add(perPing);
         for (int channel : channels) {
            BroadbandData broadbandData = ping.getBroadbandData(channel);
            if (broadbandData == null) {
               continue;
            }

            channelInfoAccumulator.accumulate(broadbandData);

            List<FloatRange> depthRanges = getLSSS().getRegionManager().getDepthRangesForChannel(region, ping, channel).getFloatRanges();
            if (depthRanges.isEmpty()) {
               continue;
            }

            RawFileTransducer transducer = ping.getRawFileConfiguration().getTransducers().get(channel - 1);
            BroadbandSvExportPerChannel perChannel = new BroadbandSvExportPerChannel(transducer.getChannelId());
            perPing.channels.add(perChannel);
            perChannel.nominalFrequency = transducer.getFrequency();

            BroadbandSvChannelCache channelCache = BroadbandSvChannelCache.from(ping, channel, depthRanges, broadbandSvModule);
            if (!channelCache.tooSmallDepthRanges().isEmpty()) {
               perChannel.error = "Too small depth ranges: " + channelCache.tooSmallDepthRanges().stream()
                     .map(depthRange -> depthRange.roundToMultipleOf(0.001))
                     .map(FloatRange::toString)
                     .collect(Collectors.joining(", "));
            }
            if (channelCache.svData().isEmpty()) {
               continue;
            }

            Average average = computeAverageValues(channelCache.svData());

            Accumulator accumulator = accumulators.get(channel);
            if (accumulator == null) {
               accumulator = new Accumulator(transducer.getChannelId(), perChannel.nominalFrequency, channelCache.frequencyRange(), average.sv.length);
               accumulators.put(channel, accumulator);
            }
            accumulator.onlineAverageAndVariance.update(average.sv, average.weight);

            perChannel.minFrequency = channelCache.frequencyRange().min();
            perChannel.maxFrequency = channelCache.frequencyRange().max();
            perChannel.numFrequencies = average.sv.length;
            perChannel.sv = average.sv;
            ArrayMath.map(perChannel.sv, PowerData::svToLogSv);
            ArrayMath.round(perChannel.sv, 100);
            perChannel.depth = MathUtils.round(average.representativeDepth, 1000);
         }
      }

      for (int channel : channels) {
         Accumulator accumulator = accumulators.get(channel);
         if (accumulator == null) {
            continue;
         }
         perRegion.averages.add(accumulator.createExport());
      }

      return perRegion;
   }

   private static Average computeAverageValues(List<BroadbandSvData> svDatas) {
      double weightSum = 0;
      double depthSum = 0;
      float[] sum = new float[svDatas.getFirst().sv().length];
      for (BroadbandSvData svData : svDatas) {
         float weight = svData.depthRange().getSize();
         weightSum += weight;
         depthSum += svData.depthRange().getCenter() * weight;
         float[] sv = svData.sv();
         for (int i = 0; i < sum.length; i++) {
            sum[i] += sv[i] * weight;
         }
      }
      ArrayMath.divide(sum, (float) weightSum);
      return new Average(sum, (float) (depthSum / weightSum), (float) weightSum);
   }

   private static final class Accumulator {
      private final BroadbandSvExportPerChannel perChannel;
      private final OnlineAverageAndVariance onlineAverageAndVariance;

      private Accumulator(String channelId, float nominalFrequency, FloatRange frequencyRange, int length) {
         perChannel = new BroadbandSvExportPerChannel(channelId);
         perChannel.nominalFrequency = nominalFrequency;
         perChannel.minFrequency = frequencyRange.min();
         perChannel.maxFrequency = frequencyRange.max();
         perChannel.numFrequencies = length;

         onlineAverageAndVariance = new OnlineAverageAndVariance(length);
      }

      private BroadbandSvExportPerChannel createExport() {
         perChannel.sv = onlineAverageAndVariance.getMeans();
         ArrayMath.map(perChannel.sv, PowerData::svToLogSv);
         ArrayMath.round(perChannel.sv, 100);
         return perChannel;
      }
   }

   private record Average(float[] sv, float representativeDepth, float weight) {
   }
}
