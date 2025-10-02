package no.imr.korona.computation.categorization;

import no.imr.korona.computation.categorization.apriori.PerPingAPriori;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import org.jspecify.annotations.Nullable;

/**
 * ExtendedPings are pulled through the chain of CategorizationSubModules.
 * An ExtendedPing contains a Ping and a CategorizationPing if that Ping
 * contains PowerData. The CategorizationPing is used for categorization calculations
 * and is discarded in the end when the categorization datagram is created
 * and added to the ping.
 */
final class ExtendedPing {
   private final Ping ping;
   private final @Nullable CategorizationPing categorizationPing;

   ExtendedPing(Ping ping, Configurator configurator) {
      this.ping = ping;
      PowerData referenceDatagram = ping.getPowerData(configurator.getReferenceChannel());
      if (referenceDatagram != null) {
         PerPingAPriori perPingAPriori = new PerPingAPriori(configurator, ping);
         categorizationPing = new CategorizationPing(referenceDatagram, perPingAPriori);
      } else {
         categorizationPing = null;
      }
   }

   Ping getPing() {
      return ping;
   }

   @Nullable CategorizationPing getCategorizationPing() {
      return categorizationPing;
   }
}
