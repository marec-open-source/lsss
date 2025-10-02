package no.imr.lsss.util.growing;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.data.util.mask.DepthRangeExtractor;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;

import java.util.List;

public final class SchoolCandidateDepthRangeExtractor implements DepthRangeExtractor {
   private final LSSS lsss;
   private final DataFileSet dataFileSet;
   private final PingRange pingRange;
   private final int channel;

   public SchoolCandidateDepthRangeExtractor(LSSS lsss) {
      this.lsss = lsss;
      InterpretationSettings interpretationSettings = lsss.getInterpretationSettings();
      dataFileSet = interpretationSettings.getDataFileSet();
      pingRange = interpretationSettings.getPingRange();
      channel = interpretationSettings.getChannel();
   }

   @Override
   public List<FloatRange> depthRanges(PingIndex pingIndex) {
      if (!pingRange.contains(pingIndex)) {
         return List.of();
      }
      Ping ping = dataFileSet.getPing(pingIndex);
      PowerData powerData = ping.getPowerData(channel);
      if (powerData == null) {
         return List.of();
      }
      FloatRange boundaryDepthRange = lsss.getRegionManager().getLayerManager().getBoundaryDepthRange(pingIndex);
      FloatRange logSvRange = lsss.getRegionManager().getThresholdManager().getLogSvRange(pingIndex);
      List<FloatRange> depthRanges = DataUtils.findDepthRanges(powerData, boundaryDepthRange, powerData.getLogSv(), logSvRange);
      return subtractExistingSchools(pingIndex, depthRanges);
   }

   private List<FloatRange> subtractExistingSchools(PingIndex pingIndex, List<FloatRange> depthRanges) {
      List<FloatRange> schoolDepthRanges = lsss.getRegionManager().getSchoolManager().getSchools().stream()
            .flatMap(school -> school.getDepthRanges(pingIndex).getFloatRanges().stream())
            .toList();
      if (schoolDepthRanges.isEmpty()) {
         return depthRanges;
      }
      FloatRangeSet depthRangeSet = FloatRangeSet.of(depthRanges).subtract(FloatRangeSet.of(schoolDepthRanges));
      return depthRangeSet.getFloatRanges();
   }
}
