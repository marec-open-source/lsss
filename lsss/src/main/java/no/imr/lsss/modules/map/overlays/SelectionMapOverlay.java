package no.imr.lsss.modules.map.overlays;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.MapSelection;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.overlays.RegionEditOverlay;
import no.imr.lsss.modules.map.MapModule;
import no.imr.tools.misc.SelectionAction;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public final class SelectionMapOverlay extends BaseMapOverlay {
   private @Nullable Point2D referencePoint;

   public SelectionMapOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, MapModule mapModule) {
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
   public boolean keyPressed(KeyEvent keyEvent) {
      switch (keyEvent.getKeyCode()) {
         case KeyEvent.VK_ESCAPE -> {
            referencePoint = null;
            setEmptyDisplayData();
         }
         default -> {
            return false;
         }
      }
      return true;
   }

   @Override
   public void mousePressed(MouseEvent mouseEvent) {
      referencePoint = mouseEvent.getPoint();
      compute(mouseEvent);
   }

   @Override
   public void mouseReleased(MouseEvent mouseEvent) {
      if (getUnwrappedDisplayData() instanceof DisplayData displayData) {
         Rectangle2D geoRect = getMapModule().getGeoTransform().pixToGeo(displayData.box);
         if (geoRect.getWidth() == 0) {
            geoRect.add(Math.nextUp(geoRect.getX()), geoRect.getY());
         }
         if (geoRect.getHeight() == 0) {
            geoRect.add(geoRect.getX(), Math.nextUp(geoRect.getY()));
         }
         SelectionAction action = SelectionAction.fromModifiersEx(mouseEvent.getModifiersEx());
         getInterpretationSettings().getMapSettings().doMapSelection(new MapSelection(action, geoRect));
      }
      referencePoint = null;
      setEmptyDisplayData();
   }

   @Override
   public void mouseDragged(MouseEvent mouseEvent) {
      compute(mouseEvent);
   }

   private void compute(MouseEvent mouseEvent) {
      if (referencePoint == null) {
         return;
      }
      Rectangle2D.Float box = new Rectangle2D.Float();
      box.setFrameFromDiagonal(referencePoint, mouseEvent.getPoint());
      setDisplayData(new DisplayData(box));
      getMapModule().setSelectionGeoBox(getMapModule().getGeoTransform().pixToGeo(box));
   }

   private void setEmptyDisplayData() {
      setDisplayData(null);
      getMapModule().setSelectionGeoBox(null);
   }

   @Override
   public void onActivate() {
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
   }

   private record DisplayData(Rectangle2D box) implements OverlayDisplayData {
      @Override
      public void draw(Graphics2D g2d) {
         g2d.setStroke(RegionEditOverlay.SELECT_STROKE);
         g2d.setColor(Color.RED);
         g2d.draw(box);
      }
   }
}
