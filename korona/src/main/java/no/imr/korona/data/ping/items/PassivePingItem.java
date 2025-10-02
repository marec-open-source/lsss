package no.imr.korona.data.ping.items;

import no.imr.korona.data.datagrams.BaseDatagram;

import java.util.List;

public final class PassivePingItem implements PingItem {
   private final BaseDatagram datagram;

   public PassivePingItem(BaseDatagram datagram) {
      this.datagram = datagram;
   }

   @Override
   public long getNTDate() {
      return datagram.getNTDate();
   }

   @Override
   public void setNTDate(long ntDate) {
      datagram.setNTDate(ntDate);
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
