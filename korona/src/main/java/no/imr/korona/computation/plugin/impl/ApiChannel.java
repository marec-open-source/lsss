package no.imr.korona.computation.plugin.impl;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.marec.api.korona.Channel;

public abstract class ApiChannel implements Channel {
   ApiChannel() {
   }

   public static ApiChannel create(Ping ping, int channel) {
      PowerData powerData = ping.getPowerData(channel);
      if (powerData != null) {
         return new NormalChannel(ping, powerData);
      } else {
         return new NullChannel(ping, channel);
      }
   }

   abstract void applyValidArray();
}
