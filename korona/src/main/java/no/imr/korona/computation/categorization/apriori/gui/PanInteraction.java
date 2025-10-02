package no.imr.korona.computation.categorization.apriori.gui;

import no.marec.lsss.api.util.GeoPoint;

import java.awt.event.MouseEvent;

final class PanInteraction extends EditorInteraction {
   private final GeoAPrioriEditor geoAPrioriEditor;
   private final GeoPoint geoPoint;

   PanInteraction(GeoAPrioriEditor geoAPrioriEditor, GeoPoint geoPoint) {
      this.geoAPrioriEditor = geoAPrioriEditor;
      this.geoPoint = geoPoint;
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      geoAPrioriEditor.setGeoZoom(geoAPrioriEditor.getGeoZoom().withGeoPointAtPixPoint(geoPoint, e.getPoint()));
   }

   @Override
   public void mouseReleased(MouseEvent e) {
      geoAPrioriEditor.setInteraction(null);
   }
}
