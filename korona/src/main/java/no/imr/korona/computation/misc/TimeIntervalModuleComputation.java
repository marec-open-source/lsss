package no.imr.korona.computation.misc;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

final class TimeIntervalModuleComputation extends GeneralPingModuleComputation {
   private final long minMillis;
   private final long maxMillis;

   private final long minRelativePingNumber;
   private final long maxRelativePingNumber;

   private int relativePingNumber;
   private boolean ended;

   TimeIntervalModuleComputation(TimeIntervalModule module, ComputationContext computationContext, PingSource pingSource) {
      super(module, computationContext, pingSource);

      minMillis = module.startDate.toMillisOrDefault(Long.MIN_VALUE);
      maxMillis = module.endDate.toMillisOrDefault(Long.MAX_VALUE);

      minRelativePingNumber = module.startRelativePingNumber.getValue().orElse(Integer.MIN_VALUE);
      maxRelativePingNumber = module.endRelativePingNumber.getValue().orElse(Integer.MAX_VALUE);
   }

   @Override
   protected @Nullable Ping generateOutput() throws IOException {
      if (ended) {
         return null;
      }
      while (true) {
         Ping ping = inputPing();
         relativePingNumber++;
         if (getAsyncHandle().isCancelled() || ping == null
               || ping.getTimeInMillis() > maxMillis
               || relativePingNumber > maxRelativePingNumber
         ) {
            ended = true;
            return null;
         }
         if (ping.getTimeInMillis() >= minMillis
               && relativePingNumber >= minRelativePingNumber
         ) {
            return ping;
         }
      }
   }
}
