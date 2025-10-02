package no.imr.korona.data.datagrams;

import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.ping.items.PingItem;

import java.util.List;

/**
 * A datagram that is also a ping item.
 */
public abstract class DatagramPingItem extends BaseDatagram implements PingItem {
   protected DatagramPingItem(long ntDate) {
      super(ntDate);
   }

   @Override
   public void addPingItems(PingConversion pingConversion) {
      pingConversion.addPingItem(this);
   }

   @Override
   public List<BaseDatagram> toDatagrams() {
      return List.of(this);
   }

   @Override
   public DatagramPingItem makeCopy() {
      return (DatagramPingItem) super.makeCopy();
   }
}
