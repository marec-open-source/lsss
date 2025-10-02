package no.imr.lsss.modules.map.overlays;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.map.MapModule;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Panning by dragging with the mouse.
 */
public final class DragOverlay extends BaseMapOverlay {
   private @Nullable Point2D grabPoint;
   private @Nullable Rectangle2D geoRect;

   public DragOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, MapModule mapModule) {
      super(moduleInfo, mapModule);
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      return null;
   }

   @Override
   public boolean isBackgroundOverlay() {
      return true;
   }

   @Override
   public void onActivate() {
      setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
   }

   @Override
   public void mousePressed(MouseEvent mouseEvent) {
      grabPoint = mouseEvent.getPoint();
      geoRect = getMapModule().getGeoRect();
   }

   @Override
   public void mouseDragged(MouseEvent mouseEvent) {
      if (grabPoint == null || geoRect == null) {
         return;
      }
      double fx = (mouseEvent.getX() - grabPoint.getX()) / getWidth();
      double fy = (mouseEvent.getY() - grabPoint.getY()) / getHeight();
      double x = geoRect.getCenterX() - fx * geoRect.getWidth();
      double y = geoRect.getCenterY() + fy * geoRect.getHeight();
      getMapModule().setGeoCenter(new GeoPoint(x, y));
   }
}
