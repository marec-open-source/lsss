package no.imr.lsss.modules.korona.tracking;

import com.google.common.collect.ImmutableMap;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.tools.math.Mean;
import no.imr.tools.math.PeakFinding;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.Range;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

final class TrackEditingDetection {
   private final TrackInfoModule trackInfoModule;
   private final Set<TrackId> trackIds;
   private final TrackId newTrackId = new TrackId(-1, -1);
   private final DataFileSet dataFileSet;
   private final int channel;
   private final float verticalExtent;
   private final NavigableMap<PingIndex, TrackBorder> trackBorders = new ConcurrentSkipListMap<>();
   private PingRange pingRange;
   private boolean didEdit;

   TrackEditingDetection(TrackInfoModule trackInfoModule, Set<TrackId> trackIds) {
      this.trackInfoModule = trackInfoModule;
      this.trackIds = trackIds;

      InterpretationSettings interpretationSettings = trackInfoModule.getLSSS().getInterpretationSettings();
      dataFileSet = interpretationSettings.getDataFileSet();
      channel = interpretationSettings.getChannel();

      pingRange = trackIds.stream()
            .map(trackId -> trackInfoModule.getTrackInfos().get(trackId).pingRange())
            .reduce(PingRange.EMPTY_RANGE, PingRange::union);

      Mean verticalExtentMean = new Mean();
      dataFileSet.getPingIndices(pingRange).forEach(pingIndex -> {
         Ping ping = dataFileSet.getPing(pingIndex);
         trackInfoModule.getTrackEditing().getTrackBorders(ping)
               .filter(trackBorder -> trackIds.contains(trackBorder.trackId()))
               .forEach(trackBorder -> {
                  trackBorders.put(pingIndex, trackBorder);
                  verticalExtentMean.update(trackBorder.depthRange().getSize());
               });
      });
      verticalExtent = (float) verticalExtentMean.getMean();
   }

   Set<TrackId> getTrackIds() {
      return trackIds;
   }

   NavigableMap<PingIndex, TrackBorder> getTrackBorders() {
      return trackBorders;
   }

   ImmutableMap<PingIndex, TrackBorder> getTrackBorders(TrackId newTrackId) {
      ImmutableMap.Builder<PingIndex, TrackBorder> builder = ImmutableMap.builder();
      trackBorders.forEach((pingIndex, trackBorder) -> {
         builder.put(pingIndex, trackBorder.withId(newTrackId));
      });
      return builder.build();
   }

   PingRange getPingRange() {
      return pingRange;
   }

   boolean didEdit() {
      return didEdit;
   }

   void fillHole(Range<PingIndex> holeRange) {
      dataFileSet.getPingIndices(holeRange).forEach(pingIndex -> {
         extend(pingIndex, false, Float.NEGATIVE_INFINITY);
      });
   }

   @Nullable TrackBorder extend(boolean left, float minTSU) {
      PingIndex pingIndex = dataFileSet.getPingIndexOrNull(left ? pingRange.begin().getPingNumber() - 1 : pingRange.end().getPingNumber());
      if (pingIndex == null || pingIndex.equals(dataFileSet.getTotalRange().end())) {
         return null;
      }
      return extend(pingIndex, left, minTSU);
   }

   private @Nullable TrackBorder extend(PingIndex pingIndex, boolean left, float minTSU) {
      PingIndex pingIndex1 = dataFileSet.getPingIndexOrNull(pingIndex.getPingNumber() + (left ? 1 : -1));
      if (pingIndex1 == null) {
         return null;
      }
      TrackBorder trackBorder1 = trackBorders.get(pingIndex1);
      if (trackBorder1 == null) {
         trackInfoModule.getLSSS().showError("Inconsistent track ping range.\nYou may need to reprocess the data.");
         return null;
      }
      float depth = trackBorder1.peakDepth();
      PingIndex pingIndex2 = dataFileSet.getPingIndexOrNull(pingIndex.getPingNumber() + (left ? 2 : -2));
      if (pingIndex2 != null) {
         TrackBorder trackBorder2 = trackBorders.get(pingIndex2);
         if (trackBorder2 != null) {
            long t = pingIndex.getNTDate();
            long t1 = pingIndex1.getNTDate();
            long t2 = pingIndex2.getNTDate();
            float a = (t2 - t) / (float) (t2 - t1);
            depth = a * trackBorder1.peakDepth() + (1 - a) * trackBorder2.peakDepth();
         }
      }
      return extend(pingIndex, depth, minTSU);
   }

