package no.imr.korona.data.ping;

import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * PingIndex defined by extrapolation.
 */
public final class ExtrapolatedPingIndex extends DefaultPingIndex {
   /**
    * Creates a new PingIndex by constant extrapolation.
    *
    * @param pingIndex the ping index to extrapolate from
    */
   public ExtrapolatedPingIndex(PingIndex pingIndex) {
      double seconds = 1.0;
      double hours = seconds / 3600;
      double knots = 11;

      super(
            pingIndex.getInstant().plusNanos((long) (seconds * 1e9)),
            pingIndex.getPingNumber() + 1,
            pingIndex.getVesselDistance() + hours * knots,
            pingIndex.getGeographicalPosition() // use same location
      );
   }

   /**
    * Creates a new PingIndex by linear extrapolation.
    * <pre>
    * second + (second - first)
    * </pre>
    *
    * @param first  the first PingIndex
    * @param second the second PingIndex
    */
   public ExtrapolatedPingIndex(PingIndex first, PingIndex second) {
      // Avoid overflow by using the form b + (b - a) instead of 2*b - a.
      super(
            second.getInstant().plus(first.getInstant().until(second.getInstant())),
            second.getPingNumber() + (second.getPingNumber() - first.getPingNumber()),
            second.getVesselDistance() + (second.getVesselDistance() - first.getVesselDistance()),
            extrapolateGeoPos(first.getGeographicalPosition(), second.getGeographicalPosition())
      );
   }

   private static @Nullable GeoPoint extrapolateGeoPos(@Nullable GeoPoint first, @Nullable GeoPoint second) {
      if (first != null && second != null) {
         return new GeoPoint(
               second.getX() + (second.getX() - first.getX()),
               second.getY() + (second.getY() - first.getY())
         );
      } else {
         return null;
      }
   }

   /**
    * Creates a new PingIndex by using the last elements of a list.
    *
    * @param pingIndices a list
    * @return an extrapolated ping index
    * @throws IllegalArgumentException if the list is empty
    */
   public static ExtrapolatedPingIndex create(List<? extends PingIndex> pingIndices) {
      if (pingIndices.isEmpty()) {
         throw new IllegalArgumentException();
      }
      int n = pingIndices.size();
      if (n == 1) {
         return new ExtrapolatedPingIndex(pingIndices.getLast());
      } else {
         return new ExtrapolatedPingIndex(pingIndices.get(n - 2), pingIndices.get(n - 1));
      }
   }
}
