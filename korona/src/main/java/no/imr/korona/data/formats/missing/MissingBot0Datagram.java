package no.imr.korona.data.formats.missing;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;

/**
 * Represents a missing Bot0Datagram, with all depths set to 0.
 */
public final class MissingBot0Datagram extends Bot0Datagram {
   /**
    * Creates a new MissingBot0Datagram.
    *
    * @param rawFileConfiguration the corresponding configuration datagram
    * @param pingIndex            the corresponding PingIndex
    */
   public MissingBot0Datagram(RawFileConfiguration rawFileConfiguration, PingIndex pingIndex) {
      super(pingIndex.getInstant(), new double[rawFileConfiguration.getTransducerCount()]);
   }
}
