package no.imr.lsss.modules.map.overlays;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.map.MapModule;
import no.imr.lsss.resources.LsssCursors;
import no.imr.tools.swing.GuiUtils;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public final class ZoomMapOverlay extends BaseMapOverlay {
   private static final int MIN_ZOOM_BOX_SIZE = 3;
   private static final double ZOOM_FACTOR = 2;

   private @Nullable Point2D referencePoint;
   private @Nullable Point2D currentPoint;

   public ZoomMapOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, MapModule mapModule) {
      super(moduleInfo, mapModule);
   }

   @Override
   public boolean isBackgroundOverlay() {
      return true;
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      switch (keyEvent.getKeyCode()) {
         case KeyEvent.VK_ESCAPE -> {
            referencePoint = null;
            currentPoint = null;
            recompute();
         }
         case KeyEvent.VK_LEFT -> shiftReferencePoint(-1, 0);
         case KeyEvent.VK_RIGHT -> shiftReferencePoint(1, 0);
         case KeyEvent.VK_UP -> shiftReferencePoint(0, -1);
         case KeyEvent.VK_DOWN -> shiftReferencePoint(0, 1);
         default -> {
            return false;
         }
      }
      return true;
   }

   private void shiftReferencePoint(double dx, double dy) {
      if (referencePoint == null) {
         return;
      }
      referencePoint = clamp(new Point2D.Double(referencePoint.getX() + dx, referencePoint.getY() + dy));
      recompute();
   }

   @Override
   public void mouseClicked(MouseEvent mouseEvent) {
      GeoPoint geoPoint = getMapModule().getGeoTransform().pixToGeo(mouseEvent.getPoint());

      if (SwingUtilities.isMiddleMouseButton(mouseEvent)
            || (SwingUtilities.isLeftMouseButton(mouseEvent) && mouseEvent.isAltDown())) {
         getMapModule().setGeoCenterAndLatitudeExtent(geoPoint, getMapModule().getLatitudeExtent() * ZOOM_FACTOR);
      } else if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
         getMapModule().setGeoCenterAndLatitudeExtent(geoPoint, getMapModule().getLatitudeExtent() / ZOOM_FACTOR);
      }
   }

   @Override
   public void mousePressed(MouseEvent mouseEvent) {
      referencePoint = mouseEvent.getPoint();
      currentPoint = referencePoint;
   }

   @Override
   public void mouseReleased(MouseEvent mouseEvent) {
      if (getUnwrappedDisplayData() instanceof DisplayData displayData) {
         getMapModule().fitGeoRect(getMapModule().getGeoTransform().pixToGeo(displayData.box), 1);
      }
      referencePoint = null;
      currentPoint = null;
      recompute();
   }

   @Override
   public void mouseDragged(MouseEvent mouseEvent) {
      if (referencePoint == null) {
         return;
      }
      currentPoint = clamp(mouseEvent.getPoint());
      recompute();
   }

   private Point2D clamp(Point2D p) {
      return new Point2D.Double(
            Math.clamp(p.getX(), 0, getWidth() - 1),
            Math.clamp(p.getY(), 0, getHeight() - 1));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      if (referencePoint == null || currentPoint == null) {
         getMapModule().setSelectionGeoBox(null);
         return null;
      }
      Rectangle2D.Float box = new Rectangle2D.Float();
      box.setFrameFromDiagonal(referencePoint, currentPoint);
      if (box.getWidth() < MIN_ZOOM_BOX_SIZE || box.getHeight() < MIN_ZOOM_BOX_SIZE) {
         getMapModule().setSelectionGeoBox(null);
         return null;
      }
      getMapModule().setSelectionGeoBox(getMapModule().getGeoTransform().pixToGeo(box));
      return new DisplayData(box);
   }

   @Override
   public void onActivate() {
      setCursor(LsssCursors.ZOOM);
   }

   private record DisplayData(Rectangle2D.Float box) implements OverlayDisplayData {
      @Override
      public void draw(Graphics2D g2d) {
         g2d.setColor(Color.BLACK);
         g2d.setStroke(GuiUtils.STROKE_3);
         g2d.draw(box);

         g2d.setColor(Color.WHITE);
         g2d.setStroke(GuiUtils.STROKE_1);
         g2d.draw(box);
      }
   }
}
