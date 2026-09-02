package no.imr.korona.computation.datareduction;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.ModuleUtils;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.logging.Log;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class ComplexToRealModuleComputation extends ConcurrentPingModuleComputation {
   private final ComplexToRealModule module;

   ComplexToRealModuleComputation(ComplexToRealModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
      super(module, computationContext, pingSource);

      this.module = module;
      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      RawFileConfiguration inConfiguration = pingConfiguration.getRawFileConfiguration();
      RawFileConfiguration outConfiguration = inConfiguration.makeCopy();
      List<RawFileTransducer> transducers = inConfiguration.getTransducers();
      Map<Integer, ChannelData> inputChannelToChannelData = ModuleUtils.getInputChannelToChannelData(this);
      for (int channelIndex = 0; channelIndex < transducers.size(); channelIndex++) {
         RawFileTransducer transducer = transducers.get(channelIndex);
         int channel = channelIndex + 1;
         ChannelData channelData = inputChannelToChannelData.get(channel);
         if (channelData == null) {
            if (peekPingSourcePing(0) != null) {
               Log.global.warning("No data for channel " + channel + ", " + transducer.getChannelId());
            }
            continue;
         }
         if (channelData instanceof PowerData ||
               channelData instanceof BroadbandData && module.keepBroadband.getBooleanValue()) {
            continue;
         }
         RawFileTransducer transducerCopy = channelData.getPowerData().getTransducer().makeCopy();
         transducerCopy.setFrequency(transducer.getFrequency());
         outConfiguration.getTransducers().set(channelIndex, transducerCopy);
      }
      PingConfiguration newPingConfiguration = pingConfiguration.createCopy(outConfiguration);
      setNewPingConfiguration(newPingConfiguration);
   }

   @Override
   protected void convertPing(Ping ping, Ping newPing) {
      for (PingItem pingItem : ping.getPingItems()) {
         if (pingItem instanceof ChannelData channelData) {
            newPing.add(convertChannelData(channelData));
         } else {
            newPing.add(pingItem);
         }
      }
   }

   private ChannelData convertChannelData(ChannelData channelData) {
      if (channelData instanceof PowerData ||
            channelData instanceof BroadbandData && module.keepBroadband.getBooleanValue()) {
         return channelData;
      }
      return toPowerData(channelData, module.computeAngles.getBooleanValue());
   }

   public static PowerData toPowerData(ChannelData channelData, boolean computeAngles) {
      if (channelData instanceof PowerData powerData) {
         return powerData;
      }

      PowerData powerData = channelData.getPowerData();
      powerData.setReadWrite();
      powerData.setFrequency(channelData.getTransducer().getFrequency());
      if (computeAngles) {
         AngleData angleData = powerData.getAngleData();
         if (angleData != null) {
            angleData.getElectricalAngles();
         }
      } else {
         powerData.removeAngles();
      }
      return powerData;
   }
}
