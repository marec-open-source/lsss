package no.imr.korona.region;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.tools.CyclicList;
import no.imr.tools.range.IntRange;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public final class SchoolBoundaryObject {
   private static final long PING_RANGE_INTERVAL = 100;

   private final CyclicList<EchogramPoint> boundary;
   private final ListMultimap<PingRange, IntRange> pingRangeToIndexRanges = ArrayListMultimap.create();
   private final PingRange pingRange;

   public SchoolBoundaryObject(CyclicList<EchogramPoint> boundary, PingContainer pingContainer) {
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

   public CyclicList<EchogramPoint> getBoundary() {
      return boundary;
   }

   public PingRange getPingRange() {
      return pingRange;
   }

   static void sortPingRanges(List<PingRange> pingRanges, PingIndex thisIndex, EchogramPingSettings pingSettings) {
      pingRanges.sort((o1, o2) -> {
         boolean o1Contains = o1.contains(thisIndex);
         boolean o2Contains = o2.contains(thisIndex);
         if (o1Contains && !o2Contains) {
            return -1;
         }
         if (!o1Contains && o2Contains) {
            return 1;
         }
         double d1 = Math.min(imageSpaceDistance(pingSettings, thisIndex, o1.begin()), imageSpaceDistance(pingSettings, thisIndex, o1.end()));
         double d2 = Math.min(imageSpaceDistance(pingSettings, thisIndex, o2.begin()), imageSpaceDistance(pingSettings, thisIndex, o2.end()));
         return Double.compare(d1, d2);
      });
   }

   private static double imageSpaceDistance(EchogramPingSettings pingSettings, PingIndex firstIndex, PingIndex secondIndex) {
      return Math.abs(pingSettings.pingIndexToX(firstIndex) - pingSettings.pingIndexToX(secondIndex));
   }

   private static @Nullable EchogramPoint imagePointToEchogramPoint(EchogramPingSettings pingSettings, EchogramZSettings zSettings, Point2D point) {
      PingIndex pingIndex = pingSettings.xToContainingPingIndex(point.getX());
      return pingIndex != null ? new EchogramPoint(pingIndex, zSettings.yToDepth(point.getY(), pingIndex)) : null;
   }

   private static Point2D.Float echogramPointToImagePoint(EchogramPingSettings pingSettings, EchogramZSettings zSettings, EchogramPoint point) {
      return new Point2D.Float(pingSettings.pingIndexToX(point.pingIndex()), zSettings.depthToY(point.depth(), point.pingIndex()));
   }

   public SchoolBoundaryIntersectionInfo getClosestIntersection(EchogramPoint point, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      //todo: needs to be improved
      double closestDistSquared = Double.MAX_VALUE;
      EchogramPoint closestPoint;
      Point2D imagePoint = echogramPointToImagePoint(pingSettings, zSettings, point);
      int closestIndexA = -1;
      int closestIndexB = -1;
      //sort the ping ranges according to distance from this point
      List<PingRange> pingRanges = new ArrayList<>(pingRangeToIndexRanges.keySet());
      PingIndex thisIndex = point.pingIndex();

      sortPingRanges(pingRanges, thisIndex, pingSettings);

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
                  closestIndexA = i < boundary.size() ? i : -1;
                  closestIndexB = i + 1 < boundary.size() ? i + 1 : 0;
               } else if (dist == closestDistSquared) {
                  // if the line is extending to the right, we choose this one
                  if (boundary.get(i + 1).pingIndex().compareTo(point.pingIndex()) > 0) {
                     closestIndexA = i < boundary.size() ? i : -1;
                     closestIndexB = i + 1 < boundary.size() ? i + 1 : 0;
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
            closestIndexA = -1;
            closestIndexB = 0;
         } else if (dist == closestDistSquared) {
            // if the line is extending to the right, we choose this one
            if (boundary.getFirst().pingIndex().compareTo(point.pingIndex()) > 0) {
               closestIndexA = -1;
               closestIndexB = 0;
            }
         }
      }
      EchogramPoint pointA = closestIndexA < 0 ? boundary.getLast() : boundary.get(closestIndexA);
      EchogramPoint pointB = boundary.get(closestIndexB);

      //linear interpolation between closestIndexA and closestIndexB
      Point2D startPoint = echogramPointToImagePoint(pingSettings, zSettings, pointA);
      Point2D endPoint = echogramPointToImagePoint(pingSettings, zSettings, pointB);

      double distanceSquared = Point2D.distanceSq(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
      double intersectionDistSquared = Point2D.distanceSq(startPoint.getX(), startPoint.getY(), imagePoint.getX(), imagePoint.getY()) - closestDistSquared;
      double s = distanceSquared > 0 ? Math.sqrt(intersectionDistSquared / distanceSquared) : 0;
      s = Math.clamp(s, 0, 1);

      if (s <= 0) {
         closestPoint = pointA;
      } else if (s >= 1) {
         closestPoint = pointB;
      } else {
         Point2D interpolatedPoint = new Point2D.Double(startPoint.getX() + s * (endPoint.getX() - startPoint.getX()),
               startPoint.getY() + s * (endPoint.getY() - startPoint.getY()));
         EchogramPoint echogramPoint = imagePointToEchogramPoint(pingSettings, zSettings, interpolatedPoint);
         if (echogramPoint == null) {
            closestPoint = s < 0.5 ? pointA : pointB;
         } else {
            closestPoint = echogramPoint;
         }
      }
      return new SchoolBoundaryIntersectionInfo(this, closestIndexA, closestIndexB, closestPoint, closestDistSquared);
   }
}
