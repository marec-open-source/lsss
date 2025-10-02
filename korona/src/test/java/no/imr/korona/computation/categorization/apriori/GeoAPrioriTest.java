package no.imr.korona.computation.categorization.apriori;

import no.marec.lsss.api.util.GeoPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class GeoAPrioriTest {
   @Test
   void simple() {
      GeoAPriori geoAPriori = new GeoAPriori();

      APrioriPolygon p1 = new APrioriPolygon();
      p1.setAPrioriValue(1f);
      p1.getGeoPoints().add(new GeoPoint(0, 0));
      p1.getGeoPoints().add(new GeoPoint(10, 0));
      p1.getGeoPoints().add(new GeoPoint(10, 10));
      p1.getGeoPoints().add(new GeoPoint(0, 10));
      p1.update();
      geoAPriori.getPolygons().add(p1);

      APrioriPolygon p2 = new APrioriPolygon();
      p2.setAPrioriValue(0.5f);
      p2.getGeoPoints().add(new GeoPoint(0, 0));
      p2.getGeoPoints().add(new GeoPoint(20, 0));
      p2.getGeoPoints().add(new GeoPoint(20, 20));
      p2.getGeoPoints().add(new GeoPoint(0, 20));
      p2.update();
      geoAPriori.getPolygons().add(p2);

      assertEquals(1f, geoAPriori.getAPrioriValue(new GeoPoint(0, 5)));
      assertEquals(0.5f, geoAPriori.getAPrioriValue(new GeoPoint(0, 15)));
      assertEquals(0f, geoAPriori.getAPrioriValue(new GeoPoint(0, 30)));
   }
}
