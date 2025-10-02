package no.imr.korona.data.datagrams.subdatagrams;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.LsssDatagram;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.ping.items.PingItem;

import java.util.List;

public abstract class BaseSubDatagram extends SubDatagram implements PingItem {
   protected BaseSubDatagram(long ntDate) {
      super(ntDate);
   }

   @Override
   public void addPingItems(PingConversion pingConversion) {
      pingConversion.addPingItem(this);
   }

   @Override
   public List<BaseDatagram> toDatagrams() {
      return List.of(new LsssDatagram(this));
   }

   @Override
   public BaseSubDatagram makeCopy() {
      return (BaseSubDatagram) super.makeCopy();
   }
}
