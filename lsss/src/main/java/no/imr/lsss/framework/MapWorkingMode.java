package no.imr.lsss.framework;

import no.imr.lsss.modules.map.overlays.BaseMapOverlay;
import no.imr.lsss.modules.map.overlays.DragOverlay;
import no.imr.lsss.modules.map.overlays.SelectionMapOverlay;
import no.imr.lsss.modules.map.overlays.ZoomMapOverlay;

public enum MapWorkingMode {
   ZOOM(ZoomMapOverlay.class),
   DRAG(DragOverlay.class),
   SELECTION(SelectionMapOverlay.class);

   public final Class<? extends BaseMapOverlay> overlayClass;

   MapWorkingMode(Class<? extends BaseMapOverlay> overlayClass) {
      this.overlayClass = overlayClass;
   }
}
