package no.imr.korona.computation;

import no.imr.tools.parameter.Name;

import java.nio.file.Path;

/**
 * Thrown when a config file is not configured or does not exist.
 */
public final class ConfigFileSettingsException extends ModuleConfigurationException {
   private final Name name;

   public ConfigFileSettingsException(BaseModule module, Name configFileServiceName, String explanation) {
      super(module, explanation);

      name = configFileServiceName;
   }

   public ConfigFileSettingsException(BaseModule module, Name configFileServiceName, Path file, String explanation) {
      this(module, configFileServiceName, "Error with config file \"" + configFileServiceName.displayName() + "\": " + file + " (" + explanation + ")");
   }

   public Name getName() {
      return name;
   }
}
