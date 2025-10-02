package no.imr.tools.geo;

import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Rectangle2D;

public final class GeoBoxBuilder {
   private Rectangle2D.@Nullable Double result;

   public GeoBoxBuilder() {
   }

   public void add(GeoPoint geoPoint) {
      add(geoPoint.getLongitude(), geoPoint.getLatitude());
   }

   public void add(double longitude, double latitude) {
      if (result == null) {
         result = new Rectangle2D.Double(longitude, latitude, 0, 0);
      } else {
         result.add(longitude, latitude);
      }
   }

   public void add(Rectangle2D geoBox) {
      if (result == null) {
         result = new Rectangle2D.Double();
         result.setRect(geoBox);
      } else {
         result.add(geoBox);
      }
   }

   public @Nullable Rectangle2D build() {
      return result;
   }
}
