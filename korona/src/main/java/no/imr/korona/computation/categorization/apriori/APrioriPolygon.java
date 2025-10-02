package no.imr.korona.computation.categorization.apriori;

import no.imr.tools.Utils;
import no.marec.lsss.api.util.GeoPoint;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

public final class APrioriPolygon {
   private static final String XML_POLYGON = "polygon";
   private static final String XML_A_PRIORI = "apriori";
   private static final String XML_POINT = "point";
   private static final String XML_LON = "lon";
   private static final String XML_LAT = "lat";

   private float aPrioriValue = 1;
   private final List<GeoPoint> geoPoints = new ArrayList<>();
   private Path2D.Double geoPolygon;

   public APrioriPolygon() {
      geoPolygon = new Path2D.Double();
   }

   public APrioriPolygon(Element polygonElement) {
      String aPrioriAttribute = polygonElement.attributeValue(XML_A_PRIORI);
      if (aPrioriAttribute != null) {
         aPrioriValue = Float.parseFloat(aPrioriAttribute);
      }
      polygonElement.elements().forEach(pointElement -> {
         double longitude = Double.parseDouble(pointElement.attributeValue(XML_LON));
         double latitude = Double.parseDouble(pointElement.attributeValue(XML_LAT));
         geoPoints.add(new GeoPoint(longitude, latitude));
      });
      geoPolygon = toPolygon(geoPoints);
   }

   public Element toXml() {
      Element polygonElement = DocumentHelper.createElement(XML_POLYGON)
            .addAttribute(XML_A_PRIORI, Utils.toString(aPrioriValue));
      geoPoints.forEach(geoPoint -> {
         polygonElement.addElement(XML_POINT)
               .addAttribute(XML_LON, String.valueOf(geoPoint.getLongitude()))
               .addAttribute(XML_LAT, String.valueOf(geoPoint.getLatitude()));
      });
      return polygonElement;
   }

   private static Path2D.Double toPolygon(List<GeoPoint> geoPoints) {
      Path2D.Double polygon = new Path2D.Double();
      if (!geoPoints.isEmpty()) {
         GeoPoint firstGeoPoint = geoPoints.getFirst();
         polygon.moveTo(firstGeoPoint.x, firstGeoPoint.y);
         for (int i = 1; i < geoPoints.size(); i++) {
            GeoPoint geoPoint = geoPoints.get(i);
            polygon.lineTo(geoPoint.x, geoPoint.y);
         }
         polygon.closePath();
      }
      return polygon;
   }

   public List<GeoPoint> getGeoPoints() {
      return geoPoints;
   }

   public void update() {
      geoPolygon = toPolygon(geoPoints);
   }

   public float getAPrioriValue() {
      return aPrioriValue;
   }

   public void setAPrioriValue(float aPrioriValue) {
      this.aPrioriValue = aPrioriValue;
   }

   public boolean contains(GeoPoint geoPoint) {
      return geoPolygon.contains(geoPoint);
   }
}
