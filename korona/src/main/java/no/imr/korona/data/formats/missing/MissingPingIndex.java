package no.imr.korona.data.formats.missing;

import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.math.MathUtils;
import no.imr.tools.time.TimeUtils;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

/**
 * PingIndex defined by linear interpolation.
 */
public final class MissingPingIndex extends DefaultPingIndex {
   private MissingPingIndex(PingIndex a, PingIndex b, long pingNumber, double f) {
      super(
            TimeUtils.interpolateInstant(a.getInstant(), b.getInstant(), f),
            pingNumber,
            MathUtils.interpolate(a.getVesselDistance(), b.getVesselDistance(), f),
            interpolateGeoPos(a.getGeographicalPosition(), b.getGeographicalPosition(), f)
      );
   }

   private static @Nullable GeoPoint interpolateGeoPos(@Nullable GeoPoint a, @Nullable GeoPoint b, double f) {
      if (a != null && b != null) {
         return new GeoPoint(
               MathUtils.interpolate(a.getX(), b.getX(), f),
               MathUtils.interpolate(a.getY(), b.getY(), f)
         );
      } else {
         return null;
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
