package no.imr.lsss.util.phantom.echogram.overlays;

import no.imr.lsss.modules.BaseModuleOverlay;
import no.imr.lsss.modules.BaseOverlaidModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.util.phantom.echogram.BasePhantomEchogramModule;
import no.imr.lsss.util.phantom.echogram.PhantomEchogramPingSettings;
import no.imr.lsss.util.phantom.echogram.PhantomEchogramSettings;
import no.imr.lsss.util.phantom.echogram.PhantomEchogramZSettings;

/**
 * Base class for phantom echogram overlays.
 */
public abstract class BasePhantomOverlay extends BaseModuleOverlay {
   private final BasePhantomEchogramModule phantomEchogramModule;

   protected BasePhantomOverlay(ModuleInfo<?> moduleInfo, BasePhantomEchogramModule phantomEchogramModule) {
      super(moduleInfo, phantomEchogramModule);

      this.phantomEchogramModule = phantomEchogramModule;
   }

   @Override
   public BaseOverlaidModule<?> getOverlaidModule() {
      return phantomEchogramModule;
   }

   public BasePhantomEchogramModule getPhantomEchogramModule() {
      return phantomEchogramModule;
   }

   public PhantomEchogramSettings getPhantomEchogramSettings() {
      return phantomEchogramModule.getPhantomEchogramSettings();
   }

   public PhantomEchogramPingSettings getPingSettings() {
      return getPhantomEchogramSettings().getEchogramPingSettings();
   }

   public PhantomEchogramZSettings getZSettings() {
      return getPhantomEchogramSettings().getEchogramZSettings();
   }
}
