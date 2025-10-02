package no.imr.lsss.server.pojo;

import no.imr.lsss.modules.BaseLsssModule;
import no.imr.lsss.modules.BaseOverlaidModule;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import org.jspecify.annotations.Nullable;

public final class ModuleInfo {
   public String id;
   public boolean enabled;
   public boolean hasData;
   public boolean hasView;
   public boolean hasOverlays;
   public @Nullable PojoData data;

   public ModuleInfo(BaseLsssModule module, boolean includeData) {
      id = module.getPersistentName();
      enabled = module.isEnabled();
      hasData = module instanceof PojoDataContainer;
      hasView = module instanceof BaseViewModule;
      hasOverlays = module instanceof BaseOverlaidModule;
      if (includeData && hasData && enabled) {
         data = ((PojoDataContainer) module).getPojoData();
      }
   }

   @Override
   public String toString() {
      return "ModuleInfo{" +
            "id='" + id + '\'' +
            ", enabled=" + enabled +
            ", hasData=" + hasData +
            ", hasView=" + hasView +
            ", hasOverlays=" + hasOverlays +
            '}';
   }
}
