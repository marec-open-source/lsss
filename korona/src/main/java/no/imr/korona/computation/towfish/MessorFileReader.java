package no.imr.korona.computation.towfish;

import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.time.TimeUtils;

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

final class MessorFileReader implements TowfishMetaData.MetadataFileReader {
   private static final String COLUMN_SPLITTER = "\\t"; //Strings in MESSOR metadata file are "\\t" (tab) delimited

   private final DateTimeFormatter dateTimeFormatter = TimeUtils.createUTCDateTimeFormatter("ddMMyyyyHHmmss");
   private int iVesselLog;
   private int iDepth;

   MessorFileReader() {
   }

   @Override
   public void updateMetaDataFileMap(Collection<Path> metaDataFiles, NavigableMap<Instant, Path> metaDataFileMap) {
      //read first time and date in each file
      for (Path metaDataFile : metaDataFiles) {
         try (BufferedReader reader = Files.newBufferedReader(metaDataFile, Utils.ISO_8859_1)) {
            // First line contains headers. Get column for vessel-log and depth
            String s = reader.readLine();
            if (s == null) {
               Log.global.warning("No header in file " + metaDataFile);
               continue;
            }
            iVesselLog = getColumn(s, "EK_Log-Dist");      //Tag in MESSOR metadata file
            iDepth = getColumn(s, "CTD_Depth(m)");         //Tag in MESSOR metadata file

            //start on second line
            s = reader.readLine();
            while (s != null) {
               try {
                  Instant instant = parseDate(s);
                  metaDataFileMap.put(instant, metaDataFile);
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

   private static int getColumn(String s, String sCompare) {
      int i;
      String[] split = s.split(COLUMN_SPLITTER);

      for (i = 0; i < split.length; i++) {
         if (split[i].contentEquals(sCompare)) {
            break;
         }
      }
      return i;
   }

   private Instant parseDate(String dateSString) {
      if (dateSString.length() < 14) {
         throw new DateTimeParseException("Date string too short", dateSString, 0);
      }
      return dateTimeFormatter.parse(dateSString.substring(0, 14), Instant::from);
   }

   @Override
   public void parseFiles(Collection<Path> metaDataFiles, Map<Instant, Float> depthMap,
                          TowfishMetaData.Function<Instant, Float> vesselLogData,
                          TowfishMetaData.Function<Instant, Float> cableLengthData) {
      Map<Instant, Float> vesselLogMap = vesselLogData.getMap();
      Map<Instant, Float> cableLengthMap = cableLengthData.getMap();
      for (Path metaDataFile : metaDataFiles) {
         try (BufferedReader reader = Files.newBufferedReader(metaDataFile, Utils.ISO_8859_1)) {
            //start on second line
            reader.readLine();
            String s = reader.readLine();
            while (s != null) {
               String[] split = s.split(COLUMN_SPLITTER);  //String  <tab> delimited. Some data columns may contain two-word strings
               if (split.length < 28) {
                  //something wrong with this line
                  s = reader.readLine();
                  continue;
               }
               Instant instant;
               try {
                  instant = parseDate(split[0]);
               } catch (DateTimeParseException _) {
                  Log.global.warning("Cannot parse date in string " + s + " in file " + metaDataFile);
                  s = reader.readLine();
                  continue;
               }

               //vesselLogMap.put(millis, parseVesselLog(split[8]));
               try {
                  vesselLogMap.put(instant, parseVesselLog(split[iVesselLog]));
               } catch (NumberFormatException _) {
                  Log.global.warning("Supposed to be vessel log: " + split[iVesselLog] + " in file " + metaDataFile);
                  s = reader.readLine();
                  continue;
               }

               //todo: no cable length in this file type??
               cableLengthMap.put(instant, 0f);

               //depthMap.put(millis, parseTowfishDepth(split[26]));
               depthMap.put(instant, parseTowfishDepth(split[iDepth]));

               s = reader.readLine();
            }
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error reading " + metaDataFile, e);
         }
      }
   }

   private static float parseVesselLog(String vesselLog) {
      return Float.parseFloat(vesselLog);
   }

   private static float parseTowfishDepth(String depthData) {
      return Float.parseFloat(depthData);
   }
}
