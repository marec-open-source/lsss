package no.imr.lsss.framework.wizards.newsurvey;

import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.range.ArrayRangeMap;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.RangeMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;

final class NewSurveyUtils {
   private NewSurveyUtils() {
   }

   /**
    * {@return base of a config file name for this platform}
    * <p>
    * Convention: <code>filename_countryName_platformName[platformNumber]</code>
    *
    * @param originalFile a config file name
    * @param survey       a survey
    */
   static String getPlatformSpecificBaseName(Path originalFile, Survey survey) {
      Platform platform = survey.getPlatform();
      return FileUtils.baseName(originalFile)
            + '_' + platform.getNation().getNationName().replace(" ", "")
            + '_' + platform.findPlatformName(survey).replace(" ", "")
            + '[' + platform.getCompId().getPlatform() + ']';
   }

   /**
    * Given a config file base name (by platform and nation) and a directory, this method builds a map
    * between start dates and configuration files.
    *
    * @param platformFileName base of a config file name for this platform
    * @param parentFile       the directory to look in
    * @return a map between start date and config files
    */
   static NavigableMap<Integer, String> createConfigFileDateMap(String platformFileName, Path parentFile) {
      NavigableMap<Integer, String> map = new TreeMap<>();

      List<Path> files;
      try {
         files = FileUtils.listFiles(parentFile);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
         return map;
      }

      for (Path file : files) {
         if (!file.getFileName().toString().startsWith(platformFileName)) {
            continue;
         }

         String baseName = FileUtils.baseName(file);
         if (baseName.length() < 8) {
            continue;
         }

         String fileDate = baseName.substring(baseName.length() - 8);
         try {
            Integer date = Integer.valueOf(fileDate);
            map.put(date, file.getFileName().toString());
         } catch (NumberFormatException e) {
            Log.global.fine("Could not extract date from file " + fileDate);
         }
      }

      return map;
   }

   /**
    * Takes a map of dates and config files, and builds a range map so
    * that each file gets a range of validity.
    *
    * @param dateMap the date->file map
    * @return a date->file range map
    */
   static RangeMap<Integer, String> createConfigFileRangeMap(NavigableMap<Integer, String> dateMap) {
      RangeMap<Integer, String> rangeMap = new ArrayRangeMap<>();

      for (Map.Entry<Integer, String> entry : dateMap.entrySet()) {
         Integer date = entry.getKey();
         String fileName = entry.getValue();
         rangeMap.put(new DefaultRange<>(date, 99999999), fileName);
      }

      return rangeMap;
   }
}
