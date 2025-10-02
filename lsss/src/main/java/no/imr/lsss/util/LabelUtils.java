package no.imr.lsss.util;

import com.google.common.collect.ImmutableSet;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class LabelUtils {
   private LabelUtils() {
   }

   public static void add(List<Region> regions, String label) {
      writable(regions).forEach(region -> region.addLabel(label));
   }

   public static void addAcousticCategories(List<Region> regions, LSSS lsss) {
      Map<Integer, AcousticCategory> acousticCategoryMap = lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticCategoryMap();
      writable(regions).forEach(region -> {
         List<String> categoryLabels = region.getChannelInterpretation(lsss.getInterpretationSettings().getChannel()).getAssignments().keySet().stream()
               .map(acousticCategory -> lsss.getConfigurationManager().getLanguageUtils().getAcCatInitials(acousticCategoryMap.get(acousticCategory)))
               .toList();
         region.addLabels(categoryLabels);
      });
   }

   public static void remove(List<Region> regions, String label) {
      writable(regions).forEach(region -> region.removeLabel(label));
   }

   public static void removeAll(List<Region> regions) {
      writable(regions).forEach(region -> region.setLabels(ImmutableSet.of()));
   }

   public static void select(RegionManager regionManager, String label) {
      regionManager.selectRegions(region -> region.hasLabel(label));
   }

   public static void deselect(RegionManager regionManager, String label) {
      regionManager.deselectRegions(region -> region.hasLabel(label));
   }

   public static void retain(RegionManager regionManager, String label) {
      regionManager.deselectRegions(region -> !region.hasLabel(label));
   }

   private static Stream<Region> writable(List<Region> regions) {
      return regions.stream()
            .filter(Region::isWritable);
   }
}
