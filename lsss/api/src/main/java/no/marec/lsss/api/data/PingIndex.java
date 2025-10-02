package no.marec.lsss.api.data;

import no.marec.lsss.api.DoNotImplement;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * An index into a {@link PingDataset} for a {@link Ping}.
 */
@DoNotImplement
public interface PingIndex {
   /**
    * {@return the ping number of this ping}
    */
   long getPingNumber();

   /**
    * {@return the vessel distance in nautical miles of this ping}
    */
   double getVesselDistance();

   /**
    * {@return the time of this ping}
    */
   Instant getInstant();

   /**
    * Returns the geographical coordinates of this ping.
    *
    * @return geographical coordinates, or {@code null} if not available
    */
   @Nullable GeoPoint getGeographicalPosition();
}
