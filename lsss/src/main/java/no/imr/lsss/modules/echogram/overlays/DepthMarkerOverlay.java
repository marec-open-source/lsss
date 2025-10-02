package no.imr.lsss.modules.echogram.overlays;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Draw lines at constant z values.
 */
public final class DepthMarkerOverlay extends BaseEchogramOverlay {
   private final DepthMarkerEngine depthMarkerEngine;

   public DepthMarkerOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);

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
