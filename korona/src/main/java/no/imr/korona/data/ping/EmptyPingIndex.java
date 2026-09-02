package no.imr.korona.data.ping;

import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * The empty ping index is used by {@link PingRange#EMPTY_RANGE}.
 */
public final class EmptyPingIndex extends AbstractPingIndex {
   public static final EmptyPingIndex INSTANCE = new EmptyPingIndex();

   private EmptyPingIndex() {
   }

   @Override
   public Instant getInstant() {
      return Instant.EPOCH;
   }

   @Override
   public long getPingNumber() {
      return -1;
   }

   @Override
   public void setPingNumber(long pingNumber) {
      throw new UnsupportedOperationException();
   }

   @Override
   public double getVesselDistance() {
      return -1;
   }

   @Override
   public void setVesselDistance(double vesselDistance) {
      throw new UnsupportedOperationException();
   }

   @Override
   public @Nullable GeoPoint getGeographicalPosition() {
      return null;
   }

   @Override
   public void setGeographicalPosition(@Nullable GeoPoint geographicalPosition) {
      throw new UnsupportedOperationException();
   }
}
