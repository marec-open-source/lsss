package no.imr.lsss.modules.korona.tracking;

import no.imr.korona.computation.tracking.data.Measurement;
import no.imr.korona.computation.tracking.impl.StationaryPositionFunction;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.region.Region;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.export.ExportFile;
import no.imr.lsss.framework.export.StreamingExporter;
import no.imr.tools.ProgressHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.Listener;
import no.imr.tools.math.Mean;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.parameter.Name;
import no.imr.tools.range.FloatRangeBuilder;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.range.RangeSet;
import tools.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Track exporter.
 */
public final class TrackExporter extends StreamingExporter {
   public TrackExporter(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("Tracks"), "Tracks in selected regions");
   }

   @Override
   public boolean isJson() {
      return false;
   }

   @Override
   public void doExport(AsyncHandle asyncHandle, ProgressHandler progressHandler) throws IOException {
      PingRange pingRange = getLSSS().getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return;
      }
      DataFileSet dataFileSet = getLSSS().getInterpretationSettings().getDataFileSet();

      ExportFile perTrackFile = new ExportFile("TracksOverview", ".txt");
      ExportFile perPingFile = new ExportFile("TracksPerPing", ".txt");
      ExportFile cdsFile = createCdsFileWrapper();
      initExportFiles(pingRange, perTrackFile, perPingFile, cdsFile);
      writeModuleSetup(cdsFile, dataFileSet, pingRange);

      try (PrintWriter perTrackOut = perTrackFile.newPrintWriter();
           PrintWriter perPingOut = perPingFile.newPrintWriter()) {
         write(asyncHandle, progressHandler, perTrackOut, perPingOut);
      }
   }

   @Override
   public void exportToStream(AsyncHandle asyncHandle, ProgressHandler progressHandler, OutputStream out, ObjectWriter objectWriter) {
      try (PrintWriter perTrackOut = new PrintWriter(new OutputStreamWriter(OutputStream.nullOutputStream(), Utils.UTF_8));
           PrintWriter perPingOut = new PrintWriter(new OutputStreamWriter(out, Utils.UTF_8))) {
         write(asyncHandle, progressHandler, perTrackOut, perPingOut);
      }
   }

   private void write(AsyncHandle asyncHandle, ProgressHandler progressHandler, PrintWriter perTrackOut, PrintWriter perPingOut) {
      DataFileSet dataFileSet = getLSSS().getInterpretationSettings().getDataFileSet();

      TrackInfoModule trackInfoModule = getLSSS().getModuleManager().getModule(TrackInfoModule.class);
      trackInfoModule.waitForTrackInfoLoader(asyncHandle);

      writeStandardHeader(perTrackOut, "1.6.1");
      perTrackOut.println("# Id: Unique identifier for the track within a raw file");
      perTrackOut.println("# TSU / TSC: Mean TS (uncorrected / corrected) for all samples in all pings in the track");
      perTrackOut.println("# PingCount: Last ping - first ping + 1. Pings in between where track is not detected are included");
      perTrackOut.println("# ZVariation: (max z - min z) / pingCount");
      perTrackOut.println("#");

      writeColumnNames(perTrackOut, List.of("Id[-]", "Frequency[Hz]", "Transceiver[-]",
            "TSU[dB re m2]", "TSC[dB re m2]", "PingCount[-]", "ZVariation[m]"));

      writeStandardHeader(perPingOut, "1.6.1");
      perPingOut.println("# Id: Unique identifier for the track within a raw file");
      perPingOut.println("# Date: YYYYMMDD");
      perPingOut.println("# Time: HHMMSSXX, where XX are hundredths of a second");
      perPingOut.println("# TSU / TSC: Mean TS (uncorrected / corrected) for all samples in this ping in the track");
      perPingOut.println("# x, y, z: Coordinates relative to transducer. x towards the ships bow, y towards starboard, z downwards");
      perPingOut.println("#          (The transducer offset in the files HorizontalTransducerOffset... and VerticalTransducerOffset... are not used");
      perPingOut.println("#");

      writeColumnNames(perPingOut, List.of("Id[-]", "Date[YYYYMMDD]", "Time[HHMMSSXX]",
            "Frequency[Hz]", "Transceiver[-]", "TSU[dB re m2]", "TSC[dB re m2]", "x[m]", "y[m]", "z[m]"));

      Map<TrackId, TrackAccumulator> trackMap = new HashMap<>();

      int channel = getLSSS().getInterpretationSettings().getChannel();
      List<Region> selectedRegions = getLSSS().getRegionManager().getSelectedRegions();
      RangeSet<PingIndex> visiblePingRangeSet = getVisiblePingRangeSet(selectedRegions);
      List<PingIndex> pingIndexes = getVisiblePingIndexes(selectedRegions);
      Listener progressListener = progressHandler.asCountingListener(pingIndexes.size());
      for (PingIndex pingIndex : pingIndexes) {
         if (asyncHandle.isCancelled()) {
            return;
         }
         progressListener.listen();
         Ping ping = dataFileSet.getPing(pingIndex);
         FloatRangeSet depthRanges = getDepthRanges(selectedRegions, ping, channel);

         trackInfoModule.getTrackEditing().getTrackBorders(ping, channel).forEach(trackBorder -> {
            DatabaseTime databaseTime = new DatabaseTime(pingIndex.getTimeInMillis());
            TrackId trackId = trackBorder.trackId();
            TrackAccumulator trackAccumulator = trackMap.get(trackId);
            if (trackAccumulator == null) {
               TrackInfo trackInfo = trackInfoModule.getTrackInfos().get(trackId);
               if (trackInfo == null || !visiblePingRangeSet.containsAll(trackInfo.pingRange())) {
                  return;
               }
               trackAccumulator = new TrackAccumulator(trackInfo.pingRange());
               trackMap.put(trackId, trackAccumulator);
            }
            trackAccumulator.accumulate(databaseTime, ping, trackBorder, depthRanges);
            if (pingIndex.getPingNumber() == trackAccumulator.pingRange.end().getPingNumber() - 1) {
               trackMap.remove(trackId);
               if (trackAccumulator.isExportable()) {
                  trackAccumulator.writePerPing(perPingOut);
                  trackAccumulator.writePerTrack(perTrackOut, ping, trackId, channel);
               }
            }
         });
      }
   }

   private static final class TrackAccumulator {
      private final PingRange pingRange;
      private final List<List<String>> values = new ArrayList<>();
      private final FloatRangeBuilder zExtent = new FloatRangeBuilder();
      private final Mean tsuTrack = new Mean();
      private final Mean tscTrack = new Mean();
      private boolean outsideDepthRanges;

      private TrackAccumulator(PingRange pingRange) {
         this.pingRange = pingRange;
      }

      private void accumulate(DatabaseTime databaseTime, Ping ping, TrackBorder trackBorder, FloatRangeSet depthRanges) {
         if (outsideDepthRanges || !depthRanges.contains(trackBorder.depthRange())) {
            outsideDepthRanges = true;
            return;
         }
         PowerData powerData = ping.getPowerData(trackBorder.channel());
         if (powerData == null) {
            return;
         }
         int iBegin = powerData.depthToSampleIndex(trackBorder.depthRange().min());
         int iEnd = powerData.depthToSampleIndex(trackBorder.depthRange().max());
         boolean useTsc = trackBorder.useAngles() && powerData.getAngleData() != null;
         double tsuSum = 0;
         double tscSum = 0;
         double xSum = 0;
         double ySum = 0;
         double zSum = 0;
         for (int i = iBegin; i < iEnd; i++) {
            tsuSum += powerData.getLinearTSU(i);
            if (useTsc) {
               tscSum += powerData.getLinearTSC(i);
            }
            Vec3 pos = StationaryPositionFunction.measurementToGlobalPosition(new Measurement(powerData, i, 0));
            xSum += pos.x();
            ySum += pos.y();
            zSum += pos.z();
         }
         int count = iEnd - iBegin;

         tsuTrack.update(tsuSum / count, count);

         double z = zSum / count;

         if (trackBorder.useAngles()) {
            if (useTsc) {
               tscTrack.update(tscSum / count, count);
            }
            zExtent.expand(z);
         }

         values.add(List.of(
               Integer.toString(trackBorder.trackId().id()),
               Integer.toString(databaseTime.getDate()),
               Utils.format("%08d", databaseTime.getTime()),
               Integer.toString(Math.round(ping.getRawFileConfiguration().getTransducers().get(trackBorder.channel() - 1).getFrequency())),
               Integer.toString(trackBorder.channel()),
               Utils.format("%.2f", PowerData.svToLogSv((float) (tsuSum / count))),
               Utils.format("%.2f", useTsc ? PowerData.svToLogSv((float) (tscSum / count)) : Float.NaN),
               Utils.format("%.2f", trackBorder.useAngles() ? xSum / count : Float.NaN),
               Utils.format("%.2f", trackBorder.useAngles() ? ySum / count : Float.NaN),
               Utils.format("%.2f", trackBorder.useAngles() ? z : Float.NaN)));
      }

      private boolean isExportable() {
         return !outsideDepthRanges;
      }

      private void writePerPing(PrintWriter out) {
         for (List<String> values : values) {
            writeValues(out, values);
         }
      }

      private void writePerTrack(PrintWriter out, Ping ping, TrackId trackId, int channel) {
         int pingCount = pingRange.getPingCount();
         List<String> values = List.of(
               Integer.toString(trackId.id()),
               Integer.toString(Math.round(ping.getRawFileConfiguration().getTransducers().get(channel - 1).getFrequency())),
               Integer.toString(channel),
               Utils.format("%.2f", PowerData.svToLogSv((float) tsuTrack.getMean())),
               Utils.format("%.2f", PowerData.svToLogSv((float) tscTrack.getMean())),
               Integer.toString(pingCount),
               Utils.format("%.3f", zExtent.toFloatRange().getSize() / pingCount));
         writeValues(out, values);
      }
   }
}
