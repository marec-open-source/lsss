package no.imr.korona.data.ping;

import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * The default implementation of a {@link PingIndex}.
 */
public class DefaultPingIndex extends AbstractPingIndex {
   private Instant instant;
   private long pingNumber;
   private double vesselDistance;
   private @Nullable GeoPoint geographicalPosition;

   public DefaultPingIndex(Instant instant, long pingNumber, double vesselDistance, @Nullable GeoPoint geographicalPosition) {
      this.instant = instant;
      this.pingNumber = pingNumber;
      this.vesselDistance = vesselDistance;
      this.geographicalPosition = geographicalPosition;
   }

   public DefaultPingIndex(PingIndex pingIndex) {
      this(pingIndex.getInstant(), pingIndex.getPingNumber(), pingIndex.getVesselDistance(), pingIndex.getGeographicalPosition());
   }

   @Override
   public long getPingNumber() {
      return pingNumber;
   }

   @Override
   public void setPingNumber(long pingNumber) {
      this.pingNumber = pingNumber;
   }

   @Override
   public double getVesselDistance() {
      return vesselDistance;
   }

   @Override
   public void setVesselDistance(double vesselDistance) {
      this.vesselDistance = vesselDistance;
   }

   @Override
   public Instant getInstant() {
      return instant;
   }

   public void setInstant(Instant instant) {
      this.instant = instant;
   }

   @Override
   public @Nullable GeoPoint getGeographicalPosition() {
      return geographicalPosition;
   }

   @Override
   public void setGeographicalPosition(@Nullable GeoPoint geographicalPosition) {
      this.geographicalPosition = geographicalPosition;
   }
}
