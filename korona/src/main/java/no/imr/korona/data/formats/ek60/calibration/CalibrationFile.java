package no.imr.korona.data.formats.ek60.calibration;

import no.imr.tools.io.FileUtils;
import no.imr.tools.xml.XmlException;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public final class CalibrationFile {
   public static final String FILE_NAME = "calibration.xml";
   public static final CalibrationFile EMPTY = new CalibrationFile(null);

   private final @Nullable Path file;
   private final @Nullable Instant lastModified;
   private final @Nullable Path ek80File;
   private final @Nullable Instant ek80LastModified;
   private final CalibrationContent calibrationContent;
   private final @Nullable String error;

   private volatile Instant nextCheckTime = Instant.now().plusSeconds(1);
   private volatile boolean modified;

   CalibrationFile(@Nullable Path file) {
      this.file = file;
      lastModified = null;
      ek80File = null;
      ek80LastModified = null;
      calibrationContent = new CalibrationContent();
      error = null;
   }

   CalibrationFile(Path file, @Nullable Instant lastModified, Exception exception) {
      this.file = file;
      this.lastModified = lastModified;
      ek80File = null;
      ek80LastModified = null;
      calibrationContent = new CalibrationContent();
      error = exception.getCause() instanceof XmlException
            ? exception.getCause().getMessage()
            : exception.getMessage();
   }

   public CalibrationFile(@Nullable Path file, @Nullable Instant lastModified, @Nullable Path ek80File, @Nullable Instant ek80LastModified, CalibrationContent calibrationContent) {
      this.file = file;
      this.lastModified = lastModified;
      this.ek80File = ek80File;
      this.ek80LastModified = ek80LastModified;
      this.calibrationContent = calibrationContent;
      error = null;
   }

   public static CalibrationFile forDirectory(Path dir) {
      return CalibrationLoader.forDirectory(dir);
   }

   public @Nullable Path getDir() {
      return file != null ? file.getParent() : null;
   }

   public CalibrationContent getContent() {
      return calibrationContent;
   }

   public boolean isModified(boolean forceCheck) {
      if (forceCheck || Instant.now().isAfter(nextCheckTime)) {
         modified = isModified(file, lastModified) || isModified(ek80File, ek80LastModified);
         nextCheckTime = Instant.now().plusSeconds(1);
      }
      return modified;
   }

   private static boolean isModified(@Nullable Path file, @Nullable Instant lastModified) {
      return file != null && !Objects.equals(FileUtils.lastModifiedOrNull(file), lastModified);
   }

   public boolean exists() {
      return lastModified != null;
   }

   public @Nullable String getError() {
      return error;
   }
}
