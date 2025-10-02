package no.imr.korona.computation.noise;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.datagrams.Nqp0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.util.TvgArray;
import no.imr.korona.incubator.KoronaIncubatorFeatureToggles;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

/**
 * Removes noise based on parameters in noise quantification datagrams.
 */
public final class NoiseRemoverModule extends GeneralPingModule {
   public final BooleanParameter removeNoiseFromStart = new BooleanParameter(
         new Name("RemoveNoiseFromStart", "Remove noise from start"),
         true,
         "Search for noise parameter datagram and use it for the first datagrams");

   public final IntParameter maxBufferSize = new IntParameter(
         new Name("MaxBufferSize", "Max buffer size"),
         10, Unit.COUNT, ValueConstraints.gte(0),
         "Maximum number of pings in buffer when searching for first noise parameter datagram");

   public final OptionalFloatParameter sampleSizeCorrection = new OptionalFloatParameter(
         new Name("SampleSizeCorrection", "Sample size correction (n)"),
         Optional.empty(), Unit.NONE, ValueConstraints.gt(0f),
         "<html>Incubating feature: Corrected N<sub>H</sub> = N<sub>E</sub> + (N<sub>H</sub> - N<sub>E</sub>) * sqrt(n)");

   public NoiseRemoverModule() {
      removeNoiseFromStart.addListenerAndNotify(maxBufferSize::setEnabled);

      sampleSizeCorrection.setVisible(KoronaIncubatorFeatureToggles.NOISE_NH_CORRECTION);
      sampleSizeCorrection.setPersistable(KoronaIncubatorFeatureToggles.NOISE_NH_CORRECTION);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            removeNoiseFromStart,
            maxBufferSize,
            sampleSizeCorrection
      );
   }

   @Override
   public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new NoiseRemoverModuleComputation(this, computationContext, pingSource);
   }

   private static final class NoiseRemoverModuleComputation extends GeneralPingModuleComputation {
      private final NoiseRemoverModule module;
      private final Queue<Ping> outputQueue = new ArrayDeque<>();
      private final int channelCount;
      private final float[] channelToNe;
      private final float[] channelToNh;
      private boolean firstTime = true;

      private NoiseRemoverModuleComputation(NoiseRemoverModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);

         this.module = module;
         PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
         PingConfiguration newPingConfiguration = pingConfiguration.createCopy();
         RawFileConfiguration rawFileConfiguration = newPingConfiguration.getRawFileConfiguration();

         channelCount = rawFileConfiguration.getTransducerCount();

         for (int channel = 1; channel <= channelCount; channel++) {
            RawFileTransducer transducer = rawFileConfiguration.getTransducers().get(channel - 1);
            transducer.setChannelId(transducer.getChannelId() + " Noise removed");
         }

         channelToNe = new float[channelCount + 1];
         channelToNh = new float[channelCount + 1];

         setNewPingConfiguration(newPingConfiguration);
      }

      private static void removeNoise(PowerData powerData, double ne, double nh, Optional<Float> sampleSizeCorrection) {
         float[] sv = powerData.getSv();
         TvgArray tvg = powerData.getTVGArray();

         if (sampleSizeCorrection.isPresent()) {
            nh = ne + (nh - ne) * Math.sqrt(sampleSizeCorrection.get());
         }

         for (int i = 0; i < sv.length; i++) {
            // Doing intermediate calculations in double precision.
            // This avoids infinity if sv is very large and tvg < 1.
            double tvgValue = tvg.get(i);
            double noise = sv[i] / tvgValue;
            if (noise >= nh) {
               sv[i] = (float) ((noise - ne) * tvgValue);
            } else {
               sv[i] = 0;
            }
         }

         powerData.setSv(sv);
      }

      private boolean isValidChannel(int channel) {
         return channel >= 1 && channel <= channelCount;
      }

      private void removeNoiseFromStart() throws IOException {
         List<Ping> buffer = new ArrayList<>();
         boolean[] initialized = new boolean[channelToNh.length];
         int uninitializedCount = initialized.length - 1;
         while (buffer.size() < module.maxBufferSize.getIntValue()
               && uninitializedCount > 0) {
            Ping ping = inputPing();
            if (ping == null) {
               break;
            }
            buffer.add(ping);
            for (Nqp0Datagram nqp : ping.getPingItems(Nqp0Datagram.class).toList()) {
               int channel = nqp.getChannel();
               if (isValidChannel(channel) && !initialized[channel]) {
                  channelToNe[channel] = nqp.getAverage();
                  channelToNh[channel] = nqp.getUpperLimit();
                  uninitializedCount--;
                  initialized[channel] = true;
               }
            }
         }

         if (uninitializedCount > 0 && uninitializedCount < initialized.length - 1) {
            Log.global.info("Could not remove noise from start.");
         }

         for (Ping ping : buffer) {
            processPing(ping);
            outputQueue.add(ping);
         }
      }

      private void initAfterConfigure() throws IOException {
         if (module.removeNoiseFromStart.getBooleanValue()) {
            removeNoiseFromStart();
         }
      }

      @Override
      protected void convertPing(Ping ping, Ping newPing) {
         newPing.addAll(ping.getPingItems());
      }

      @Override
      protected @Nullable Ping generateOutput() throws IOException {
         if (firstTime) {
            firstTime = false;
            initAfterConfigure();
         }
         if (!outputQueue.isEmpty()) {
            return outputQueue.remove();
         }
         Ping ping = inputPing();
         if (ping != null) {
            processPing(ping);
         }
         return ping;
      }

      private void processPing(Ping ping) {
         ping.getPingItems(Nqp0Datagram.class).forEach(nqp -> {
            int channel = nqp.getChannel();
            if (isValidChannel(channel)) {
               channelToNe[channel] = nqp.getAverage();
               channelToNh[channel] = nqp.getUpperLimit();
            }
         });

         ping.getNonNullPowerDatas().forEach(powerData -> {
            int channel = powerData.getChannel();
            removeNoise(powerData, channelToNe[channel], channelToNh[channel], module.sampleSizeCorrection.getValue());
         });
      }
   }
}
