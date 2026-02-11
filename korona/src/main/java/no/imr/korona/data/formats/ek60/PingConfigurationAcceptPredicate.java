package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.PerChannelDatagram;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.tools.logging.LogOnce;

import java.util.function.Predicate;

public final class PingConfigurationAcceptPredicate implements Predicate<BaseDatagram> {
   private final PingConfiguration pingConfiguration;

   public PingConfigurationAcceptPredicate(PingConfiguration pingConfiguration) {
      this.pingConfiguration = pingConfiguration;
   }

   @Override
   public boolean test(BaseDatagram datagram) {
      if (datagram instanceof PerChannelDatagram perChannelDatagram) {
         int channel = perChannelDatagram.getChannel();
         if (channel <= 0 || channel > pingConfiguration.getRawFileConfiguration().getTransducerCount()) {
            // Do not include channel number in warning message to avoid excessive logging, se #1567.
            LogOnce.warning("Rejecting " + datagram.getDatagramType().getAsciiQuad()
                  + " datagram with invalid channel number in file "
                  + pingConfiguration.getRawFileConfiguration().getDataFile(), pingConfiguration);
            return false;
         }
      }
      return true;
   }
}
