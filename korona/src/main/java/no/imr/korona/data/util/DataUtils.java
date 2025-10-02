package no.imr.korona.data.util;

import no.imr.korona.data.DatagramSource;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Mru0Datagram;
import no.imr.korona.data.datagrams.MruDatagram;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.ExtrapolatedPingIndex;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingMappingArgument;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.NmeaPingItem;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.geo.Earth;
import no.imr.tools.geo.GeoBoxBuilder;
import no.imr.tools.range.DoubleRange;
import no.imr.tools.range.FloatRange;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Data related utility functions.
 */
public final class DataUtils {
   private DataUtils() {
   }

   public static DoubleRange convertPingMappingRange(DataFileSet dataFileSet, PingMapping sourcePingMapping, DoubleRange sourceRange, PingMapping targetPingMapping) {
      if (sourcePingMapping == targetPingMapping) {
         return sourceRange;
      }
      return DoubleRange.of(
            convertPingMappingValue(dataFileSet, sourcePingMapping, sourceRange.begin(), targetPingMapping),
            convertPingMappingValue(dataFileSet, sourcePingMapping, sourceRange.end(), targetPingMapping));
   }

   public static double convertPingMappingValue(DataFileSet dataFileSet, PingMapping sourcePingMapping, double sourceValue, PingMapping targetPingMapping) {
      if (sourcePingMapping == targetPingMapping) {
         return sourceValue;
      }
      PingIndex before = dataFileSet.getContainingPingIndex(sourceValue, sourcePingMapping);
      if (before == null) {
         before = dataFileSet.getClosestPingIndex(sourceValue, sourcePingMapping);
      }
      PingIndex after = dataFileSet.nextOrNull(before);
      if (after == null) {
         return targetPingMapping.valueOf(before);
      }
      double alpha = (sourceValue - sourcePingMapping.valueOf(before)) / sourcePingMapping.distance(before, after);
      return targetPingMapping.valueOf(before) + alpha * targetPingMapping.distance(before, after);
   }

   public static <T extends PingMappingArgument> T getClosestPingIndex(List<T> pingMappingArguments, double value, PingMapping pingMapping) {
      int i = binarySearchForPingIndex(pingMappingArguments, value, pingMapping);

      if (i < 0) { // not exact match
         i = -(i + 1); // conversion to insertion point

         if (i == pingMappingArguments.size()) {
            i = pingMappingArguments.size() - 1;
         } else if (i > 0) {
            if (Math.abs(value - pingMapping.valueOf(pingMappingArguments.get(i - 1)))
                  < Math.abs(value - pingMapping.valueOf(pingMappingArguments.get(i)))) {
               i = i - 1;
            }
         }
      } else { // exact match
         // Use the highest ping index with this exact value.
         while (i + 1 < pingMappingArguments.size() && pingMapping.valueOf(pingMappingArguments.get(i + 1)) == value) {
            i++;
         }
      }

      return pingMappingArguments.get(i);
   }

   public static <T extends PingMappingArgument> @Nullable T getContainingPingIndex(List<T> pingMappingArguments, @Nullable PingMappingArgument end, double value, PingMapping pingMapping) {
      int i = binarySearchForPingIndex(pingMappingArguments, value, pingMapping);

      if (i < 0) { // not exact match
         i = -(i + 1); // conversion to insertion point

         if (i == 0) {
            return null;
         }

         if (i == pingMappingArguments.size() && end != null && value >= pingMapping.valueOf(end)) {
            return null;
         }

         i = i - 1;
      } else { // exact match
         // Use the highest ping index with this exact value.
         while (i + 1 < pingMappingArguments.size() && pingMapping.valueOf(pingMappingArguments.get(i + 1)) == value) {
            i++;
         }
      }

      return pingMappingArguments.get(i);
   }

   private static <T extends PingMappingArgument> int binarySearchForPingIndex(List<T> pingMappingArguments, double value, PingMapping pingMapping) {
      return Utils.binarySearchForDouble(pingMappingArguments, value, pingMapping::valueOf);
   }

   public static <T extends BaseDatagram> List<T> getAll(DatagramSource datagramSource, Class<T> datagramClass) throws IOException {
      List<T> datagrams = new ArrayList<>();
      while (true) {
         BaseDatagram datagram = datagramSource.nextDatagram();
         if (datagram == null) {
            break;
         }
         if (datagramClass.isInstance(datagram)) {
            datagrams.add(datagramClass.cast(datagram));
         }
      }
      return datagrams;
   }

