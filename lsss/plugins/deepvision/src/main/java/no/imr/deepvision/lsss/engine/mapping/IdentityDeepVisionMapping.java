package no.imr.deepvision.lsss.engine.mapping;

import no.imr.deepvision.lsss.engine.data.DeepVisionFileInfo;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.util.KoronaUtils;

import java.util.List;

final class IdentityDeepVisionMapping extends DeepVisionMapping {
   IdentityDeepVisionMapping(List<DeepVisionFileInfo> deepVisionFileInfos, DataFileSet lsssDataFileSet, float distanceBehindShip) {
      double distanceNmi = KoronaUtils.meterToNmi(distanceBehindShip);
      ClosestPingIndexFinder closestPingIndexFinder = (deepVisionTime, _, _) -> {
         PingIndex closestPingIndex = lsssDataFileSet.getContainingPingIndex(PingMapping.instantToTimeValue(deepVisionTime), PingMapping.TIME);
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
