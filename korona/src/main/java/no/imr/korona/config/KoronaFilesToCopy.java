package no.imr.korona.config;

import no.imr.korona.Korona;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

final class KoronaFilesToCopy extends FilesToCopy {
   private final Korona korona;

   KoronaFilesToCopy(Korona korona, long previousLastModified) {
      super(previousLastModified);

      this.korona = korona;
   }

   void search() {
      Path koronaConfigDir = korona.getKoronaSettings().getKoronaConfigDir().getFile();
      if (koronaConfigDir == null) {
         return;
      }
      Map<Path, Path> sourceDirToDestDir = findSourceDirToDestDir(koronaConfigDir);
      findFilesToCopy(sourceDirToDestDir);
   }

   private Map<Path, Path> findSourceDirToDestDir(Path koronaConfigDir) {
      Map<Path, Path> sourceDirToDestDir = new HashMap<>();
      for (ConfigFileService service : korona.createConfigFileSettings().getFileServices()) {
         String subDir = service.getInstallationSubDirName();
         Path destDir = koronaConfigDir.resolve(subDir).normalize();
         sourceDirToDestDir.put(service.getInstallationConfigDir().resolve(subDir).normalize(), destDir);
         for (Path additionalDir : service.getAdditionalInstallationConfigDirs()) {
            sourceDirToDestDir.put(additionalDir.resolve(subDir).normalize(), destDir);
         }
      }
      return sourceDirToDestDir;
   }

   private void findFilesToCopy(Map<Path, Path> sourceDirToDestDir) {
      sourceDirToDestDir.forEach((sourceDir, destDir) -> {
         try {
            for (FileInfo sourceFileInfo : FileUtils.listFilesWithAttributes(sourceDir)) {
               Path sourceFile = sourceFileInfo.file();
               if (sourceDirToDestDir.containsKey(sourceFile)) {
                  // This file is itself a source dir that will be handled separately.
                  return;
               }
               Path destFile = destDir.resolve(sourceFile.getFileName());
               add(sourceFileInfo, destFile);
            }
         } catch (IOException e) {
            Log.global.log(Level.WARNING, e.getMessage(), e);
         }
      });
   }
}
