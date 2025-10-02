package no.imr.korona.computation.categorization.apriori.gui;

import no.imr.korona.computation.categorization.apriori.APrioriPolygon;
import no.imr.tools.swing.GuiUtils;
import no.marec.lsss.api.util.GeoPoint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

final class AddPolygonInteraction extends EditorInteraction {
   private final GeoAPrioriEditor geoAPrioriEditor;
   private final List<GeoPoint> geoPoints = new ArrayList<>();
   private final Path2D path = new Path2D.Float();
   private GeoPoint currentGeoPoint = new GeoPoint();
   private boolean closeToFirst;

   AddPolygonInteraction(GeoAPrioriEditor geoAPrioriEditor) {
      this.geoAPrioriEditor = geoAPrioriEditor;
   }

   @Override
   void paint(Graphics2D graphics2D) {
      graphics2D.draw(path);
      Point2D currentPixPoint = geoAPrioriEditor.getGeoZoom().geoTransform.geoToPix(currentGeoPoint);
      graphics2D.draw(GeoAPrioriEditor.pixPointToMarker(currentPixPoint));
      if (!geoPoints.isEmpty()) {
         Point2D firstPixPoint = geoAPrioriEditor.getGeoZoom().geoTransform.geoToPix(geoPoints.getFirst());
         if (closeToFirst) {
            graphics2D.setColor(Color.RED);
            graphics2D.setStroke(GuiUtils.STROKE_3);
         }
         graphics2D.draw(GeoAPrioriEditor.pixPointToMarker(firstPixPoint));
         graphics2D.setColor(Color.BLACK);
         graphics2D.setStroke(GuiUtils.STROKE_1);
      }
   }

   @Override
   public void mouseMoved(MouseEvent e) {
      currentGeoPoint = geoAPrioriEditor.getGeoZoom().geoTransform.pixToGeo(e.getPoint());
      closeToFirst = !geoPoints.isEmpty() && GeoAPrioriEditor.isPixPointClose(geoAPrioriEditor.getGeoZoom().geoTransform.geoToPix(geoPoints.getFirst()), e.getPoint());
      updatePath();
      geoAPrioriEditor.repaint();
   }

   @Override
   public void mouseClicked(MouseEvent e) {
      if (closeToFirst) {
         if (geoPoints.size() > 2) {
            APrioriPolygon aPrioriPolygon = new APrioriPolygon();
            aPrioriPolygon.getGeoPoints().addAll(geoPoints);
            aPrioriPolygon.update();
            geoAPrioriEditor.getGeoAPriori().getPolygons().addFirst(aPrioriPolygon);
            geoAPrioriEditor.update();
         }
         geoAPrioriEditor.setInteraction(null);
         return;
      }
      geoPoints.add(geoAPrioriEditor.getGeoZoom().geoTransform.pixToGeo(e.getPoint()));
      updatePath();
   }

   private void updatePath() {
      path.reset();
      if (!geoPoints.isEmpty()) {
         for (int i = 0; i < geoPoints.size(); i++) {
            Point2D pixPoint = geoAPrioriEditor.getGeoZoom().geoTransform.geoToPix(geoPoints.get(i));
            if (i == 0) {
               path.moveTo(pixPoint.getX(), pixPoint.getY());
            } else {
               path.lineTo(pixPoint.getX(), pixPoint.getY());
            }
         }
         Point2D currentPixPoint = geoAPrioriEditor.getGeoZoom().geoTransform.geoToPix(currentGeoPoint);
         path.lineTo(currentPixPoint.getX(), currentPixPoint.getY());
      }
      geoAPrioriEditor.repaint();
   }
}
