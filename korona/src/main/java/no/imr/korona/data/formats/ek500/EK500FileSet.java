package no.imr.korona.data.formats.ek500;

import no.imr.korona.data.DataException;
import no.imr.tools.Utils;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The files in a {@link EK500SegmentHandle}.
 */
final class EK500FileSet {
   // Example file name: N058-S010-S2007106-F018000-T01-D20070427-T053434-Ping
   // Groups:                                               1                    2       3      4             5
   static final Pattern PATTERN = Pattern.compile("^(N\\d+-S\\d+-S\\d+)-F(\\d+)-T(\\d+)-(D\\d+-T\\d+)-(\\w+)$");
   static final DateTimeFormatter DATE_TIME_FORMATTER = Utils.createUTCDateTimeFormatter("'D'yyyyMMdd'-T'HHmmss");
   static final int MAX_DIFF_MILLIS = 2_000;
   static final int MAIN_FREQUENCY = 38_000;

   private final String nss;
   private final String dateTime;

   private final List<FrequencyFileSet> frequencyFileSets;
   private final List<Path> files;

   EK500FileSet(String nss, String dateTime, List<Path> files) {
      this.nss = nss;
      this.dateTime = dateTime;
      this.files = files.stream()
            .sorted()
            .toList();
      frequencyFileSets = this.files.stream() // this.files is already sorted.
            .map(file -> {
               String name = file.getFileName().toString();
               if (name.endsWith(EK500DataFormatPlugin.INFO_SUFFIX)) {
                  String baseName = name.substring(0, name.length() - EK500DataFormatPlugin.INFO_SUFFIX.length());
                  return new FrequencyFileSet(file.getParent(), baseName);
               } else {
                  return null;
               }
            })
            .filter(Objects::nonNull)
            .toList();
   }

   void exceptionIfNoData() throws DataException {
      if (frequencyFileSets.isEmpty()) {
         throw new DataException("No data");
      }
   }

   String getNSS() {
      return nss;
   }

   String getDateTime() {
      return dateTime;
   }

   List<FrequencyFileSet> getFrequencyFileSets() {
      return frequencyFileSets;
   }

   List<Path> getFiles() {
      return files;
   }

   Path getMainFile() {
      return files.getFirst();
   }

   /**
    * The EK500 files for a single frequency.
    */
   record FrequencyFileSet(
         Path directory,
         String baseName
   ) {
      TimeShiftManager.DirectoryTimeShift getTimeShift() {
         return TimeShiftManager.getDirectoryTimeShift(directory);
      }

      Path getInfoFile() {
         return createFile(EK500DataFormatPlugin.INFO_SUFFIX);
      }

      Path getPingFile() {
         return createFile(EK500DataFormatPlugin.PING_SUFFIX);
      }

      Path getTimeFile() {
         return createFile(EK500DataFormatPlugin.TIME_SUFFIX);
      }

      Path getDataFile() {
         return createFile(EK500DataFormatPlugin.DATA_SUFFIX);
      }

      private Path createFile(String suffix) {
         return directory.resolve(baseName + suffix);
      }

      @Nullable Path getExistingSnapOrWorkFile(Path workDirectory) {
         Path snapFile = workDirectory.resolve(baseName + EK500DataFormatPlugin.SNAP_SUFFIX);
         if (Files.exists(snapFile)) {
            return snapFile;
         }
         Path workFile = workDirectory.resolve(baseName + EK500DataFormatPlugin.WORK_SUFFIX);
         if (Files.exists(workFile)) {
            return workFile;
         }
         return null;
      }
   }
}
