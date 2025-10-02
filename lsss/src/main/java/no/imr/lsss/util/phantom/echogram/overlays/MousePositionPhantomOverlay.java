package no.imr.lsss.util.phantom.echogram.overlays;

import no.imr.korona.data.ping.PingIndex;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.util.overlays.MousePositionDisplayData;
import no.imr.lsss.util.phantom.echogram.BasePhantomEchogramModule;
import no.imr.tools.listening.ListenerRegistry;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Draws a vertical line corresponding to the mouse position.
 */
public final class MousePositionPhantomOverlay extends BasePhantomOverlay {
   public MousePositionPhantomOverlay(ModuleInfo<?> moduleInfo, BasePhantomEchogramModule phantomEchogramModule) {
      super(moduleInfo, phantomEchogramModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            getPhantomEchogramModule().getEchogramAreaChangeManager(),
            getInterpretationSettings().mouseover().frozen(),
            getInterpretationSettings().mouseover().pingIndex(),
            getInterpretationSettings().mouseover().depth()
      ));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      if (getPhantomEchogramModule().getMousePosition() != null && !getInterpretationSettings().mouseover().isFrozen()) {
         return null;
      }
      PingIndex lsssPingIndex = getInterpretationSettings().mouseover().getPingIndex();
      if (lsssPingIndex == null) {
         return null;
      }
      PingIndex phantomPingIndex = getPhantomEchogramSettings().getPhantomPingIndexConverter().lsssToContainingOther(lsssPingIndex);
      if (phantomPingIndex == null) {
         return null;
      }
      float x = getPingSettings().pingIndexToX(phantomPingIndex);
      Float depth = getInterpretationSettings().mouseover().getDepth();
      Float y = depth != null ? getZSettings().depthToY(depth, phantomPingIndex) : null;
      return new MousePositionDisplayData(x, y, getHeight());
   }
}
