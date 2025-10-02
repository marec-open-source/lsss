package no.imr.korona.computation.misc;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.computation.ModuleProcessingException;
import no.imr.korona.computation.SimplePingModule;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TemporaryComputationsEndModule extends SimplePingModule {
   public TemporaryComputationsEndModule() {
   }

   @Override
   public SimplePingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new TemporaryEndComputation(this, computationContext, pingSource);
   }

   private static final class TemporaryEndComputation extends SimplePingModuleComputation {
      private final TemporaryComputationsEndModule module;
      private final List<ChannelMapping> channelMappings;

      private TemporaryEndComputation(TemporaryComputationsEndModule module, ComputationContext computationContext, PingSource pingSource) throws ModuleConfigurationException {
         super(module, computationContext, pingSource);

         this.module = module;
         PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
         TemporaryComputationsConfigurationItem tmpConfigurationItem = pingConfiguration.getConfigurationItem(TemporaryComputationsConfigurationItem.class);
         if (tmpConfigurationItem == null) {
            throw new ModuleConfigurationException(module, "Missing start of temporary computations");
         }
         RawFileConfiguration newFileConfiguration = (RawFileConfiguration) tmpConfigurationItem.configurationItems.getFirst();
         PingConfiguration newPingConfiguration = pingConfiguration.createCopy(newFileConfiguration);
         newPingConfiguration.getConfigurationItems().remove(tmpConfigurationItem);
         newPingConfiguration.getConfigurationItems().addAll(tmpConfigurationItem.configurationItems.subList(1, tmpConfigurationItem.configurationItems.size()));

         channelMappings = makeTransducerMapping(pingConfiguration.getRawFileConfiguration().getTransducers(), newFileConfiguration.getTransducers());

         setNewPingConfiguration(newPingConfiguration);
      }

      private static List<ChannelMapping> makeTransducerMapping(List<RawFileTransducer> inTransducers, List<RawFileTransducer> outTransducers) {
         Map<RawFileTransducer, Integer> transducerToInChannelIndex = new HashMap<>();
         for (int inChannelIndex = 0; inChannelIndex < inTransducers.size(); inChannelIndex++) {
            RawFileTransducer inTransducer = inTransducers.get(inChannelIndex);
            transducerToInChannelIndex.put(inTransducer.getOriginalTransducer(), inChannelIndex);
         }
         List<ChannelMapping> channelMappings = new ArrayList<>();
         for (int outChannelIndex = 0; outChannelIndex < outTransducers.size(); outChannelIndex++) {
            RawFileTransducer outTransducer = outTransducers.get(outChannelIndex);
            Integer inChannelIndex = transducerToInChannelIndex.get(outTransducer.getOriginalTransducer());
            if (inChannelIndex != null) {
               channelMappings.add(new ChannelMapping(inChannelIndex, outChannelIndex));
            }
         }
         return channelMappings;
      }

      @Override
      protected void convertPing(Ping ping, Ping newPing) throws ModuleProcessingException {
         TemporaryComputationsPingItem tmpPingItem = ping.getPingItem(TemporaryComputationsPingItem.class);
         if (tmpPingItem == null) {
            throw new ModuleProcessingException(module, "Missing temporary computations data");
         }
         newPing.addAll(tmpPingItem.pingItems);
         for (PingItem pingItem : ping.getPingItems()) {
            if (!(pingItem instanceof ChannelData) && !(pingItem instanceof TemporaryComputationsPingItem)) {
               newPing.add(pingItem);
            }
         }
      }

      @Override
      protected Bot0Datagram convertBot0(Ping ping) throws ModuleProcessingException {
         TemporaryComputationsPingItem tmpPingItem = ping.getPingItem(TemporaryComputationsPingItem.class);
         if (tmpPingItem == null) {
            throw new ModuleProcessingException(module, "Missing temporary computations data");
         }
         Bot0Datagram outBot0Datagram = tmpPingItem.bot0Datagram;
         Bot0Datagram inBot0Datagram = ping.getBot0Datagram();
         for (ChannelMapping channelMapping : channelMappings) {
            outBot0Datagram.getChannelDepths()[channelMapping.outChannelIndex] = inBot0Datagram.getChannelDepths()[channelMapping.inChannelIndex];
         }
         return outBot0Datagram;
      }
   }

   private record ChannelMapping(int inChannelIndex, int outChannelIndex) {
   }
}
