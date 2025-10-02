package no.imr.korona.data.ping;

import no.imr.tools.time.NTDate;
import no.marec.lsss.api.util.GeoPoint;

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

      setNTDate(pingIndex.getNTDate() + (long) (seconds * NTDate.UNITS_PER_SECOND));
      setPingNumber(pingIndex.getPingNumber() + 1);
      setVesselDistance(pingIndex.getVesselDistance() + hours * knots);
      setGeographicalPosition(pingIndex.getGeographicalPosition()); // use same location
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

      setNTDate(second.getNTDate() + (second.getNTDate() - first.getNTDate()));
      setPingNumber(second.getPingNumber() + (second.getPingNumber() - first.getPingNumber()));
      setVesselDistance(second.getVesselDistance() + (second.getVesselDistance() - first.getVesselDistance()));

      GeoPoint firstPos = first.getGeographicalPosition();
      GeoPoint secondPos = second.getGeographicalPosition();
      if (firstPos != null && secondPos != null) {
         double x = secondPos.getX() + (secondPos.getX() - firstPos.getX());
         double y = secondPos.getY() + (secondPos.getY() - firstPos.getY());
         setGeographicalPosition(new GeoPoint(x, y));
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
