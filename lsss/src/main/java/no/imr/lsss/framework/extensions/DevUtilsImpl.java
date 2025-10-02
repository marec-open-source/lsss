package no.imr.lsss.framework.extensions;

import no.imr.lsss.LSSS;
import no.imr.tools.logging.Log;
import no.marec.lsss.api.internal.DevUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class DevUtilsImpl implements DevUtils {
   static final DevUtilsImpl INSTANCE = new DevUtilsImpl();

   private DevUtilsImpl() {
   }

   @Override
   public void startLsss(String[] args) {
      String cp = System.getProperty("java.class.path");
      Path lsssInstallationDir = Stream.of(cp.split(File.pathSeparator))
            .filter(f -> f.endsWith("marec-lsss-api.jar"))
            .map(f -> Path.of(f).getParent().getParent().getParent())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Cannot find marec-lsss-api.jar"));
      Log.global.info("LSSS installation dir: " + lsssInstallationDir);
      System.setProperty("TOP_INSTALLATION_DIR", lsssInstallationDir.toString());

      Path libNativeDir = lsssInstallationDir.resolve("lib/native");
      try (Stream<Path> stream = Files.list(libNativeDir)) {
         String libPath = stream
               .filter(Files::isDirectory)
               .map(Path::toString)
               .collect(Collectors.joining(File.pathSeparator));
         System.setProperty("java.library.path", libPath);
         System.setProperty("jna.library.path", libPath);
      } catch (IOException e) {
         Log.global.log(Level.SEVERE, "Error listing files in " + libNativeDir, e);
      }

      LSSS.main(args);
   }
}
