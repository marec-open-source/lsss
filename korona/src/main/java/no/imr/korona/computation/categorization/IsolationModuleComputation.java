package no.imr.korona.computation.categorization;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.Cad0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;

import java.util.Arrays;
import java.util.List;

final class IsolationModuleComputation extends ConcurrentPingModuleComputation {
   private final byte categoryNumber;

   IsolationModuleComputation(IsolationModule module, ComputationContext computationContext, PingSource pingSource) throws ModuleConfigurationException {
      super(module, computationContext, pingSource);

      Cac0Datagram cac0Datagram = getRequiredConfigItem(Cac0Datagram.class, "No categorization configuration datagram");
      List<String> allCategories = cac0Datagram.getCategories().stream()
            .map(Cac0Datagram.Category::getName)
            .toList();
      module.setAvailableCategories(allCategories);

      String categoryName = module.category.getValue();
      Cac0Datagram.Category category = cac0Datagram.nameToCategory(categoryName);
      if (category == null) {
         throw new ModuleConfigurationException(module, "No category with name " + categoryName);
      }
      categoryNumber = category.getNumber();

      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      RawFileConfiguration rawFileConfiguration = pingConfiguration.getRawFileConfiguration();
      for (RawFileTransducer transducer : rawFileConfiguration.getTransducers()) {
         transducer.setChannelId(categoryName);
      }
   }

   @Override
   protected void processPing(Ping ping) {
      Cad0Datagram cad0Datagram = ping.getPingItem(Cad0Datagram.class);
      if (cad0Datagram == null) {
         ping.getNonNullPowerDatas().forEach(powerData -> {
            float[] sv = powerData.getSv();
            Arrays.fill(sv, 0);
            powerData.setSv(sv);
         });
         return;
      }

      ping.getNonNullPowerDatas().forEach(powerData -> {
         float[] sv = powerData.getSv();
         for (int i = 0; i < sv.length; i++) {
            float depth = powerData.getSampleDepth(i);
            if (cad0Datagram.getBestCategory(cad0Datagram.depthToIndex(depth)) != categoryNumber) {
               sv[i] = 0;
            }
         }
         powerData.setSv(sv);
      });
   }
}
