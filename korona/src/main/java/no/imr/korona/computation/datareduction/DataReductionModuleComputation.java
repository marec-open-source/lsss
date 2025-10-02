package no.imr.korona.computation.datareduction;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.computation.offset.TransducerParameterManager;
import no.imr.korona.computation.offset.TransducerParameters;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.data.datagrams.Dep0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

final class DataReductionModuleComputation extends ConcurrentPingModuleComputation {
   private final DataReductionModule module;
   private final float[] ranges;
   private final float[] blindZones;

   DataReductionModuleComputation(DataReductionModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
      super(module, computationContext, pingSource);

      this.module = module;
      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      RawFileConfiguration rawFileConfiguration = pingConfiguration.getRawFileConfiguration();

      blindZones = new float[rawFileConfiguration.getTransducerCount()];
      Arrays.fill(blindZones, Float.NEGATIVE_INFINITY);

      ranges = new float[rawFileConfiguration.getTransducerCount()];
      Arrays.fill(ranges, Float.POSITIVE_INFINITY);

      if (module.useTransducerBlindZone.getBooleanValue() || module.useTransducerRange.getBooleanValue()) {
         Path file = module.getOptionalConfigFile(TransducerRangesFileService.NAME);
         if (file == null) {
            throw new ModuleConfigurationException(module, "Need config file \"" + TransducerRangesFileService.NAME.displayName()
                  + "\" if parameter " + module.useTransducerBlindZone.getDisplayName() + " or " + module.useTransducerRange.getDisplayName() + " is selected.");
         }
         TransducerParameterManager transducerParameterManager = new TransducerParameterManager(TransducerParameters.ParameterType.VERTICAL, XmlUtils.readDocument(file));

         for (int channelIndex = 0; channelIndex < rawFileConfiguration.getTransducerCount(); channelIndex++) {
            int kHz = rawFileConfiguration.getTransducers().get(channelIndex).getKHz();
            Optional<Float> blindZone = transducerParameterManager.getBlindZone(kHz);
            Optional<Float> range = transducerParameterManager.getRange(kHz);
            if (blindZone.isPresent() && range.isPresent()) {
               if (module.useTransducerBlindZone.getBooleanValue()) {
                  blindZones[channelIndex] = blindZone.get();
               }
               if (module.useTransducerRange.getBooleanValue()) {
                  ranges[channelIndex] = range.get();
               }
            } else {
               Log.global.warning("Missing configuration for " + kHz + " kHz in file " + file);
            }
         }
      }
   }

   @Override
   protected void processPing(Ping ping) {
      for (ChannelData channelData : ping.getChannelDatas()) {
         if (channelData == null) {
            continue;
         }
         int channelIndex = channelData.getChannel() - 1;

         float minRange = Math.max(channelData.getMinRange(), blindZones[channelIndex]);
         if (module.minRange.getValue().isPresent()) {
            minRange = Math.max(minRange, module.minRange.getValue().get());
         }
         if (module.minDepth.getValue().isPresent()) {
            minRange = Math.max(minRange, channelData.depthToRange(module.minDepth.getValue().get()));
         }

         float maxRange = Math.min(channelData.getMaxRange(), ranges[channelIndex]);
         if (module.maxRange.getValue().isPresent()) {
            maxRange = Math.min(maxRange, module.maxRange.getValue().get());
         }
         if (module.maxDepth.getValue().isPresent()) {
            maxRange = Math.min(maxRange, channelData.depthToRange(module.maxDepth.getValue().get()));
         }
         if (module.maxBelowBottom.getValue().isPresent()) {
            Dep0Datagram dep0Datagram = ping.getDep0Datagram();
            if (dep0Datagram != null) {
               maxRange = Math.min(maxRange, channelData.depthToRange(dep0Datagram.getDepth() + module.maxBelowBottom.getValue().get()));
            }
         }

         int beginIndex = Math.clamp((int) Math.ceil(channelData.rangeToSampleIndexAsFloat(minRange)), 0, channelData.getCount());
         int endIndex = Math.clamp((int) Math.floor(channelData.rangeToSampleIndexAsFloat(maxRange)), beginIndex, channelData.getCount());

         if (beginIndex > 0 || endIndex < channelData.getCount()) {
            channelData.reduceData(beginIndex, endIndex);
         }
      }
   }
}
