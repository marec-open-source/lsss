package no.imr.lsss.modules.broadband.sv;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.region.Region;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.export.ExportFile;
import no.imr.lsss.framework.export.ExportUtils;
import no.imr.lsss.framework.export.StreamingExporter;
import no.imr.lsss.framework.export.pojo.ExportInfo;
import no.imr.lsss.modules.broadband.BroadbandChannelInfoAccumulator;
import no.imr.tools.ProgressHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.Listener;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.misc.JsonWriter;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Exports broadband sample data.
 */
public final class BroadbandSampleDataExporter extends StreamingExporter {
   private final BooleanParameter allFrequencies = new BooleanParameter(
         new Name("AllFrequencies", "All frequencies"),
         false);

   public BroadbandSampleDataExporter(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("BroadbandSampleData", "BB sample data"), "Broadband sample data for selected regions");
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

      ExportFile exportFile = new ExportFile("BroadbandSampleData", ".json");
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

      List<Integer> channels = allFrequencies.getBooleanValue()
            ? IntStream.rangeClosed(1, dataFileSet.getRawFileConfiguration().getTransducerCount()).boxed().toList()
            : List.of(getLSSS().getInterpretationSettings().getChannel());

      try (JsonGenerator json = objectWriter.createGenerator(out)) {
         json.writeStartObject();

         json.writePOJOProperty("info", getExportInfo());

         json.writeName("regions");
         json.writeStartArray();
         BroadbandChannelInfoAccumulator channelInfoAccumulator = new BroadbandChannelInfoAccumulator();
         List<Region> selectedRegions = getLSSS().getRegionManager().getSelectedRegions();
         long pingsToLoad = selectedRegions.stream().mapToLong(region -> getVisiblePingIndexes(List.of(region)).size()).sum();
         int numberOfPasses = 1 + 2 * channels.size(); // Initial pass for metadata, then for each channel for amplitude and phase
         Listener progressListener = progressHandler.asCountingListener(numberOfPasses * pingsToLoad);
         for (Region region : selectedRegions) {
            writeRegion(json, dataFileSet, region, channels, channelInfoAccumulator, asyncHandle, progressListener);
            if (asyncHandle.isCancelled()) {
               return;
            }
         }
         json.writeEndArray();

         json.writePOJOProperty("channelInfo", channelInfoAccumulator.getChannelInfos());

         json.writeEndObject();
      }
   }

   private ExportInfo getExportInfo() {
      ExportInfo exportInfo = new ExportInfo("2.8.0", getName().persistentName());

      exportInfo.parameters.put(allFrequencies.getPersistentName(), allFrequencies.getValue());

      // exportInfo.units.put(allFrequencies.getPersistentName(), "");
      exportInfo.units.put("objectNumber", "1");
      // exportInfo.units.put("labels", "");
      // exportInfo.units.put("scrutiny", "");
      // exportInfo.units.put("assignment", "");
      // exportInfo.units.put("id", "");
      // exportInfo.units.put("times", "");
      exportInfo.units.put("minRange", "m");
      exportInfo.units.put("maxRange", "m");
      exportInfo.units.put("sampleDistance", "m");
      exportInfo.units.put("re", "W");
      exportInfo.units.put("im", "V A");
      BroadbandChannelInfoAccumulator.addUnits(exportInfo.units);

      exportInfo.comments.add("If a region spans more than one depth range for a given ping then the enclosing depth range is used.");

      return exportInfo;
   }

   private void writeRegion(JsonGenerator json, DataFileSet dataFileSet, Region region, List<Integer> channels, BroadbandChannelInfoAccumulator channelInfoAccumulator, AsyncHandle asyncHandle, Listener progressListener) {
      PingRange exportablePingRange = region.getPingRange().intersection(getLSSS().getInterpretationSettings().getPingRange());
      Map<Integer, ChannelMetadata> channelToMetadata = new HashMap<>();
      channels.forEach(channel -> channelToMetadata.put(channel, new ChannelMetadata()));
      for (PingIndex pingIndex : dataFileSet.getPingIndices(exportablePingRange)) {
         progressListener.listen();
         Ping ping = dataFileSet.getPing(pingIndex);
         if (asyncHandle.isCancelled()) {
            return;
         }
         for (int channel : channels) {
            BroadbandData broadbandData = ping.getBroadbandData(channel);
            if (broadbandData == null) {
               continue;
            }
            channelInfoAccumulator.accumulate(broadbandData);
            FloatRange depthRange = getLSSS().getRegionManager().getDepthRangesForChannel(region, ping, channel).getBoundingRange();
            if (depthRange.isEmpty()) {
               continue;
            }
            FloatRange rangeRange = FloatRange.of(broadbandData.depthToRange(depthRange.min()), broadbandData.depthToRange(depthRange.max()));
            ChannelMetadata channelMetadata = channelToMetadata.get(channel);
            channelMetadata.add(pingIndex, broadbandData, rangeRange);
         }
      }
      channelToMetadata.values().forEach(ChannelMetadata::end);

      JsonWriter jsonWriter = new JsonWriter(json);
      jsonWriter.writeObject(() -> {
         json.writeNumberProperty("objectNumber", region.getObjectNumber());
         jsonWriter.writeArrayField("labels", region.getLabels(), json::writeString);
         json.writePOJOProperty("scrutiny", ExportUtils.makeScrutiny(getLSSS(), channels, region.getInterpretation()));
         jsonWriter.writeArrayField("channels", channels, channel -> {
            ChannelMetadata channelMetadata = channelToMetadata.get(channel);
            if (channelMetadata.blocks.isEmpty()) {
               return;
            }
            jsonWriter.writeObject(() -> {
               json.writeStringProperty("id", channelMetadata.channelId);
               json.writeNumberProperty("nominalFrequency", channelMetadata.nominalFrequency);
               json.writeNumberProperty("minRange", Utils.round(channelMetadata.rangeRange.min(), 100));
               json.writeNumberProperty("maxRange", Utils.round(channelMetadata.rangeRange.max(), 100));
               jsonWriter.writeArrayField("blocks", channelMetadata.blocks, block -> {
                  jsonWriter.writeObject(() -> {
                     json.writeNumberProperty("sampleDistance", block.sampleDistance);

                     jsonWriter.writeArrayField("times", block.pingIndices,
                           pingIndex -> json.writeString(pingIndex.getInstant().toString()));

                     jsonWriter.writeArrayField("re", block.pingIndices, pingIndex -> {
                        progressListener.listen();
                        Ping ping = dataFileSet.getPing(pingIndex);
                        if (asyncHandle.isCancelled()) {
                           return;
                        }
                        BroadbandData broadbandData = ping.getBroadbandData(channel);
                        if (broadbandData == null) {
                           return;
                        }

                        ComplexArray pulseCompressedSignal = broadbandData.getAveragePulseCompressedSignal();

                        jsonWriter.writeArray(() -> {
                           int sampleIndexBegin = broadbandData.rangeToSampleIndex(channelMetadata.rangeRange.min());
                           int sampleIndexEnd = broadbandData.rangeToSampleIndex(channelMetadata.rangeRange.max());

                           int clampedSampleIndexBegin = Math.clamp(sampleIndexBegin, 0, broadbandData.getCount());
                           int clampedSampleIndexEnd = Math.clamp(sampleIndexEnd, 0, broadbandData.getCount());

                           for (int i = sampleIndexBegin; i < clampedSampleIndexBegin; i++) {
                              json.writeNumber(0);
                           }
                           for (int i = clampedSampleIndexBegin; i < clampedSampleIndexEnd; i++) {
                              json.writeNumber(Utils.roundToNumberOfDigits(pulseCompressedSignal.re(i), 6));
                           }
                           for (int i = clampedSampleIndexEnd; i < sampleIndexEnd; i++) {
                              json.writeNumber(0);
                           }
                        });
                     });
                     jsonWriter.writeArrayField("im", block.pingIndices, pingIndex -> {
                        progressListener.listen();
                        Ping ping = dataFileSet.getPing(pingIndex);
                        if (asyncHandle.isCancelled()) {
                           return;
                        }
                        BroadbandData broadbandData = ping.getBroadbandData(channel);
                        if (broadbandData == null) {
                           return;
                        }

                        ComplexArray pulseCompressedSignal = broadbandData.getAveragePulseCompressedSignal();

                        jsonWriter.writeArray(() -> {
                           int sampleIndexBegin = broadbandData.rangeToSampleIndex(channelMetadata.rangeRange.min());
                           int sampleIndexEnd = broadbandData.rangeToSampleIndex(channelMetadata.rangeRange.max());

                           int clampedSampleIndexBegin = Math.clamp(sampleIndexBegin, 0, broadbandData.getCount());
                           int clampedSampleIndexEnd = Math.clamp(sampleIndexEnd, 0, broadbandData.getCount());

                           for (int i = sampleIndexBegin; i < clampedSampleIndexBegin; i++) {
                              json.writeNumber(0);
                           }
                           for (int i = clampedSampleIndexBegin; i < clampedSampleIndexEnd; i++) {
                              json.writeNumber(Utils.roundToNumberOfDigits(pulseCompressedSignal.im(i), 6));
                           }
                           for (int i = clampedSampleIndexEnd; i < sampleIndexEnd; i++) {
                              json.writeNumber(0);
                           }
                        });
                     });
                  });
               });
            });
         });
      });
   }

   private static final class ChannelMetadata {
      private FloatRange rangeRange = FloatRange.EMPTY_RANGE;
      private String channelId = "";
      private float nominalFrequency;
      private final List<Block> blocks = new ArrayList<>();
      private @Nullable Block currentBlock;

      private ChannelMetadata() {
      }

      private void add(PingIndex pingIndex, BroadbandData broadbandData, FloatRange pingRangeRange) {
         rangeRange = rangeRange.union(pingRangeRange);
         float sampleDistance = broadbandData.getSampleDistance();
         if (currentBlock == null) {
            channelId = broadbandData.getTransducer().getChannelId();
            nominalFrequency = broadbandData.getTransducer().getFrequency();
            currentBlock = new Block(sampleDistance);
         } else if (currentBlock.sampleDistance != sampleDistance) {
            blocks.add(currentBlock);
            currentBlock = new Block(sampleDistance);
         }
         currentBlock.pingIndices.add(pingIndex);
      }

      private void end() {
         if (currentBlock != null) {
            blocks.add(currentBlock);
            currentBlock = null;
         }
      }

      private static final class Block {
         private final float sampleDistance;
         private final List<PingIndex> pingIndices = new ArrayList<>();

         private Block(float sampleDistance) {
            this.sampleDistance = sampleDistance;
         }
      }
   }
}
