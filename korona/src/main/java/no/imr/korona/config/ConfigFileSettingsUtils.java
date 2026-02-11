package no.imr.korona.config;

import no.imr.korona.Korona;
import no.imr.korona.apps.relay.KoronaRelay;
import no.imr.korona.config.pojo.CopiedConfigFilesInfo;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.swing.FileChooserToolTip;
import org.jspecify.annotations.Nullable;

import javax.swing.JFileChooser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ConfigFileSettingsUtils {
   public static final String INFO_FILE = "info.json";
   public static final String REFERENCE_FILES = "ReferenceFiles";

   private ConfigFileSettingsUtils() {
   }

   public static void copy(ConfigFileSettings configFileSettings, Path destinationDirectory, AsyncHandle asyncHandle) throws IOException {
      FileUtils.createDirectories(destinationDirectory);

      ConfigFileSettings cfs = new ConfigFileSettings();
      cfs.setContext(configFileSettings.getContext());
      Map<Path, Path> filesToCopy = new HashMap<>();
      Path referenceFilesDir = destinationDirectory.resolve(REFERENCE_FILES);
      for (ConfigFileService configFileService : configFileSettings.getFileServices()) {
         if (!configFileService.isCopyable()) {
            continue;
         }
         Path sourceFile = configFileSettings.getFile(configFileService.getName());
         if (sourceFile == null) {
            continue;
         }
         Path dir = configFileService.useReferenceFilesDir() ? referenceFilesDir : destinationDirectory;
         Path destinationFile = dir.resolve(sourceFile.getFileName());
         filesToCopy.put(sourceFile, destinationFile);
         filesToCopy.putAll(configFileService.getAdditionalFilesToCopy(sourceFile, destinationFile));

         cfs.getFileParameter(configFileService.getName()).setFile(destinationFile);
      }

      Path sourceCfsFile = configFileSettings.getFile();
      Path destinationCfsFile = destinationDirectory.resolve(sourceCfsFile != null ? sourceCfsFile.getFileName() : Path.of(ConfigFileSettings.DEFAULT_FILE_NAME));
      cfs.save(destinationCfsFile);

      Instant time = Instant.now().truncatedTo(ChronoUnit.SECONDS);

      Path infoFile = destinationDirectory.resolve(INFO_FILE);
      CopiedConfigFilesInfo info = new CopiedConfigFilesInfo(
            time.toString(),
            destinationCfsFile.getFileName().toString()
      );
      JsonUtils.writeValuePrettily(infoFile, info);

      Path readmeFile = destinationDirectory.resolve("README-DIRECTORY-IS-AUTOMATICALLY-SYNCED.txt");

      Set<Path> filesToDelete = new HashSet<>(FileUtils.listFiles(destinationDirectory));
      filesToDelete.addAll(FileUtils.listFiles(referenceFilesDir));
      filesToDelete.remove(referenceFilesDir);
      filesToDelete.remove(destinationCfsFile);
      filesToDelete.remove(infoFile);
      filesToDelete.remove(readmeFile);
      filesToDelete.removeAll(filesToCopy.values());
      for (Path file : filesToDelete) {
         FileUtils.deleteRecursively(file);
      }

      for (Map.Entry<Path, Path> entry : filesToCopy.entrySet()) {
         FileUtils.syncRecursively(entry.getKey(), entry.getValue(), asyncHandle);
      }

      // Write readme file last, so that its timestamps are updated only if all copying succeeded.
      String readmeContents = "Information about " + destinationDirectory.getFileName()
            + "\n"
            + "\nThis directory contains the KORONA config files used"
            + "\nduring the processing that started at " + time + "."
            + "\n"
            + "\nThe files in this directory are automatically updated."
            + "\nNB: MANUAL EDITS WILL BE OVERWRITTEN!";
      FileUtils.replaceFileSafely(readmeFile, readmeContents, Utils.UTF_8);
   }

   public static void installTooltip(JFileChooser fileChooser) {
      FileChooserToolTip.install(fileChooser, file -> {
         Path infoFile = file.resolve(KoronaRelay.COPIED_CONFIG_FILES_DIR_NAME).resolve(INFO_FILE);
         try {
            CopiedConfigFilesInfo info = readInfoFile(infoFile);
            if (info == null) {
               return null;
            }
            return new HtmlStringBuilder()
                  .text("Last processed at " + info.time())
                  .html("<br>").text("using " + info.cfs())
                  .build();
         } catch (Exception e) {
            return new HtmlStringBuilder()
                  .text("Error reading ").text(infoFile.toString())
                  .html("<br>").text(e.toString())
                  .build();
         }
      });
   }

   private static @Nullable CopiedConfigFilesInfo readInfoFile(Path infoFile) {
      if (!Files.exists(infoFile)) {
         return null;
      }
      return JsonUtils.JSON_MAPPER.readValue(infoFile, CopiedConfigFilesInfo.class);
   }

   public static @Nullable ConfigFileSettings loadConfigFileSettingsFromCopiedConfigFiles(Path dataDir, Korona korona) throws IOException {
      Path dir = dataDir.resolve(KoronaRelay.COPIED_CONFIG_FILES_DIR_NAME);
      Path infoFile = dir.resolve(INFO_FILE);
      CopiedConfigFilesInfo info = readInfoFile(infoFile);
      if (info == null) {
         return null;
      }
      ConfigFileSettings configFileSettings = korona.createConfigFileSettings();
      configFileSettings.load(dir.resolve(info.cfs()));
      return configFileSettings;
   }
}
