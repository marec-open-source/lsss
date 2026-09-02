package no.imr.korona.data.ping;

import java.time.Instant;

/**
 * Interface for classes that can be evaluated by a {@link PingMapping}.
 */
public interface PingMappingArgument {
   /**
    * Used by {@link PingMapping#NUMBER}.
    *
    * @return the ping number
    */
   long getPingNumber();

   /**
    * Used by {@link PingMapping#DISTANCE}.
    *
    * @return the vessel distance in nmi
    */
   double getVesselDistance();

   /**
    * Used by {@link PingMapping#TIME}.
    *
    * @return the time of this ping
    */
   Instant getInstant();
}
