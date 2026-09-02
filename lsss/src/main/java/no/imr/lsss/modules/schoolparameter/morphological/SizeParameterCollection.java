package no.imr.lsss.modules.schoolparameter.morphological;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.School;
import no.imr.korona.util.KoronaUtils;
import no.imr.lsss.LSSS;
import no.imr.lsss.modules.schoolparameter.SchoolParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;

import java.util.List;
import java.util.Map;

public final class SizeParameterCollection implements MorphologicalParameterCollection {
   private static final SchoolParameter LENGTH = new SchoolParameter(new Name("length", "Length"), Unit.METER);
   private static final SchoolParameter HEIGHT = new SchoolParameter(new Name("height", "Height"), Unit.METER);
   private static final SchoolParameter CORRECTED_LENGTH = new SchoolParameter(new Name("correctedLength", "Corrected length"), Unit.METER);
   private static final SchoolParameter CORRECTED_HEIGHT = new SchoolParameter(new Name("correctedHeight", "Corrected height"), Unit.METER);

   private final LSSS lsss;

   public SizeParameterCollection(LSSS lsss) {
      this.lsss = lsss;
   }

   @Override
   public List<SchoolParameter> getParameters() {
      return List.of(
            LENGTH,
            HEIGHT,
            CORRECTED_LENGTH,
            CORRECTED_HEIGHT
      );
   }

   @Override
   public Map<String, Float> computeValues(DataFileSet dataFileSet, RegionManager regionManager, School school) {
      float length = computeLength(school);
      float height = computeHeight(dataFileSet, regionManager, school);

      float heightCorrection = 0;
      float lengthCorrection = 0;
      PingRange pingRange = school.getPingRange();
      int mainChannel = dataFileSet.firstChannelClosestTo(lsss.getConfigurationManager().getSurveyMiscConf().mainFrequency.getFloatValue());
      if (!pingRange.isEmpty() && mainChannel > 0) {
         float beamWidthAlongship = dataFileSet.getRawFileConfiguration().getTransducers().get(mainChannel - 1).getBeamWidthAlongship();

         PingIndex firstPingIndex = pingRange.begin();
         Ping firstPing = dataFileSet.getPing(firstPingIndex);
         PowerData firstPowerData = firstPing.getPowerData(mainChannel);
         if (firstPowerData != null) {
            heightCorrection = firstPowerData.getEffectivePulseDuration() * firstPowerData.getSoundVelocity();
            lengthCorrection += computeLengthCorrection(regionManager.getNonMaskedRegionDepthRanges(school, firstPingIndex).getFloatRanges(), firstPowerData, beamWidthAlongship);
         }

         PingIndex lastPingIndex = dataFileSet.previousOrSame(pingRange.end());
         Ping lastPing = dataFileSet.getPing(lastPingIndex);
         PowerData lastPowerData = lastPing.getPowerData(mainChannel);
         if (lastPowerData != null) {
            lengthCorrection += computeLengthCorrection(regionManager.getNonMaskedRegionDepthRanges(school, lastPingIndex).getFloatRanges(), lastPowerData, beamWidthAlongship);
         }
      }

      return Map.of(
            LENGTH.getPersistentName(), length,
            HEIGHT.getPersistentName(), height,
            CORRECTED_LENGTH.getPersistentName(), length - lengthCorrection,
            CORRECTED_HEIGHT.getPersistentName(), height - heightCorrection
      );
   }

   private static float computeLength(School school) {
      PingRange pingRange = school.getPingRange();
      return (float) KoronaUtils.nmiToMeter(pingRange.getVesselDistance());
   }

   private static float computeHeight(DataFileSet dataFileSet, RegionManager regionManager, School school) {
      return (float) dataFileSet.getPingIndexStream(school.getPingRange())
            .mapToDouble(pingIndex -> regionManager.getNonMaskedRegionDepthRanges(school, pingIndex).getBoundingRange().getSize())
            .reduce(0, Math::max);
   }

   private static float computeLengthCorrection(List<FloatRange> depthRanges, PowerData powerData, float beamWidthAlongship) {
      if (depthRanges.isEmpty()) {
         return 0;
      }
      float depth = (depthRanges.getFirst().min() + depthRanges.getLast().max()) / 2;
      float range = powerData.depthToRange(depth);
      return range * (float) Math.tan(Math.toRadians(beamWidthAlongship / 2));
   }
}
