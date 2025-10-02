package no.imr.korona.computation.datareduction;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.ChannelData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class ChannelDataRemovalModuleComputation extends ConcurrentPingModuleComputation {
   private final ChannelDataRemovalModule module;
   private Predicate<ChannelData> remove = __ -> false;

   ChannelDataRemovalModuleComputation(ChannelDataRemovalModule module, ComputationContext computationContext, PingSource pingSource) {
      super(module, computationContext, pingSource);

      this.module = module;
      updateChannelsToRemove();
   }

   public void updateChannelsToRemove() {
      List<Optional<Predicate<ChannelData>>> predicates = new ArrayList<>();

      Set<Integer> selectedChannels = ChannelDataRemovalModule.selectedChannels(getPingConfiguration(),
            module.channels, module.channelsFromEnd, module.frequencies, module.pingId);
      if (!selectedChannels.isEmpty()) {
         predicates.add(Optional.of(channelData -> selectedChannels.contains(channelData.getChannel())));
      }
      predicates.add(module.dataType.getValue().predicate());
      predicates.add(module.transmitMode.getValue().predicate());

      Predicate<ChannelData> predicate = predicates.stream()
            .flatMap(Optional::stream)
            .reduce(Predicate::or)
            .orElse(__ -> false);

      remove = module.keepSpecified.getBooleanValue() ? predicate.negate() : predicate;
   }

   @Override
   protected void processPing(Ping ping) {
      for (ChannelData channelData : ping.getChannelDatas()) {
         if (channelData != null && remove.test(channelData)) {
            ping.remove(channelData);
         }
      }
   }
}
