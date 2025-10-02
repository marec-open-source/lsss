package no.marec.lsss.api.util;

import java.awt.geom.Point2D;

/**
 * A geographical point represented as a {@link Point2D}.
 * <ul>
 *    <li>Longitude corresponds to the x-coordinate.</li>
 *    <li>Latitude corresponds to the y-coordinate.</li>
 * </ul>
 */
public final class GeoPoint extends Point2D.Double {
   /**
    * Creates a new point located at (0°N, 0°E).
    */
   public GeoPoint() {
   }

   /**
    * Creates a new point at the specified location.
    *
    * @param longitude a longitude (x-coordinate)
    * @param latitude  a latitude (y-coordinate)
    */
   public GeoPoint(double longitude, double latitude) {
      super(longitude, latitude);
   }

   /**
    * {@return the longitude (x-coordinate) of this point}
    */
   public double getLongitude() {
      return x;
   }

   /**
    * Sets the longitude (x-coordinate) of this point.
    *
    * @param longitude a longitude (x-coordinate)
    */
   public void setLongitude(double longitude) {
      x = longitude;
   }

   /**
    * {@return the latitude (y-coordinate) of this point}
    */
   public double getLatitude() {
      return y;
   }

   /**
    * Sets the latitude (y-coordinate) of this point.
    *
    * @param latitude a latitude (y-coordinate)
    */
   public void setLatitude(double latitude) {
      y = latitude;
   }
}