   @Nullable TrackBorder extend(PingIndex pingIndex, float depth, float minTSU) {
      Ping ping = dataFileSet.getPing(pingIndex);
      PowerData powerData = ping.getPowerData(channel);
      if (powerData == null) {
         return null;
      }

      float linearMinTsu = PowerData.logSvToSv(minTSU);

      int i0 = powerData.depthToClampedSampleIndex(depth - verticalExtent * 5);
      int i1 = powerData.depthToClampedSampleIndex(depth + verticalExtent * 5);
      float[] values = new float[i1 - i0];
      for (int i = 0; i < values.length; i++) {
         values[i] = powerData.getLinearTSU(i + i0);
      }
      List<PeakFinding.Peak> peaks = PeakFinding.getPeaks(values, values.length / 2, 0.5f);
      PeakFinding.Peak maxPeak = null;
      for (PeakFinding.Peak peak : peaks) {
         if (values[peak.peakIndex()] < linearMinTsu) {
            continue;
         }
         if (maxPeak == null ||
               Math.abs(powerData.getSampleDepth(peak.peakIndex() + i0) - depth)
                     < Math.abs(powerData.getSampleDepth(maxPeak.peakIndex() + i0) - depth)) {
            maxPeak = peak;
         }
      }

      if (maxPeak == null) {
         return null;
      }

      float peakDepth = powerData.getSampleDepth(maxPeak.peakIndex() + i0);

      boolean overlapsExistingTracks = trackInfoModule.getTrackEditing().getTrackBorders(ping, channel)
            .filter(trackBorder -> !trackIds.contains(trackBorder.trackId()))
            .anyMatch(trackBorder -> trackBorder.depthRange().contains(peakDepth));
      if (overlapsExistingTracks) {
         return null;
      }

      FloatRange depthRange = FloatRange.of(
            powerData.getSampleDepth(maxPeak.beginIndex() + i0),
            powerData.getSampleDepth(maxPeak.endIndex() + i0));

      TrackBorder trackBorder = new TrackBorder(newTrackId, channel, depthRange, peakDepth, false);
      trackBorders.put(pingIndex, trackBorder);

      pingRange = PingRange.from(trackBorders, dataFileSet);
      didEdit = true;

      return trackBorder;
   }

   void delete(PingIndex pingIndex) {
      if (trackBorders.remove(pingIndex) != null) {
         pingRange = PingRange.from(trackBorders, dataFileSet);
         didEdit = true;
      }
   }

   static ImmutableMap<PingIndex, TrackBorder> fillHoleLinearly(TrackInfoModule trackInfoModule, TrackId newTrackId, Set<TrackId> existingTrackIds, Range<PingIndex> holeRange) {
      InterpretationSettings interpretationSettings = trackInfoModule.getLSSS().getInterpretationSettings();
      DataFileSet dataFileSet = interpretationSettings.getDataFileSet();

      Ping pingA = dataFileSet.getPing(dataFileSet.previousOrSame(holeRange.begin()));
      TrackBorder trackBorderA = trackInfoModule.getTrackEditing().getTrackBorders(pingA)
            .filter(trackBorder -> existingTrackIds.contains(trackBorder.trackId()))
            .findFirst()
            .orElseThrow();

      Ping pingB = dataFileSet.getPing(holeRange.end());
      TrackBorder trackBorderB = trackInfoModule.getTrackEditing().getTrackBorders(pingB)
            .filter(trackBorder -> existingTrackIds.contains(trackBorder.trackId()))
            .findFirst()
            .orElseThrow();

      int channel = interpretationSettings.getChannel();
      PingMapping pingMapping = interpretationSettings.getPingMapping();
      double holeDistance = pingMapping.distance(pingA, pingB);

      ImmutableMap.Builder<PingIndex, TrackBorder> builder = ImmutableMap.builder();
      dataFileSet.getPingIndices(holeRange).forEach(pingIndex -> {
         float weightB = (float) (pingMapping.distance(pingA, pingIndex) / holeDistance);
         float weightA = 1 - weightB;
         float min = weightA * trackBorderA.depthRange().min() + weightB * trackBorderB.depthRange().min();
         float max = weightA * trackBorderA.depthRange().max() + weightB * trackBorderB.depthRange().max();
         float peak = weightA * trackBorderA.peakDepth() + weightB * trackBorderB.peakDepth();
         builder.put(pingIndex, new TrackBorder(newTrackId, channel, FloatRange.of(min, max), peak, false));
      });
      return builder.build();
   }
}
