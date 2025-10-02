package no.imr.lsss.modules.trawl.biotic;

import no.imr.lsss.modules.trawl.biotic.pojo.BioticFile;
import no.imr.lsss.modules.trawl.biotic.pojo.BioticFishStation;
import no.imr.lsss.modules.trawl.biotic.pojo.BioticMission;
import no.imr.tools.xml.JaxbUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

public final class BioticUtils {
   private BioticUtils() {
   }

   public static BioticFile loadBioticFile(InputStream in) throws IOException {
      Object value = JaxbUtils.readIgnoringNamespace(BioticFile.class, in);
      return switch (value) {
         case BioticFile bioticFile -> bioticFile;
         case BioticMission bioticMission -> {
            BioticFile bioticFile = new BioticFile();
            bioticFile.missions.add(bioticMission);
            yield bioticFile;
         }
         default -> throw new IllegalStateException("Unknown biotic value: " + value);
      };
   }

   public static @Nullable Instant parseDateAndTime(@Nullable String date, @Nullable String time) {
      if (date == null) {
         return null;
      }
      LocalDate localDate = LocalDate.parse(removeTrailingZ(date));
      LocalTime localTime = time != null ? LocalTime.parse(removeTrailingZ(time)) : LocalTime.MIN;
      return localDate.atTime(localTime).toInstant(ZoneOffset.UTC);
   }

   private static String removeTrailingZ(String s) {
      return s.endsWith("Z") ? s.substring(0, s.length() - 1) : s;
   }

   public static String gearName(BioticFishStation station) {
      if (station.gear != null) {
         return "Trawl " + station.gear;
      }
      if (station.gearno != null) {
         return "Trawl " + station.gearno;
      }
      return "Trawl";
   }
}
