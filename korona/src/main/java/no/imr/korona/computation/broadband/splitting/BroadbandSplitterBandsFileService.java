package no.imr.korona.computation.broadband.splitting;

import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.KoronaConfigFileService;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;

import java.nio.file.Path;

public final class BroadbandSplitterBandsFileService extends KoronaConfigFileService {
   public static final Name NAME = new Name("BroadbandSplitterBands", "Broadband splitter bands");
   public static final String BROADBAND_SPLITTER_BANDS_FILE = "BroadbandSplitterBands.xml";
   public static final String SPLITTER_BANDS_XML = "splitterBands";
   public static final String BAND_XML = "band";
   public static final String FREQUENCY_NOMINAL = "frequencyNominal";
   public static final String FREQUENCY_START = "frequencyStart";
   public static final String FREQUENCY_STOP = "frequencyStop";

   public BroadbandSplitterBandsFileService() {
      super(NAME);
   }

   @Override
   public String getInstallationSubDirName() {
      return getName().persistentName();
   }

   @Override
   public Path getInstallationLocation() {
      return getInstallationConfigDir().resolve(getInstallationSubDirName()).resolve(BROADBAND_SPLITTER_BANDS_FILE);
   }

   @Override
   protected FileParameter.Editor createFileParameterEditor(ConfigFileSettings configFileSettings) {
      return new BroadbandSplitterFileParameterEditor(this, configFileSettings);
   }

   @Override
   public boolean canBePlatformSpecific() {
      return false;
   }
}
