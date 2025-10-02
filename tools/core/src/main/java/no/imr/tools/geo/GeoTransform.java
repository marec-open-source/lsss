package no.imr.tools.geo;

import no.marec.lsss.api.util.GeoPoint;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * An immutable transformation between geographical coordinates and pixel coordinates.
 */
public final class GeoTransform implements no.marec.lsss.api.util.GeoTransform {
   private final double geoMinX;
   private final double geoMaxY;

   private final double pixPerGeoX;
   private final double pixPerGeoY;

   public GeoTransform(Rectangle2D geoRect, int width, int height) {
      geoMinX = geoRect.getMinX();
      geoMaxY = geoRect.getMaxY();

      pixPerGeoX = width / geoRect.getWidth();
      pixPerGeoY = height / geoRect.getHeight();
   }

   @Override
   public Point2D geoToPix(GeoPoint geoPoint) {
      Point2D pixPoint = new Point2D.Double();
      geoToPix(geoPoint, pixPoint);
      return pixPoint;
   }

   public void geoToPix(GeoPoint geoPoint, Point2D pixPoint) {
      double x = (geoPoint.getX() - geoMinX) * pixPerGeoX;
      double y = (geoMaxY - geoPoint.getY()) * pixPerGeoY;
      pixPoint.setLocation(x, y);
   }

   public Rectangle2D geoToPix(Rectangle2D geoRect) {
      Rectangle2D pixRect = new Rectangle2D.Double();
      geoToPix(geoRect, pixRect);
      return pixRect;
   }

   public void geoToPix(Rectangle2D geoRect, Rectangle2D pixRect) {
      Point2D p = geoToPix(new GeoPoint(geoRect.getX(), geoRect.getMaxY()));
      pixRect.setFrame(p.getX(), p.getY(),
            geoRect.getWidth() * pixPerGeoX, geoRect.getHeight() * pixPerGeoY);
   }

   @Override
   public Point2D geoToPix(double longitude, double latitude) {
      GeoPoint geoPoint = new GeoPoint(longitude, latitude);
      return geoToPix(geoPoint);
   }

   @Override
   public GeoPoint pixToGeo(Point2D pixPoint) {
      if (pixPoint instanceof GeoPoint) {
         throw new IllegalArgumentException();
      }
      return pixToGeo(pixPoint.getX(), pixPoint.getY());
   }

   @Override
   public GeoPoint pixToGeo(double x, double y) {
      double geoX = geoMinX + x / pixPerGeoX;
      double geoY = geoMaxY - y / pixPerGeoY;
      return new GeoPoint(geoX, geoY);
   }

   public Rectangle2D pixToGeo(Rectangle2D pixRect) {
      GeoPoint p = pixToGeo(pixRect.getX(), pixRect.getMaxY());
      return new Rectangle2D.Double(p.getX(), p.getY(),
            pixRect.getWidth() / pixPerGeoX, pixRect.getHeight() / pixPerGeoY);
   }
}
