package no.imr.lsss.modules.echogram.overlays;

import no.imr.lsss.modules.OverlayDisplayData;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public abstract class ZoomBaseEngine implements OverlayDisplayData {
   ZoomBaseEngine() {
   }

   public abstract void update(MouseEvent mouseEvent);

   public boolean keyPressed(KeyEvent keyEvent) {
      return false;
   }

   public void doZoom() {
   }
}
