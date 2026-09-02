package no.imr.korona.computation.misc;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.logging.Log;
import no.imr.tools.math.MathUtils;
import no.imr.tools.math.Quantile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class RescaleModuleComputation extends SimplePingModuleComputation {
   private final RescaleModule module;

   private final int channelsIn;
   private final int firstChannelToRescale;

   private final float[] scaling;
   private final float[] translation;

   RescaleModuleComputation(RescaleModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
      super(module, computationContext, pingSource);

      this.module = module;
      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      PingConfiguration newPingConfiguration = pingConfiguration.createCopy();
      RawFileConfiguration rawFileConfiguration = newPingConfiguration.getRawFileConfiguration();
      channelsIn = rawFileConfiguration.getTransducerCount();

      scaling = new float[channelsIn + 1];
      Arrays.fill(scaling, 1);

      translation = new float[channelsIn + 1];
      Arrays.fill(translation, 0);

      firstChannelToRescale = switch (module.rescaleChannels.getValue()) {
         case RescaleModule.RESCALE_ALL -> 1;
         case RescaleModule.RESCALE_HALF -> channelsIn / 2 + 1;
         case RescaleModule.RESCALE_LAST -> Math.max(1, channelsIn);
         case RescaleModule.RESCALE_NONE -> Integer.MAX_VALUE;
         default -> {
            Log.global.warning("Unknown rescale mode: " + module.rescaleChannels.getValue());
            yield Integer.MAX_VALUE;
         }
      };

      for (int channel = firstChannelToRescale; channel <= channelsIn; channel++) {
         RawFileTransducer transducer = rawFileConfiguration.getTransducers().get(channel - 1);
         transducer.setChannelId(transducer.getChannelId() + " (rescaled)");
      }

      initializeRescaling();

      setNewPingConfiguration(newPingConfiguration);
   }

   private boolean shallRescale(PowerData powerData) {
      return powerData.getChannel() >= firstChannelToRescale;
   }

   private void rescale(PowerData powerData) {
      if (!shallRescale(powerData)) {
         return;
      }
      float[] logSv = powerData.getLogSv();
      float s = scaling[powerData.getChannel()];
      float t = translation[powerData.getChannel()];
      for (int i = 0; i < logSv.length; i++) {
         logSv[i] = s * logSv[i] + t;
      }
      if (MathUtils.avoidInfinities(logSv)) {
         Log.global.warning("RescaleModule: Clamped infinities in result.");
      }
   }

   private void initializeRescalingChannel(List<float[]> buffer, int channel) {
      int sampleCount = 0;
      for (float[] array : buffer) {
         sampleCount += array.length;
      }

      float minVal;
      float maxVal;
      if (sampleCount == 0) {
         Log.global.info("Found no samples for channel " + channel);
         minVal = 0;
         maxVal = 0;
      } else {
         float[] samples = new float[sampleCount];
         int sampleIndex = 0;
         for (float[] array : buffer) {
            System.arraycopy(array, 0, samples, sampleIndex, array.length);
            sampleIndex += array.length;
         }
         float q = module.minMaxFraction.getFloatValue();
         minVal = Quantile.quickSelect(samples, q);
         maxVal = Quantile.quickSelect(samples, 1 - q);
      }

      if (Math.abs(maxVal - minVal) <= Float.MIN_VALUE) {
         scaling[channel] = 1;
         translation[channel] = (module.desiredMaximum.getFloatValue() + module.desiredMinimum.getFloatValue()) / 2 - minVal;
      } else {
         scaling[channel] = (module.desiredMaximum.getFloatValue() - module.desiredMinimum.getFloatValue()) / (maxVal - minVal);
         translation[channel] = module.desiredMinimum.getFloatValue() - minVal * scaling[channel];
      }

      Log.global.info("Rescaling for channel " + channel
            + ": " + scaling[channel] + " * x + " + translation[channel]);
   }

   private void initializeRescaling() throws IOException {
      List<List<float[]>> rawBuffer = new ArrayList<>();

      rawBuffer.add(List.of());
      for (int i = 0; i < channelsIn; i++) {
         rawBuffer.add(new ArrayList<>(module.initializationDatagramCount.getIntValue()));
      }

      for (int channelsToBufferUp = channelsIn, peekIndex = 0;
           channelsToBufferUp > 0 && peekIndex < module.initializationMaxPingCount.getIntValue();
           peekIndex++) {

         Ping ping = peekPingSourcePing(peekIndex);
         if (ping == null) {
            break;
         }
         for (PowerData powerData : ping.getNonNullPowerDatas().toList()) {
            int channel = powerData.getChannel();
            rawBuffer.get(channel).add(powerData.getLogSv());
            if (rawBuffer.get(channel).size() >= module.initializationDatagramCount.getIntValue()) {
               --channelsToBufferUp;
            }
         }
      }

      for (int channel = 1; channel < rawBuffer.size(); channel++) {
         if (channel >= firstChannelToRescale) {
            initializeRescalingChannel(rawBuffer.get(channel), channel);
         }
      }
   }

   @Override
   protected void convertPing(Ping ping, Ping newPing) {
      newPing.addAll(ping.getPingItems());
   }

   @Override
   protected void processPing(Ping ping) {
      ping.getNonNullPowerDatas().forEach(this::rescale);
   }
}
