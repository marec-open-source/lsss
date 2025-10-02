package no.imr.korona.computation.broadband.notchfilter;

import no.imr.korona.config.ConfigFileParameterEditor;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.KoronaConfigFileService;
import no.imr.tools.parameter.Name;

import java.nio.file.Path;

public final class BroadbandNotchFiltersFileService extends KoronaConfigFileService {
   public static final Name NAME = new Name("BroadbandNotchFilters", "Broadband notch filters");
   public static final String BROADBAND_NOTCH_FILTER_XML_FILE = "BroadbandNotchFilters.xml";
   public static final String BROADBAND_NOTCH_FILTER_XML = "notchFilters";
   public static final String FILTER_XML = "filter";
   public static final String REJECTION_FREQUENCY_KHZ = "RejectionFrequencyKHz";
   public static final String BANDWIDTH_KHZ = "BandwidthKHz";

   public BroadbandNotchFiltersFileService() {
      super(NAME);
   }

   @Override
   public String getInstallationSubDirName() {
      return getName().persistentName();
   }

   @Override
   public Path getInstallationLocation() {
      return getInstallationConfigDir().resolve(getInstallationSubDirName()).resolve(BROADBAND_NOTCH_FILTER_XML_FILE);
   }

   @Override
   protected ConfigFileParameterEditor createFileParameterEditor(ConfigFileSettings configFileSettings) {
      return new BroadbandNotchFilterFileParameterEditor(this, configFileSettings);
   }

   @Override
   public boolean canBePlatformSpecific() {
      return false;
   }
}
