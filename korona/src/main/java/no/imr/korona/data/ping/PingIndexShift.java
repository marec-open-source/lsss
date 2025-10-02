package no.imr.korona.data.ping;

import java.util.Collection;

/**
 * Represents how a {@link PingIndex} should be adjusted.
 */
public record PingIndexShift(long pingNumberShift, double vesselDistanceShift) {

   public static final PingIndexShift NO_SHIFT = new PingIndexShift(0, 0);

   public static PingIndexShift create(long pingNumberShift, double vesselDistanceShift) {
      if (pingNumberShift == 0 && vesselDistanceShift == 0) {
         return NO_SHIFT;
      }
      return new PingIndexShift(pingNumberShift, vesselDistanceShift);
   }

   @Override
   public String toString() {
      return "{" + pingNumberShift + ", " + vesselDistanceShift + "}";
   }

   public void apply(PingIndex pingIndex) {
      if (pingNumberShift == 0 && vesselDistanceShift == 0) {
         return;
      }
      pingIndex.setPingNumber(pingIndex.getPingNumber() + pingNumberShift);
      pingIndex.setVesselDistance(pingIndex.getVesselDistance() + vesselDistanceShift);
   }

   public void apply(Collection<? extends PingIndex> pingIndices) {
      if (pingNumberShift == 0 && vesselDistanceShift == 0) {
         return;
      }
      for (PingIndex pingIndex : pingIndices) {
         apply(pingIndex);
      }
   }

   public void apply(PingRange pingRange) {
      if (pingNumberShift == 0 && vesselDistanceShift == 0) {
         return;
      }
      apply(pingRange.begin());
      apply(pingRange.end());
   }
}
