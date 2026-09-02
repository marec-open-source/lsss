package no.imr.korona.data.ping.items;

import java.time.Instant;

public abstract class AbstractPingItem implements PingItem {
   private Instant instant;

   protected AbstractPingItem(Instant instant) {
      this.instant = instant;
   }

   @Override
   public Instant getInstant() {
      return instant;
   }

   @Override
   public void setInstant(Instant instant) {
      this.instant = instant;
   }
}
