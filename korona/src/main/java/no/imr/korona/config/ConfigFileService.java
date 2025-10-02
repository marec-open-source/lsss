package no.imr.korona.config;

import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.plugins.BaseService;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class ConfigFileService extends BaseService {
   protected ConfigFileService(Name name) {
      super(name);
   }

   public abstract Path getInstallationConfigDir();

   public List<Path> getAdditionalInstallationConfigDirs() {
      return List.of();
   }

   public String getInstallationSubDirName() {
      return ".";
   }

   public @Nullable Path getDefaultInConfigDirectory(Path configDir) {
      Path installationLocation = getInstallationLocation();
      if (installationLocation == null) {
         return null;
      }
      String relativePath = Stream.concat(
                  Stream.of(getInstallationConfigDir()),
                  getAdditionalInstallationConfigDirs().stream()
            )
            .map(dir -> {
               if (FileUtils.isInDir(installationLocation, dir)) {
                  return FileUtils.relativePath(installationLocation, dir);
               }
               return null;
            })
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
      if (relativePath == null) {
         return null;
      }
      return configDir.resolve(relativePath);
   }

   public boolean isCopyable() {
      return true;
   }

   public boolean isModuleConfiguration() {
      return false;
   }

   public boolean useReferenceFilesDir() {
      return !isModuleConfiguration();
   }

   public boolean canBePlatformSpecific() {
      return true;
   }

   public Map<Path, Path> getAdditionalFilesToCopy(Path sourceFile, Path destinationFile) {
      return Map.of();
   }

   public ConfigFileSettingsContext getContext() {
      return ConfigFileSettings.ALL;
   }

   public abstract @Nullable Path getInstallationLocation();

   public List<Path> getAdditionalInstallationLocations() {
      return List.of();
   }

   public FileParameter createFileParameter(ConfigFileSettings configFileSettings) {
      return new ConfigFileParameter(this, configFileSettings);
   }

   protected FileParameter.@Nullable Editor createFileParameterEditor(ConfigFileSettings configFileSettings) {
      return null;
   }

   protected FileParameter.@Nullable Copier createFileParameterCopier(ConfigFileSettings configFileSettings) {
      return isCopyable() ? new FileParameter.DefaultCopier(configFileSettings.getFileParameter(getName())) : null;
   }
}
