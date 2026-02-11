package no.imr.korona.computation;

import no.imr.korona.config.ConfigFileParameterEditor;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.KoronaConfigFileService;
import no.imr.tools.parameter.Name;

import java.nio.file.Path;
import java.util.List;

public final class ModuleConfigurationFileService extends KoronaConfigFileService {
   public static final Name NAME = new Name("ModuleConfiguration", "Module configuration");
   public static final String SUB_DIR_NAME = "KoronaModuleSetup";

   public ModuleConfigurationFileService() {
      super(NAME);
   }

   @Override
   public String getInstallationSubDirName() {
      return SUB_DIR_NAME;
   }

   @Override
   public Path getInstallationLocation() {
      return getInstallationConfigDir().resolve(getInstallationSubDirName()).resolve("KoronaModuleSetup_Example01__FULL_MULTIFREQUENCY.cds");
   }

   @Override
   public List<Path> getAdditionalInstallationLocations() {
      return List.of(
            getInstallationConfigDir().resolve(getInstallationSubDirName()).resolve("KoronaModuleSetup_Example02__TRAINING.cds")
      );
   }

   @Override
   protected ConfigFileParameterEditor createFileParameterEditor(ConfigFileSettings configFileSettings) {
      return new CdsEditor(this, configFileSettings);
   }

   @Override
   public boolean isModuleConfiguration() {
      return true;
   }

   @Override
   public boolean canBePlatformSpecific() {
      return false;
   }
}
