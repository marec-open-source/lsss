package no.imr.deepvision.lsss.engine.mapping;

import no.imr.deepvision.lsss.engine.data.DeepVisionDataUtils;
import no.imr.deepvision.lsss.engine.data.DeepVisionFileInfo;
import no.imr.deepvision.lsss.engine.data.pojo.DeepVisionFrame;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.Range;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DeepVisionMapping {
   private final Map<DeepVisionFileInfo, TimeInterpolator> timeMapping = new HashMap<>();
   private final Map<DeepVisionFileInfo, DistanceInterpolator> distanceMapping = new HashMap<>();

   @FunctionalInterface
   interface ClosestPingIndexFinder {
      @Nullable PingIndex getClosestPingIndex(Instant deepVisionTime, @Nullable GeoPoint deepVisionGeoPos, PingIndex prevLsssIndex);
   }

   DeepVisionMapping() {
   }

   void addTimes(DataFileSet lsssDataFileSet, DeepVisionFileInfo deepVisionFileInfo, ClosestPingIndexFinder closestPingIndexFinder) {
      int numPoints = 0;
      Instant prevLsssTime = null;
      PingIndex prevLsssPingIndex = lsssDataFileSet.getTotalRange().begin();
      List<DeepVisionFrame> frames = deepVisionFileInfo.getDeepVisionFile().frames.frames;
      Instant[] deepVisionTimes = new Instant[frames.size()];
      Instant[] lsssTimes = new Instant[frames.size()];
      float[] athwartShipDistance = new float[frames.size()];
      ElapsedTime elapsedTime = null;
      Duration interval = Duration.ofSeconds(5);
      boolean containsPositions = frames.stream().anyMatch(DeepVisionDataUtils::validPosition);
      for (DeepVisionFrame frame : frames) {
         Instant dvTime = DeepVisionDataUtils.time(frame);
         elapsedTime = ElapsedTime.checkElapsedTime(elapsedTime, dvTime, interval);
         if (elapsedTime.inInterval()) {
            continue;
         }
         GeoPoint geoPos = DeepVisionDataUtils.geoPoint(frame);
         if (containsPositions && geoPos == null) {
            continue;
         }
         elapsedTime = new ElapsedTime(dvTime, interval);
         PingIndex pingIndex = closestPingIndexFinder.getClosestPingIndex(dvTime, geoPos, prevLsssPingIndex);
         if (pingIndex != null) {
            long timeDiff = pingIndex.getInstant().until(dvTime, ChronoUnit.NANOS);
            if (prevLsssTime != null) {
               long clampedTimeDiff = Math.clamp(timeDiff, 0, Math.max(0, prevLsssTime.until(dvTime, ChronoUnit.NANOS)));
               prevLsssTime = dvTime.minusNanos(clampedTimeDiff);
            } else {
               prevLsssTime = dvTime.minusNanos(timeDiff);
            }
            prevLsssPingIndex = lsssDataFileSet.getClosestPingIndex(PingMapping.instantToTimeValue(prevLsssTime), PingMapping.TIME);
            float athwart = containsPositions ? DistanceInterpolator.computeAthwartDistance(lsssDataFileSet, geoPos, prevLsssPingIndex) : 0;

            deepVisionTimes[numPoints] = dvTime;
            lsssTimes[numPoints] = prevLsssTime;
            athwartShipDistance[numPoints] = athwart;
            numPoints++;
         }
      }
      if (numPoints < frames.size()) {
         deepVisionTimes = Arrays.copyOfRange(deepVisionTimes, 0, numPoints);
         lsssTimes = Arrays.copyOfRange(lsssTimes, 0, numPoints);
         athwartShipDistance = Arrays.copyOfRange(athwartShipDistance, 0, numPoints);
      }
      timeMapping.put(deepVisionFileInfo, new TimeInterpolator(deepVisionTimes, lsssTimes));
      distanceMapping.put(deepVisionFileInfo, new DistanceInterpolator(deepVisionTimes, athwartShipDistance));
   }

   public Instant deepVisionTimeToLsssTime(Instant deepVisionTime, DeepVisionFileInfo deepVisionFileInfo) {
      TimeInterpolator timeInterpolator = timeMapping.get(deepVisionFileInfo);
      if (timeInterpolator == null) {
         return Instant.EPOCH;
      }
      return timeInterpolator.deepVisionTimeToLsssTime(deepVisionTime);
   }

   public Range<Instant> deepVisionTimeRangeToLsssTimeRange(Range<Instant> deepVisionTimeRange, DeepVisionFileInfo deepVisionFileInfo) {
      return new DefaultRange<>(
            deepVisionTimeToLsssTime(deepVisionTimeRange.begin(), deepVisionFileInfo),
            deepVisionTimeToLsssTime(deepVisionTimeRange.end(), deepVisionFileInfo));
   }

   public Instant lsssTimeToDeepVisionTime(Instant lsssTime, DeepVisionFileInfo deepVisionFileInfo) {
      TimeInterpolator timeInterpolator = timeMapping.get(deepVisionFileInfo);
      if (timeInterpolator == null) {
         return Instant.EPOCH;
      }
      return timeInterpolator.lsssTimeToDeepVisionTime(lsssTime);
   }

   public float deepVisionTimeToAthwartDistanceMeters(Instant deepVisionTime, DeepVisionFileInfo deepVisionFileInfo) {
      DistanceInterpolator distanceInterpolator = distanceMapping.get(deepVisionFileInfo);
      if (distanceInterpolator == null) {
         return 0;
      }
      return distanceInterpolator.deepVisionTimeToAthwartDistance(deepVisionTime);
   }
}
