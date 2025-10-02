package no.imr.tools.io;

import no.imr.tools.concurrent.AsyncHandle;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

public record DirectoryListing(
      Map<Path, BasicFileAttributes> map
) {
   public static DirectoryListing of() {
      return new DirectoryListing(Map.of());
   }

   public static DirectoryListing of(Path directory, AsyncHandle asyncHandle) throws IOException {
      return new DirectoryListing(FileUtils.fileInfosToMap(FileUtils.listFilesWithAttributes(directory, asyncHandle)));
   }

   public static DirectoryListing ofOrEmpty(Path directory, AsyncHandle asyncHandle) {
      try {
         return of(directory, asyncHandle);
      } catch (IOException e) {
         return of();
      }
   }

   public boolean exists(Path path) {
      return map.containsKey(path);
   }

   public long lastModifiedOr0(Path path) {
      BasicFileAttributes attributes = map.get(path);
      return attributes != null ? attributes.lastModifiedTime().toMillis() : 0;
   }
}
