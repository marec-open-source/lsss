package no.imr.korona.data.ping.items;

import no.imr.korona.data.datagrams.BaseDatagram;

import java.time.Instant;
import java.util.List;

public final class PassivePingItem implements PingItem {
   private final BaseDatagram datagram;

   public PassivePingItem(BaseDatagram datagram) {
      this.datagram = datagram;
   }

   @Override
   public Instant getInstant() {
      return datagram.getInstant();
   }

   @Override
   public void setInstant(Instant instant) {
      datagram.setInstant(instant);
   }

   @Override
   public List<BaseDatagram> toDatagrams() {
      return List.of(datagram);
   }

   @Override
   public PingItem makeCopy() {
      return new PassivePingItem(datagram.makeCopy());
   }
}
