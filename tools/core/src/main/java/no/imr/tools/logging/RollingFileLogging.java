package no.imr.tools.logging;

import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;

public final class RollingFileLogging {
   private final Path dir;
   private final String fileName;
   private @Nullable FileHandler fileHandler;

   public RollingFileLogging(Path dir, String fileNamePrefix, long fileSize, int fileCount) {
      this.dir = dir;
      fileName = fileNamePrefix + "_%g.log";
      try {
         FileUtils.createDirectories(dir);
         fileHandler = new FileHandler(dir + File.separator + fileName, fileSize, fileCount, true);
         fileHandler.setFormatter(new OneLineFormatter());
         fileHandler.setEncoding(Utils.UTF_8.name());
         fileHandler.setLevel(Level.FINE);
         Log.addHandler(fileHandler);
      } catch (IOException e) {
         Log.global.log(Level.SEVERE, "Error logging to " + dir, e);
      }
   }

   public Path getDir() {
      return dir;
   }

   public Path getLogFile(int index) {
      return dir.resolve(fileName.replace("%g", Integer.toString(index)));
   }

   public void setLevel(Level level) {
      if (fileHandler == null) {
         return;
      }
      fileHandler.setLevel(level);
   }

   public void flush() {
      if (fileHandler == null) {
         return;
      }
      fileHandler.flush();
   }

   public void close() {
      if (fileHandler == null) {
         return;
      }
      Log.removeHandler(fileHandler);
      fileHandler.close();
   }
}