   public static PingRange createPingRange(List<? extends PingIndex> pingIndices) {
      return pingIndices.isEmpty() ? PingRange.EMPTY_RANGE : PingRange.of(pingIndices.getFirst(), ExtrapolatedPingIndex.create(pingIndices));
   }

   public static @Nullable Rectangle2D getGeographicalBoundingBox(List<PingIndex> pingIndices) {
      GeoBoxBuilder geoBoxBuilder = new GeoBoxBuilder();
      for (PingIndex pingIndex : pingIndices) {
         GeoPoint geoPos = pingIndex.getGeographicalPosition();
         if (geoPos != null) {
            geoBoxBuilder.add(geoPos);
         }
      }
      return geoBoxBuilder.build();
   }

   public static @Nullable PingIndex geoPosToClosestPingIndex(GeoPoint geoPos, List<PingIndex> pingIndices) {
      GeoPoint metersPerGeoDegree = Earth.getMetersPerGeoDegree(geoPos);
      double minDist = Double.POSITIVE_INFINITY;
      PingIndex closestPingIndex = null;
      for (PingIndex pingIndex : pingIndices) {
         GeoPoint pingGeoPos = pingIndex.getGeographicalPosition();
         if (pingGeoPos == null) {
            continue;
         }
         double dx = metersPerGeoDegree.getX() * (pingGeoPos.getX() - geoPos.getX());
         double dy = metersPerGeoDegree.getY() * (pingGeoPos.getY() - geoPos.getY());
         double dist = dx * dx + dy * dy;
         if (dist < minDist) {
            minDist = dist;
            closestPingIndex = pingIndex;
         }
      }
      return closestPingIndex;
   }

   public static @Nullable PingIndex geoPosToProjectedPingIndex(GeoPoint geoPos, PingIndex referencePingIndex, DataFileSet dataFileSet) {
      GeoPoint referenceGeoPos = referencePingIndex.getGeographicalPosition();
      if (referenceGeoPos == null) {
         return null;
      }
      GeoPoint metersPerGeoDegree = Earth.getMetersPerGeoDegree(geoPos);
      Point2D tangent = getTangent(dataFileSet, metersPerGeoDegree, referencePingIndex);
      if (tangent == null) {
         return null;
      }
      double dx = (geoPos.getX() - referenceGeoPos.getX()) * metersPerGeoDegree.getX();
      double dy = (geoPos.getY() - referenceGeoPos.getY()) * metersPerGeoDegree.getY();
      double meters = tangent.getX() * dx + tangent.getY() * dy;
      double nmi = Utils.meterToNmi(meters);
      return dataFileSet.getClosestPingIndex(referencePingIndex, nmi, PingMapping.DISTANCE);
   }

   public static @Nullable Point2D getTangent(DataFileSet dataFileSet, @Nullable GeoPoint metersPerGeoDegree, PingIndex pingIndex) {
      int d = 10;

      PingIndex begin = dataFileSet.getPingIndexOrNull(pingIndex.getPingNumber() - d);
      if (begin == null) {
         if (dataFileSet.getTotalRange().isEmpty()) {
            return null;
         }
         begin = dataFileSet.getTotalRange().begin();
      }

      PingIndex end = dataFileSet.getPingIndexOrNull(pingIndex.getPingNumber() + (d + 1));
      if (end == null) {
         end = dataFileSet.getTotalRange().end();
      }

      return getTangent(dataFileSet.getPingIndices(PingRange.of(begin, end)), metersPerGeoDegree);
   }

   public static @Nullable Point2D getTangent(List<PingIndex> pingIndexes, @Nullable GeoPoint metersPerGeoDegree) {
      int sHalf = pingIndexes.size() / 2;

      GeoPoint prev = getAverageGeoPos(pingIndexes.subList(0, sHalf));
      if (prev == null) {
         return null;
      }
      GeoPoint next = getAverageGeoPos(pingIndexes.subList(sHalf, pingIndexes.size()));
      if (next == null) {
         return null;
      }

      if (metersPerGeoDegree == null) {
         metersPerGeoDegree = Earth.getMetersPerGeoDegree((prev.getLatitude() + next.getLatitude()) / 2);
      }

      double dx = (next.getX() - prev.getX()) * metersPerGeoDegree.getX();
      double dy = (next.getY() - prev.getY()) * metersPerGeoDegree.getY();
      double length = Utils.hypot(dx, dy);

      return length == 0 ? null : new Point2D.Double(dx / length, dy / length);
   }

