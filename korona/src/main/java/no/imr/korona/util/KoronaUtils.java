package no.imr.korona.util;

import no.imr.korona.Korona;
import no.imr.korona.data.formats.ek60.EK60DataFormatPlugin;
import no.imr.korona.data.formats.synthetic.SyntheticDataFormatPlugin;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.Utils;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileType;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.time.NTDate;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Miscellaneous functions.
 */
public final class KoronaUtils {
   public static final String CDS_FILE_SUFFIX = ".cds";
   public static final FileType CDS_FILE_TYPE = new FileType(CDS_FILE_SUFFIX, "Module configuration");

   public static final float MAX_KNOTS = 100;

   private KoronaUtils() {
   }

   public static double getKnots(PingIndex first, PingIndex last) {
      double nmi = last.getVesselDistance() - first.getVesselDistance();
      double hours = (last.getNTDate() - first.getNTDate()) / (3600.0 * NTDate.UNITS_PER_SECOND);
      return nmi / hours;
   }

   public static double knotsToKilometerPerHour(double knots) {
      return knots * (1852.0 / 1000.0);
   }

   public static double knotsToMeterPerSecond(double knots) {
      return knots * (1852.0 / 3600.0);
   }

   public static double meterPerSecondToKnots(double meterPerSecond) {
      return meterPerSecond / (1852.0 / 3600.0);
   }

   public static double getFraction(PingRange pingRange, PingIndex pingIndex) {
      double delta = pingIndex.getNTDate() - pingRange.begin().getNTDate();
      double total = pingRange.end().getNTDate() - pingRange.begin().getNTDate();
      return delta / total;
   }

   /**
    * Converts a value to from linear to dB.
    *
    * @param value the value to convert to dB
    * @return 10 * log<sub>10</sub>(value)
    */
   public static double toDB(double value) {
      return 10 * Math.log10(value);
   }

   /**
    * Converts a value to from dB to linear.
    *
    * @param value the value to convert to linear
    * @return 10 <sup>value / 10</sup>
    */
   public static double fromDB(double value) {
      return Math.pow(10, value / 10);
   }

   /**
    * Finds all raw files in a directory, sorted by the {@linkplain Comparable natural ordering}.
    *
    * @param directory a directory
    * @return a sorted list of raw files
    */
   public static List<Path> listRawFiles(Path directory) throws IOException {
      return listRawFiles(directory, Comparator.comparing(FileInfo::file));
   }

   /**
    * Finds all raw files in a directory.
    *
    * @param directory  a directory
    * @param comparator the comparator by which the files are ordered
    * @return a sorted list of raw files
    */
   public static List<Path> listRawFiles(Path directory, Comparator<FileInfo> comparator) throws IOException {
      List<FileInfo> fileInfos = FileUtils.listFilesWithAttributes(directory, KoronaUtils::isRawFile);
      fileInfos.sort(comparator);
      return fileInfos.stream()
            .map(FileInfo::file)
            .collect(Collectors.toList());
   }

   public static boolean isRawFile(FileInfo fileInfo) {
      String name = fileInfo.getFileName();
      return fileInfo.attributes().isRegularFile() &&
            (name.endsWith(EK60DataFormatPlugin.RAW_SUFFIX)
                  || name.endsWith(SyntheticDataFormatPlugin.LSSS_SS_SUFFIX));
   }

   /**
    * Returns the next file in the input folder. If the current file is null,
    * returns the last file in the folder, or null if the folder is empty.
    *
    * @param directory      a directory of raw files
    * @param currentRawFile the current raw file or null
    * @param comparator     the comparator by which the files are ordered
    * @return the next raw file, or null if no next file exists
    */
   public static @Nullable Path nextRawFile(Path directory, @Nullable Path currentRawFile, Comparator<FileInfo> comparator) throws IOException {
      if (currentRawFile == null) {
         return lastRawFile(directory, comparator);
      }

      FileInfo currentFileInfo;
      try {
         currentFileInfo = new FileInfo(currentRawFile);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error reading attributes of " + currentRawFile, e);
         return lastRawFile(directory, comparator);
      }

      return FileUtils.listFilesWithAttributes(directory, KoronaUtils::isRawFile).stream()
            .filter(Utils.greaterThan(comparator, currentFileInfo))
            .min(comparator)
            .map(FileInfo::file)
            .orElse(null);
   }

   private static @Nullable Path lastRawFile(Path directory, Comparator<FileInfo> comparator) throws IOException {
      return FileUtils.listFilesWithAttributes(directory, KoronaUtils::isRawFile).stream()
            .max(comparator)
            .map(FileInfo::file)
            .orElse(null);
   }

   public static Element createProcessingInfoXml() {
      return DocumentHelper.createElement("ProcessingInfo")
            .addAttribute("version", Korona.VERSION)
            .addAttribute("buildTime", Utils.BUILD_TIME.toString())
            .addAttribute("processingTime", Instant.now().toString());
   }
}
