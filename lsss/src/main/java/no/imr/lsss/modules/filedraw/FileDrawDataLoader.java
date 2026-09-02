package no.imr.lsss.modules.filedraw;

import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FilePredicates;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.time.TimeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;

final class FileDrawDataLoader {
   private static final Predicate<Path> FILE_FILTER = FilePredicates.endsWithIgnoringCase(".txt");
   private static final DateTimeFormatter DATE_TIME_FORMATTER = TimeUtils.createUTCDateTimeFormatter("yyyyMMddHHmmssSSS");

   private FileDrawDataLoader() {
   }

   static List<FileDrawLine> loadFiles(Path dir, AsyncHandle asyncHandle) {
      List<FileDrawLine> lines = new ArrayList<>();
      try {
         for (Path file : FileUtils.listFiles(dir, asyncHandle, FILE_FILTER)) {
            if (asyncHandle.isCancelled()) {
               return List.of();
            }
            try (BufferedReader reader = Files.newBufferedReader(file, Utils.ISO_8859_1)) {
               lines.add(readFileDrawLine(reader));
            } catch (Exception e) {
               Log.global.log(Level.WARNING, "Error loading file " + file, e);
            }
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error accessing files in " + dir, e);
      }
      return List.copyOf(lines);
   }

   static FileDrawLine readFileDrawLine(BufferedReader reader) throws IOException {
      List<FileDrawPoint> points = new ArrayList<>();
      while (true) {
         String line = reader.readLine();
         if (line == null) {
            break;
         }
         line = line.trim();
         if (line.startsWith("#")) {
            continue; // Comment
         }
         if (line.startsWith("LSSS")) {
            continue; // Old format header
         }
         if (line.matches("\\d+")) {
            continue; // Old format line count
         }
         if (line.isEmpty()) {
            continue;
         }

         String[] tokens = line.split("\\s+");
         try {
            String dateTime = tokens[0] + tokens[1];
            if (dateTime.length() < 17) {
               dateTime += "0".repeat(17 - dateTime.length());
            } else if (dateTime.length() > 17) {
               dateTime = dateTime.substring(0, 17);
            }
            Instant time = DATE_TIME_FORMATTER.parse(dateTime, Instant::from);
            float depth = Float.parseFloat(tokens[2]);
            points.add(new FileDrawPoint(time, depth));
         } catch (Exception e) {
            throw new IOException("Invalid line " + line, e);
         }
      }
      return new FileDrawLine(List.copyOf(points));
   }
}
