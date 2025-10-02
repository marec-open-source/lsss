package no.imr.lsss.modules.broadband.ts;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.region.Region;
import no.imr.korona.util.ts.TSDetector;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.export.ExportFile;
import no.imr.lsss.framework.export.StreamingExporter;
import no.imr.lsss.framework.export.pojo.ExportInfo;
import no.imr.lsss.modules.broadband.BroadbandChannelInfoAccumulator;
import no.imr.lsss.modules.broadband.pojo.ts.BroadbandTsExportPerChannel;
import no.imr.lsss.modules.broadband.pojo.ts.BroadbandTsExportPerPing;
import no.imr.lsss.modules.broadband.pojo.ts.BroadbandTsExportPerTarget;
import no.imr.tools.ProgressHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.Listener;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.range.FloatRange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.IntStream;

public final class BroadbandTsExporter extends StreamingExporter {
   private final BooleanParameter allFrequencies = new BooleanParameter(
         new Name("AllFrequencies", "All frequencies"),
         false);

   public BroadbandTsExporter(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("BroadbandTS", "BB TS(f)"), "Broadband TS(f) for targets in selected regions");
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

      ExportFile exportFile = new ExportFile("BroadbandTS", ".json");
      ExportFile cdsFile = createCdsFileWrapper();
      initExportFiles(pingRange, exportFile, cdsFile);
      writeModuleSetup(cdsFile, dataFileSet, pingRange);

