package no.imr.deepvision.lsss.engine.mapping;

import no.imr.deepvision.lsss.engine.data.DeepVisionFileInfo;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.DataUtils;

import java.util.List;

final class GeoPosDeepVisionMapping extends DeepVisionMapping {
   private static final int SEARCH_CUTOFF = 5 * 60;  //search cutoff behind ship

   GeoPosDeepVisionMapping(List<DeepVisionFileInfo> deepVisionFileInfos, DataFileSet lsssDataFileSet) {
      ClosestPingIndexFinder closestPingIndexFinder = (deepVisionTime, deepVisionGeoPos, prevLsssIndex) -> {
         if (deepVisionGeoPos == null) {
            // fallback if geographical position is invalid: LSSS-time is equal to Deep Vision time
            return lsssDataFileSet.getContainingPingIndex(PingMapping.millisToTimeValue(deepVisionTime), PingMapping.TIME);
         }
         if (prevLsssIndex.equals(lsssDataFileSet.getTotalRange().begin())) {
            prevLsssIndex = lsssDataFileSet.getClosestPingIndex(PingMapping.millisToTimeValue(deepVisionTime) - SEARCH_CUTOFF, PingMapping.TIME);
         }
         PingRange pingRange = PingRange.ofUnsorted(prevLsssIndex, lsssDataFileSet.getClosestPingIndex(PingMapping.millisToTimeValue(deepVisionTime), PingMapping.TIME));
         return DataUtils.geoPosToClosestPingIndex(deepVisionGeoPos, lsssDataFileSet.getPingIndices(pingRange));
      };
      for (DeepVisionFileInfo deepVisionFileInfo : deepVisionFileInfos) {
         addTimes(lsssDataFileSet, deepVisionFileInfo, closestPingIndexFinder);
      }
   }
}
