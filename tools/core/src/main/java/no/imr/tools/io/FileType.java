package no.imr.tools.io;

import java.nio.file.Path;

/**
 * Immutable class defining a file type.
 */
public record FileType(String suffix, String description) {

   public Path ensureSuffix(Path file) {
      return FileUtils.ensureSuffix(file, suffix);
   }
}
