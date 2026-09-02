package no.imr.korona.config;

import no.imr.tools.parameter.FileParameter;

import java.nio.file.Path;

public abstract class ConfigFileParameterEditor<T extends ConfigFileService> implements FileParameter.Editor {
   private final T configFileService;
   private final ConfigFileSettings configFileSettings;

   protected ConfigFileParameterEditor(T configFileService, ConfigFileSettings configFileSettings) {
      this.configFileService = configFileService;
      this.configFileSettings = configFileSettings;
   }

   protected T getConfigFileService() {
      return configFileService;
   }

   protected ConfigFileSettings getConfigFileSettings() {
      return configFileSettings;
   }

   protected Path getFile() {
      Path file = configFileSettings.getFile(configFileService.getName());
      if (file == null) {
         throw new IllegalStateException(configFileService.getName().persistentName());
      }
      return file;
   }

   protected FileParameter getFileParameter() {
      return configFileSettings.getFileParameter(configFileService.getName());
   }
}
