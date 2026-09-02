package no.imr.lsss.modules.broadband.sv;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.export.ExportFile;
import no.imr.lsss.framework.export.StreamingExporter;
import no.imr.lsss.framework.export.pojo.ExportInfo;
import no.imr.lsss.modules.broadband.BroadbandChannelInfoAccumulator;
import no.imr.tools.ProgressHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.Listener;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.math.MathUtils;
import no.imr.tools.misc.JsonWriter;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
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
 * Exports broadband sample data around bottom.
 */
public final class BroadbandBottomDataExporter extends StreamingExporter {
   private final BooleanParameter allFrequencies = new BooleanParameter(
         new Name("AllFrequencies", "All frequencies"),
         false);

   private final FloatParameter deltaAbove = new FloatParameter(
         new Name("DeltaAbove", "Delta above"),
         1, Unit.METER,
         "Start range above bottom");

   private final FloatParameter deltaBelow = new FloatParameter(
         new Name("DeltaBelow", "Delta below"),
         1, Unit.METER,
         "End range below bottom");

   public BroadbandBottomDataExporter(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("BroadbandBottomData", "BB bottom data"), "Broadband sample data around bottom for current ping range");

      deltaAbove.subscribe(above -> deltaBelow.setAtLeastTo(-above));
      deltaBelow.subscribe(below -> deltaAbove.setAtLeastTo(-below));
   }

   @Override
   protected List<? extends BaseParameter<?>> getSettingsParameters() {
      return List.of(allFrequencies, deltaAbove, deltaBelow);
   }

   @Override
   protected void doExport(AsyncHandle asyncHandle, ProgressHandler progressHandler) throws IOException {
      PingRange pingRange = getLSSS().getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return;
      }
      DataFileSet dataFileSet = getLSSS().getInterpretationSettings().getDataFileSet();

      ExportFile exportFile = new ExportFile("BroadbandBottomData", ".json");
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

         BroadbandChannelInfoAccumulator channelInfoAccumulator = new BroadbandChannelInfoAccumulator();
         long pingsToLoad = pingRange.getPingCount();
         int numberOfPasses = 1 + 2 * channels.size(); // Initial pass for metadata, then for each channel for re and im
         Listener progressListener = progressHandler.asCountingListener(numberOfPasses * pingsToLoad);
         writePingRange(json, dataFileSet, pingRange, channels, channelInfoAccumulator, asyncHandle, progressListener);

         json.writePOJOProperty("channelInfo", channelInfoAccumulator.getChannelInfos());

         json.writeEndObject();
      }
   }

   private ExportInfo getExportInfo() {
      ExportInfo exportInfo = new ExportInfo("2.1.0", getName().persistentName());

      exportInfo.parameters.put(allFrequencies.getPersistentName(), allFrequencies.getValue());
      exportInfo.parameters.put(deltaAbove.getPersistentName(), deltaAbove.getValue());
      exportInfo.parameters.put(deltaBelow.getPersistentName(), deltaBelow.getValue());

      // exportInfo.units.put(allFrequencies.getPersistentName(), "");
      exportInfo.units.put(deltaAbove.getPersistentName(), "m");
      exportInfo.units.put(deltaBelow.getPersistentName(), "m");
      exportInfo.units.put("sampleDistance", "m");
      // exportInfo.units.put("times", "");
      exportInfo.units.put("depths", "m");
      exportInfo.units.put("re", "W");
      exportInfo.units.put("im", "V A");
      BroadbandChannelInfoAccumulator.addUnits(exportInfo.units);

      return exportInfo;
   }

   private void writePingRange(JsonGenerator json, DataFileSet dataFileSet, PingRange pingRange, List<Integer> channels, BroadbandChannelInfoAccumulator channelInfoAccumulator, AsyncHandle asyncHandle, Listener progressListener) {
      Map<Integer, ChannelMetadata> channelToMetadata = new HashMap<>();
      channels.forEach(channel -> channelToMetadata.put(channel, new ChannelMetadata()));
      for (PingIndex pingIndex : dataFileSet.getPingIndices(pingRange)) {
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
            ChannelMetadata channelMetadata = channelToMetadata.get(channel);
            channelMetadata.add(pingIndex, broadbandData, (float) ping.getBot0Datagram().getChannelDepths()[channel - 1]);
         }
      }
      channelToMetadata.values().forEach(ChannelMetadata::end);

      JsonWriter jsonWriter = new JsonWriter(json);
      jsonWriter.writeArrayField("channels", channels, channel -> {
         ChannelMetadata channelMetadata = channelToMetadata.get(channel);
         if (channelMetadata.blocks.isEmpty()) {
            return;
         }
         jsonWriter.writeObject(() -> {
            RawFileTransducer transducer = dataFileSet.getRawFileConfiguration().getTransducers().get(channel - 1);
            json.writeStringProperty("id", transducer.getChannelId());
            json.writeNumberProperty("nominalFrequency", transducer.getFrequency());
            jsonWriter.writeArrayField("blocks", channelMetadata.blocks, block -> {
               jsonWriter.writeObject(() -> {
                  json.writeNumberProperty("sampleDistance", block.sampleDistance);

                  jsonWriter.writeArrayField("times", block.pingIndices,
                        pingIndex -> json.writeString(pingIndex.getInstant().toString()));

                  jsonWriter.writeArrayField("depths", block.bottomDepths,
                        bottomDepth -> json.writeNumber(MathUtils.round(bottomDepth, 1000)));

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

                     jsonWriter.writeArray(() -> {
                        FloatRange depthRange = getDepthRange(ping, channel);

                        ComplexArray pulseCompressedSignal = broadbandData.getAveragePulseCompressedSignal();

                        int sampleIndexBegin = broadbandData.depthToSampleIndex(depthRange.min());
                        int sampleIndexEnd = broadbandData.depthToSampleIndex(depthRange.max());

                        int clampedSampleIndexBegin = Math.clamp(sampleIndexBegin, 0, broadbandData.getCount());
                        int clampedSampleIndexEnd = Math.clamp(sampleIndexEnd, 0, broadbandData.getCount());

                        for (int i = sampleIndexBegin; i < clampedSampleIndexBegin; i++) {
                           json.writeNumber(0);
                        }
                        for (int i = clampedSampleIndexBegin; i < clampedSampleIndexEnd; i++) {
                           json.writeNumber(MathUtils.roundToNumberOfDigits(pulseCompressedSignal.re(i), 6));
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

                     jsonWriter.writeArray(() -> {
                        FloatRange depthRange = getDepthRange(ping, channel);

                        ComplexArray pulseCompressedSignal = broadbandData.getAveragePulseCompressedSignal();

                        int sampleIndexBegin = broadbandData.depthToSampleIndex(depthRange.min());
                        int sampleIndexEnd = broadbandData.depthToSampleIndex(depthRange.max());

                        int clampedSampleIndexBegin = Math.clamp(sampleIndexBegin, 0, broadbandData.getCount());
                        int clampedSampleIndexEnd = Math.clamp(sampleIndexEnd, 0, broadbandData.getCount());

                        for (int i = sampleIndexBegin; i < clampedSampleIndexBegin; i++) {
                           json.writeNumber(0);
                        }
                        for (int i = clampedSampleIndexBegin; i < clampedSampleIndexEnd; i++) {
                           json.writeNumber(MathUtils.roundToNumberOfDigits(pulseCompressedSignal.im(i), 6));
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
   }

   private FloatRange getDepthRange(Ping ping, int channel) {
      float bottomDepth = (float) ping.getBot0Datagram().getChannelDepths()[channel - 1];
      return FloatRange.of(bottomDepth - deltaAbove.getFloatValue(), bottomDepth + deltaBelow.getFloatValue());
   }

   private static final class ChannelMetadata {
      private final List<Block> blocks = new ArrayList<>();
      private @Nullable Block currentBlock;

      private ChannelMetadata() {
      }

      private void add(PingIndex pingIndex, BroadbandData broadbandData, float bottomDepth) {
         float sampleDistance = broadbandData.getSampleDistance();
         if (currentBlock == null) {
            currentBlock = new Block(sampleDistance);
         } else if (currentBlock.sampleDistance != sampleDistance) {
            blocks.add(currentBlock);
            currentBlock = new Block(sampleDistance);
         }
         currentBlock.pingIndices.add(pingIndex);
         currentBlock.bottomDepths.add(bottomDepth);
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
         private final List<Float> bottomDepths = new ArrayList<>();

         private Block(float sampleDistance) {
            this.sampleDistance = sampleDistance;
         }
      }
   }
}
