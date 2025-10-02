package no.imr.deepvision.lsss.engine.mapping;

import no.imr.deepvision.lsss.engine.data.DeepVisionFileInfo;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.tools.Utils;

import java.util.List;

final class IdentityDeepVisionMapping extends DeepVisionMapping {
   IdentityDeepVisionMapping(List<DeepVisionFileInfo> deepVisionFileInfos, DataFileSet lsssDataFileSet, float distanceBehindShip) {
      double distanceNmi = Utils.meterToNmi(distanceBehindShip);
      ClosestPingIndexFinder closestPingIndexFinder = (deepVisionTime, deepVisionGeoPos, prevLsssIndex) -> {
         PingIndex closestPingIndex = lsssDataFileSet.getContainingPingIndex(PingMapping.millisToTimeValue(deepVisionTime), PingMapping.TIME);
         if (closestPingIndex == null) {
            return null;
         }
         return lsssDataFileSet.getContainingPingIndex(closestPingIndex.getVesselDistance() - distanceNmi, PingMapping.DISTANCE);
      };
      for (DeepVisionFileInfo deepVisionFileInfo : deepVisionFileInfos) {
         addTimes(lsssDataFileSet, deepVisionFileInfo, closestPingIndexFinder);
      }
   }
}
