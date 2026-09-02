package no.imr.korona.computation.broadband.pulsecompressionfilter;

import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.KoronaConfigFileService;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;

import java.nio.file.Path;

public final class PulseCompressionFiltersFileService extends KoronaConfigFileService {
   public static final Name NAME = new Name("PulseCompressionFilters", "Pulse compression filters");
   public static final String PULSE_COMPRESSION_FILTERS_XML_FILE = "PulseCompressionFilters.xml";
   public static final String PULSE_COMPRESSION_FILTER_XML = "pulseCompressionFilters";
   public static final String FILTER_XML = "filter";
   public static final String REF_STAGE = "refStage";
   public static final String MIN_SLOPE = "minSlope";
   public static final String MAX_SLOPE = "maxSlope";
   public static final String PULSE_DURATION = "pulseDuration";
   public static final String PULSE_FORM = "pulseForm";
   public static final String DECIMATION_FACTOR = "decimationFactor";
   public static final String COEFFICIENTS_RE = "re";
   public static final String COEFFICIENTS_IM = "im";
   public static final String CHANNEL_XML = "channel";
   public static final String CHANNEL_ID_XML = "id";
   public static final String APPLIES_TO_XML = "appliesTo";

   public PulseCompressionFiltersFileService() {
      super(NAME);
   }

   @Override
   public String getInstallationSubDirName() {
      return getName().persistentName();
   }

   @Override
   public Path getInstallationLocation() {
      return getInstallationConfigDir().resolve(getInstallationSubDirName()).resolve(PULSE_COMPRESSION_FILTERS_XML_FILE);
   }

   @Override
   protected FileParameter.Editor createFileParameterEditor(ConfigFileSettings configFileSettings) {
      return new PulseCompressionFilterFileParameterEditor(this, configFileSettings);
   }

   @Override
   public boolean canBePlatformSpecific() {
      return false;
   }
}
