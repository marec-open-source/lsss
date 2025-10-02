package no.imr.korona.computation.datareduction;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.BeamType;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;

final class AngleDeletionModuleComputation extends ConcurrentPingModuleComputation {
   AngleDeletionModuleComputation(AngleDeletionModule module, ComputationContext computationContext, PingSource pingSource) {
      super(module, computationContext, pingSource);

      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      PingConfiguration newPingConfiguration = pingConfiguration.createCopy();
      RawFileConfiguration rawFileConfiguration = newPingConfiguration.getRawFileConfiguration();
      rawFileConfiguration.getTransducers().forEach(transducer -> transducer.setBeamType(BeamType.SINGLE));
      setNewPingConfiguration(newPingConfiguration);
   }

   @Override
   protected void convertPing(Ping ping, Ping newPing) {
      newPing.addAll(ping.getPingItems());
   }

   @Override
   protected void processPing(Ping ping) {
      ping.getNonNullChannelDatas().forEach(ChannelData::removeAngles);
   }
}
