package no.imr.lsss.modules.trawl;

import no.imr.tools.ResourceUtils;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

final class SaData {
   private static final SaData INSTANCE = new SaData();

   private final Map<String, SaSpecies> nameToSaSpecies = new HashMap<>();

   private SaData() {
      try (BufferedReader reader = FileUtils.newBufferedReader(ResourceUtils.getUrl("no/imr/lsss/resources/modules/trawl/akustikk.nav"), Utils.UTF_8)) {
         SaSpecies saSpecies = null;
         while (true) {
            String line = reader.readLine();
            if (line == null) {
               break;
            }
            if (line.startsWith("F;")) {
               saSpecies = new SaSpecies.SaFish(line);
               continue;
            }
            if (line.startsWith("P;")) {
               saSpecies = new SaSpecies.SaPlankton(line);
               continue;
            }
            if (line.startsWith("A;") && saSpecies != null) {
               String[] parts = line.split(";");
               for (int i = 1; i < parts.length; i++) {
                  nameToSaSpecies.put(parts[i].trim(), saSpecies);
               }
            }
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
      }
   }

   static SaData getInstance() {
      return INSTANCE;
   }

   @Nullable SaSpecies getSaSpecies(String speciesName) {
      return nameToSaSpecies.get(speciesName);
   }
}
