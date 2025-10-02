package no.imr.korona.data.ping;

import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

/**
 * The empty ping index is used by {@link PingRange#EMPTY_RANGE}.
 */
public final class EmptyPingIndex extends AbstractPingIndex {
   EmptyPingIndex() {
   }

   @Override
   public long getNTDate() {
      return -1;
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
