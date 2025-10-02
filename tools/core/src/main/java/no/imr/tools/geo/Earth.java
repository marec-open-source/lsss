package no.imr.tools.geo;

import no.imr.tools.Utils;
import no.marec.lsss.api.util.GeoPoint;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Various utility functions.
 */
public final class Earth {
   public static final double EQUATORIAL_RADIUS = 6378137.0;
   public static final double POLAR_RADIUS = 6356752.3;

   private static final double A2 = EQUATORIAL_RADIUS * EQUATORIAL_RADIUS;
   private static final double A4 = A2 * A2;
   private static final double B2_A2 = POLAR_RADIUS * POLAR_RADIUS - A2;
   private static final double B4_A4 = POLAR_RADIUS * POLAR_RADIUS * POLAR_RADIUS * POLAR_RADIUS - A4;

   private Earth() {
   }

   /**
    * Calculate earth radius at a given latitude.
    * See <a href="http://en.wikipedia.org/wiki/Earth_radius#Radius_at_a_given_geodetic_latitude">http://en.wikipedia.org/wiki/Earth_radius#Radius_at_a_given_geodetic_latitude</a>
    *
    * @param latitudeDegrees latitude in degrees
    * @return the earth radius
    */
   public static double getRadius(double latitudeDegrees) {
      double sinLat = Math.sin(Math.toRadians(latitudeDegrees));
      double sinLat2 = sinLat * sinLat;
      return Math.sqrt((A4 + B4_A4 * sinLat2) / (A2 + B2_A2 * sinLat2));
   }

   /**
    * Calculates the approximate distance in meters between to geographical points.
    *
    * @param first  a geographical point
    * @param second another geographical point
    * @return the approximate distance in meters
    */
   public static double getApproximateDistance(GeoPoint first, GeoPoint second) {
      Point2D offset = getApproximateOffset(first, second);
      return Utils.hypot(offset.getX(), offset.getY());
   }

   /**
    * Calculates the approximate offset in meters between two geographical points.
    *
    * @param first  a geographical point
    * @param second another geographical point
    * @return the approximate offset in meters in longitude and latitude directions
    */
   public static Point2D getApproximateOffset(GeoPoint first, GeoPoint second) {
      double lat = (first.getLatitude() + second.getLatitude()) / 2;
      double r = getRadius(lat);
      double dx = r * Math.cos(Math.toRadians(lat)) * Math.toRadians(second.getX() - first.getX());
      double dy = r * Math.toRadians(second.getY() - first.getY());
      return new Point2D.Double(dx, dy);
   }

   /**
    * Calculates an approximate geographical point from a reference point and a delta offset in meters.
    *
    * @param referencePoint the reference point
    * @param deltaInMeters  an offset from the reference point in meters
    * @return a new geographical point representing the reference plus the offset
    */
   public static GeoPoint getApproximateGeoPoint(GeoPoint referencePoint, Point2D deltaInMeters) {
      double lat = referencePoint.getLatitude();
      double r = getRadius(lat);
      double dLon = Math.toDegrees(deltaInMeters.getX() / (r * Math.cos(Math.toRadians(lat))));
      double dLat = Math.toDegrees(deltaInMeters.getY() / r);
      return new GeoPoint(referencePoint.getX() + dLon, referencePoint.getY() + dLat);
   }

   public static GeoPoint getMetersPerGeoDegree(GeoPoint referencePoint) {
      return getMetersPerGeoDegree(referencePoint.getLatitude());
   }

   public static GeoPoint getMetersPerGeoDegree(double latitudeDegrees) {
      double r = getRadius(latitudeDegrees);
      double y = Math.toRadians(r);
      double x = y * Math.cos(Math.toRadians(latitudeDegrees));
      return new GeoPoint(x, y);
   }

   public static GeoPoint getGeoDegreesPerMeter(GeoPoint referencePoint) {
      return getGeoDegreesPerMeter(referencePoint.getLatitude());
   }

   public static GeoPoint getGeoDegreesPerMeter(double latitudeDegrees) {
      GeoPoint p = getMetersPerGeoDegree(latitudeDegrees);
      p.setLocation(1 / p.getX(), 1 / p.getY());
      return p;
   }

   public static void expand(Rectangle2D geoRect, double metersToExpand) {
      GeoPoint geoDegreesPerMeter = getGeoDegreesPerMeter(geoRect.getCenterY());
      double deltaLon = metersToExpand * geoDegreesPerMeter.getLongitude();
      double deltaLat = metersToExpand * geoDegreesPerMeter.getLatitude();
      geoRect.setFrame(geoRect.getX() - deltaLon, geoRect.getY() - deltaLat, geoRect.getWidth() + 2 * deltaLon, geoRect.getHeight() + 2 * deltaLat);
   }

   /**
    * Formats a geographical point for display.
    *
    * @param geoPoint the geographical point
    * @param format   a format string of type "%.1f"
    * @return a string with geographical coordinates
    */
   public static String formatGeoPoint(GeoPoint geoPoint, String format) {
      double lat = geoPoint.getLatitude();
      double lon = geoPoint.getLongitude();

      return Utils.format(format, Math.abs(lat)) + '°' + (lat >= 0 ? 'N' : 'S') + ' ' +
            Utils.format(format, Math.abs(lon)) + '°' + (lon >= 0 ? 'E' : 'W');
   }
}
