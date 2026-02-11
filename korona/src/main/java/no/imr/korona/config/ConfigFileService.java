package no.imr.korona.config;

import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.plugins.BaseService;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

public abstract class ConfigFileService extends BaseService {
   protected ConfigFileService(Name name) {
      super(name);
   }

   public abstract Path getInstallationConfigDir();

   public String getInstallationSubDirName() {
      return ".";
   }

   public @Nullable Path getDefaultInConfigDirectory(Path configDir) {
      Path installationLocation = getInstallationLocation();
      if (installationLocation == null) {
         return null;
      }
      Path installationConfigDir = getInstallationConfigDir();
      if (FileUtils.isInDir(installationLocation, installationConfigDir)) {
         String relativePath = FileUtils.relativePath(installationLocation, installationConfigDir);
         if (relativePath != null) {
            return configDir.resolve(relativePath);
         }
      }
      return null;
   }

   public void addInstallationConfigFilesToCopy(FilesToCopy filesToCopy, Path destinationDir) throws IOException {
      String subDirName = getInstallationSubDirName();
      if (subDirName.equals(".")) {
         return;
      }
      Path installationConfigDir = getInstallationConfigDir();
      Path subDir = installationConfigDir.resolve(subDirName);
      Files.walkFileTree(subDir, new SimpleFileVisitor<>() {
         @Override
         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            Path relativePath = installationConfigDir.relativize(file);
            Path destinationFile = destinationDir.resolve(relativePath);
            filesToCopy.add(new FileInfo(file, attrs), destinationFile);
            return FileVisitResult.CONTINUE;
         }
      });
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
