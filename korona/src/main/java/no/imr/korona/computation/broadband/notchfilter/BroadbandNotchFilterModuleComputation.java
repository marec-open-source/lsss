package no.imr.korona.computation.broadband.notchfilter;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.ConfigFileSettingsException;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

final class BroadbandNotchFilterModuleComputation extends ConcurrentPingModuleComputation {
   private final BroadbandNotchFilterModule module;

   BroadbandNotchFilterModuleComputation(BroadbandNotchFilterModule module, ComputationContext computationContext, PingSource pingSource) throws ConfigFileSettingsException {
      super(module, computationContext, pingSource);

      this.module = module;
      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      RawFileConfiguration rawFileConfigurationCopy = pingConfiguration.getRawFileConfiguration().makeCopy();
      rawFileConfigurationCopy.possiblyAddBroadbandNotchFilterConfigs(generateNotchFilterConfigsFromFile(pingConfiguration.getRawFileConfiguration().getInstant()));
      PingConfiguration newPingConfiguration = pingConfiguration.createCopy(rawFileConfigurationCopy);
      setNewPingConfiguration(newPingConfiguration);
   }

   @Override
   protected void convertPing(Ping ping, Ping newPing) {
      newPing.addAll(ping.getPingItems());
   }

   private List<BroadbandNotchFilterConfig> generateNotchFilterConfigsFromFile(Instant instant) throws ConfigFileSettingsException {
      Path file = module.getRequiredConfigFile(BroadbandNotchFiltersFileService.NAME);
      try {
         Element element = XmlUtils.readDocument(file).getRootElement();
         BroadbandNotchFilterModuleConfig broadbandNotchFilterModuleConfig = new BroadbandNotchFilterModuleConfig(element);
         return broadbandNotchFilterModuleConfig.getBroadbandTemporalNotchFilterConfigs().stream()
               .filter(config -> config.isValid(instant))
               .map(BroadbandTemporalNotchFilterConfig::getBroadbandNotchFilterConfig)
               .toList();
      } catch (IOException e) {
         throw new ConfigFileSettingsException(module, BroadbandNotchFiltersFileService.NAME, file, e.getMessage());
      }
   }
}
