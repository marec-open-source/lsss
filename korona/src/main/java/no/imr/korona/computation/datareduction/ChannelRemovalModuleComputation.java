package no.imr.korona.computation.datareduction;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.IgnoreModuleComputationException;
import no.imr.korona.computation.ModuleUtils;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.datagrams.PerChannelDatagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.channel.ChannelData;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

final class ChannelRemovalModuleComputation extends ConcurrentPingModuleComputation {
   private final int[] oldToNewChannel;
   private final int[] newToOldChannel;

   ChannelRemovalModuleComputation(ChannelRemovalModule module, ComputationContext computationContext, PingSource pingSource) throws IOException, IgnoreModuleComputationException {
      super(module, computationContext, pingSource);

      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      Set<Integer> channelsToRemove = findChannelsToRemove(module, pingConfiguration);
      if (channelsToRemove.isEmpty()) {
         throw new IgnoreModuleComputationException();
      }

      oldToNewChannel = new int[pingConfiguration.getRawFileConfiguration().getTransducerCount() + 1];
      newToOldChannel = new int[oldToNewChannel.length - channelsToRemove.size()];
      for (int oldChannel = 1, newChannel = 1; oldChannel < oldToNewChannel.length; oldChannel++) {
         if (channelsToRemove.contains(oldChannel)) {
            oldToNewChannel[oldChannel] = 0;
         } else {
            oldToNewChannel[oldChannel] = newChannel;
            newToOldChannel[newChannel] = oldChannel;
            newChannel++;
         }
      }

      PingConfiguration newPingConfiguration = pingConfiguration.createCopy();
      channelsToRemove.stream()
            .sorted(Comparator.reverseOrder())
            .forEach(newPingConfiguration.getRawFileConfiguration()::removeChannel);
      setNewPingConfiguration(newPingConfiguration);
   }

   private Set<Integer> findChannelsToRemove(ChannelRemovalModule module, PingConfiguration pingConfiguration) throws IOException {
      Set<Integer> selectedChannels = new HashSet<>(ChannelDataRemovalModule.selectedChannels(pingConfiguration,
            module.channels, module.channelsFromEnd, module.frequencies, module.pingId));

      Predicate<ChannelData> channelDataPredicate = Stream.of(
                  module.dataType.getValue().predicate(),
                  module.transmitMode.getValue().predicate()
            )
            .flatMap(Optional::stream)
            .reduce(Predicate::or)
            .orElse(null);
      if (channelDataPredicate != null) {
         ModuleUtils.getInputChannelToChannelData(this).values().stream()
               .filter(channelDataPredicate)
               .map(ChannelData::getChannel)
               .forEach(selectedChannels::add);
      }
      return module.keepSpecified.getBooleanValue()
            ? ChannelDataRemovalModule.invertChannels(pingConfiguration, selectedChannels)
            : selectedChannels;
   }

   @Override
   protected void convertPing(Ping ping, Ping newPing) {
      for (PingItem pingItem : ping.getPingItems()) {
         if (pingItem instanceof PerChannelDatagram perChannelDatagram) {
            int newChannel = oldToNewChannel[perChannelDatagram.getChannel()];
            if (newChannel == 0) {
               // To be removed
               continue;
            }
            perChannelDatagram.setChannel(newChannel);
         }
         newPing.add(pingItem);
      }
   }

   @Override
   public Bot0Datagram convertBot0(Bot0Datagram bot0Datagram) {
      double[] oldChannelDepths = bot0Datagram.getChannelDepths();
      double[] newChannelDepths = new double[getPingConfiguration().getRawFileConfiguration().getTransducerCount()];
      for (int newChannel = 1; newChannel < newToOldChannel.length; newChannel++) {
         int oldChannel = newToOldChannel[newChannel];
         newChannelDepths[newChannel - 1] = oldChannelDepths[oldChannel - 1];
      }
      return new Bot0Datagram(bot0Datagram.getInstant(), newChannelDepths);
   }
}
