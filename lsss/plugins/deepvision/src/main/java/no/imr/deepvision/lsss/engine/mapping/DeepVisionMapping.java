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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DeepVisionMapping {
   private final Map<DeepVisionFileInfo, TimeInterpolator> timeMapping = new HashMap<>();
   private final Map<DeepVisionFileInfo, DistanceInterpolator> distanceMapping = new HashMap<>();

   @FunctionalInterface
   interface ClosestPingIndexFinder {
      @Nullable PingIndex getClosestPingIndex(long deepVisionTime, @Nullable GeoPoint deepVisionGeoPos, PingIndex prevLsssIndex);
   }

   DeepVisionMapping() {
   }

   void addTimes(DataFileSet lsssDataFileSet, DeepVisionFileInfo deepVisionFileInfo, ClosestPingIndexFinder closestPingIndexFinder) {
      int numPoints = 0;
      long prevLsssTime = -1;
      PingIndex prevLsssPingIndex = lsssDataFileSet.getTotalRange().begin();
      List<DeepVisionFrame> frames = deepVisionFileInfo.getDeepVisionFile().frames.frames;
      long[] deepVisionTimes = new long[frames.size()];
      long[] lsssTimes = new long[frames.size()];
      float[] athwartShipDistance = new float[frames.size()];
      ElapsedTime elapsedTime = null;
      int interval = 5000; // 5 seconds
      boolean containsPositions = frames.stream().anyMatch(DeepVisionDataUtils::validPosition);
      for (DeepVisionFrame frame : frames) {
         long dvTime = DeepVisionDataUtils.timeInMillis(frame);
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
            long timeDiff = dvTime - pingIndex.getTimeInMillis();
            if (numPoints > 0) {
               long clampedTimeDiff = Math.clamp(timeDiff, 0, Math.max(0, dvTime - prevLsssTime));
               prevLsssTime = dvTime - clampedTimeDiff;
            } else {
               prevLsssTime = dvTime - timeDiff;
            }
            prevLsssPingIndex = lsssDataFileSet.getClosestPingIndex(PingMapping.millisToTimeValue(prevLsssTime), PingMapping.TIME);
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

   public long deepVisionTimeToLsssTime(long deepVisionTimeMillis, DeepVisionFileInfo deepVisionFileInfo) {
      TimeInterpolator timeInterpolator = timeMapping.get(deepVisionFileInfo);
      if (timeInterpolator == null) {
         return 0;
      }
      return timeInterpolator.deepVisionTimeToLsssTime(deepVisionTimeMillis);
   }

   public Range<Long> deepVisionTimeRangeToLsssTimeRange(Range<Long> deepVisionTimeRangeMillis, DeepVisionFileInfo deepVisionFileInfo) {
      return new DefaultRange<>(
            deepVisionTimeToLsssTime(deepVisionTimeRangeMillis.begin(), deepVisionFileInfo),
            deepVisionTimeToLsssTime(deepVisionTimeRangeMillis.end(), deepVisionFileInfo));
   }

   public long lsssTimeToDeepVisionTime(long lsssTimeMillis, DeepVisionFileInfo deepVisionFileInfo) {
      TimeInterpolator timeInterpolator = timeMapping.get(deepVisionFileInfo);
      if (timeInterpolator == null) {
         return 0;
      }
      return timeInterpolator.lsssTimeToDeepVisionTime(lsssTimeMillis);
   }

   public float deepVisionTimeToAthwartDistanceMeters(long deepVisionTimeMillis, DeepVisionFileInfo deepVisionFileInfo) {
      DistanceInterpolator distanceInterpolator = distanceMapping.get(deepVisionFileInfo);
      if (distanceInterpolator == null) {
         return 0;
      }
      return distanceInterpolator.deepVisionTimeToAthwartDistance(deepVisionTimeMillis);
   }
}
