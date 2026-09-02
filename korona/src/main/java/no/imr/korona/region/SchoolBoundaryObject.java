package no.imr.korona.region;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.tools.math.MathUtils;
import no.imr.tools.range.IntRange;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class SchoolBoundaryObject {
   private static final long PING_RANGE_INTERVAL = 100;

   private final List<EchogramPoint> boundary;
   private final ListMultimap<PingRange, IntRange> pingRangeToIndexRanges = ArrayListMultimap.create();
   private final PingRange pingRange;

   public SchoolBoundaryObject(List<EchogramPoint> boundary, PingContainer pingContainer) {
      this.boundary = boundary;
      pingRange = updateIndexRangeMap(pingContainer);
   }

   @Override
   public String toString() {
      return "{pingRange=" + pingRange + ", boundary=" + boundary + '}';
   }

   private PingRange updateIndexRangeMap(PingContainer pingContainer) {
      PingRange totalRange = pingContainer.getTotalRange();
      long firstPingNumber = totalRange.begin().getPingNumber();
      long lastPingNumber = totalRange.end().getPingNumber();

      EchogramPoint firstPoint = boundary.getFirst();
      PingIndex minPingIndex = firstPoint.pingIndex();
      PingIndex maxPingIndex = minPingIndex;

      //also update the bounding boxes along the school outline
      int index = 0;
      int lastIndex = 0;

      PingRange currentRange = PingRange.EMPTY_RANGE;
      for (EchogramPoint echogramPoint : boundary) {
         PingIndex pingIndex = echogramPoint.pingIndex();
         long pingNumber = pingIndex.getPingNumber();
         if (pingNumber < minPingIndex.getPingNumber()) {
            minPingIndex = pingIndex;
         } else if (pingNumber > maxPingIndex.getPingNumber()) {
            maxPingIndex = pingIndex;
         }

         if (!currentRange.isEmpty() && (pingNumber == currentRange.begin().getPingNumber() ||
               pingNumber == currentRange.end().getPingNumber())) {
            pingRangeToIndexRanges.put(currentRange, new IntRange(lastIndex, index + 1));
            currentRange = PingRange.EMPTY_RANGE;
            lastIndex = index;
            firstPoint = boundary.get(index);
         }

         if (currentRange.isEmpty()) {
            if (pingNumber > firstPoint.pingIndex().getPingNumber()) {
               long lastRangeIndex = firstPoint.pingIndex().getPingNumber() + PING_RANGE_INTERVAL;
               if (lastRangeIndex > lastPingNumber) {
                  lastRangeIndex = lastPingNumber;
               }
               PingIndex endRangeIndex = pingContainer.getPingIndex(lastRangeIndex);
               currentRange = PingRange.ofUnsorted(firstPoint.pingIndex(), endRangeIndex);
            } else if (pingNumber < firstPoint.pingIndex().getPingNumber()) {
               long firstRangeIndex = firstPoint.pingIndex().getPingNumber() - PING_RANGE_INTERVAL;
               if (firstRangeIndex < firstPingNumber) {
                  firstRangeIndex = firstPingNumber;
               }
               PingIndex startRangeIndex = pingContainer.getPingIndex(firstRangeIndex);
               currentRange = PingRange.ofUnsorted(startRangeIndex, firstPoint.pingIndex());
            }
         }
         index++;
      }
      if (lastIndex != index && !currentRange.isEmpty()) {
         pingRangeToIndexRanges.put(currentRange, new IntRange(lastIndex, index));
      }
      return PingRange.ofUnsorted(minPingIndex, maxPingIndex);
   }

   public List<EchogramPoint> getBoundary() {
      return boundary;
   }

   public PingRange getPingRange() {
      return pingRange;
   }

   static Collection<PingRange> sortedPingRanges(Collection<PingRange> pingRanges, float x, EchogramPingSettings pingSettings) {
      if (pingRanges.size() <= 1) {
         return pingRanges;
      }
      return pingRanges.stream()
            .sorted(Comparator.comparingDouble(pingRange -> imageSpaceDistance(pingSettings, pingRange, x)))
            .toList();
   }

   static float imageSpaceDistance(EchogramPingSettings pingSettings, PingRange pingRange, float x) {
      float xBegin = pingSettings.pingIndexToX(pingRange.begin());
      if (xBegin >= x) {
         return xBegin - x;
      }
      float xEnd = pingSettings.pingIndexToX(pingRange.end());
      if (xEnd <= x) {
         return x - xEnd;
      }
      return 0;
   }

   private static EchogramPoint imagePointToClosestEchogramPoint(EchogramPingSettings pingSettings, EchogramZSettings zSettings, Point2D point) {
      PingIndex pingIndex = pingSettings.xToClosestPingIndex(point.getX());
      return new EchogramPoint(pingIndex, zSettings.yToDepth(point.getY(), pingIndex));
   }

   private static Point2D.Float echogramPointToImagePoint(EchogramPingSettings pingSettings, EchogramZSettings zSettings, EchogramPoint point) {
      return new Point2D.Float(pingSettings.pingIndexToX(point.pingIndex()), zSettings.depthToY(point.depth(), point.pingIndex()));
   }

   public SchoolBoundaryIntersectionInfo getClosestIntersection(School school, EchogramPoint point, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      //todo: needs to be improved
      double closestDistSquared = Double.MAX_VALUE;
      Point2D.Float imagePoint = echogramPointToImagePoint(pingSettings, zSettings, point);
      int closestIndexA = 0;
      // Sort the ping ranges according to distance from this point.
      Collection<PingRange> pingRanges = sortedPingRanges(pingRangeToIndexRanges.keySet(), imagePoint.x, pingSettings);

      for (PingRange range : pingRanges) {
         double startDiff = imagePoint.getX() - pingSettings.pingIndexToX(range.begin());
         double endDiff = imagePoint.getX() - pingSettings.pingIndexToX(range.end());
         //if start and end of range is farther away than the closest distance, all the echogram points in the range will also be farther away.
         if (startDiff * startDiff > closestDistSquared &&
               endDiff * endDiff > closestDistSquared) {
            continue;
         }
         //search the index ranges for the closest point.
         for (IntRange indexRange : pingRangeToIndexRanges.get(range)) {
            int firstIndex = indexRange.begin();
            int lastIndex = indexRange.end();
            Point2D startPoint = echogramPointToImagePoint(pingSettings, zSettings, boundary.get(firstIndex));

            for (int i = firstIndex; i < lastIndex - 1; i++) {
               Point2D endPoint = echogramPointToImagePoint(pingSettings, zSettings, boundary.get(i + 1));

               double dist = Line2D.ptSegDistSq(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY(),
                     imagePoint.getX(), imagePoint.getY());
               if (dist < closestDistSquared) {
                  closestDistSquared = dist;
                  closestIndexA = i;
               } else if (dist == closestDistSquared) {
                  // if the line is extending to the right, we choose this one
                  if (boundary.get(i + 1).pingIndex().compareTo(point.pingIndex()) > 0) {
                     closestIndexA = i;
                  }
               }
               startPoint = endPoint;
            }
         }
      }
      double firstPointDiff = imagePoint.getX() - pingSettings.pingIndexToX(boundary.getFirst().pingIndex());
      if (firstPointDiff * firstPointDiff < closestDistSquared) {
         //test between start and end point
         EchogramPoint pointA = boundary.getLast();
         EchogramPoint pointB = boundary.getFirst();
         Point2D startPoint = echogramPointToImagePoint(pingSettings, zSettings, pointA);
         Point2D endPoint = echogramPointToImagePoint(pingSettings, zSettings, pointB);

         double dist = Line2D.ptSegDistSq(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY(),
               imagePoint.getX(), imagePoint.getY());
         if (dist < closestDistSquared) {
            closestDistSquared = dist;
            closestIndexA = boundary.size() - 1;
         } else if (dist == closestDistSquared) {
            // if the line is extending to the right, we choose this one
            if (boundary.getFirst().pingIndex().compareTo(point.pingIndex()) > 0) {
               closestIndexA = boundary.size() - 1;
            }
         }
      }
      EchogramPoint pointA = boundary.get(closestIndexA);
      EchogramPoint pointB = boundary.get((closestIndexA + 1) % boundary.size());

      //linear interpolation between closestIndexA and closestIndexB
      Point2D startPoint = echogramPointToImagePoint(pingSettings, zSettings, pointA);
      Point2D endPoint = echogramPointToImagePoint(pingSettings, zSettings, pointB);

      double distanceSquared = startPoint.distanceSq(endPoint);
      double intersectionDistSquared = startPoint.distanceSq(imagePoint) - closestDistSquared;
      double s = distanceSquared > 0 ? Math.sqrt(intersectionDistSquared / distanceSquared) : 0;

      EchogramPoint closestPoint;
      if (s <= 0) {
         closestPoint = pointA;
      } else if (s >= 1) {
         closestPoint = pointB;
      } else {
         Point2D interpolatedPoint = new Point2D.Double(
               MathUtils.interpolate(startPoint.getX(), endPoint.getX(), s),
               MathUtils.interpolate(startPoint.getY(), endPoint.getY(), s)
         );
         closestPoint = imagePointToClosestEchogramPoint(pingSettings, zSettings, interpolatedPoint);
      }
      return new SchoolBoundaryIntersectionInfo(school, this, closestIndexA, closestPoint, closestDistSquared);
   }
}
