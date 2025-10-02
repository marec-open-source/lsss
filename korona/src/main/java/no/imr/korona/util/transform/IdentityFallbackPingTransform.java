package no.imr.korona.util.transform;

import no.imr.korona.data.ping.Ping;
import no.imr.tools.math.linalg.Matrix3;
import no.marec.lsss.api.util.GeoPoint;

public final class IdentityFallbackPingTransform extends PingTransform {
   public IdentityFallbackPingTransform(GeoPoint referenceGeoPos) {
      super(referenceGeoPos);
   }

   public IdentityFallbackPingTransform(Ping ping) {
      super(ping);
   }

   @Override
   protected Matrix3 fallbackHeadingCalculation(Ping ping) {
      return Matrix3.IDENTITY;
   }
}
