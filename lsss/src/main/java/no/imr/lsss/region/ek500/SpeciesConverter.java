package no.imr.lsss.region.ek500;

import no.imr.lsss.LSSS;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

final class SpeciesConverter {
   private static final int MAX_COMMON_SPECIES = 69;   //The first 69 species are common to all IMR/Norwegian ships
   private static final int MIN_COMMON_SPECIES = 31;   //BEI inherited the 31 first species from ND10
   private static final int COMMON_GROUP_LOW = 41;   //BEI inherited some species groups from ND10
   private static final int COMMON_GROUP_HIGH = 52;   //Do not know upper limit
   private static final int NORWAY = 58;

   private int currentNation;
   private int currentPlatform;

   private final Map<Integer, Integer> speciesMap = new HashMap<>();
   private final Path speciesMapFile;

   SpeciesConverter() {
      speciesMapFile = LSSS.getInstallationDir().resolve("data").resolve("ek500").resolve("SpeciesMap.txt");
   }

   void setPlatform(int nation, int platform) {
      if (currentNation != nation || currentPlatform != platform) {
         currentNation = nation;
         currentPlatform = platform;
         try {
            read();
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error reading file " + speciesMapFile, e);
         }
      }
   }

   private void read() throws IOException {
      speciesMap.clear();
      try (BufferedReader bufferedReader = Files.newBufferedReader(speciesMapFile, Utils.ISO_8859_1)) {
         for (int i = 1; true; i++) {
            String line = bufferedReader.readLine();
            if (line == null) {
               break;
            }

            line = line.trim();
            if (line.startsWith("#")) {
               continue;
            }

            String[] words = line.split("\\s+", 5);
            if (words.length < 4) {
               Log.global.warning("Error parsing line " + i + " in file " + speciesMapFile + ": " + line);
               continue;
            }

            try {
               int lsssSpecies = Integer.parseInt(words[0]);
               int nation = Integer.parseInt(words[1]);
               int platform = Integer.parseInt(words[2]);
               int beiSpecies = Integer.parseInt(words[3]);
               if (nation == currentNation && platform == currentPlatform) {
                  speciesMap.put(beiSpecies, lsssSpecies);
               }
            } catch (NumberFormatException e) {
               Log.global.log(Level.WARNING, "Error parsing line " + i + " in file " + speciesMapFile + ": " + line, e);
            }
         }
      }
   }

   private boolean isCommonSpecies(int beiSpecies) {
      if (currentNation == NORWAY) {
         return beiSpecies <= MAX_COMMON_SPECIES;
      } else {
         return beiSpecies <= MIN_COMMON_SPECIES
               || (beiSpecies >= COMMON_GROUP_LOW && beiSpecies <= COMMON_GROUP_HIGH);
      }
   }

   int beiToLSSS(int beiSpecies) {
      Integer lsssSpecies = speciesMap.get(beiSpecies);
      if (lsssSpecies != null) {
         return lsssSpecies;
      }
      if (isCommonSpecies(beiSpecies)) {
         return beiSpecies;
      }
      return -1;
   }
}
