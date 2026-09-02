package no.imr.korona.computation.broadband.splitting;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.ConfigFileSettingsException;
import no.imr.korona.computation.ModuleUtils;
import no.imr.korona.computation.broadband.BroadbandToAngles;
import no.imr.korona.computation.broadband.BroadbandToSvAtFrequencyBands;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.datagrams.PerChannelDatagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.range.FloatRange;
import no.imr.tools.xml.XmlUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class BroadbandSplitterModuleComputation extends ConcurrentPingModuleComputation {
   private final BroadbandSplitterModule module;
   private final Map<Integer, List<FrequencyBand>> channelToFrequencyBands;
   private final Set<Integer> channelsWithUnexpectedBroadbandData = new HashSet<>();
   private final @Nullable BroadbandSplitterGlidingWindow glidingWindow;

   BroadbandSplitterModuleComputation(BroadbandSplitterModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
      super(module, computationContext, pingSource);

      this.module = module;
      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      RawFileConfiguration oldRawFileConfiguration = pingConfiguration.getRawFileConfiguration();
      RawFileConfiguration newRawFileConfiguration = oldRawFileConfiguration.makeCopy();
      newRawFileConfiguration.clearChannels();
      List<RawFileTransducer> oldTransducers = oldRawFileConfiguration.getTransducers();
      Map<Integer, ChannelData> inputChannelToChannelData = ModuleUtils.getInputChannelToChannelData(this);
      List<FloatRange> inputFrequencyRanges = Utils.getAllOfType(inputChannelToChannelData.values(), BroadbandData.class)
            .map(BroadbandData::getFrequencyRange)
            .toList();
      List<FrequencyBand> candidateBands = getAllCandidateBands(inputFrequencyRanges)
            .filter(this::isFrequencyBandValid)
            .toList();
      channelToFrequencyBands = new HashMap<>();
      for (int channelIndex = 0; channelIndex < oldTransducers.size(); channelIndex++) {
         RawFileTransducer transducer = oldTransducers.get(channelIndex);
         int channel = channelIndex + 1;
         ChannelData channelData = inputChannelToChannelData.get(channel);

         if (channelData instanceof BroadbandData broadbandData) {
            List<FrequencyBand> frequencyBands = new ArrayList<>();
            for (FrequencyBand frequencyBand : candidateBands) {
               if (broadbandData.getFrequencyRange().contains(frequencyBand.frequencyRange)) {
                  frequencyBands.add(frequencyBand);
                  RawFileTransducer transducerCopy = newRawFileConfiguration.newChannel(transducer);
                  broadbandData.initTransducer(transducerCopy, frequencyBand.nominalFrequency());
               }
            }
            channelToFrequencyBands.put(channel, frequencyBands);
         } else {
            newRawFileConfiguration.newChannel(transducer);
         }
      }

      boolean useGlidingWindow = module.computationalMethod.getValue() == BroadbandSplitterModule.ComputationalMethod.GLIDING_FFT_WINDOW;
      glidingWindow = useGlidingWindow ? new BroadbandSplitterGlidingWindow(module, pingConfiguration) : null;

      PingConfiguration newPingConfiguration = pingConfiguration.createCopy(newRawFileConfiguration);
      setNewPingConfiguration(newPingConfiguration);
   }

   @Override
   protected void convertPing(Ping ping, Ping newPing) {
      int outputChannel = 0;

      RawFileConfiguration rawFileConfiguration = ping.getRawFileConfiguration();
      for (int channel = 1; channel <= rawFileConfiguration.getTransducerCount(); channel++) {
         ChannelData channelData = ping.getChannelData(channel);

         List<FrequencyBand> frequencyBands = channelToFrequencyBands.get(channel);
         if (frequencyBands == null) {
            outputChannel++;
            if (channelData != null) {
               if (channelData instanceof BroadbandData && channelsWithUnexpectedBroadbandData.add(channel)) {
                  Log.global.warning("Not splitting broadband data on channel " + channel
                        + ", " + channelData.getTransducer().getChannelId() + ", at " + channelData.getInstant()
                        + " in " + rawFileConfiguration.getDataFile()
                        + ". That channel did not contain broadband data at the beginning of the file.");
               }
               ChannelData channelDataCopy = channelData.makeCopy();
               channelDataCopy.setChannel(outputChannel);
               newPing.add(channelDataCopy);
            }
            continue;
         }

         if (frequencyBands.isEmpty()) {
            continue;
         }

         if (!(channelData instanceof BroadbandData broadbandData)) {
            outputChannel += frequencyBands.size();
            continue;
         }

         int downsamplingFactor = getDownsamplingFactor(channelData.getSampleDistance());

         List<FloatRange> frequencyRanges = frequencyBands.stream()
               .map(FrequencyBand::frequencyRange)
               .toList();
         BroadbandToSvAtFrequencyBands broadbandToSvAtFrequencyBands = new BroadbandToSvAtFrequencyBands(broadbandData,
               frequencyRanges, module.stopBandDistance.getFloatValue() * 1000);

         BroadbandToAngles broadbandToAngles = module.computeAngles.getBooleanValue() ? new BroadbandToAngles(broadbandData) : null;

         List<PowerData> powerDatas = glidingWindow != null ? new ArrayList<>(frequencyBands.size()) : null;

         for (int i = 0; i < frequencyBands.size(); i++) {
            outputChannel++;
            FrequencyBand frequencyBand = frequencyBands.get(i);
            PowerData powerData = new PowerData(broadbandData, (float) broadbandToSvAtFrequencyBands.getPulseCompressions().get(i).getTauEff());
            powerData.setChannel(outputChannel);
            powerData.setFrequency(frequencyBand.nominalFrequency());
            powerData.setBandWidth(frequencyBand.frequencyRange.getSize());
            powerData.setCount(broadbandData.getCount() / downsamplingFactor);
            powerData.setSampleDistance(broadbandData.getSampleDistance() * downsamplingFactor);
            powerData.setOffset(broadbandData.getOffset() / downsamplingFactor);
            RawFileConfiguration.Xml0Info xml0Info = rawFileConfiguration.getXml0Info();
            if (xml0Info != null) {
               powerData.setAbsorptionCoefficient((float) xml0Info.getAbsorption().getAbsorption(frequencyBand.frequencyRange.getCenter()));
            }
            if (glidingWindow != null) {
               powerDatas.add(powerData);
            } else {
               broadbandToSvAtFrequencyBands.calculateSv(i, powerData.getSv());
            }
            if (broadbandToAngles != null) {
               powerData.setElectricAngles(broadbandToAngles.computeElectricalAngles(broadbandToSvAtFrequencyBands.getPulseCompressions().get(i), downsamplingFactor));
            }
            newPing.add(powerData);
         }

         if (glidingWindow != null) {
            glidingWindow.computeSv(broadbandData, powerDatas, frequencyBands, downsamplingFactor, getAsyncHandle());
         }
      }

      ping.getPingItems().stream()
            .filter(pingItem -> !(pingItem instanceof PerChannelDatagram))
            .forEach(newPing::add);
   }

   private int getDownsamplingFactor(float originalSampleSize) {
      return switch (module.downsamplingMethod.getValue()) {
         case NONE -> 1;
         case FACTOR -> module.downsamplingFactor.getIntValue();
         case SAMPLE_SIZE -> Math.max(1, Math.round(module.downsamplingSampleSize.getFloatValue() / originalSampleSize));
      };
   }

   private Stream<FrequencyBand> getAllCandidateBands(List<FloatRange> inputFrequencyRanges) throws ConfigFileSettingsException {
      if (module.autoBands.getBooleanValue()) {
         return makeAutoBands(inputFrequencyRanges, module.splitCount.getIntValue(), module.splitBandwidth.getValue().orElse(0f) * 1000);
      } else {
         return makeBandsFromFile(inputFrequencyRanges);
      }
   }

   static Stream<FrequencyBand> makeAutoBands(List<FloatRange> inputFrequencyRanges, int splitCount, float bandwidth) {
      return inputFrequencyRanges.stream()
            .flatMap(inputFrequencyRange -> {
               float defaultBandwidth = inputFrequencyRange.getSize() / splitCount;
               float actualBandwidth = bandwidth > 0 ? bandwidth : defaultBandwidth;
               float startCenter = inputFrequencyRange.min() + defaultBandwidth / 2;
               return IntStream.range(0, splitCount)
                     .mapToObj(i -> FloatRange.ofCenterAndSize(startCenter + i * defaultBandwidth, actualBandwidth)
                           .intersection(inputFrequencyRange))
                     .map(range -> new FrequencyBand(range, range.getCenter()));
            });
   }

   private Stream<FrequencyBand> makeBandsFromFile(List<FloatRange> inputFrequencyRanges) throws ConfigFileSettingsException {
      Path file = module.getRequiredConfigFile(BroadbandSplitterBandsFileService.NAME);
      try {
         BroadbandSplitterConfig broadbandSplitterConfig = new BroadbandSplitterConfig(XmlUtils.readDocument(file).getRootElement());
         return broadbandSplitterConfig.getBands().stream()
               .map(band -> toFrequencyBand(band, inputFrequencyRanges));
      } catch (IOException e) {
         throw new ConfigFileSettingsException(module, BroadbandSplitterBandsFileService.NAME, file, e.getMessage());
      }
   }

   @Override
   public Bot0Datagram convertBot0(Bot0Datagram bot0Datagram) {
      double[] inputDepths = bot0Datagram.getChannelDepths();
      double[] outputDepths = new double[getPingConfiguration().getRawFileConfiguration().getTransducerCount()];
      int outputChannelIndex = 0;
      for (int inputChannelIndex = 0; inputChannelIndex < inputDepths.length; inputChannelIndex++) {
         List<FrequencyBand> frequencyBands = channelToFrequencyBands.get(inputChannelIndex + 1);
         int count = frequencyBands != null ? frequencyBands.size() : 1;
         for (int index = 0; index < count; index++) {
            outputDepths[outputChannelIndex++] = inputDepths[inputChannelIndex];
         }
      }
      return new Bot0Datagram(bot0Datagram.getInstant(), outputDepths);
   }

   record FrequencyBand(FloatRange frequencyRange, float nominalFrequency) {
   }

   private static FrequencyBand toFrequencyBand(BroadbandSplitterBand band, List<FloatRange> inputFrequencyRanges) {
      float nominalFrequency = band.nominal.getFloatValue() * 1000;
      FloatRange frequencyRange;
      Optional<Float> optionalStartKHz = band.start.getValue();
      Optional<Float> optionalStopKHz = band.stop.getValue();
      if (optionalStartKHz.isEmpty() && optionalStopKHz.isEmpty()) {
         frequencyRange = inputFrequencyRanges.stream()
               .filter(range -> range.contains(nominalFrequency))
               .min(Comparator.comparingDouble(range -> Math.abs(range.getCenter() - nominalFrequency)))
               .orElse(FloatRange.EMPTY_RANGE);
      } else {
         float startKHz = optionalStartKHz.orElseGet(() -> {
            float maxFrequency = optionalStopKHz.get() * 1000;
            double minFrequency = inputFrequencyRanges.stream()
                  .filter(range -> range.contains(maxFrequency))
                  .mapToDouble(FloatRange::min)
                  .max()
                  .orElse(maxFrequency);
            return (float) minFrequency / 1000;
         });
         float stopKHz = optionalStopKHz.orElseGet(() -> {
            float minFrequency = optionalStartKHz.get() * 1000;
            double maxFrequency = inputFrequencyRanges.stream()
                  .filter(range -> range.contains(minFrequency))
                  .mapToDouble(FloatRange::max)
                  .min()
                  .orElse(minFrequency);
            return (float) maxFrequency / 1000;
         });
         frequencyRange = FloatRange.of(startKHz * 1000, stopKHz * 1000);
      }
      return new FrequencyBand(frequencyRange, nominalFrequency);
   }

   private boolean isFrequencyBandValid(FrequencyBand frequencyBand) {
      FloatRange frequencyRange = frequencyBand.frequencyRange();
      return !frequencyRange.isEmpty() && frequencyRange.getSize() >= module.minBandwidth.getFloatValue() * 1000;
   }
}
