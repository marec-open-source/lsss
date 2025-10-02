package no.imr.lsss.framework.extensions;

import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.marec.lsss.api.echogram.EchogramDepthTransform;
import no.marec.lsss.api.echogram.EchogramPingTransform;
import no.marec.lsss.api.modules.EchogramOverlayAccess;
import no.marec.lsss.api.util.observing.Observable;

final class EchogramOverlayAccessImpl extends OverlayAccessImpl<BaseEchogramOverlay> implements EchogramOverlayAccess {
   private final EchogramPingTransform echogramPingTransform;
   private final EchogramDepthTransform echogramDepthTransform;

   EchogramOverlayAccessImpl(BaseEchogramOverlay overlay, ExtensionFeaturePlugin plugin) {
      super(overlay);

      echogramPingTransform = plugin.getEchogramPingTransform();
      echogramDepthTransform = overlay.getEchogramModule().isPelagic()
            ? plugin.getPelagicEchogramDepthTransform()
            : plugin.getBottomEchogramDepthTransform();
   }

   @Override
   public Observable<?> echogramArea() {
      return module.getEchogramModule().echogramArea();
   }

   @Override
   public EchogramPingTransform echogramPingTransform() {
      return echogramPingTransform;
   }

   @Override
   public EchogramDepthTransform echogramDepthTransform() {
      return echogramDepthTransform;
   }
}