   private static @Nullable GeoPoint getAverageGeoPos(List<PingIndex> pingIndices) {
      double x = 0;
      double y = 0;
      int n = 0;
      for (PingIndex pingIndex : pingIndices) {
         GeoPoint geoPos = pingIndex.getGeographicalPosition();
         if (geoPos != null) {
            x += geoPos.getX();
            y += geoPos.getY();
            n++;
         }
      }
      return n == 0 ? null : new GeoPoint(x / n, y / n);
   }

   public static List<PingRange> getPingRangesInGeoRect(DataFileSet dataFileSet, Rectangle2D geoRect) {
      PingIndex firstInside = null;

      List<PingRange> pingRanges = new ArrayList<>();
      for (PingIndex pingIndex : dataFileSet.getPingIndices()) {
         GeoPoint geoPos = pingIndex.getGeographicalPosition();
         if (geoPos == null) {
            continue;
         }
         if (geoRect.contains(geoPos)) {
            if (firstInside == null) {
               firstInside = pingIndex;
            }
         } else {
            if (firstInside != null) {
               pingRanges.add(PingRange.of(firstInside, pingIndex));
               firstInside = null;
            }
         }
      }
      if (firstInside != null) {
         pingRanges.add(PingRange.of(firstInside, dataFileSet.getTotalRange().end()));
      }

      return pingRanges;
   }

   public static OptionalDouble getHeadingFromNmea(Ping ping) {
      return ping.getPingItems(NmeaPingItem.class)
            .flatMapToDouble(nmea -> nmea.getHeading().stream())
            .findFirst();
   }

   public static OptionalDouble getHeading(Ping ping, SegmentData segmentData) throws IOException {
      OptionalDouble heading = getHeadingFromNmea(ping);
      if (heading.isEmpty()) {
         heading = interpolateHeading(ping.getPingIndex(), segmentData);
      }
      return heading;
   }

   private static OptionalDouble interpolateHeading(PingIndex pingIndex, SegmentData segmentData) throws IOException {
      PingAndHeading prev = searchForHeading(pingIndex, segmentData, -1);
      PingAndHeading next = searchForHeading(pingIndex, segmentData, 1);
      if (prev == null) {
         return next != null ? OptionalDouble.of(next.heading()) : OptionalDouble.empty();
      } else {
         if (next == null) {
            return OptionalDouble.of(prev.heading());
         }
         long t0 = prev.pingIndex().getNTDate();
         long t1 = next.pingIndex().getNTDate();
         double alpha = (double) (t1 - pingIndex.getNTDate()) / (double) (t1 - t0);
         return OptionalDouble.of(Utils.interpolateDegrees(prev.heading(), next.heading(), alpha));
      }
   }

   private record PingAndHeading(PingIndex pingIndex, double heading) {
   }

   private static @Nullable PingAndHeading searchForHeading(PingIndex referencePingIndex, SegmentData segmentData, int direction) throws IOException {
      int index = segmentData.pingNumberToIndex(referencePingIndex.getPingNumber());
      for (int i = 0; i < 10; i++) {
         index += direction;
         if (index < 0 || index >= segmentData.getPingIndices().size()) {
            break;
         }
         PingIndex pingIndex = segmentData.getPingIndices().get(index);
         Ping ping = segmentData.loadPing(pingIndex, new AsyncHandle());
         OptionalDouble heading = getHeadingFromNmea(ping);
         if (heading.isPresent()) {
            return new PingAndHeading(pingIndex, heading.getAsDouble());
         }
      }
      return null;
   }

   public static double getKnots(Ping ping, DataFileSet dataFileSet) {
      OptionalDouble meterPerSec = ping.getPingItems(NmeaPingItem.class)
            .flatMapToDouble(nmea -> nmea.getMeterPerSec().stream())
            .findFirst();
      if (meterPerSec.isPresent()) {
         return KoronaUtils.meterPerSecondToKnots(meterPerSec.getAsDouble());
      }

      // If speed not in NMEA datagram, then compute from vessel distance and time.
      PingIndex first = getPingIndex(ping, dataFileSet, -10);
      PingIndex last = getPingIndex(ping, dataFileSet, 10);
      return KoronaUtils.getKnots(first, last);
   }

