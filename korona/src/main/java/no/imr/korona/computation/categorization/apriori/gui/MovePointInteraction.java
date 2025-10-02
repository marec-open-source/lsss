package no.imr.korona.computation.categorization.apriori.gui;

import no.imr.korona.computation.categorization.apriori.APrioriPolygon;
import no.marec.lsss.api.util.GeoPoint;

import java.awt.event.MouseEvent;

final class MovePointInteraction extends EditorInteraction {
   private final GeoAPrioriEditor geoAPrioriEditor;
   private final APrioriPolygon aPrioriPolygon;
   private final GeoPoint geoPoint;

   MovePointInteraction(GeoAPrioriEditor geoAPrioriEditor, APrioriPolygon aPrioriPolygon, int pointIndex) {
      this.geoAPrioriEditor = geoAPrioriEditor;
      this.aPrioriPolygon = aPrioriPolygon;
      geoPoint = aPrioriPolygon.getGeoPoints().get(pointIndex);
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      geoPoint.setLocation(geoAPrioriEditor.getGeoZoom().geoTransform.pixToGeo(e.getPoint()));
      geoAPrioriEditor.update();
   }

   @Override
   public void mouseReleased(MouseEvent e) {
      aPrioriPolygon.update();
      geoAPrioriEditor.setInteraction(null);
   }
}
