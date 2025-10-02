package no.imr.lsss.modules.reflog;

import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public final class LoaderRefCsv {
   private LoaderRefCsv() {
   }

   static List<LogLine> load(Path file) throws IOException {
      try (BufferedReader in = Files.newBufferedReader(file, Utils.ISO_8859_1)) {
         return load(in);
      }
   }

   static List<LogLine> load(BufferedReader in) throws IOException {
      List<LogLine> logLines = new ArrayList<>();
      String header = in.readLine();
      if (header == null) {
         Log.global.warning("No header");
         return logLines;
      }
      String[] tokens = header.replace("\"", "").split(",");
      if (tokens.length <= 7) {
         Log.global.warning("Invalid header: " + header);
         return logLines;
      }
      String date = tokens[7];

      String fieldExplanation = nextNonEmptyLine(in);
      if (fieldExplanation == null) {
         Log.global.warning("No field explanation");
         return logLines;
      }
      List<String> allNames = Arrays.asList(fieldExplanation.replace("\"", "").split(","));
      if (allNames.size() < 2) {
         return List.of();
      }
      int locStNoIndex = Utils.indexOfIgnoringCase(allNames, "Loc.St.no");

      List<LogLineField> fields = IntStream.range(2, allNames.size()) // Skip station type and time.
            .mapToObj(allNames::get)
            .map(name -> new LogLineField(name, ""))
            .toList();

      DateTimeFormatter dateTimeFormatter = Utils.createUTCDateTimeFormatter("dd.MM.yyyy HH:mm:ss");

      while (true) {
         String line = in.readLine();
         if (line == null) {
            break;
         }

         String[] allValues = line.replace("\"", "").split(",", -1);
         if (allValues.length < 2) {
            continue;
         }

         String stationType = allValues[0];

         // Accepts two different time formats:
         // 1. hh:mm:ss
         // 2. hhmmss.xx
         String time = allValues[1];
         if (time.length() < 7) {
            continue;
         }
         if (time.charAt(6) == '.') {
            // Convert hhmmss.xx to hh:mm:ss
            time = time.substring(0, 2) + ":" + time.substring(2, 4) + ":" + time.substring(4, 6);
         }
         long timeInMillis;
         try {
            timeInMillis = dateTimeFormatter.parse(date + " " + time, Instant::from).toEpochMilli();
         } catch (DateTimeParseException e) {
            continue;
         }

         String localStationNumber = Utils.getOrDefault(allValues, locStNoIndex, "");
         boolean start = stationType.contains("start");
         ActivityType activityType = stationTypeToActivityType(stationType);

         List<String> fieldValues = IntStream.range(2, allNames.size()) // Skip station type and time.
               .mapToObj(i -> Utils.getOrDefault(allValues, i, ""))
               .toList();

         logLines.add(new LogLine(timeInMillis, activityType, start,
               stationType, localStationNumber,
               fields, fieldValues));
      }
      return logLines;
   }

   private static @Nullable String nextNonEmptyLine(BufferedReader bufferedReader) throws IOException {
      while (true) {
         String line = bufferedReader.readLine();
         if (line == null || !line.isEmpty()) {
            return line;
         }
      }
   }

   public static ActivityType stationTypeToActivityType(String stationType) {
      if (stationType.contains("CTD")) {
         return ActivityType.CTD;
      }
      if (stationType.contains("Plankton-net")) {
         return ActivityType.PLANKTON_NET;
      }
      if (stationType.contains("Pelagic trawl")) {
         return ActivityType.PELAGIC_TRAWL;
      }
      if (stationType.contains("Bottom trawl")) {
         return ActivityType.BOTTOM_TRAWL;
      }
      if (stationType.contains("Mocness")) {
         return ActivityType.MOCNESS;
      }
      if (stationType.contains("Bergen")) {
         return ActivityType.BERGEN;
      }
      return ActivityType.OTHER;
   }
}
