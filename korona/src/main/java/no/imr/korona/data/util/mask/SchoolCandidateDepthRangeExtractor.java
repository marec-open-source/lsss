package no.imr.korona.data.util.mask;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.region.RegionManager;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;

public final class SchoolCandidateDepthRangeExtractor implements DepthRangeExtractor {
   private final RegionManager regionManager;
   private final DataFileSet dataFileSet;
   private final int channel;

   public SchoolCandidateDepthRangeExtractor(RegionManager regionManager, DataFileSet dataFileSet, int channel) {
      this.regionManager = regionManager;
      this.dataFileSet = dataFileSet;
      this.channel = channel;
   }

   @Override
   public FloatRangeSet depthRanges(PingIndex pingIndex) {
      Ping ping = dataFileSet.getPing(pingIndex);
      PowerData powerData = ping.getPowerData(channel);
      if (powerData == null) {
         return FloatRangeSet.of();
      }
      FloatRange boundaryDepthRange = regionManager.getLayerManager().getBoundaryDepthRange(pingIndex);
      FloatRange logSvRange = regionManager.getThresholdManager().getLogSvRange(pingIndex);
      FloatRangeSet depthRanges = FloatRangeSet.of(DataUtils.findDepthRanges(powerData, boundaryDepthRange, powerData.getLogSv(), logSvRange));
      FloatRangeSet schoolDepthRanges = regionManager.getSchoolManager().depthRangesForPingIndex(pingIndex);
      return depthRanges.subtract(schoolDepthRanges);
   }
}
