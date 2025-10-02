package no.imr.korona.computation.offset;

import no.imr.korona.config.ConfigFileParameterEditor;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.KoronaConfigFileService;
import no.imr.tools.parameter.Name;

import java.nio.file.Path;

public final class TransducerRangesFileService extends KoronaConfigFileService {
   public static final Name NAME = new Name("TransducerRanges", "Transducer ranges");

   public TransducerRangesFileService() {
      super(NAME);
   }

   @Override
   public String getInstallationSubDirName() {
      return getName().persistentName();
   }

   @Override
   public Path getInstallationLocation() {
      return getInstallationConfigDir().resolve(getInstallationSubDirName()).resolve("TransducerRanges.xml");
   }

   @Override
   protected ConfigFileParameterEditor createFileParameterEditor(ConfigFileSettings configFileSettings) {
      return new TransducerFileParameterEditor(this, configFileSettings, TransducerParameters.ParameterType.RANGE);
   }

   @Override
   public boolean canBePlatformSpecific() {
      return false;
   }
}
