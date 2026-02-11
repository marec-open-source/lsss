package no.imr.lsss.modules.interpretation;

import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.ChannelInterpretation;
import no.imr.korona.region.Interpretation;
import no.imr.korona.region.Region;
import no.imr.lsss.LSSS;
import no.imr.lsss.modules.integration.IntegrationArea;
import no.imr.lsss.modules.integration.RegionIntegrationModule;
import no.imr.tools.listening.Listener;
import no.imr.tools.swing.WorkerDialog;
import no.marec.lsss.api.util.observing.Subscription;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InterpretationUtils {
   private InterpretationUtils() {
   }

   public static void setLowerThresholdAndKeepAssignedSa(LSSS lsss, float minLogSv) {
      RegionIntegrationModule regionIntegrationModule = lsss.getModuleManager().getModule(RegionIntegrationModule.class);
      PingRange visiblePingRange = lsss.getInterpretationSettings().getPingRange();
      int channel = lsss.getInterpretationSettings().getChannel();

      record RegionInfo(Region region, Map<Integer, Float> originalAssignments, float originalSa) {
      }

      List<RegionInfo> regionInfos = lsss.getRegionManager().getSelectedRegions().stream()
            .filter(region -> region.getPingRange().intersects(visiblePingRange))
            .map(region -> new RegionInfo(region,
                  region.getChannelInterpretation(channel).getAssignments(),
                  regionIntegrationModule.getSa(region, IntegrationArea.TOTAL)))
            .toList();

      Listener listener = () -> {
         for (RegionInfo regionInfo : regionInfos) {
            float newSa = regionIntegrationModule.getSa(regionInfo.region, IntegrationArea.TOTAL);
            if (newSa == 0) {
               continue;
            }
            Interpretation interpretation = regionInfo.region.getInterpretation();
            Set<Integer> restSpecies = interpretation.getRestSpecies();
            ChannelInterpretation channelInterpretation = interpretation.getChannelInterpretation(channel);
            regionInfo.originalAssignments.forEach((acousticCategoryId, originalAssignment) -> {
               if (!restSpecies.contains(acousticCategoryId)) {
                  // We want that originalSa * originalAssignment = newSa * newAssignment.
                  float newAssignment = regionInfo.originalSa * originalAssignment / newSa;
                  channelInterpretation.setAssignment(acousticCategoryId, newAssignment);
               }
            });
            interpretation.updateRestInterpretation();
         }
         lsss.getRegionManager().getInterpretationChangeManager().notifyListeners(regionInfos);
      };
      Subscription subscription = regionIntegrationModule.getRegionIntegrationChangeManager().subscribe(listener);
      try {
         lsss.getRegionManager().getThresholdManager().set(visiblePingRange, null, minLogSv, null);
         new WorkerDialog(lsss::getReferenceComponent, "Waiting for computations...")
               .start(asyncHandle -> {
                  while (true) {
                     if (regionIntegrationModule.isIdle() || asyncHandle.isCancelled()) {
                        return;
                     }
                     asyncHandle.sleep(10);
                  }
               });
      } finally {
         subscription.unsubscribe();
      }
   }
}
