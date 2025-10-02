package no.imr.lsss.server.pojo;

import no.imr.lsss.modules.BaseModuleOverlay;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import org.jspecify.annotations.Nullable;

public final class OverlayInfo {
   public String id;
   public boolean enabled;
   public boolean hasData;
   public @Nullable PojoData data;

   public OverlayInfo(BaseModuleOverlay overlay, boolean includeData) {
      id = overlay.getPersistentName();
      enabled = overlay.isEnabledByUser();
      hasData = overlay instanceof PojoDataContainer;
      if (includeData && hasData && enabled) {
         data = ((PojoDataContainer) overlay).getPojoData();
      }
   }

   @Override
   public String toString() {
      return "ModuleInfo{" +
            "id='" + id + '\'' +
            ", enabled=" + enabled +
            ", hasData=" + hasData +
            '}';
   }
}
