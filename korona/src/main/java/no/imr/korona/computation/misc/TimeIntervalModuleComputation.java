package no.imr.korona.computation.misc;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.time.Instant;

final class TimeIntervalModuleComputation extends GeneralPingModuleComputation {
   private final Instant minTime;
   private final Instant maxTime;

   private final long minRelativePingNumber;
   private final long maxRelativePingNumber;

   private int relativePingNumber;
   private boolean ended;

   TimeIntervalModuleComputation(TimeIntervalModule module, ComputationContext computationContext, PingSource pingSource) {
      super(module, computationContext, pingSource);

      minTime = module.startDate.getValue().orElse(Instant.MIN);
      maxTime = module.endDate.getValue().orElse(Instant.MAX);

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
               || ping.getInstant().isAfter(maxTime)
               || relativePingNumber > maxRelativePingNumber
         ) {
            ended = true;
            return null;
         }
         if (!ping.getInstant().isBefore(minTime)
               && relativePingNumber >= minRelativePingNumber
         ) {
            return ping;
         }
      }
   }
}
