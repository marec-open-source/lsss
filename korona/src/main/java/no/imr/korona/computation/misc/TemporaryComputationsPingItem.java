package no.imr.korona.computation.misc;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.items.AbstractPingItem;
import no.imr.korona.data.ping.items.PingItem;

import java.util.List;
import java.util.stream.Collectors;

public final class TemporaryComputationsPingItem extends AbstractPingItem {
   final Bot0Datagram bot0Datagram;
   final List<PingItem> pingItems;

   TemporaryComputationsPingItem(long ntDate, Bot0Datagram bot0Datagram, List<PingItem> pingItems) {
      super(ntDate);

      this.bot0Datagram = bot0Datagram;
      this.pingItems = pingItems;
   }

   @Override
   public List<BaseDatagram> toDatagrams() {
      return List.of();
   }

   @Override
   public PingItem makeCopy() {
      List<PingItem> copiedPingItems = pingItems.stream()
            .map(PingItem::makeCopy)
            .collect(Collectors.toList());
      return new TemporaryComputationsPingItem(getNTDate(), bot0Datagram.makeCopy(), copiedPingItems);
   }
}
