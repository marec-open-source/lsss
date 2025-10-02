package no.imr.korona.data.formats.missing;

import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.PingIndex;
import no.marec.lsss.api.util.GeoPoint;

/**
 * PingIndex defined by linear interpolation.
 */
public final class MissingPingIndex extends DefaultPingIndex {
   private MissingPingIndex(PingIndex a, PingIndex b, long pingNumber, double f) {
      setPingNumber(pingNumber);

      setNTDate(a.getNTDate() + Math.round(f * (b.getNTDate() - a.getNTDate())));
      setVesselDistance(a.getVesselDistance() + f * (b.getVesselDistance() - a.getVesselDistance()));

      GeoPoint aPos = a.getGeographicalPosition();
      GeoPoint bPos = b.getGeographicalPosition();
      if (aPos != null && bPos != null) {
         double x = aPos.getX() + f * (bPos.getX() - aPos.getX());
         double y = aPos.getY() + f * (bPos.getY() - aPos.getY());
         setGeographicalPosition(new GeoPoint(x, y));
      }
   }

   public static MissingPingIndex create(PingIndex a, PingIndex b, long pingNumber) {
      double f = (double) (pingNumber - a.getPingNumber()) / (double) (b.getPingNumber() - a.getPingNumber());
      return new MissingPingIndex(a, b, pingNumber, f);
   }

   public static MissingPingIndex create(PingIndex a, PingIndex b, long pingNumber, double fraction) {
      return new MissingPingIndex(a, b, pingNumber, fraction);
   }
}
