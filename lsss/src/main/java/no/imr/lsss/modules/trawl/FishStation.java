package no.imr.lsss.modules.trawl;

import no.imr.lsss.modules.trawl.biotic.BioticUtils;
import no.imr.lsss.modules.trawl.biotic.pojo.BioticFile;
import no.imr.lsss.modules.trawl.biotic.pojo.BioticFishStation;
import no.imr.lsss.modules.trawl.spd.SLine;
import no.imr.lsss.modules.trawl.spd.SpdStation;
import no.imr.tools.Utils;
import no.imr.tools.math.WelfordsMethod;
import no.imr.tools.range.FloatRange;
import no.imr.tools.time.TimeUtils;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

final class FishStation {
   static final DateTimeFormatter DATE_TIME_FORMATTER = TimeUtils.createUTCDateTimeFormatter("yyyy.MM.dd HH:mm");

   final int serialNumber;
   final int stationNumber;
   final String toolName;
   final @Nullable Instant startTime;
   final @Nullable Instant stopTime;
   final double longitude;
   final double latitude;
   final double logStart;
   final double logDistance;
   final FloatRange fishingDepth;
   private final String state;
   private final String quality;
   private final String toolCode;

   private final List<FishTarget> targets;

   private FishStation(SpdStation station) {
      SLine sLine = station.sLine();
      serialNumber = sLine.serialNo;
      stationNumber = sLine.stationNo;
      toolName = sLine.getToolName();
      startTime = sLine.parseTime(sLine.startTime);
      stopTime = sLine.parseTime(sLine.stopTime);
      longitude = sLine.getLongitude();
      latitude = sLine.getLatitude();
      logStart = sLine.startLog;
      logDistance = sLine.distance / 10;
      fishingDepth = sLine.getFishingDepth();
      state = Character.toString(sLine.state);
      quality = Character.toString(sLine.quality);
      toolCode = sLine.toolCode;

      targets = station.targets().stream()
            .map(FishTarget::new)
            .sorted(byWeightComparator())
            .toList();
   }

   private FishStation(BioticFishStation station) {
      serialNumber = station.serialnumber;
      stationNumber = station.station;

      toolName = BioticUtils.gearName(station);
      logStart = station.logstart;
      startTime = BioticUtils.parseDateAndTime(station.stationstartdate, station.stationstarttime);
      stopTime = BioticUtils.parseDateAndTime(station.stationstopdate, station.stationstoptime);
      longitude = station.longitudestart;
      latitude = station.latitudestart;
      logDistance = station.distance;
      fishingDepth = FloatRange.of(station.fishingdepthmin, station.fishingdepthmax);
      state = Objects.requireNonNullElse(station.gearcondition, "");
      quality = Objects.requireNonNullElse(station.samplequality, "");
      toolCode = Objects.requireNonNullElse(station.gearno, "");

      targets = station.catchsamples.stream()
            .map(FishTarget::new)
            .sorted(byWeightComparator())
            .toList();
   }

   static List<FishStation> fromSpd(List<SpdStation> stations) {
      return stations.stream()
            .map(FishStation::new)
            .toList();
   }

   static List<FishStation> fromBiotic(BioticFile bioticFile) {
      return bioticFile.missions.stream()
            .flatMap(mission -> mission.fishstations.stream())
            .map(FishStation::new)
            .toList();
   }

   String getStationInfo() {
      return Utils.format("%s   Station:%04d:%05d   Q:%s:%s[%s]   T:%s",
            startTime != null ? DATE_TIME_FORMATTER.format(startTime) : "Unspecified time",
            stationNumber, serialNumber,
            state, quality, SLine.getQualityString(state, quality),
            toolCode);
   }

   String getDepthRange() {
      return Utils.format("Depth: %.0f-%.0f m   Distance: %.3f nmi", fishingDepth.min(), fishingDepth.max(), logDistance);
   }

   List<FishTarget> getTargets() {
      return targets;
   }

   CategoryDataset getWeightDataset(Language language, boolean percent, boolean all) {
      DefaultCategoryDataset dataset = new DefaultCategoryDataset();
      String key = "Weight " + (percent ? "[%]" : "[kg]");

      if (targets.isEmpty()) {
         dataset.addValue(Double.NaN, key, "NO DATA AVAILABLE");
      } else {
         double totalWeight = targets.stream()
               .mapToDouble(FishTarget::getWeight)
               .sum();
         for (FishTarget target : targets) {
            if (!all && !target.isPlankton() && target.getIndividuals().isEmpty()) { // Remove non-plankton species without individuals
               continue;
            }
            double value = percent
                  ? 100 * target.getWeight() / totalWeight
                  : target.getWeight();
            dataset.addValue(value, key, target.getCatchName(language));
         }
      }
      return dataset;
   }

   CategoryDataset getLengthDataset(Language language, boolean all) {
      DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
      String key = "Mean length [cm]";

      if (targets.isEmpty()) {
         dataset.add(Double.NaN, Double.NaN, key, "NO DATA AVAILABLE");
      } else {
         for (FishTarget target : targets) {
            if (!all && target.getIndividuals().isEmpty()) {
               continue;
            }
            WelfordsMethod welfordsMethod = target.getFishLengths();
            dataset.add(welfordsMethod.getMean(), welfordsMethod.getStdDev(), key, target.getCatchName(language));
         }
      }
      return dataset;
   }

   CategoryDataset getSaDataset(Language language, boolean percent, boolean gr0, double threshold, boolean zooplankton) {
      DefaultCategoryDataset dataset = new DefaultCategoryDataset();
      String key = "Calculated sA" + (percent ? " [%]" : "");

      if (targets.isEmpty()) {
         dataset.addValue(Double.NaN, key, "NO DATA AVAILABLE");
      } else {
         List<FishTarget> selectedTargets = targets.stream()
               .filter(target -> {
                  if (!target.isPlankton() && target.getIndividuals().isEmpty()) { // Remove non-plankton species without individuals
                     return false;
                  }
                  if (target.getSaSpecies() == null) { // Remove species with unknown sa
                     return false;
                  }
                  if (target.isPlankton() && !zooplankton) { // Remove zooplankton if Z-button not pressed
                     return false;
                  }
                  return true;
               })
               .toList();

         double totalSa = selectedTargets.stream()
               .mapToDouble(target -> target.getSa(threshold) / logDistance)
               .sum();
         double gr0Sa = 0;

         for (FishTarget target : selectedTargets) {
            double sa = target.getSa(threshold) / logDistance;
            if (gr0 && target.is0Group()) {
               gr0Sa += sa;
               continue;
            }
            double value = percent
                  ? 100 * sa / totalSa
                  : sa;
            dataset.addValue(value, key, target.getCatchName(language));
         }

         if (gr0) {
            double value = percent
                  ? 100 * gr0Sa / totalSa
                  : gr0Sa;
            dataset.addValue(value, key, "0-group");
         }
      }
      return dataset;
   }

   private static Comparator<FishTarget> byWeightComparator() {
      return Comparator.comparingDouble(FishTarget::getWeight).reversed();
   }
}
