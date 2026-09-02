package no.imr.korona.computation.filters;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.ChannelData;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * Adds missing channel data.
 */
public final class FillMissingDataModule extends GeneralPingModule {
   public FillMissingDataModule() {
   }

   @Override
   public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new FillMissingDataModuleComputation(this, computationContext, pingSource);
   }

   private static final class FillMissingDataModuleComputation extends GeneralPingModuleComputation {
      private @Nullable Ping prevPing;

      private FillMissingDataModuleComputation(FillMissingDataModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);
      }

      @Override
      protected @Nullable Ping generateOutput() throws IOException {
         if (prevPing == null) {
            prevPing = inputPing();
         }
         Ping ping = inputPing();
         if (ping != null && prevPing != null) {
            int transducerCount = ping.getRawFileConfiguration().getTransducerCount();
            ChannelData heaveChannelData = ping.getFirstAvailableChannelData();
            for (int channel = 1; channel <= transducerCount; channel++) {
               ChannelData channelData = ping.getChannelData(channel);
               ChannelData prevChannelData = prevPing.getChannelData(channel);
               if (isMissing(channelData) && !isMissing(prevChannelData)) {
                  if (channelData != null) {
                     ping.remove(channelData);
                  }
                  ChannelData copy = prevChannelData.makeCopy();
                  copy.setInstant(ping.getInstant());
                  if (heaveChannelData != null) {
                     copy.setHeave(heaveChannelData.getHeave());
                     copy.setRoll(heaveChannelData.getRoll());
                     copy.setPitch(heaveChannelData.getPitch());
                     copy.setHeading(heaveChannelData.getHeading());
                  } else {
                     heaveChannelData = copy;
                  }
                  ping.add(copy);
               }
            }
         }
         Ping resultPing = prevPing;
         prevPing = ping;
         return resultPing;
      }
   }

   private static boolean isMissing(@Nullable ChannelData channelData) {
      return channelData == null || channelData.getCount() == 0;
   }
}
