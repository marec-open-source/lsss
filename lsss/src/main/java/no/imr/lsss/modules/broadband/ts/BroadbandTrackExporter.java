package no.imr.lsss.modules.broadband.ts;

import com.google.common.collect.ImmutableSet;
import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.impl.StationaryPositionFunction;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.region.Region;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.export.ExportFile;
import no.imr.lsss.framework.export.StreamingExporter;
import no.imr.lsss.framework.export.pojo.ExportInfo;
import no.imr.lsss.modules.broadband.BroadbandChannelInfoAccumulator;
import no.imr.lsss.modules.broadband.pojo.tracks.BroadbandTrackExportPerChannel;
import no.imr.lsss.modules.broadband.pojo.tracks.BroadbandTrackExportPerTrack;
import no.imr.lsss.modules.korona.tracking.TrackBorder;
import no.imr.lsss.modules.korona.tracking.TrackId;
import no.imr.lsss.modules.korona.tracking.TrackInfo;
import no.imr.lsss.modules.korona.tracking.TrackInfoModule;
import no.imr.tools.ProgressHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.Listener;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.range.RangeSet;
import no.marec.lsss.api.util.GeoPoint;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Broadband track exporter.
 */
public final class BroadbandTrackExporter extends StreamingExporter {
   private final BooleanParameter allFrequencies = new BooleanParameter(
         new Name("AllFrequencies", "All frequencies"),
         false);

   public BroadbandTrackExporter(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("BroadbandTracks", "BB TS(f) tracks"), "Broadband TS(f) for tracks in selected regions");
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

      ExportFile exportFile = new ExportFile("BroadbandTracks", ".json");
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

      BroadbandTsModule broadbandTsModule = getLSSS().getModuleManager().getModule(BroadbandTsModule.class);
      TrackInfoModule trackInfoModule = getLSSS().getModuleManager().getModule(TrackInfoModule.class);
      trackInfoModule.waitForTrackInfoLoader(asyncHandle);

      List<Integer> channels = allFrequencies.getBooleanValue()
            ? IntStream.rangeClosed(1, dataFileSet.getRawFileConfiguration().getTransducerCount()).boxed().toList()
            : List.of(getLSSS().getInterpretationSettings().getChannel());

