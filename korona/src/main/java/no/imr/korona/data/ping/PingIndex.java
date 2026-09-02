package no.imr.korona.data.ping;

import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Interface for ping indices.
 */
public interface PingIndex extends PingMappingArgument, Comparable<PingIndex>, no.marec.lsss.api.data.PingIndex {
   /**
    * Returns the number of this ping.
    *
    * @return the ping number
    */
   @Override
   long getPingNumber();

   /**
    * Sets the number of this ping.
    *
    * @param pingNumber the new number
    */
   void setPingNumber(long pingNumber);

   /**
    * Returns the vessel distance for this ping.
    *
    * @return the vessel distance in nmi
    */
   @Override
   double getVesselDistance();

   /**
    * Sets the vessel distance for this ping.
    *
    * @param vesselDistance the new vessel distance in nmi
    */
   void setVesselDistance(double vesselDistance);

   @Override
   Instant getInstant();

   /**
    * Returns the latitude and longitude of this ping index.
    *
    * @return the geographical position, or {@code null} if not available
    */
   @Override
   @Nullable GeoPoint getGeographicalPosition();

   /**
    * Sets the latitude and longitude of this ping index.
    *
    * @param geographicalPosition the geographical position, or {@code null} if not available
    */
   void setGeographicalPosition(@Nullable GeoPoint geographicalPosition);

   @Override
   default int compareTo(PingIndex pingIndex) {
      return Long.compare(getPingNumber(), pingIndex.getPingNumber());
   }
}