   private static PingIndex getPingIndex(Ping ping, DataFileSet dataFileSet, int offset) {
      int step = offset > 0 ? -1 : 1;
      for (int i = offset; i != 0; i += step) {
         PingIndex pingIndex = dataFileSet.getPingIndexOrNull(ping.getPingNumber() + i);
         if (pingIndex != null) {
            return pingIndex;
         }
      }
      return ping.getPingIndex();
   }

   public static MruDatagram interpolateMru(MruDatagram first, MruDatagram second, long ntDate) {
      float f = Math.clamp((float) (second.getNTDate() - ntDate) / (second.getNTDate() - first.getNTDate()), 0, 1);
      return new Mru0Datagram(ntDate,
            f * first.getHeave() + (1 - f) * second.getHeave(),
            (float) Utils.interpolateDegrees(first.getRoll(), second.getRoll(), f),
            (float) Utils.interpolateDegrees(first.getPitch(), second.getPitch(), f),
            (float) Utils.interpolateDegrees(first.getHeading(), second.getHeading(), f));
   }

   public static boolean hasSamples(Ping ping) {
      return ping.getNonNullChannelDatas()
            .anyMatch(DataUtils::hasSamples);
   }

   private static boolean hasSamples(ChannelData channelData) {
      return channelData.getCount() > 0;
   }

   public static float getMinMaxRange(Ping ping) {
      return (float) ping.getNonNullChannelDatas()
            .filter(DataUtils::hasSamples)
            .mapToDouble(ChannelData::getMaxRange)
            .min()
            .orElse(0);
   }

   public static float getMinRange(Ping ping) {
      return (float) ping.getNonNullChannelDatas()
            .filter(DataUtils::hasSamples)
            .mapToDouble(ChannelData::getMinRange)
            .min()
            .orElse(0);
   }

   public static float getMaxRange(Ping ping) {
      return (float) ping.getNonNullChannelDatas()
            .filter(DataUtils::hasSamples)
            .mapToDouble(ChannelData::getMaxRange)
            .max()
            .orElse(0);
   }

   public static int getMinOffset(Ping ping) {
      return ping.getNonNullChannelDatas()
            .filter(DataUtils::hasSamples)
            .mapToInt(ChannelData::getOffset)
            .min()
            .orElse(0);
   }

   public static int getMinCount(Ping ping) {
      return ping.getNonNullChannelDatas()
            .filter(DataUtils::hasSamples)
            .mapToInt(ChannelData::getCount)
            .min()
            .orElse(0);
   }

   public static int getMaxCount(Ping ping) {
      return ping.getNonNullChannelDatas()
            .filter(DataUtils::hasSamples)
            .mapToInt(ChannelData::getCount)
            .max()
            .orElse(0);
   }

   public static int getMaxIndex(Ping ping) {
      return ping.getNonNullChannelDatas()
            .filter(DataUtils::hasSamples)
            .mapToInt(channelData -> channelData.getCount() + channelData.getOffset())
            .max()
            .orElse(0);
   }

   public static double getPingWidthMeters(DataFileSet dataFileSet, PingIndex pingIndex) {
      PingIndex nextPingIndex = dataFileSet.nextOrSame(pingIndex);
      return Utils.nmiToMeter(nextPingIndex.getVesselDistance() - pingIndex.getVesselDistance());
   }

   public static List<FloatRange> findDepthRanges(PowerData powerData, FloatRange depthLimit, float[] values, FloatRange valueRange) {
      List<FloatRange> depthRanges = new ArrayList<>();
      int i = Math.max(powerData.depthToSampleIndex(depthLimit.min()), 0);
      int iEnd = Math.min(powerData.depthToSampleIndex(depthLimit.max()), values.length);
      while (true) {
         // Search for start of next depth range:
         while (i < iEnd && !valueRange.contains(values[i])) {
            i++;
         }
         if (i >= iEnd) {
            break;
         }
         float minDepth = powerData.getSampleDepth(i);

         // Search for end of this depth range:
         while (i < iEnd && valueRange.contains(values[i])) {
            i++;
         }
         float maxDepth = powerData.getSampleDepth(i);

         depthRanges.add(FloatRange.of(minDepth, maxDepth));
      }
      return depthRanges;
   }
}
