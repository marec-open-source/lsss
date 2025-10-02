package no.imr.lsss.modules.trawl;

import no.imr.tools.ResourceUtils;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

final class NameTranslation {
   private static final NameTranslation INSTANCE = new NameTranslation();

   private final List<String> norwegian = new ArrayList<>();
   private final List<String> latin = new ArrayList<>();
   private final List<String> english = new ArrayList<>();

   private NameTranslation() {
      try (BufferedReader reader = FileUtils.newBufferedReader(ResourceUtils.getUrl("no/imr/lsss/resources/modules/trawl/spd31tax.csv"), Utils.UTF_8)) {
         String input;
         while ((input = reader.readLine()) != null) {
            String[] parts = input.split(";");
            if (parts.length > 3) {
               norwegian.add(parts[1]);
               latin.add(parts[2]);
               english.add(parts[3]);
            }
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
      }
   }

   static NameTranslation getInstance() {
      return INSTANCE;
   }

   private int indexOf(String speciesName) {
      if (norwegian.contains(speciesName)) {
         return norwegian.indexOf(speciesName);
      } else { // check substring
         for (String st : norwegian) {
            if (st.contains(speciesName)) {
               return norwegian.indexOf(st);
            }
         }
         String equivalent = switch (speciesName) {
            // Hard code herring equivalents
            case "SILD'G03" -> "NORSK VÅRGYTENDE SILD";
            case "SILD'G05" -> "NORDSJØSILD";
            case "SILD'G07" -> "KVITSJØSILD";
            case "SILD'G14" -> "ROMSDALSFJORDSILD";

            // and some other
            case "STORHAVNÅL" -> "STOR HAVNÅL";
            case "LITENHAVNÅL" -> "LITEN HAVNÅL";

            // the rest
            case "KRÅKEBOLLEFA" -> "KRÅKEBOLLEFAMILIEN";
            default -> speciesName;
         };
         return norwegian.indexOf(equivalent);
      }
   }

   private String lookUp(String speciesName, List<String> list) {
      int i = indexOf(speciesName);
      return i < 0 ? speciesName : list.get(i);
   }

   String translate(Language language, String speciesName) {
      return switch (language) {
         case ENGLISH -> lookUp(speciesName, english);
         case NORWEGIAN -> speciesName;
         case LATIN -> lookUp(speciesName, latin);
      };
   }
}
