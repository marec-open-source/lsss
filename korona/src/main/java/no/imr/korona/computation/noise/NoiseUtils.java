package no.imr.korona.computation.noise;

import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.util.TvgArray;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.math.Median;
import no.imr.tools.time.DateTimeMillis;
import no.imr.tools.time.TimeUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Predicate;

public final class NoiseUtils {
   public static final String MEDIAN_NOISE_SUBFOLDER = "medianNoiseHistory";
   public static final String FILE_NOISE_FILE_PREFIX = "file_noise";
   static final String DAY_NOISE_DIR = "day_noise";
   static final String DAY_NOISE_LOW_DIR = "day_noise_low";
   static final String DAY_NOISE_HIGH_DIR = "day_noise_high";
   private static final String SURVEY_NOISE_MEDIAN_FILE_PREFIX = "survey_noise";
   private static final String SURVEY_NOISE_LOW_FILE_PREFIX = "survey_noise_low";
   private static final String SURVEY_NOISE_HIGH_FILE_PREFIX = "survey_noise_high";

   private NoiseUtils() {
   }

   public static float medianNoise(PowerData powerData, int count) {
      float[] sv = powerData.getSv();
      TvgArray tvg = powerData.getTVGArray();
      float[] tmpNoise = new float[Math.min(sv.length, count)];
      float delta = (float) (sv.length - 1) / (tmpNoise.length + 1); // +1 to pick in interior.
      for (int i = 0; i < tmpNoise.length; i++) {
         int sampleIndex = Math.round(delta * (i + 1));
         tmpNoise[i] = sv[sampleIndex] / tvg.get(sampleIndex);
      }
      return Median.quickSelect(tmpNoise);
   }

   private static Instant parseTimeDate(String fileName) throws ParseException {
      String[] fileNameParts = fileName.split("[_.]");
      String dateString = "NaN";
      String timeString = "NaN";
      for (String fileNamePart : fileNameParts) {
         if (fileNamePart.startsWith("D") && fileNamePart.length() > 1) {
            dateString = fileNamePart.substring(1);
         } else if (fileNamePart.startsWith("T") && fileNamePart.length() > 1) {
            timeString = fileNamePart.substring(1);
         }
      }
      if (dateString.equals("NaN")) {
         throw new ParseException("Could not find date from file name " + fileName, 0);
      }
      if (timeString.equals("NaN")) {
         throw new ParseException("Could not find time from file name " + fileName, 0);
      }
      DateTimeFormatter dateTimeFormatter = TimeUtils.createUTCDateTimeFormatter("yyyyMMddHHmmss");

      return dateTimeFormatter.parse(dateString + timeString, Instant::from);
   }

   public static List<Path> listNoiseFiles(Path directory, Predicate<Path> predicate) throws IOException {
      List<Path> files = FileUtils.listFiles(directory, predicate);
      files.sort(null);
      return files;
   }

   public static @Nullable Path findClosestNoiseFileBefore(Instant time, List<Path> fileList) {
      NavigableMap<Instant, Path> timeToFileMap = new TreeMap<>();
      for (Path file : fileList) {
         try {
            Instant instant = parseTimeDate(file.getFileName().toString());
            timeToFileMap.put(instant, file);
         } catch (ParseException e) {
            Log.global.warning("Could not parse time/date of " + file + ": " + e);
         }
      }
      if (timeToFileMap.isEmpty()) {
         return null;
      }
      Instant closestTimeBefore = timeToFileMap.firstKey();
      for (Instant instant : timeToFileMap.keySet()) {
         if (instant.isAfter(time)) {
            return timeToFileMap.get(closestTimeBefore);
         }
         closestTimeBefore = instant;
      }
      return timeToFileMap.get(closestTimeBefore);
   }

   public static NavigableMap<Integer, PerChannelNoiseFile.NoiseData> medianNoiseData(List<PerChannelNoiseFile> noiseDataList) {
      Map<Integer, List<Float>> combined = new TreeMap<>();

      for (PerChannelNoiseFile perChannelNoiseFile : noiseDataList) {
         for (Map.Entry<Integer, PerChannelNoiseFile.NoiseData> integerNoiseDataEntry : perChannelNoiseFile.getEntries()) {
            List<Float> floats = combined.computeIfAbsent(integerNoiseDataEntry.getKey(), _ -> new ArrayList<>());
            floats.add(integerNoiseDataEntry.getValue().ne());
         }
      }
      NavigableMap<Integer, PerChannelNoiseFile.NoiseData> result = new TreeMap<>();
      for (Map.Entry<Integer, List<Float>> integerListEntry : combined.entrySet()) {
         List<Float> noiseValues = integerListEntry.getValue();
         float[] noiseValueArray = new float[noiseValues.size()];
         for (int i = 0; i < noiseValues.size(); i++) {
            noiseValueArray[i] = noiseValues.get(i);
         }
         float ne = Median.quickSelect(noiseValueArray);
         result.put(integerListEntry.getKey(), new PerChannelNoiseFile.NoiseData(ne, ne * 2));
      }
      return result;
   }

