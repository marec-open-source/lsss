package no.imr.korona.config;

import no.imr.korona.Korona;
import no.imr.korona.resources.KoronaResource;
import no.imr.tools.parameter.Name;

import java.nio.file.Path;

public abstract class KoronaConfigFileService extends ConfigFileService {
   public static final ConfigFileSettingsContext CONTEXT = new ConfigFileSettingsContext(new Name("Korona", "KORONA"), KoronaResource.KORONA);

   protected KoronaConfigFileService(Name name) {
      super(name);
   }

   @Override
   public ConfigFileSettingsContext getContext() {
      return CONTEXT;
   }

   @Override
   public Path getInstallationConfigDir() {
      return Korona.getInstallationConfigDir();
   }
}
