package no.imr.korona.computation.filters;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * Creates missing ping data.
 * Pings with missing PowerData will after passing through this module have same values on the missing
 * PowerData as the previous ping had.
 * <p>
 * This module is used in front of other modules that don't handle missing pings in the middle of a stream.
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
            for (int channel = 1; channel <= transducerCount; channel++) {
               PowerData powerData = ping.getPowerData(channel);
               PowerData prevPowerData = prevPing.getPowerData(channel);
               if (isMissing(powerData) && !isMissing(prevPowerData)) {
                  if (powerData != null) {
                     ping.remove(powerData);
                  }
                  ping.add(copy(prevPowerData, ping.getNTDate()));
               }
            }
         }
         Ping resultPing = prevPing;
         prevPing = ping;
         return resultPing;
      }
   }

   private static boolean isMissing(@Nullable PowerData powerData) {
      return powerData == null || powerData.getCount() == 0;
   }

   public static PowerData copy(PowerData powerData, long ntDate) {
      PowerData copy = powerData.makeCopyWithAllData();
      copy.setNTDate(ntDate);
      return copy;
   }
}
