package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.PingIndex;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.util.overlays.MousePositionDisplayData;
import no.imr.tools.listening.ListenerRegistry;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Draws a vertical line corresponding to the mouse position.
 */
public final class MapPositionOverlay extends BaseEchogramOverlay {
   private List<EchogramModule> echogramModules = List.of();

   public MapPositionOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      echogramModules = getModuleManager().getModules(EchogramModule.class).toList();

      registry.add(createRecomputeListener(), List.of(
            getEchogramModule().echogramArea(),
            getInterpretationSettings().mouseover().frozen(),
            getInterpretationSettings().mouseover().pingIndex(),
            getInterpretationSettings().mouseover().depth()
      ));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      if (echogramModules.stream().anyMatch(module -> module.getMousePosition() != null) && !getInterpretationSettings().mouseover().isFrozen()) {
         return null;
      }
      PingIndex pingIndex = getInterpretationSettings().mouseover().getPingIndex();
      if (pingIndex == null) {
         return null;
      }
      int x = getPingSettings().pingIndexToXIndex(pingIndex); // Use x index, same as PingMarkerOverlay.
      Float depth = getInterpretationSettings().mouseover().getDepth();
      Float y = depth != null ? getZSettings().depthToY(depth, pingIndex) : null;
      return new MousePositionDisplayData(x, y, getHeight());
   }
}
