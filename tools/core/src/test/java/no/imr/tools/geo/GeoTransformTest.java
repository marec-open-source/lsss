package no.imr.tools.geo;

import no.marec.lsss.api.util.GeoPoint;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.*;

final class GeoTransformTest {
   @Test
   void test() {
      GeoTransform transform = new GeoTransform(
            new Rectangle2D.Double(4, 60, 8, 4),
            200, 100);

      testPoint(transform, new GeoPoint(4, 60), new Point2D.Double(0, 100));
      testPoint(transform, new GeoPoint(8, 62), new Point2D.Double(100, 50));
      testPoint(transform, new GeoPoint(12, 64), new Point2D.Double(200, 0));

      testRect(transform, new Rectangle2D.Double(4, 60, 8, 4), new Rectangle2D.Double(0, 0, 200, 100));
      testRect(transform, new Rectangle2D.Double(6, 61, 4, 2), new Rectangle2D.Double(50, 25, 100, 50));
   }

   private static void testPoint(GeoTransform transform, GeoPoint geoPoint, Point2D.Double pixPoint) {
      assertEquals(geoPoint, transform.pixToGeo(pixPoint));
      assertEquals(pixPoint, transform.geoToPix(geoPoint));
   }

   private static void testRect(GeoTransform transform, Rectangle2D geoRect, Rectangle2D pixRect) {
      assertEquals(geoRect, transform.pixToGeo(pixRect));
      assertEquals(pixRect, transform.geoToPix(geoRect));
   }
}
