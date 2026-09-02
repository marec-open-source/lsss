package no.imr.lsss.util.phantom.echogram.overlays;

import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.EchogramRectangle;
import no.imr.korona.region.EchogramSelection;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModuleUtils;
import no.imr.lsss.modules.echogram.overlays.RegionEditOverlay;
import no.imr.lsss.util.phantom.echogram.BasePhantomEchogramModule;
import no.imr.tools.misc.SelectionAction;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

public final class SelectionPhantomOverlay extends BasePhantomOverlay {
   private @Nullable EchogramPoint referencePoint;
   private @Nullable EchogramPoint dragPoint;

   public SelectionPhantomOverlay(ModuleInfo<?> moduleInfo, BasePhantomEchogramModule phantomEchogramModule) {
      super(moduleInfo, phantomEchogramModule);
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      if (referencePoint == null || dragPoint == null) {
         return null;
      }
      EchogramRectangle echogramRectangle = EchogramModuleUtils.toSelectionEchogramRectangle(referencePoint, dragPoint, getPingSettings(), getZSettings());
      Rectangle2D.Float box = EchogramModuleUtils.toImageRectangle(echogramRectangle, getPingSettings(), getZSettings());
      return new DisplayData(box);
   }

   @Override
   public boolean isBackgroundOverlay() {
      return true;
   }

   @Override
   public void onActivate() {
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
   }

   @Override
   public void mousePressed(MouseEvent mouseEvent) {
      referencePoint = EchogramModuleUtils.imagePointToClampedEchogramPoint(mouseEvent.getPoint(), getPingSettings(), getZSettings());
      dragPoint = referencePoint;
      recompute();
   }

   @Override
   public void mouseDragged(MouseEvent mouseEvent) {
      if (referencePoint == null) {
         return;
      }
      dragPoint = EchogramModuleUtils.imagePointToClampedEchogramPoint(mouseEvent.getPoint(), getPingSettings(), getZSettings());
      recompute();
   }

   @Override
   public void mouseReleased(MouseEvent mouseEvent) {
      if (referencePoint != null && dragPoint != null) {
         EchogramRectangle echogramRectangle = EchogramModuleUtils.toSelectionEchogramRectangle(referencePoint, dragPoint, getPingSettings(), getZSettings());
         SelectionAction action = SelectionAction.fromModifiersEx(mouseEvent.getModifiersEx());
         getPhantomEchogramModule().doEchogramSelection(new EchogramSelection(action, echogramRectangle));
      }
      referencePoint = null;
      dragPoint = null;
      recompute();
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      switch (keyEvent.getKeyCode()) {
         case KeyEvent.VK_ESCAPE -> {
            if (dragPoint != null) {
               referencePoint = null;
               dragPoint = null;
               recompute();
            } else {
               getInterpretationSettings().getEchogramSettings().useDefault();
            }
         }
         default -> {
            return false;
         }
      }
      return true;
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
