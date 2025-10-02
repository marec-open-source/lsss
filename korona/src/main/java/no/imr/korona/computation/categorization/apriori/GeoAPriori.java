package no.imr.korona.computation.categorization.apriori;

import no.imr.tools.Utils;
import no.marec.lsss.api.util.GeoPoint;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

public final class GeoAPriori {
   static final String XML_GEO = "geo";
   private static final String XML_A_PRIORI = "apriori";

   private float defaultAPriori;
   private final List<APrioriPolygon> polygons = new ArrayList<>();

   public GeoAPriori() {
   }

   public GeoAPriori(Element geoElement) {
      String aPrioriAttribute = geoElement.attributeValue(XML_A_PRIORI);
      if (aPrioriAttribute != null) {
         defaultAPriori = Float.parseFloat(aPrioriAttribute);
      }
      geoElement.elements().stream()
            .map(APrioriPolygon::new)
            .forEach(polygons::add);
   }

   public Element toXml() {
      Element geoElement = DocumentHelper.createElement(XML_GEO)
            .addAttribute(XML_A_PRIORI, Utils.toString(defaultAPriori));
      polygons.stream()
            .map(APrioriPolygon::toXml)
            .forEach(geoElement::add);
      return geoElement;
   }

   public GeoAPriori copy() {
      return new GeoAPriori(toXml());
   }

   public float getDefaultAPriori() {
      return defaultAPriori;
   }

   public void setDefaultAPriori(float defaultAPriori) {
      this.defaultAPriori = defaultAPriori;
   }

   public List<APrioriPolygon> getPolygons() {
      return polygons;
   }

   public float getAPrioriValue(GeoPoint geoPoint) {
      for (APrioriPolygon polygon : polygons) {
         if (polygon.contains(geoPoint)) {
            return polygon.getAPrioriValue();
         }
      }
      return defaultAPriori;
   }
}
