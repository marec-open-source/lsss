package no.imr.korona.computation.misc;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.SimplePingModule;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class TemporaryComputationsBeginModule extends SimplePingModule {
   public TemporaryComputationsBeginModule() {
   }

   @Override
   public SimplePingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new TemporaryBeginComputation(this, computationContext, pingSource);
   }

   private static final class TemporaryBeginComputation extends SimplePingModuleComputation {
      private TemporaryBeginComputation(TemporaryComputationsBeginModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);

         List<PingItem> tmpConfigurationItems = new ArrayList<>();
         List<PingItem> newConfigurationItems = new ArrayList<>();
         PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
         for (PingItem configurationItem : pingConfiguration.getConfigurationItems()) {
            switch (configurationItem) {
               case RawFileConfiguration _ -> {
                  tmpConfigurationItems.add(configurationItem.makeCopy());
                  newConfigurationItems.add(configurationItem);
               }
               case TemporaryComputationsConfigurationItem _ -> {
                  tmpConfigurationItems.add(configurationItem);
               }
               default -> {
                  newConfigurationItems.add(configurationItem);
               }
            }
         }
         newConfigurationItems.add(new TemporaryComputationsConfigurationItem(pingConfiguration.getRawFileConfiguration().getNTDate(), tmpConfigurationItems));
         PingConfiguration newPingConfiguration = new PingConfiguration(newConfigurationItems);
         setNewPingConfiguration(newPingConfiguration);
      }

      @Override
      protected void convertPing(Ping ping, Ping newPing) {
         List<PingItem> tmpPingItems = new ArrayList<>();
         for (PingItem pingItem : ping.getPingItems()) {
            switch (pingItem) {
               case ChannelData _ -> {
                  tmpPingItems.add(pingItem.makeCopy());
                  newPing.add(pingItem);
               }
               case TemporaryComputationsPingItem _ -> {
                  tmpPingItems.add(pingItem);
               }
               default -> {
                  newPing.add(pingItem);
               }
            }
         }
         newPing.add(new TemporaryComputationsPingItem(ping.getNTDate(), ping.getBot0Datagram().makeCopy(), tmpPingItems));
      }
   }
}
