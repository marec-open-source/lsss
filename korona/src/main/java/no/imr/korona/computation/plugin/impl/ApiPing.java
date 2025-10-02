package no.imr.korona.computation.plugin.impl;

import no.marec.api.korona.Channel;
import no.marec.api.korona.Ping;

import java.util.Arrays;
import java.util.Collection;

public final class ApiPing implements Ping {
   private final no.imr.korona.data.ping.Ping ping;
   private final ApiChannel[] channels;

   public ApiPing(no.imr.korona.data.ping.Ping ping) {
      this.ping = ping;
      channels = new ApiChannel[ping.getRawFileConfiguration().getTransducerCount()];
      for (int channel = 1; channel <= channels.length; channel++) {
         channels[channel - 1] = ApiChannel.create(ping, channel);
      }
   }

   public no.imr.korona.data.ping.Ping getPing() {
      return ping;
   }

   public void applyValidArray() {
      for (ApiChannel channel : channels) {
         channel.applyValidArray();
      }
   }

   @Override
   public Channel getChannel(int channelNumber) {
      return channels[channelNumber - 1];
   }

   @Override
   public Collection<Channel> getChannels() {
      return Arrays.asList(channels);
   }
}
