package no.imr.korona.data.ping.items.channel;

public final class ChannelDataReadOnlyException extends RuntimeException {
   private final ChannelData channelData;

   ChannelDataReadOnlyException(ChannelData channelData) {
      this.channelData = channelData;
   }

   public ChannelData getChannelData() {
      return channelData;
   }
}
