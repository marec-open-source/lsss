package no.imr.lsss.framework.extensions;

import no.imr.lsss.modules.BaseModuleOverlay;
import no.marec.lsss.api.modules.LsssOverlayAccess;
import no.marec.lsss.api.util.observing.ObservableValue;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Optional;

sealed class OverlayAccessImpl<M extends BaseModuleOverlay> extends LsssModuleAccessImpl<M> implements LsssOverlayAccess
      permits EchogramOverlayAccessImpl, MapOverlayAccessImpl {

   OverlayAccessImpl(M overlay) {
      super(overlay);
   }

   @Override
   public void repaint() {
      module.repaint();
   }

   @Override
   public void recompute() {
      module.recompute();
   }

   @Override
   public int getWidth() {
      return module.getWidth();
   }

   @Override
   public int getHeight() {
      return module.getHeight();
   }

   @Override
   public Rectangle getBounds() {
      return module.getOverlaidModule().getBounds();
   }

   @Override
   public ObservableValue<Optional<Point>> getMousePosition() {
      return module.getOverlaidModule().mousePosition();
   }

   @Override
   public void setCursor(Cursor cursor) {
      module.setCursor(cursor);
   }
}
