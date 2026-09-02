package no.imr.tools.io;

import java.util.Comparator;

public final class FileInfoComparator {
   private FileInfoComparator() {
   }

   public static Comparator<FileInfo> lastModified() {
      return Comparator.comparing(FileInfo::lastModified)
            .thenComparing(FileInfo::file);
   }

   public static Comparator<FileInfo> path() {
      return Comparator.comparing(FileInfo::file);
   }
}
