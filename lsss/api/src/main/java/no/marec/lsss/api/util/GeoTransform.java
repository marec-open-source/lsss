package no.marec.lsss.api.util;

import no.marec.lsss.api.DoNotImplement;

import java.awt.geom.Point2D;

/**
 * Converts between geographical coordinates and image coordinates.
 */
@DoNotImplement
public interface GeoTransform {

   /**
    * Converts image coordinates to geographical coordinates.
    *
    * @param pixPoint image coordinates
    * @return geographical coordinates
    */
   GeoPoint pixToGeo(Point2D pixPoint);

   /**
    * Converts image coordinates to geographical coordinates.
    *
    * @param x image x-coordinate
    * @param y image y-coordinate
    * @return geographical coordinates
    */
   GeoPoint pixToGeo(double x, double y);

   /**
    * Converts geographical coordinates to image coordinates.
    *
    * @param geoPoint geographical coordinates
    * @return image coordinates
    */
   Point2D geoToPix(GeoPoint geoPoint);

   /**
    * Converts geographical coordinates to image coordinates.
    *
    * @param longitude longitude
    * @param latitude  latitude
    * @return image coordinates
    */
   Point2D geoToPix(double longitude, double latitude);
}
