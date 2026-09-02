package no.imr.lsss.framework.extensions;

import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.marec.lsss.api.modules.EchogramOverlay;
import no.marec.lsss.api.modules.EchogramOverlayAccess;
import no.marec.lsss.api.modules.LsssOverlayDisplayData;
import org.jspecify.annotations.Nullable;

import javax.swing.JPopupMenu;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Function;

final class ExtensionEchogramOverlay extends BaseEchogramOverlay {
   private final EchogramOverlay overlay;

   ExtensionEchogramOverlay(ModuleInfo<ExtensionFeaturePlugin> moduleInfo, EchogramModule echogramModule, Function<EchogramOverlayAccess, ? extends EchogramOverlay> factory) {
      super(moduleInfo, echogramModule);

      overlay = factory.apply(new EchogramOverlayAccessImpl(this, moduleInfo.plugin()));
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return ExtensionUtils.getParameters(overlay);
   }

   @Override
   public boolean isConfigurable() {
      return overlay.getConfig() != null;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      overlay.onEnable(registry);
   }

   @Override
   protected void onDisable() {
      overlay.onDisable();
   }

   @Override
   public @Nullable String getToolTipText(Point point) {
      return overlay.getToolTipText(point);
   }

   @Override
   public @Nullable JPopupMenu getPopupMenu(Point point) {
      return overlay.getPopupMenu(point);
   }

   @Override
   public void onActivate() {
      overlay.onActivate();
   }

   @Override
   public void onDeactivate() {
      overlay.onDeactivate();
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      return overlay.keyPressed(keyEvent);
   }

   @Override
   public boolean keyReleased(KeyEvent keyEvent) {
      return overlay.keyReleased(keyEvent);
   }

   @Override
   public boolean keyTyped(KeyEvent keyEvent) {
      return overlay.keyTyped(keyEvent);
   }

   @Override
   public void mouseClicked(MouseEvent mouseEvent) {
      overlay.mouseClicked(mouseEvent);
   }

   @Override
   public void mouseDragged(MouseEvent mouseEvent) {
      overlay.mouseDragged(mouseEvent);
   }

   @Override
   public void mouseEntered(MouseEvent mouseEvent) {
      overlay.mouseEntered(mouseEvent);
   }

   @Override
   public void mouseExited(MouseEvent mouseEvent) {
      overlay.mouseExited(mouseEvent);
   }

   @Override
   public void mouseMoved(MouseEvent mouseEvent) {
      overlay.mouseMoved(mouseEvent);
   }

   @Override
   public void mousePressed(MouseEvent mouseEvent) {
      overlay.mousePressed(mouseEvent);
   }

   @Override
   public void mouseReleased(MouseEvent mouseEvent) {
      overlay.mouseReleased(mouseEvent);
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      LsssOverlayDisplayData data = overlay.computeDisplayData();
      return data != null ? transformed(new ExtensionOverlayDisplayData(data)) : null;
   }
}
