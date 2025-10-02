package no.imr.lsss.database.reports;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class GetIces {
   private static final Map<Integer, String> ACOUSTIC_CATEGORY_MAP = loadAcousticCategories();
   private static final Map<Short, String> NATION_MAP = loadNations();
   public static final String UNKNOWN_ACOUSTIC_CATEGORY = "UNK";

   private GetIces() {
   }

   private static Map<Integer, String> loadAcousticCategories() {
      Path file = LSSS.getInstallationDir().resolve("data").resolve("ices").resolve("IcesAcousticCategory.xml");
      try {
         return XmlUtils.readDocument(file).getRootElement().elements().stream()
               .collect(Collectors.toUnmodifiableMap(
                     i -> Integer.parseInt(i.attributeValue("acousticCategory")),
                     i -> i.attributeValue("ices")));
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Cannot load " + file, e);
         return Map.of();
      }
   }

   private static Map<Short, String> loadNations() {
      Path file = LSSS.getInstallationDir().resolve("data").resolve("ices").resolve("IcesNation.xml");
      try {
         return XmlUtils.readDocument(file).getRootElement().elements().stream()
               .collect(Collectors.toUnmodifiableMap(
                     i -> Short.parseShort(i.attributeValue("nation")),
                     i -> i.attributeValue("ices")));
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Cannot load " + file, e);
         return Map.of();
      }
   }

   public static String acousticCategory(AcousticCategory acousticCategory) {
      return ACOUSTIC_CATEGORY_MAP.getOrDefault(acousticCategory.getCompId().getAcousticCategory(), UNKNOWN_ACOUSTIC_CATEGORY);
   }

   public static String nation(Nation nation) {
      return NATION_MAP.getOrDefault(nation.getNation(), "??");
   }
}
