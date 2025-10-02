package no.imr.korona.data.ping;

import no.imr.korona.data.datagrams.Bot0Datagram;

/**
 * The default ping implementations that stores a (strong) reference to the ping data.
 */
public final class DefaultPing extends Ping {
   private final PingIndex pingIndex;
   private final Bot0Datagram bot0Datagram;
   private final PingData pingData;

   public DefaultPing(PingConfiguration pingConfiguration, PingIndex pingIndex, Bot0Datagram bot0Datagram) {
      this(pingIndex, bot0Datagram, new PingData(pingConfiguration));
   }

   public DefaultPing(PingIndex pingIndex, Bot0Datagram bot0Datagram, PingData pingData) {
      this.pingIndex = pingIndex;
      this.bot0Datagram = bot0Datagram;
      this.pingData = pingData;
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return pingData.getPingConfiguration();
   }

   @Override
   public PingIndex getPingIndex() {
      return pingIndex;
   }

   @Override
   public Bot0Datagram getBot0Datagram() {
      return bot0Datagram;
   }

   @Override
   public PingData getPingData() {
      return pingData;
   }

   @Override
   public PingData getAvailablePingData() {
      return pingData;
   }
}
