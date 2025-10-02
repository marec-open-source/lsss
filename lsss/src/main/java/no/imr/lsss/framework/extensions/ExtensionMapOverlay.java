package no.imr.lsss.framework.extensions;

import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.map.MapModule;
import no.imr.lsss.modules.map.overlays.BaseMapOverlay;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.marec.lsss.api.modules.LsssOverlayDisplayData;
import no.marec.lsss.api.modules.MapOverlay;
import no.marec.lsss.api.modules.MapOverlayAccess;
import org.jspecify.annotations.Nullable;

import javax.swing.JPopupMenu;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.Function;

final class ExtensionMapOverlay extends BaseMapOverlay {
   private final MapOverlay overlay;

   ExtensionMapOverlay(ModuleInfo<ExtensionFeaturePlugin> moduleInfo, MapModule mapModule, Function<MapOverlayAccess, ? extends MapOverlay> factory) {
      super(moduleInfo, mapModule);

      overlay = factory.apply(new MapOverlayAccessImpl(this));
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
      return data != null ? new DisplayData(data) : null;
   }

   private final class DisplayData extends TransformedDisplayData {
      private final LsssOverlayDisplayData data;

      private DisplayData(LsssOverlayDisplayData data) {
         this.data = data;
      }

      @Override
      protected void transformedDraw(Graphics2D g2d) {
         data.draw(g2d);
      }

      @Override
      public void drawText(Graphics2D g2d) {
         data.drawText(g2d);
      }

      @Override
      public boolean intersects(Rectangle2D rectangle) {
         return data.intersects(rectangle);
      }
   }
}
