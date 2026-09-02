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

final class MustFileReader implements TowfishMetaData.MetadataFileReader {
   private static final String COLUMN_SPLITTER = "\\s+, "; //Strings in MUST metadata file are " , " delimited (including spaces)

   private final DateTimeFormatter dateTimeFormatter = TimeUtils.createUTCDateTimeFormatter("yyyyMMddHHmmssSSS");
   private int iVesselLog;
   private int iDepth;

   MustFileReader() {
   }

   @Override
   public void updateMetaDataFileMap(Collection<Path> metaDataFiles, NavigableMap<Instant, Path> metaDataFileMap) {
      //read first time and date in each file
      for (Path metaDataFile : metaDataFiles) {
         try (BufferedReader reader = Files.newBufferedReader(metaDataFile, Utils.ISO_8859_1)) {
            // First line contains column headers
            String s = reader.readLine();
            if (s == null) {
               Log.global.warning("No header in file " + metaDataFile);
               continue;
            }
            iVesselLog = getColumn(s, "ShipER60VesselLog");  //Tag in MUST metadata file
            iDepth = getColumn(s, "CTDDepth");               //Tag in MUST metadata file

            if (iVesselLog == iDepth) {
               continue; // iVesselLog and iDepth cannot have same value (i.e. depth and log cannot be in same column): read next file
            }

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
      String[] split = s.split(COLUMN_SPLITTER);   //Strings are " , " delimited (including spaces)

      for (i = 0; i < split.length; i++) {
         if (split[i].contentEquals(sCompare)) {
            break;
         }
      }
      return i;
   }

   private Instant parseDate(String dateString) {
      if (dateString.length() < 16) {
         throw new DateTimeParseException("Date string too short", dateString, 0);
      }
      if (dateString.length() == 16 || (dateString.length() > 17 && dateString.charAt(17) == ',') || dateString.charAt(16) == ' ') {
         String str = dateString.substring(0, 16) + "0";    //Date in file is really yyyyMMddHHmmssSS (1/100 sec), so add zero to get SSS
         return dateTimeFormatter.parse(str, Instant::from);
      }
      throw new DateTimeParseException("Wrong format", dateString, 0);
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
               String[] split = s.split(COLUMN_SPLITTER);
               if (split.length != 44) {
                  //something wrong with this line: MUST meta-file should contain 44 columns
                  break;  // Try next file
               }
               Instant instant;
               try {
                  instant = parseDate(split[0]);
               } catch (DateTimeParseException _) {
                  Log.global.warning("Cannot parse date in string " + s + " in file " + metaDataFile);
                  s = reader.readLine();
                  continue;
               }

               vesselLogMap.put(instant, parseVesselLog(split[iVesselLog]));

               float depth = parseTowfishDepth(split[iDepth]);
               // Just guessing that cable length is 3 x depth: this file-type does not contain cable length
               float cableLength = 3.0f * depth;

               //No cable length in this file type
               cableLengthMap.put(instant, cableLength);

               depthMap.put(instant, depth);

               s = reader.readLine();
            }
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error reading " + metaDataFile, e);
         }
      }
   }

   private static float parseVesselLog(String vesselLog) {
      if (vesselLog.isEmpty()) {
         Log.global.warning("Cannot parse: setting vessel_log=0.0");
         return 0.0f;
      }
      return Float.parseFloat(vesselLog);
   }

   private static float parseTowfishDepth(String depthData) {
      if (depthData.isEmpty()) {
         Log.global.warning("Cannot parse depth of towfish. Setting depth=0.0m");
         return 0.0f;
      }
      return Float.parseFloat(depthData);
   }
}
