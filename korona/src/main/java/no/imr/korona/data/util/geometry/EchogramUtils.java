package no.imr.korona.data.util.geometry;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.ExtrapolatedPingIndex;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.math.MathUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EchogramUtils {
   private EchogramUtils() {
   }

   public static List<EchogramPoint> computeLine(DepthTransform depthTransform, EchogramPoint fromPoint, EchogramPoint toPoint, PingContainer pingContainer) {
      PingRange pingRange = PingRange.ofUnsorted(fromPoint.pingIndex(), toPoint.pingIndex());
      if (pingRange.getPingCount() <= 1) {
         // No intermediate points.
         return List.of(fromPoint, toPoint);
      }
      // Linear interpolation of intermediate points.
      boolean forward = fromPoint.pingIndex().compareTo(toPoint.pingIndex()) < 0;
      EchogramPoint startPoint = forward ? fromPoint : toPoint;
      EchogramPoint endPoint = forward ? toPoint : fromPoint;
      float startZ = depthTransform.depthToZ(startPoint);
      float endZ = depthTransform.depthToZ(endPoint);
      float deltaZ = (endZ - startZ) / pingRange.getPingCount();
      long startPingNumber = startPoint.pingIndex().getPingNumber();
      List<EchogramPoint> points = new ArrayList<>(pingRange.getPingCount() + 1);
      pingContainer.getPingIndices(pingRange).forEach(pingIndex -> {
         float z = startZ + (pingIndex.getPingNumber() - startPingNumber) * deltaZ;
         points.add(new EchogramPoint(pingIndex, depthTransform.zToDepth(z, pingIndex)));
      });
      points.add(endPoint);
      if (!forward) {
         Collections.reverse(points);
      }
      return points;
   }

   public static List<EchogramPoint> addMissingPoints(List<EchogramPoint> curve, PingContainer pingContainer, DepthTransform depthTransform) {
      List<EchogramPoint> result = new ArrayList<>();
      EchogramPoint previous = null;
      for (EchogramPoint point : curve) {
         if (previous != null && Math.abs(point.pingIndex().getPingNumber() - previous.pingIndex().getPingNumber()) > 1) {
            List<EchogramPoint> line = computeLine(depthTransform, previous, point, pingContainer);
            result.addAll(line.subList(1, line.size() - 1));
         }
         result.add(point);
         previous = point;
      }
      return result;
   }

   public static double computeCircumference(List<EchogramPoint> boundary) {
      if (boundary.isEmpty()) {
         return 0;
      }
      double distance = 0;
      EchogramPoint previousPoint = boundary.getLast();
      for (EchogramPoint point : boundary) {
         distance += computeDistance(previousPoint, point);
         previousPoint = point;
      }
      return distance;
   }

   private static double computeDistance(EchogramPoint a, EchogramPoint b) {
      double horizontalDist = KoronaUtils.nmiToMeter(a.pingIndex().getVesselDistance() - b.pingIndex().getVesselDistance());
      double verticalDist = a.depth() - b.depth();
      return MathUtils.hypot(horizontalDist, verticalDist);
   }

   public static PingContainer listPingContainer(PingConfiguration pingConfiguration, List<PingIndex> pingIndices) {
      return new PingContainer() {
         private final PingRange totalRange = pingIndices.isEmpty()
               ? PingRange.EMPTY_RANGE
               : PingRange.of(pingIndices.getFirst(), ExtrapolatedPingIndex.create(pingIndices));

         @Override
         public PingConfiguration getPingConfiguration() {
            return pingConfiguration;
         }

         @Override
         public PingRange getTotalRange() {
            return totalRange;
         }

         @Override
         public PingIndex getClosestPingIndex(double value, PingMapping pingMapping) {
            return DataUtils.getClosestPingIndex(pingIndices, totalRange.end(), value, pingMapping);
         }

         @Override
         public @Nullable PingIndex getContainingPingIndex(double value, PingMapping pingMapping) {
            return DataUtils.getContainingPingIndex(pingIndices, totalRange.end(), value, pingMapping);
         }
      };
   }
}
