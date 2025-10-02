package no.imr.korona.computation.broadband.pulsecompressionfilter;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.ConfigFileSettingsException;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.xml.XmlUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

final class PulseCompressionFilterModuleComputation extends ConcurrentPingModuleComputation {
   private final PulseCompressionFilterModule module;

   PulseCompressionFilterModuleComputation(PulseCompressionFilterModule module, ComputationContext computationContext, PingSource pingSource) throws ConfigFileSettingsException {
      super(module, computationContext, pingSource);

      this.module = module;
      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      RawFileConfiguration rawFileConfigurationCopy = pingConfiguration.getRawFileConfiguration().makeCopy();
      rawFileConfigurationCopy.setPulseCompressionFilterMap(generatePulseCompressionFilterConfigsFromFile(pingConfiguration.getRawFileConfiguration().getInstant()));
      PingConfiguration newPingConfiguration = pingConfiguration.createCopy(rawFileConfigurationCopy);
      setNewPingConfiguration(newPingConfiguration);
   }

   @Override
   protected void convertPing(Ping ping, Ping newPing) {
      newPing.addAll(ping.getPingItems());
   }

   private Map<String, List<PulseCompressionFilterConfig>> generatePulseCompressionFilterConfigsFromFile(Instant instant) throws ConfigFileSettingsException {
      Path file = module.getRequiredConfigFile(PulseCompressionFiltersFileService.NAME);
      try {
         PulseCompressionFilterModuleConfig pulseCompressionFilterModuleConfig = new PulseCompressionFilterModuleConfig(XmlUtils.readDocument(file).getRootElement());
         return Map.copyOf(pulseCompressionFilterModuleConfig.getPulseCompressionFiltersPerChannel());
      } catch (IOException e) {
         throw new ConfigFileSettingsException(module, PulseCompressionFiltersFileService.NAME, file, e.getMessage());
      }
   }
}
