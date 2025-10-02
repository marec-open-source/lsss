package no.imr.korona.computation.plankton;

import no.imr.korona.config.ConfigFileParameterEditor;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.KoronaConfigFileService;
import no.imr.tools.parameter.Name;

import java.nio.file.Path;

public final class PlanktonFileService extends KoronaConfigFileService {
   public static final Name NAME = new Name("Plankton");

   public PlanktonFileService() {
      super(NAME);
   }

   @Override
   public String getInstallationSubDirName() {
      return getName().persistentName();
   }

   @Override
   public Path getInstallationLocation() {
      return getInstallationConfigDir().resolve(getInstallationSubDirName()).resolve(PlanktonFile.PLANKTON_XML_FILE);
   }

   @Override
   protected ConfigFileParameterEditor createFileParameterEditor(ConfigFileSettings configFileSettings) {
      return new PlanktonFileParameterEditor(this, configFileSettings);
   }

   @Override
   public boolean canBePlatformSpecific() {
      return false;
   }
}
