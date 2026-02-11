package no.imr.korona.computation.misc;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.computation.offset.TransducerParameterManager;
import no.imr.korona.computation.offset.TransducerParameters;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.datagrams.Dep0Datagram;
import no.imr.korona.data.formats.missing.MissingBot0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.util.bottomdetection.Algorithms;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class DepthModuleComputation extends SimplePingModuleComputation {
   private final DepthModule module;
   private final Map<Integer, Float> transducerRanges = new HashMap<>();
   private final Algorithms algorithms;

   DepthModuleComputation(DepthModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
      super(module, computationContext, pingSource);

      this.module = module;

      Path file = module.getRequiredConfigFile(TransducerRangesFileService.NAME);
      TransducerParameterManager transducerParameterManager = new TransducerParameterManager(TransducerParameters.ParameterType.VERTICAL, XmlUtils.readDocument(file));

      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      RawFileConfiguration rawFileConfiguration = pingConfiguration.getRawFileConfiguration();
      List<RawFileTransducer> transducers = rawFileConfiguration.getTransducers();
      for (int channel = 1; channel <= transducers.size(); channel++) {
         RawFileTransducer transducer = transducers.get(channel - 1);
         int kHz = transducer.getKHz();

         if (kHz < module.minKHz.getFloatValue() || kHz > module.maxKHz.getFloatValue() || module.doNotUseKHz.getValue().contains(kHz)) {
            continue;
         }

         Optional<Float> range = transducerParameterManager.getRange(kHz);
         if (range.isEmpty()) {
            Log.global.warning("No range configured for " + kHz + " kHz in file " + file);
            continue;
         }

         transducerRanges.put(channel, range.get() * module.maxRangeFactor.getFloatValue());
      }

      int preferredChannel = module.preferredKHz.getValue()
            .map(kHz -> rawFileConfiguration.firstChannelClosestTo(kHz * 1000))
            .filter(transducerRanges::containsKey)
            .orElse(-1);

      Algorithms.DistanceToIndexFunc depthToIndexFunc = PowerData::depthToSampleIndex;
      algorithms = new Algorithms(depthToIndexFunc, PowerData::getSampleDepth, depthToIndexFunc,
            module.signalStrengthThreshold.getFloatValue(),
            module.minBottomDepth.getFloatValue(),
            module.maxBottomDepth.getFloatValue(),
            transducerRanges,
            preferredChannel);
   }

   @Override
   protected void processPing(Ping ping) {
      ping.removeAll(Dep0Datagram.class);

      List<PowerData> powerDatas = ping.getNonNullPowerDatas()
            .filter(powerData -> transducerRanges.containsKey(powerData.getChannel()))
            .toList();
      if (!powerDatas.isEmpty()) {
         Dep0Datagram depDatagram = new Dep0Datagram(ping.getNTDate());
         Bot0Datagram bot0Datagram = ping.getBot0Datagram();

         if (!module.forceDetection.getBooleanValue() && !(bot0Datagram instanceof MissingBot0Datagram) && allBot0DepthsZero(bot0Datagram)) {
            // A zero-depth in bot0 datagram is an indication that there is no detectable depth in the ping.
            return;
         } else if (module.keepBottomDeeperThanData.getBooleanValue() && someBot0DepthDeeperThanData(bot0Datagram, powerDatas)) {
            // BOT0 depth is deeper than data depth => Keep BOT0 depths.
            float depth = (float) powerDatas.stream()
                  .mapToDouble(powerData -> bot0Datagram.getChannelDepths()[powerData.getChannel() - 1])
                  .min()
                  .orElse(0);
            depDatagram.setDepth(depth);
            depDatagram.setMinimumDepth(depth);
            ping.add(depDatagram);
            return;
         } else {
            int channelCount = ping.getRawFileConfiguration().getTransducerCount();
            List<PowerData> nonNullPowerDatas = ping.getNonNullPowerDatas().toList();
            float bottomDepth = switch (module.algorithm.getValue()) {
               case EK500 -> algorithms.findBottomByEK500(powerDatas);
               case Gradient -> algorithms.findBottomByGradient(powerDatas);
               case Threshold -> algorithms.findBottomByThreshold(powerDatas);
               case Pelagic -> algorithms.findBottomByPelagic();
            };
            Algorithms.BackstepDepths backstepResult = algorithms.backstepAndSetMinDepth(bottomDepth, nonNullPowerDatas, channelCount,
                  module.minDepthValueFraction.getFloatValue(), module.minimumDepthThresholdFactor.getFloatValue(), module.minDepthLimit.getFloatValue());
            fillBackstepResult(depDatagram, bot0Datagram, channelCount, backstepResult);
         }

         if (algorithms.isValidBottom()) {
            ping.add(depDatagram);
         }
      }
   }

   private void fillBackstepResult(Dep0Datagram dep0Datagram, Bot0Datagram bot0Datagram, int channelCount, Algorithms.BackstepDepths backstepResult) {
      for (int i = 0; i < channelCount; i++) {
         bot0Datagram.getChannelDepths()[i] = backstepResult.channelDepths()[i];
      }
      dep0Datagram.setDepth(backstepResult.depth());
      dep0Datagram.setMinimumDepth(backstepResult.minimumDepth() - module.coordinatedBottomOffset.getFloatValue());
   }

   private static boolean allBot0DepthsZero(Bot0Datagram bot0Datagram) {
      for (double depth : bot0Datagram.getChannelDepths()) {
         if (depth != 0) {
            return false;
         }
      }
      return true;
   }

   private static boolean someBot0DepthDeeperThanData(Bot0Datagram bot0Datagram, List<PowerData> powerDatas) {
      for (PowerData powerData : powerDatas) {
         if (bot0Datagram.getChannelDepths()[powerData.getChannel() - 1] >= powerData.getMaxDepth() - 10) {
            return true;
         }
      }
      return false;
   }
}
