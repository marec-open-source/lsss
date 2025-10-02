package no.imr.korona.computation.offset;

import no.imr.korona.config.ConfigFileParameterEditor;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.KoronaConfigFileService;
import no.imr.tools.parameter.Name;

import java.nio.file.Path;

public final class HorizontalTransducerOffsetsFileService extends KoronaConfigFileService {
   public static final Name NAME = new Name("HorizontalTransducerOffsets", "Horizontal transducer offsets");

   public HorizontalTransducerOffsetsFileService() {
      super(NAME);
   }

   @Override
   public String getInstallationSubDirName() {
      return getName().persistentName();
   }

   @Override
   public Path getInstallationLocation() {
      return getInstallationConfigDir().resolve(getInstallationSubDirName()).resolve("HorizontalTransducerOffsets.xml");
   }

   @Override
   protected ConfigFileParameterEditor createFileParameterEditor(ConfigFileSettings configFileSettings) {
      return new TransducerFileParameterEditor(this, configFileSettings, TransducerParameters.ParameterType.HORIZONTAL);
   }
}
