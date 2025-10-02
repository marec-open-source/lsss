package no.imr.korona.data.ping;

import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

/**
 * The default implementation of a {@link PingIndex}.
 */
public class DefaultPingIndex extends AbstractPingIndex {
   private long ntDate;
   private long pingNumber;
   private double vesselDistance;
   private @Nullable GeoPoint geographicalPosition;

   public DefaultPingIndex() {
   }

   public DefaultPingIndex(long ntDate, long pingNumber, double vesselDistance, @Nullable GeoPoint geographicalPosition) {
      this.ntDate = ntDate;
      this.pingNumber = pingNumber;
      this.vesselDistance = vesselDistance;
      this.geographicalPosition = geographicalPosition;
   }

   public DefaultPingIndex(PingIndex pingIndex) {
      this(pingIndex.getNTDate(), pingIndex.getPingNumber(), pingIndex.getVesselDistance(), pingIndex.getGeographicalPosition());
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
   public long getNTDate() {
      return ntDate;
   }

   protected void setNTDate(long ntDate) {
      this.ntDate = ntDate;
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
