package no.imr.tools.geo;

import no.imr.tools.Utils;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public final class GeoZoom {
   private static final double MIN_LATITUDE_EXTENT = 1e4 * Math.ulp(180.0);

   public final GeoPoint geoCenter;
   public final double latitudeExtent;
   public final int width;
   public final int height;
   public final GeoTransform geoTransform;

   public GeoZoom() {
      this(new GeoPoint(0, 0), 180, 1, 1);
   }

   private GeoZoom(GeoPoint geoCenter, double latitudeExtent, int width, int height) {
      this.geoCenter = geoCenter;
      this.latitudeExtent = latitudeExtent;
      this.width = width;
      this.height = height;
      geoTransform = new GeoTransform(getGeoRect(), width, height);
   }

   @Override
   public String toString() {
      return "Center: " + geoCenter + ", Extent: " + latitudeExtent + ", Size: " + width + "x" + height;
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof GeoZoom that
            && geoCenter.equals(that.geoCenter)
            && Double.doubleToLongBits(latitudeExtent) == Double.doubleToLongBits(that.latitudeExtent)
            && width == that.width
            && height == that.height;
   }

   @Override
   public int hashCode() {
      int result = geoCenter.hashCode();
      result = 31 * result + Double.hashCode(latitudeExtent);
      result = 31 * result + width;
      result = 31 * result + height;
      return result;
   }

   public boolean sameGeoCenterAndLatitudeExtent(GeoZoom geoZoom) {
      return geoCenter.equals(geoZoom.geoCenter) && latitudeExtent == geoZoom.latitudeExtent;
   }

   public GeoZoom withSize(int newWidth, int newHeight) {
      int w = Math.max(newWidth, 1);
      int h = Math.max(newHeight, 1);
      return new GeoZoom(geoCenter, latitudeExtent, w, h)
            .withGeoCenterAndLatitudeExtent(geoCenter, latitudeExtent);
   }

   public GeoZoom withGeoCenterAndLatitudeExtent(GeoPoint newGeoCenter, double newLatitudeExtent) {
      newLatitudeExtent = Math.clamp(newLatitudeExtent, MIN_LATITUDE_EXTENT, Math.max(180, 360 / getGeoAspectRatio(newGeoCenter.getY())));
      double x = Utils.mod(newGeoCenter.getX() + 180, 360) - 180;
      double maxY = Math.max(0, 90 - newLatitudeExtent / 2);
      double y = Math.clamp(newGeoCenter.getY(), -maxY, maxY);

      if (latitudeExtent != newLatitudeExtent || geoCenter.getX() != x || geoCenter.getY() != y) {
         return new GeoZoom(new GeoPoint(x, y), newLatitudeExtent, width, height);
      }

      return this;
   }

   public GeoZoom withGeoPointAtPixPoint(GeoPoint geoPoint, Point2D pixPoint) {
      GeoPoint actualGeoPoint = geoTransform.pixToGeo(pixPoint);
      double dLon = geoPoint.getLongitude() - actualGeoPoint.getLongitude();
      double dLat = geoPoint.getLatitude() - actualGeoPoint.getLatitude();
      GeoPoint newGeoCenter = new GeoPoint(geoCenter.getLongitude() + dLon, geoCenter.getLatitude() + dLat);
      return withGeoCenterAndLatitudeExtent(newGeoCenter, latitudeExtent);
   }

   public GeoZoom withContainingGeoRect(Rectangle2D geoRect, double factor) {
      double y = geoRect.getCenterY();
      double ySize = geoRect.getHeight() * factor;
      double minY = Math.clamp(y - ySize / 2, -90, 90);
      double maxY = Math.clamp(y + ySize / 2, -90, 90);
      GeoPoint newGeoCenter = new GeoPoint(geoRect.getCenterX(), (minY + maxY) / 2);
      double xSize = Math.min(geoRect.getWidth() * factor, 360);
      double newLatitudeExtent = Math.max(maxY - minY, xSize / getGeoAspectRatio(newGeoCenter.getLatitude()));
      return withGeoCenterAndLatitudeExtent(newGeoCenter, newLatitudeExtent);
   }

   public GeoZoom zoomAroundPixPos(Point2D pixPoint, double zoomFactor) {
      GeoPoint zoomGeoCenter = geoTransform.pixToGeo(pixPoint);
      double newLatitudeExtent = latitudeExtent * zoomFactor;
      double y = zoomGeoCenter.getY() - (0.5 - pixPoint.getY() / height) * newLatitudeExtent;
      double x = zoomGeoCenter.getX() - (pixPoint.getX() / width - 0.5) * getGeoAspectRatio(y) * newLatitudeExtent;
      GeoPoint geoCenter = new GeoPoint(x, y);
      return withGeoCenterAndLatitudeExtent(geoCenter, newLatitudeExtent);
   }

   public double getGeoAspectRatio(double latitude) {
      double ratio = (double) width / (double) height;
      double maxLatitude = 85; // avoids extreme values.
      double clampedLatitude = Math.clamp(latitude, -maxLatitude, maxLatitude);
      return ratio / Math.cos(Math.toRadians(clampedLatitude));
   }

   public Rectangle2D getGeoRect() {
      double w = getLongitudeExtent();
      double h = latitudeExtent;
      return new Rectangle2D.Double(geoCenter.getX() - w / 2, geoCenter.getY() - h / 2, w, h);
   }

   public double getLongitudeExtent() {
      return getGeoAspectRatio(geoCenter.getY()) * latitudeExtent;
   }

   public double getPixelsPerMeter() {
      GeoPoint metersPerGeoDegree = Earth.getMetersPerGeoDegree(geoCenter);
      double meters = metersPerGeoDegree.getLatitude() * latitudeExtent;
      return height / meters;
   }
}
