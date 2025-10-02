package no.imr.korona.computation.misc;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.ping.items.AbstractPingItem;
import no.imr.korona.data.ping.items.PingItem;

import java.util.List;
import java.util.stream.Collectors;

public final class TemporaryComputationsConfigurationItem extends AbstractPingItem {
   final List<PingItem> configurationItems;

   TemporaryComputationsConfigurationItem(long ntDate, List<PingItem> configurationItems) {
      super(ntDate);

      this.configurationItems = configurationItems;
   }

   public void add(PingItem configurationItem) {
      configurationItems.add(configurationItem);
   }

   @Override
   public List<BaseDatagram> toDatagrams() {
      return List.of();
   }

   @Override
   public PingItem makeCopy() {
      List<PingItem> copiedConfigurationItems = configurationItems.stream()
            .map(PingItem::makeCopy)
            .collect(Collectors.toList());
      return new TemporaryComputationsConfigurationItem(getNTDate(), copiedConfigurationItems);
   }
}
