package no.imr.korona.util.transform;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.util.DataUtils;
import no.imr.tools.math.linalg.Matrix3;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Point2D;

/**
 * Calculate heading through tangent if NMEA heading is not present.
 */
public final class PingTransformWithTangent extends PingTransform {
   private final DataFileSet dataFileSet;

   public PingTransformWithTangent(DataFileSet dataFileSet, @Nullable GeoPoint referenceGeoPos) {
      super(referenceGeoPos);

      this.dataFileSet = dataFileSet;
   }

   public PingTransformWithTangent(DataFileSet dataFileSet, Ping referencePing) {
      this(dataFileSet, referencePing.getPingIndex().getGeographicalPosition());
   }

   @Override
   protected Matrix3 fallbackHeadingCalculation(Ping ping) {
      Point2D tangent = DataUtils.getTangent(dataFileSet, getMetersPerGeoDegree(), ping.getPingIndex());
      return tangentToRotation(tangent);
   }

   static Matrix3 tangentToRotation(@Nullable Point2D tangent) {
      if (tangent == null) {
         return Matrix3.IDENTITY;
      } else {
         float x = (float) tangent.getX();
         float y = (float) tangent.getY();

         return new Matrix3(
               x, -y, 0,
               y, x, 0,
               0, 0, 1);
      }
   }
}