   public static NavigableMap<Integer, PerChannelNoiseFile.NoiseData> lowNoiseData(List<PerChannelNoiseFile> noiseDataList) {
      NavigableMap<Integer, PerChannelNoiseFile.NoiseData> result = new TreeMap<>();

      for (PerChannelNoiseFile perChannelNoiseFile : noiseDataList) {
         for (Map.Entry<Integer, PerChannelNoiseFile.NoiseData> integerNoiseDataEntry : perChannelNoiseFile.getEntries()) {
            float ne = integerNoiseDataEntry.getValue().ne();
            PerChannelNoiseFile.NoiseData value = result.get(integerNoiseDataEntry.getKey());
            if (value == null || ne < value.ne()) {
               value = new PerChannelNoiseFile.NoiseData(ne, ne * 2);
               result.put(integerNoiseDataEntry.getKey(), value);
            }
         }
      }
      return result;
   }

   public static NavigableMap<Integer, PerChannelNoiseFile.NoiseData> highNoiseData(List<PerChannelNoiseFile> noiseDataList) {
      NavigableMap<Integer, PerChannelNoiseFile.NoiseData> result = new TreeMap<>();

      for (PerChannelNoiseFile perChannelNoiseFile : noiseDataList) {
         for (Map.Entry<Integer, PerChannelNoiseFile.NoiseData> integerNoiseDataEntry : perChannelNoiseFile.getEntries()) {
            float ne = integerNoiseDataEntry.getValue().ne();
            PerChannelNoiseFile.NoiseData value = result.get(integerNoiseDataEntry.getKey());
            if (value == null || ne > value.ne()) {
               value = new PerChannelNoiseFile.NoiseData(ne, ne * 2);
               result.put(integerNoiseDataEntry.getKey(), value);
            }
         }
      }
      return result;
   }

   public static Path getDayNoiseFile(Path noiseDir, RawFileConfiguration rawFileConfiguration) {
      return getDayNoiseFile(noiseDir, rawFileConfiguration, DAY_NOISE_DIR);
   }

   static Path getDayNoiseFile(Path noiseDir, RawFileConfiguration rawFileConfiguration, String filePrefix) {
      DateTimeMillis dateTime = new DateTimeMillis(rawFileConfiguration.getInstant());
      int date = dateTime.getDate();
      return noiseDir.resolve(filePrefix).resolve(filePrefix + "_D" + date + ".xml");
   }

   public static Path getSurveyLowNoiseFile(Path noiseDir) {
      return getNoiseFile(noiseDir.resolve(SURVEY_NOISE_LOW_FILE_PREFIX), SURVEY_NOISE_LOW_FILE_PREFIX);
   }

   public static Path getSurveyHighNoiseFile(Path noiseDir) {
      return getNoiseFile(noiseDir.resolve(SURVEY_NOISE_HIGH_FILE_PREFIX), SURVEY_NOISE_HIGH_FILE_PREFIX);
   }

   public static Path getSurveyNoiseFile(Path noiseDir) {
      return getNoiseFile(noiseDir.resolve(SURVEY_NOISE_MEDIAN_FILE_PREFIX), SURVEY_NOISE_MEDIAN_FILE_PREFIX);
   }

   static Path getPerFileNoiseFile(Path noiseDir, RawFileConfiguration rawFileConfiguration) {
      DateTimeMillis dateTime = new DateTimeMillis(rawFileConfiguration.getInstant());
      int date = dateTime.getDate();
      String time = Utils.format("%06d", dateTime.getTime() / 1000);
      return getNoiseFile(noiseDir.resolve(FILE_NOISE_FILE_PREFIX), FILE_NOISE_FILE_PREFIX + "_D" + date + "_T" + time);
   }

   private static Path getNoiseFile(Path noiseDir, String filePrefix) {
      return noiseDir.resolve(filePrefix + ".xml");
   }

   static List<PerChannelNoiseFile> parseNoiseFiles(Path dir, Predicate<Path> predicate) throws IOException {
      List<Path> files = FileUtils.listFiles(dir, predicate);
      return files.stream()
            .map(PerChannelNoiseFile::new)
            .toList();
   }

   static Predicate<Path> dayFilePredicate(RawFileConfiguration rawFileConfiguration) {
      // A predicate for accepting all file-noise files corresponding to a given day.
      int dateStart = (FILE_NOISE_FILE_PREFIX + "_D").length();
      DateTimeMillis dateTime = new DateTimeMillis(rawFileConfiguration.getInstant());
      String dateString = Integer.toString(dateTime.getDate());
      return file -> {
         String name = file.getFileName().toString();
         return name.startsWith(FILE_NOISE_FILE_PREFIX)
               && name.endsWith(".xml")
               && name.regionMatches(dateStart, dateString, 0, 8);
      };
   }

   static Predicate<Path> dayDirFilePredicate() {
      return file -> {
         String name = file.getFileName().toString();
         return name.startsWith(DAY_NOISE_DIR) && name.endsWith(".xml");
      };
   }

   public static Predicate<Path> surveyFilePredicate() {
      // A predicate for accepting all file-noise files.
      return file -> {
         String name = file.getFileName().toString();
         return name.startsWith(FILE_NOISE_FILE_PREFIX) && name.endsWith(".xml");
      };
   }
}
