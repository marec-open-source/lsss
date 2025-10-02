package no.imr.korona.data.util.geometry;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.ExtrapolatedPingIndex;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
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

   public static PingContainer listPingContainer(PingConfiguration pingConfiguration, List<PingIndex> pingIndices) {
      if (pingIndices.isEmpty()) {
         return emptyPingContainer();
      }
      return new PingContainer() {
         private final PingRange totalRange = PingRange.of(pingIndices.getFirst(), ExtrapolatedPingIndex.create(pingIndices));

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
            return DataUtils.getClosestPingIndex(pingIndices, value, pingMapping);
         }

         @Override
         public @Nullable PingIndex getContainingPingIndex(double value, PingMapping pingMapping) {
            return DataUtils.getContainingPingIndex(pingIndices, totalRange.end(), value, pingMapping);
         }
      };
   }

   public static PingContainer emptyPingContainer() {
      return new PingContainer() {
         private final PingConfiguration pingConfiguration = PingConfiguration.newEmpty();

         @Override
         public PingConfiguration getPingConfiguration() {
            return pingConfiguration;
         }

         @Override
         public PingRange getTotalRange() {
            return PingRange.EMPTY_RANGE;
         }

         @Override
         public PingIndex getClosestPingIndex(double value, PingMapping pingMapping) {
            return PingRange.EMPTY_RANGE.end();
         }

         @Override
         public @Nullable PingIndex getContainingPingIndex(double value, PingMapping pingMapping) {
            return null;
         }
      };
   }
}
