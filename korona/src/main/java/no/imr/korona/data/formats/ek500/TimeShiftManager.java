package no.imr.korona.data.formats.ek500;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;

final class TimeShiftManager {
   private static final int MAX_FILES_PER_FREQUENCY = 100;

   private static final LoadingCache<Path, DirectoryTimeShift> CACHE = CacheBuilder.newBuilder()
         .maximumSize(10)
         .build(CacheLoader.from(DirectoryTimeShift::new));

   private TimeShiftManager() {
   }

   static DirectoryTimeShift getDirectoryTimeShift(Path directory) {
      return CACHE.getUnchecked(directory);
   }

   /**
    * Representative time shifts for all files in a directory.
    */
   static final class DirectoryTimeShift {
      private final Map<Integer, Duration> transceiverAndFrequencyToTimeShift = new HashMap<>();

      private DirectoryTimeShift(Path directory) {
         Map<Integer, List<Duration>> map = new HashMap<>();
         Map<Integer, Integer> fileCounts = new HashMap<>();

         List<Path> files;
         try {
            files = FileUtils.listFiles(directory);
            files.sort(null);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, e.getMessage(), e);
            files = List.of();
         }
         for (Path file : files) {
            String fileName = file.getFileName().toString();
            if (fileName.endsWith(EK500DataFormatPlugin.TIME_SUFFIX)) {
               Matcher matcher = EK500FileSet.PATTERN.matcher(fileName);
               if (matcher.matches()) {
                  try {
                     int frequency = Integer.parseInt(matcher.group(2));
                     if (frequency == 0) {
                        continue;
                     }
                     int transceiver = Integer.parseInt(matcher.group(3));

                     int key = getKey(frequency, transceiver);
                     if (fileCounts.merge(key, 1, Integer::sum) > MAX_FILES_PER_FREQUENCY) {
                        continue;
                     }
                     List<Duration> timeShifts = map.computeIfAbsent(key, _ -> new ArrayList<>());

                     String baseName = fileName.substring(0, fileName.length() - EK500DataFormatPlugin.TIME_SUFFIX.length());
                     Path infoFile = directory.resolve(baseName + EK500DataFormatPlugin.INFO_SUFFIX);
                     Path pingFile = directory.resolve(baseName + EK500DataFormatPlugin.PING_SUFFIX);

                     InfoRecord infoRecord = new InfoRecord(infoFile);
                     List<IndexRecord> indexRecords = EK500Utils.readIndexRecords(pingFile, infoRecord);
                     List<TimeRecord> timeRecords = EK500Utils.readTimeRecords(file, infoRecord);
                     int n = Math.min(indexRecords.size(), timeRecords.size());

                     for (int i = 0; i < n; i++) {
                        IndexRecord indexRecord = indexRecords.get(i);
                        TimeRecord timeRecord = timeRecords.get(i);
                        Duration timeShift = indexRecord.getInstant().until(timeRecord.getInstant(indexRecord));
                        timeShifts.add(timeShift);
                     }
                  } catch (IOException e) {
                     Log.global.log(Level.INFO, "Error analyzing " + file, e);
                  }
               }
            }
         }

         for (Map.Entry<Integer, List<Duration>> entry : map.entrySet()) {
            List<Duration> timeShifts = entry.getValue();
            if (!timeShifts.isEmpty()) {
               timeShifts.sort(null);
               transceiverAndFrequencyToTimeShift.put(entry.getKey(), timeShifts.get(timeShifts.size() / 2));
            }
         }
      }

      Duration getTimeShift(InfoRecord infoRecord) {
         Duration timeShift = transceiverAndFrequencyToTimeShift.get(getKey(infoRecord.frequency, infoRecord.transceiver));
         return timeShift != null ? timeShift : Duration.ZERO;
      }

      private static int getKey(int frequency, int transceiver) {
         return frequency * 1000 + transceiver;
      }
   }
}
