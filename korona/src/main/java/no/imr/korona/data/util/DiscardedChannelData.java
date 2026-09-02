package no.imr.korona.data.util;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.PerChannelDatagram;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.channel.ChannelData;

import java.time.Instant;
import java.util.List;

public final class DiscardedChannelData implements PingItem, PerChannelDatagram {
   private final ChannelData channelData;

   DiscardedChannelData(ChannelData channelData) {
      this.channelData = channelData;
   }

   @Override
   public Instant getInstant() {
      return channelData.getInstant();
   }

   @Override
   public void setInstant(Instant instant) {
      channelData.setInstant(instant);
   }

   @Override
   public int getChannel() {
      return channelData.getChannel();
   }

   @Override
   public void setChannel(int channel) {
      channelData.setChannel(channel);
   }

   public ChannelData getChannelData() {
      return channelData;
   }

   @Override
   public List<BaseDatagram> toDatagrams() {
      return List.of();
   }

   @Override
   public PingItem makeCopy() {
      return new DiscardedChannelData(channelData.makeCopy());
   }
}
