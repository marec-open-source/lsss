package no.imr.korona.data.ping.items;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.ping.PingConfiguration;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * An item in a ping.
 */
public interface PingItem {
   List<BaseDatagram> toDatagrams();

   Instant getInstant();

   void setInstant(Instant instant);

   /**
    * Test if compatible with another ping configuration.
    *
    * @param pingConfiguration another ping configuration
    * @return {@code null} if compatible, or a reason for why not
    */
   default @Nullable String getIncompatibility(PingConfiguration pingConfiguration) {
      return null;
   }

   PingItem makeCopy();

   default void setPingConfiguration(PingConfiguration pingConfiguration) {
   }

   @SuppressWarnings("unchecked")
   static <T extends PingItem> T copy(T pingItem) {
      return (T) pingItem.makeCopy();
   }
}
