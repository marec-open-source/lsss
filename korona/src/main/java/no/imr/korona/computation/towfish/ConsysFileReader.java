package no.imr.korona.computation.towfish;

import no.imr.tools.Utils;
import no.imr.tools.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.logging.Level;

final class ConsysFileReader implements TowfishMetaData.MetadataFileReader {
   private static final String PRESSURE = "Pressure";
   private static final String EKVLG = "$EKVLG";
   private static final String LENGTH = "L";

   private final DateTimeFormatter dateTimeFormatter = Utils.createUTCDateTimeFormatter("dd.MM.yyyy,HH:mm:ss");

   ConsysFileReader() {
   }

   @Override
   public void updateMetaDataFileMap(Collection<Path> metaDataFiles, NavigableMap<Long, Path> metaDataFileMap) {
      //read first time and date in each file
      for (Path metaDataFile : metaDataFiles) {
         try (BufferedReader reader = Files.newBufferedReader(metaDataFile, Utils.ISO_8859_1)) {
            String s = reader.readLine();
            while (s != null) {
               try {
                  Instant instant = parseDate(s);
                  metaDataFileMap.put(instant.toEpochMilli(), metaDataFile);
                  break;
               } catch (DateTimeParseException _) {
                  Log.global.warning("Cannot parse date in string " + s + " in file " + metaDataFile);
               }
               s = reader.readLine();
            }
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error reading " + metaDataFile, e);
         }
      }
   }

   private Instant parseDate(String dateString) {
      if (dateString.length() < 20) {
         throw new DateTimeParseException("Date string too short", dateString, 0);
      }
      return dateTimeFormatter.parse(dateString.substring(0, 19), Instant::from);
   }

   @Override
   public void parseFiles(Collection<Path> metaDataFiles, Map<Long, Float> depthMap, TowfishMetaData.Function<Long, Float> vesselLogData, TowfishMetaData.Function<Long, Float> cableLengthData) {
      Map<Long, Float> vesselLogMap = vesselLogData.getMap();
      Map<Long, Float> cableLengthMap = cableLengthData.getMap();
      for (Path metaDataFile : metaDataFiles) {
         try (BufferedReader reader = Files.newBufferedReader(metaDataFile, Utils.ISO_8859_1)) {
            String s = reader.readLine();
            while (s != null) {
               if (s.length() >= 20) {
                  Instant instant;
                  try {
                     instant = parseDate(s);
                  } catch (DateTimeParseException _) {
                     Log.global.warning("Cannot parse date in string " + s + " in file " + metaDataFile);
                     s = reader.readLine();
                     continue;
                  }
                  long millis = instant.toEpochMilli();
                  s = s.replaceAll("=", " ");
                  s = s.replaceAll("   ", " ");
                  String[] split = s.substring(20).split("[, ]");
                  //printData(split);
                  if (split.length == 0) {
                     s = reader.readLine();
                     continue;
                  }
                  switch (split[0]) {
                     case PRESSURE -> {
                        depthMap.put(millis, parsePressure(split));
                     }
                     case EKVLG -> {
                        if (split.length >= 5) {
                           vesselLogMap.put(millis, parseVesselLog(split));
                        }
                     }
                     case LENGTH -> {
                        cableLengthMap.put(millis, parseCableLength(split));
                     }
                     default -> {
                     }
                  }
               }
               s = reader.readLine();
            }
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error reading " + metaDataFile, e);
         }
      }
   }

   private static float parsePressure(String[] pressureData) {
      float pressureDesiBar = Float.parseFloat(pressureData[1]);
      return pressureDesiBar * 10000 / (TowfishMetaData.WATER_DENSITY * TowfishMetaData.GRAVITY_ACCEL); //depth
   }

   private static float parseVesselLog(String[] vesselLogData) {
      return Float.parseFloat(vesselLogData[4]);
   }

   private static float parseCableLength(String[] cableLengthData) {
      String l = cableLengthData[1];
      l = l.replaceAll("m", "");
      return Float.parseFloat(l);
   }
}
