package no.imr.tools.io;

import java.nio.file.Path;

public record FileType(String suffix, String description) {

   public Path ensureSuffix(Path file) {
      return FileUtils.ensureSuffix(file, suffix);
   }
}
