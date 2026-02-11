package no.imr.korona.config;

import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

public final class FilesToCopy {
   private final Map<Path, Path> filesToCopy = new TreeMap<>();
   private final Map<Path, Path> existingFiles = new HashMap<>();
   private final long previousLastModified;
   private long nextLastModifiedSource;

   public FilesToCopy(long previousLastModified) {
      this.previousLastModified = previousLastModified;
   }

   public Map<Path, Path> getFilesToCopy() {
      return filesToCopy;
   }

   public Map<Path, Path> getExistingFiles() {
      return existingFiles;
   }

   public long getNextLastModifiedSource() {
      return nextLastModifiedSource;
   }

   public void add(FileInfo sourceFile, Path destFile) {
      nextLastModifiedSource = Math.max(nextLastModifiedSource, sourceFile.lastModifiedTime().toMillis());

      if (Files.exists(destFile)) {
         if (isReplacementCandidate(sourceFile, destFile)) {
            existingFiles.put(sourceFile.file(), destFile);
         }
      } else {
         filesToCopy.put(sourceFile.file(), destFile);
      }
   }

   private boolean isReplacementCandidate(FileInfo sourceFile, Path destFile) {
      boolean newer = newerThanPrevLastModified(sourceFile);
      if (sourceFile.isDirectory()) {
         try {
            for (FileInfo file : FileUtils.listFilesWithAttributes(sourceFile.file())) {
               newer |= newerThanPrevLastModified(file);
            }
         } catch (IOException e) {
            Log.global.log(Level.WARNING, e.getMessage(), e);
         }
      }
      return newer && !FileUtils.equalsRecursively(sourceFile.file(), destFile);
   }

   private boolean newerThanPrevLastModified(FileInfo sourceFile) {
      return sourceFile.lastModifiedTime().toMillis() > previousLastModified;
   }
}
