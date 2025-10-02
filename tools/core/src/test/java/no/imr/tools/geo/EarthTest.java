package no.imr.tools.geo;

import no.marec.lsss.api.util.GeoPoint;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.*;

final class EarthTest {
   @Test
   void earthRadius() {
      assertEquals(Earth.EQUATORIAL_RADIUS, Earth.getRadius(0));
      assertEquals(Earth.POLAR_RADIUS, Earth.getRadius(90));
      assertEquals(Earth.POLAR_RADIUS, Earth.getRadius(-90));
      for (int i = 1, n = 20; i < n; i++) {
         double lat = 90 * i / (double) n;
         double r = Earth.getRadius(lat);
         assertTrue(r <= Earth.EQUATORIAL_RADIUS, i + ", " + r);
         assertTrue(r >= Earth.POLAR_RADIUS, i + ", " + r);
      }
   }

   @Test
   void approximateOffset() {
      GeoPoint p = new GeoPoint(60, 60);
      GeoPoint p1 = Earth.getApproximateGeoPoint(p, new Point2D.Double(100, 0));
      assertEquals(100, Earth.getApproximateDistance(p, p1), 10e-10);

      assertEquals(-100, Earth.getApproximateOffset(p, Earth.getApproximateGeoPoint(p, new Point2D.Double(-100, 0))).getX(), 10e-10);
      assertEquals(-100, Earth.getApproximateOffset(p, Earth.getApproximateGeoPoint(p, new Point2D.Double(0, -100))).getY(), 10e-6);
   }

   @Test
   void geoDegreesAndMeters() {
      GeoPoint p = new GeoPoint(60, 60);
      GeoPoint metersPerGeoDegree = Earth.getMetersPerGeoDegree(p);
      GeoPoint p2 = Earth.getApproximateGeoPoint(p, new Point2D.Double(2 * metersPerGeoDegree.getX(), 3 * metersPerGeoDegree.getY()));
      assertEquals(p.getX() + 2, p2.getX());
      assertEquals(p.getY() + 3, p2.getY());

      GeoPoint geoDegreesPerMeter = Earth.getGeoDegreesPerMeter(p);
      GeoPoint p3 = new GeoPoint(p.getX() + 2 * geoDegreesPerMeter.getX(), p.getY() + 3 * geoDegreesPerMeter.getY());
      Point2D dist = Earth.getApproximateOffset(p, p3);
      assertEquals(2, dist.getX(), 1e-6);
      assertEquals(3, dist.getY(), 1e-8);
   }
}
