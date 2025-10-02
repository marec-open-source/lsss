package no.imr.korona.config;

import no.imr.tools.parameter.FileParameter;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigFileParameter extends FileParameter {
   private final ConfigFileService configFileService;
   private final ConfigFileSettings configFileSettings;

   public ConfigFileParameter(ConfigFileService configFileService, ConfigFileSettings configFileSettings) {
      this(configFileService, configFileSettings, Mode.FILE);
   }

   public ConfigFileParameter(ConfigFileService configFileService, ConfigFileSettings configFileSettings, Mode mode) {
      super(configFileService.getName(), null, mode);

      this.configFileService = configFileService;
      this.configFileSettings = configFileSettings;
   }

   @Override
   public @Nullable Path getDefaultBrowseDirectory() {
      Path dir = findRelatedDir();
      return dir != null ? adjustForReferenceFilesDir(dir) : null;
   }

   private @Nullable Path findRelatedDir() {
      for (ConfigFileService otherConfigFileService : configFileSettings.getFileServices()) {
         Path otherFile = configFileSettings.getFile(otherConfigFileService.getName());
         if (otherFile != null) {
            Path dir = otherFile.getParent();
            String dirName = dir.getFileName().toString();
            if (dirName.equals(ConfigFileSettingsUtils.REFERENCE_FILES)
                  || dirName.equals(otherConfigFileService.getInstallationSubDirName())) {
               return dir.getParent();
            }
            return dir;
         }
      }
      Path file = configFileSettings.getFile();
      if (file != null) {
         return file.getParent();
      }
      return configFileSettings.getDefaultBrowseDirectorySupplier().get();
   }

   private Path adjustForReferenceFilesDir(Path dir) {
      if (configFileService.useReferenceFilesDir()) {
         Path referenceFilesDir = dir.resolve(ConfigFileSettingsUtils.REFERENCE_FILES);
         if (Files.exists(referenceFilesDir)) {
            return referenceFilesDir;
         }
      }
      Path subDir = dir.resolve(configFileService.getInstallationSubDirName()).normalize();
      if (Files.exists(subDir)) {
         return subDir;
      }
      return dir;
   }

   @Override
   public @Nullable Editor getEditor() {
      return configFileService.createFileParameterEditor(configFileSettings);
   }

   @Override
   public @Nullable Copier getCopier() {
      return configFileService.createFileParameterCopier(configFileSettings);
   }
}