      exportToStream(asyncHandle, progressHandler, exportFile);
   }

   @Override
   public void exportToStream(AsyncHandle asyncHandle, ProgressHandler progressHandler, OutputStream out, ObjectWriter objectWriter) throws IOException {
      PingRange pingRange = getLSSS().getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return;
      }
      DataFileSet dataFileSet = getLSSS().getInterpretationSettings().getDataFileSet();

      BroadbandTsModule broadbandTsModule = getLSSS().getModuleManager().getModule(BroadbandTsModule.class);

      List<Integer> channels = allFrequencies.getBooleanValue()
            ? IntStream.rangeClosed(1, dataFileSet.getRawFileConfiguration().getTransducerCount()).boxed().toList()
            : List.of(getLSSS().getInterpretationSettings().getChannel());

      try (JsonGenerator json = objectWriter.createGenerator(out)) {
         json.writeStartObject();

         json.writeObjectField("info", getExportInfo(broadbandTsModule));

         json.writeFieldName("pings");
         json.writeStartArray();
         BroadbandChannelInfoAccumulator channelInfoAccumulator = new BroadbandChannelInfoAccumulator();
         List<Region> selectedRegions = getLSSS().getRegionManager().getSelectedRegions();
         List<PingIndex> pingIndexes = getVisiblePingIndexes(selectedRegions);
         Listener progressListener = progressHandler.asCountingListener(pingIndexes.size());
         for (PingIndex pingIndex : pingIndexes) {
            progressListener.listen();
            BroadbandTsExportPerPing perPing = makeExport(broadbandTsModule, channels, pingIndex, channelInfoAccumulator, asyncHandle);
            if (asyncHandle.isCancelled()) {
               return;
            }
            if (!perPing.channels.isEmpty()) {
               json.writeObject(perPing);
            }
         }
         json.writeEndArray();

         json.writeObjectField("channelInfo", channelInfoAccumulator.getChannelInfos());

         json.writeEndObject();
      }
   }

   private ExportInfo getExportInfo(BroadbandTsModule broadbandTsModule) {
      ExportInfo exportInfo = new ExportInfo("2.3.0", getName().persistentName());

      exportInfo.parameters.put(allFrequencies.getPersistentName(), allFrequencies.getValue());
      exportInfo.parameters.put(broadbandTsModule.targetExtentMode.getPersistentName(), broadbandTsModule.targetExtentMode.getValue());
      if (broadbandTsModule.targetExtentMode.getValue() == TargetExtentMode.MANUAL) {
         exportInfo.parameters.put(broadbandTsModule.manualTargetExtentSymmetrical.getPersistentName(), broadbandTsModule.manualTargetExtentSymmetrical.getValue());
         if (broadbandTsModule.manualTargetExtentSymmetrical.getValue()) {
            exportInfo.parameters.put(broadbandTsModule.manualTargetExtent.getPersistentName(), broadbandTsModule.manualTargetExtent.getValue());
         } else {
            exportInfo.parameters.put(broadbandTsModule.manualTargetExtentAbove.getPersistentName(), broadbandTsModule.manualTargetExtentAbove.getValue());
            exportInfo.parameters.put(broadbandTsModule.manualTargetExtentBelow.getPersistentName(), broadbandTsModule.manualTargetExtentBelow.getValue());
         }
      }
      exportInfo.parameters.put(broadbandTsModule.frequencyWindowing.getPersistentName(), broadbandTsModule.frequencyWindowing.getValue());
      exportInfo.parameters.put(broadbandTsModule.frequencyResolution.getPersistentName(), broadbandTsModule.frequencyResolution.getValue() * 1000);
      exportInfo.parameters.put(broadbandTsModule.minTS.getPersistentName(), broadbandTsModule.minTS.getValue());
      exportInfo.parameters.put(broadbandTsModule.maxGainCompensation.getPersistentName(), broadbandTsModule.maxGainCompensation.getValue());
      exportInfo.parameters.put(broadbandTsModule.minEchoLength.getPersistentName(), broadbandTsModule.minEchoLength.getValue());
      exportInfo.parameters.put(broadbandTsModule.maxEchoLength.getPersistentName(), broadbandTsModule.maxEchoLength.getValue());

      // exportInfo.units.put(allFrequencies.getPersistentName(), "");
      // exportInfo.units.put(broadbandTsModule.targetExtentMode.getPersistentName(), "");
      if (broadbandTsModule.targetExtentMode.getValue() == TargetExtentMode.MANUAL) {
         // exportInfo.units.put(broadbandTsModule.manualTargetExtentSymmetrical.getPersistentName(), "");
         if (broadbandTsModule.manualTargetExtentSymmetrical.getValue()) {
            exportInfo.units.put(broadbandTsModule.manualTargetExtent.getPersistentName(), "m");
         } else {
            exportInfo.units.put(broadbandTsModule.manualTargetExtentAbove.getPersistentName(), "m");
            exportInfo.units.put(broadbandTsModule.manualTargetExtentBelow.getPersistentName(), "m");
         }
      }
      exportInfo.units.put(broadbandTsModule.frequencyWindowing.getPersistentName(), "%");
      exportInfo.units.put(broadbandTsModule.frequencyResolution.getPersistentName(), "Hz");
      exportInfo.units.put(broadbandTsModule.minTS.getPersistentName(), "dB re 1 m^2");
      exportInfo.units.put(broadbandTsModule.maxGainCompensation.getPersistentName(), "dB");
      exportInfo.units.put(broadbandTsModule.minEchoLength.getPersistentName(), "1");
      exportInfo.units.put(broadbandTsModule.maxEchoLength.getPersistentName(), "1");
      exportInfo.units.put("number", "1");
      // exportInfo.units.put("time", "");
      // exportInfo.units.put("id", "");
      exportInfo.units.put("nominalFrequency", "Hz");
      exportInfo.units.put("minFrequency", "Hz");
      exportInfo.units.put("maxFrequency", "Hz");
      exportInfo.units.put("numFrequencies", "1");
      exportInfo.units.put("tsc", "dB re 1 m^2");
      exportInfo.units.put("range", "m");
      exportInfo.units.put("alongshipAngle", "degree");
      exportInfo.units.put("athwartshipAngle", "degree");
      BroadbandChannelInfoAccumulator.addUnits(exportInfo.units);

      exportInfo.comments.add("TS values are beam compensated.");
      exportInfo.comments.add("Range and angles are calculated at sample with peak value.");
      exportInfo.comments.add("Angles are calculated at nominal frequency.");

      return exportInfo;
   }

   private BroadbandTsExportPerPing makeExport(BroadbandTsModule broadbandTsModule, List<Integer> channels, PingIndex pingIndex, BroadbandChannelInfoAccumulator channelInfoAccumulator, AsyncHandle asyncHandle) {
      BroadbandTsExportPerPing perPing = new BroadbandTsExportPerPing(pingIndex);

      DataFileSet dataFileSet = getLSSS().getDataManager().getDataFileSet();
      Ping ping = dataFileSet.getPing(pingIndex);
      if (asyncHandle.isCancelled()) {
         return perPing;
      }

      TSDetector tsDetector = broadbandTsModule.createTSDetector();

      for (int channel : channels) {
         BroadbandData broadbandData = ping.getBroadbandData(channel);
         if (broadbandData == null) {
            continue;
         }

         channelInfoAccumulator.accumulate(broadbandData);

         RawFileTransducer transducer = ping.getRawFileConfiguration().getTransducers().get(channel - 1);
         BroadbandTsExportPerChannel perChannel = new BroadbandTsExportPerChannel(transducer.getChannelId());
         perChannel.nominalFrequency = transducer.getFrequency();

         for (Region region : getLSSS().getRegionManager().getSelectedRegions()) {
            List<FloatRange> depthRanges = getLSSS().getRegionManager().getDepthRangesForChannel(region, ping, channel).getFloatRanges();
            BroadbandTsChannelCache channelCache = new BroadbandTsChannelCache(ping, channel, depthRanges, tsDetector, broadbandTsModule);
            for (BroadbandTsData tsData : channelCache.getTsData()) {
               BroadbandTsExportPerTarget perTarget = new BroadbandTsExportPerTarget();
               perChannel.targets.add(perTarget);

               perChannel.minFrequency = tsData.frequencyRange().min();
               perChannel.maxFrequency = tsData.frequencyRange().max();
               perChannel.numFrequencies = tsData.values().length;

               perTarget.tsc = tsData.values();
               ArrayMath.round(perTarget.tsc, 100);
               perTarget.range = Utils.round(broadbandData.depthToRange(tsData.depth()), 1000);
               int peakIndex = broadbandData.depthToSampleIndex(tsData.depth());
               perTarget.alongshipAngle = Utils.round(broadbandData.getMechanicalAlongAngle(peakIndex, perChannel.nominalFrequency), 100);
               perTarget.athwartshipAngle = Utils.round(broadbandData.getMechanicalAthwartAngle(peakIndex, perChannel.nominalFrequency), 100);
            }
         }
         if (!perChannel.targets.isEmpty()) {
            perPing.channels.add(perChannel);
         }
      }
      return perPing;
   }
}
