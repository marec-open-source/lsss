package no.imr.korona.region.schooledit;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.PingRangeBuilder;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.region.SchoolBoundaryObject;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.tools.CyclicList;

import java.util.ArrayList;
import java.util.List;

final class OutlineMoveSettings {
   private final SchoolBoundaryObject schoolBoundary;
   private CyclicList<EchogramPoint> movedBoundary = new CyclicList<>();

   OutlineMoveSettings(SchoolBoundaryObject schoolBoundary) {
      this.schoolBoundary = schoolBoundary;
   }

   PingRange move(EchogramPoint startPoint, EchogramPoint point, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      float dx = pingSettings.pingIndexToX(point.pingIndex()) - pingSettings.pingIndexToX(startPoint.pingIndex());
      DepthTransform depthTransform = zSettings.getDepthTransform();
      float dz = depthTransform.depthToZ(point) - depthTransform.depthToZ(startPoint);

      PingRangeBuilder pingRangeBuilder = new PingRangeBuilder();
      pingRangeBuilder.add(schoolBoundary.getPingRange().begin());
      pingRangeBuilder.add(schoolBoundary.getPingRange().end());

      CyclicList<EchogramPoint> newMovedBoundary = new CyclicList<>();
      for (EchogramPoint echogramPoint : schoolBoundary.getBoundary()) {
         float z = depthTransform.depthToZ(echogramPoint) + dz;
         float x = pingSettings.pingIndexToX(echogramPoint.pingIndex()) + dx;
         PingIndex newPingIndex = pingSettings.xToClosestPingIndex(x);
         newMovedBoundary.add(new EchogramPoint(newPingIndex, depthTransform.zToDepth(z, newPingIndex)));

         pingRangeBuilder.add(echogramPoint.pingIndex());
         pingRangeBuilder.add(newPingIndex);
      }
      movedBoundary = newMovedBoundary;

      return pingRangeBuilder.build(pingSettings.getPingContainer());
   }

   List<EchogramPoint> getMovedBoundary() {
      return movedBoundary;
   }

   List<List<EchogramPoint>> getSortedEditPoints() {
      return boundaryToSortedPoints(movedBoundary);
   }

   List<List<EchogramPoint>> getSortedToBeRemovedPoints() {
      return boundaryToSortedPoints(schoolBoundary.getBoundary());
   }

   private static List<List<EchogramPoint>> boundaryToSortedPoints(CyclicList<EchogramPoint> boundary) {
      if (boundary.isEmpty()) {
         return List.of();
      }
      List<List<EchogramPoint>> list = new ArrayList<>();
      list.addAll(SchoolEditUtils.leftToRightLists(boundary));
      // Close the boundary.
      list.addAll(SchoolEditUtils.leftToRightLists(boundary.subList(-1, 1)));
      return list;
   }
}
