package no.imr.lsss.framework;

import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.imr.lsss.modules.echogram.overlays.MaskingEditOverlay;
import no.imr.lsss.modules.echogram.overlays.RegionAddOverlay;
import no.imr.lsss.modules.echogram.overlays.RegionEditOverlay;
import no.imr.lsss.modules.echogram.overlays.ZoomOverlay;

public enum EchogramWorkingMode {
   EDIT(RegionEditOverlay.class),
   DELETE(MaskingEditOverlay.class),
   ZOOM(ZoomOverlay.class),
   ADD(RegionAddOverlay.class);

   public final Class<? extends BaseEchogramOverlay> overlayClass;

   EchogramWorkingMode(Class<? extends BaseEchogramOverlay> overlayClass) {
      this.overlayClass = overlayClass;
   }
}
