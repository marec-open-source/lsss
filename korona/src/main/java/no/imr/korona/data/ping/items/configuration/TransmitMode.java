package no.imr.korona.data.ping.items.configuration;

import no.imr.korona.data.ping.items.channel.ChannelData;

public final class TransmitMode {
   public static final short UNKNOWN = -1;
   public static final short ACTIVE = 0;
   public static final short PASSIVE = 1;
   public static final short TEST = 2;

   private TransmitMode() {
   }

   public static String getTransmitModeString(ChannelData channelData) {
      return getTransmitModeString(channelData.getTransmitMode());
   }

   public static String getTransmitModeString(short transmitMode) {
      return switch (transmitMode) {
         case UNKNOWN -> "UNKNOWN";
         case ACTIVE -> "ACTIVE";
         case PASSIVE -> "PASSIVE";
         case TEST -> "TEST";
         default -> "";
      };
   }
}