      try (JsonGenerator json = objectWriter.createGenerator(out)) {
         json.writeStartObject();

         json.writePOJOProperty("info", getExportInfo(broadbandTsModule));

         json.writeName("tracks");
         json.writeStartArray();

         Map<TrackId, TrackAccumulator> trackMap = new HashMap<>();

         int channel = getLSSS().getInterpretationSettings().getChannel();
         BroadbandChannelInfoAccumulator channelInfoAccumulator = new BroadbandChannelInfoAccumulator();
         List<Region> selectedRegions = getLSSS().getRegionManager().getSelectedRegions();
         RangeSet<PingIndex> activePingRangeSet = getVisiblePingRangeSet(selectedRegions);
         List<PingIndex> pingIndexes = getVisiblePingIndexes(selectedRegions);
         Listener progressListener = progressHandler.asCountingListener(pingIndexes.size());
         for (PingIndex pingIndex : pingIndexes) {
            if (asyncHandle.isCancelled()) {
               return;
            }
            progressListener.listen();
            Ping ping = dataFileSet.getPing(pingIndex);
            FloatRangeSet depthRanges = getDepthRanges(selectedRegions, ping, channel);

            channels.stream()
                  .map(ping::getBroadbandData)
                  .filter(Objects::nonNull)
                  .forEach(channelInfoAccumulator::accumulate);

            trackInfoModule.getTrackEditing().getTrackBorders(ping, channel).forEach(trackBorder -> {
               TrackId trackId = trackBorder.trackId();
               TrackAccumulator trackAccumulator = trackMap.get(trackId);
               if (trackAccumulator == null) {
                  ImmutableSet<String> labels = trackInfoModule.getTrackLabelling().getLabels(trackId);
                  TrackInfo trackInfo = trackInfoModule.getTrackInfos().get(trackId);
                  if (trackInfo == null || !activePingRangeSet.containsAll(trackInfo.pingRange())) {
                     return;
                  }
                  trackAccumulator = new TrackAccumulator(labels, trackInfo.pingRange(), ping.getPingConfiguration(), channels);
                  trackMap.put(trackId, trackAccumulator);
               }
               trackAccumulator.accumulate(ping, trackBorder, depthRanges, broadbandTsModule);
               if (pingIndex.getPingNumber() == trackAccumulator.pingRange.end().getPingNumber() - 1) {
                  trackMap.remove(trackId);
                  if (trackAccumulator.isExportable()) {
                     json.writePOJO(trackAccumulator.perTrack);
                  }
               }
            });
         }

         json.writeEndArray();

         json.writePOJOProperty("channelInfo", channelInfoAccumulator.getChannelInfos());

         json.writeEndObject();
      }
   }

   private ExportInfo getExportInfo(BroadbandTsModule broadbandTsModule) {
      ExportInfo exportInfo = new ExportInfo("2.3.0", getName().persistentName());

      exportInfo.parameters.put(allFrequencies.getPersistentName(), allFrequencies.getValue());
      exportInfo.parameters.put(broadbandTsModule.frequencyWindowing.getPersistentName(), broadbandTsModule.frequencyWindowing.getValue());
      exportInfo.parameters.put(broadbandTsModule.frequencyResolution.getPersistentName(), broadbandTsModule.frequencyResolution.getValue() * 1000);

      // exportInfo.units.put(allFrequencies.getPersistentName(), "");
      exportInfo.units.put(broadbandTsModule.frequencyWindowing.getPersistentName(), "%");
      exportInfo.units.put(broadbandTsModule.frequencyResolution.getPersistentName(), "Hz");
      exportInfo.units.put("pingNumber", "1");
      // exportInfo.units.put("time", "");
      // exportInfo.units.put("id", "");
      exportInfo.units.put("nominalFrequency", "Hz");
      exportInfo.units.put("minFrequency", "Hz");
      exportInfo.units.put("maxFrequency", "Hz");
      exportInfo.units.put("numFrequencies", "1");
      exportInfo.units.put("peakRange", "m");
      exportInfo.units.put("fftDistanceBefore", "m");
      exportInfo.units.put("fftDistanceAfter", "m");
      exportInfo.units.put("tsc", "dB re 1 m^2");
      exportInfo.units.put("minDepth", "m");
      exportInfo.units.put("maxDepth", "m");
      exportInfo.units.put("peakDepth", "m");
      exportInfo.units.put("alongshipAngle", "degree");
      exportInfo.units.put("athwartshipAngle", "degree");
      exportInfo.units.put("longitude", "degrees_east");
      exportInfo.units.put("latitude", "degrees_north");
      exportInfo.units.put("heading", "degrees_true");
      exportInfo.units.put("heave", "m");
      exportInfo.units.put("pitch", "degree");
      exportInfo.units.put("roll", "degree");
      exportInfo.units.put("x", "m");
      exportInfo.units.put("y", "m");
      exportInfo.units.put("z", "m");
      BroadbandChannelInfoAccumulator.addUnits(exportInfo.units);

      exportInfo.comments.add("TS values are beam compensated.");
      exportInfo.comments.add("Angles are calculated at sample with peak value at nominal frequency.");

      return exportInfo;
   }

   private static final class TrackAccumulator {
      private final BroadbandTrackExportPerTrack perTrack = new BroadbandTrackExportPerTrack();
      private final PingRange pingRange;
      private final List<Integer> channels;
      private boolean outsideDepthRanges;

      private TrackAccumulator(ImmutableSet<String> labels, PingRange pingRange, PingConfiguration pingConfiguration, List<Integer> channels) {
         this.channels = channels;
         perTrack.labels = labels;
         this.pingRange = pingRange;
         for (int channel : channels) {
            RawFileTransducer transducer = pingConfiguration.getRawFileConfiguration().getTransducers().get(channel - 1);
            perTrack.channels.add(new BroadbandTrackExportPerChannel(transducer));
         }
      }

      private void accumulate(Ping ping, TrackBorder trackBorder, FloatRangeSet depthRanges, BroadbandTsModule broadbandTsModule) {
         if (outsideDepthRanges || !depthRanges.contains(trackBorder.peakDepth())) {
            outsideDepthRanges = true;
            return;
         }

         perTrack.pingNumber.add(ping.getPingNumber());
         perTrack.time.add(ping.getInstant().toString());

         GeoPoint geoPos = ping.getPingIndex().getGeographicalPosition();
         perTrack.longitude.add(geoPos != null ? Utils.round(geoPos.getLongitude(), 1e8) : Double.NaN);
         perTrack.latitude.add(geoPos != null ? Utils.round(geoPos.getLatitude(), 1e8) : Double.NaN);
         perTrack.heading.add((float) DataUtils.getHeadingFromNmea(ping).orElse(Double.NaN));

         PowerData detectionRaw = ping.getPowerData(trackBorder.channel());
         perTrack.heave.add(detectionRaw != null ? detectionRaw.getHeave() : Float.NaN);
         perTrack.pitch.add(detectionRaw != null ? detectionRaw.getPitch() : Float.NaN);
         perTrack.roll.add(detectionRaw != null ? detectionRaw.getRoll() : Float.NaN);

         perTrack.minDepth.add(Utils.round(trackBorder.depthRange().min(), 1000));
         perTrack.maxDepth.add(Utils.round(trackBorder.depthRange().max(), 1000));
         perTrack.peakDepth.add(Utils.round(trackBorder.peakDepth(), 1000));

         for (int i = 0; i < channels.size(); i++) {
            BroadbandTrackExportPerChannel perChannel = perTrack.channels.get(i);
            BroadbandData broadbandData = ping.getBroadbandData(channels.get(i));
            if (broadbandData == null) {
               perChannel.minFrequency.add(Float.NaN);
               perChannel.maxFrequency.add(Float.NaN);
               perChannel.numFrequencies.add(0);
               perChannel.peakRange.add(Float.NaN);
               perChannel.fftDistanceBefore.add(Float.NaN);
               perChannel.fftDistanceAfter.add(Float.NaN);
               perChannel.alongshipAngle.add(Float.NaN);
               perChannel.athwartshipAngle.add(Float.NaN);
               perChannel.x.add(Float.NaN);
               perChannel.y.add(Float.NaN);
               perChannel.z.add(Float.NaN);
               perChannel.tsc.add(Utils.EMPTY_FLOAT_ARRAY);
               continue;
            }

            float peakDepth = broadbandData.getChannel() == trackBorder.channel() ? trackBorder.peakDepth() : Float.NaN;
            BroadbandTsChannelCache.Target target = new BroadbandTsChannelCache.Target(peakDepth, trackBorder.depthRange());
            List<BroadbandTsData> tsDataList = BroadbandTsChannelCache.computeTsData(broadbandData, broadbandTsModule, Stream.of(target));
            BroadbandTsData tsData = tsDataList.getFirst();

            perChannel.minFrequency.add(tsData.frequencyRange().min());
            perChannel.maxFrequency.add(tsData.frequencyRange().max());
            perChannel.numFrequencies.add(trackBorder.useAngles() ? tsData.values().length : 0);
            float peakRange = broadbandData.depthToRange(tsData.depth());
            perChannel.peakRange.add(Utils.round(peakRange, 1000));
            perChannel.fftDistanceBefore.add(Utils.round(peakRange - broadbandData.depthToRange(tsData.depthRange().min()), 1000));
            perChannel.fftDistanceAfter.add(Utils.round(broadbandData.depthToRange(tsData.depthRange().max()) - peakRange, 1000));
            int peakIndex = broadbandData.depthToSampleIndex(tsData.depth());
            float nominalFrequency = broadbandData.getTransducer().getFrequency();
            float alongshipAngle = broadbandData.getMechanicalAlongAngle(peakIndex, nominalFrequency);
            float athwartshipAngle = broadbandData.getMechanicalAthwartAngle(peakIndex, nominalFrequency);
            perChannel.alongshipAngle.add(trackBorder.useAngles() ? Utils.round(alongshipAngle, 100) : Float.NaN);
            perChannel.athwartshipAngle.add(trackBorder.useAngles() ? Utils.round(athwartshipAngle, 100) : Float.NaN);
            Vec3 pos = StationaryPositionFunction.measurementToGlobalPosition(new Measurement(peakRange,
                  (float) Math.toRadians(alongshipAngle), (float) Math.toRadians(athwartshipAngle), 0));
            perChannel.x.add(trackBorder.useAngles() ? Utils.round(pos.x(), 1000) : Float.NaN);
            perChannel.y.add(trackBorder.useAngles() ? Utils.round(pos.y(), 1000) : Float.NaN);
            perChannel.z.add(trackBorder.useAngles() ? Utils.round(pos.z(), 1000) : Float.NaN);
            float[] tsc = tsData.values();
            ArrayMath.round(tsc, 100);
            perChannel.tsc.add(trackBorder.useAngles() ? tsc : Utils.EMPTY_FLOAT_ARRAY);
         }
      }

      private boolean isExportable() {
         return !outsideDepthRanges;
      }
   }
}
