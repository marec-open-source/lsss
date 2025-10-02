package no.imr.korona.computation.towfish;

import no.imr.korona.config.ConfigFileParameter;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.KoronaConfigFileService;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public final class TowfishFileService extends KoronaConfigFileService {
   public static final Name NAME = new Name("Towfish");

   public TowfishFileService() {
      super(NAME);
   }

   @Override
   public boolean isCopyable() {
      return false;
   }

   @Override
   public @Nullable Path getInstallationLocation() {
      return null;
   }

   @Override
   public FileParameter createFileParameter(ConfigFileSettings configFileSettings) {
      return new ConfigFileParameter(this, configFileSettings, FileParameter.Mode.DIRECTORY);
   }
}
