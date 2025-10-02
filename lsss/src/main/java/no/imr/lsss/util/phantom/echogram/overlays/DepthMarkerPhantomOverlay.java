package no.imr.lsss.util.phantom.echogram.overlays;

import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.overlays.DepthMarkerEngine;
import no.imr.lsss.util.phantom.echogram.BasePhantomEchogramModule;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class DepthMarkerPhantomOverlay extends BasePhantomOverlay {
   private final DepthMarkerEngine depthMarkerEngine;

   public DepthMarkerPhantomOverlay(ModuleInfo<?> moduleInfo, BasePhantomEchogramModule phantomEchogramModule) {
      super(moduleInfo, phantomEchogramModule);

      depthMarkerEngine = new DepthMarkerEngine(this, getZSettings());
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return depthMarkerEngine.getParameters();
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(depthMarkerEngine.getChangeManager(), createRecomputeListener());
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      return depthMarkerEngine.getDisplayData();
   }
}
