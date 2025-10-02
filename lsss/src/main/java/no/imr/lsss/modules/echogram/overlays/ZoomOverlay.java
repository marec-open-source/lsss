package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramRectangle;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.EchogramSettings;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.resources.LsssCursors;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.swing.GuiListeners;
import org.jspecify.annotations.Nullable;

import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Used as background overlay in EchogramModule.
 */
public final class ZoomOverlay extends BaseEchogramOverlay {
   public enum Mode {
      ZOOM, PAN
   }

   private final EchogramSettings echogramSettings;
   private @Nullable ZoomBaseEngine zoom;

   public ZoomOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);

      echogramSettings = getInterpretationSettings().getEchogramSettings();
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(echogramSettings.zoomSubMode,
            GuiListeners.coalescingLater(this::updateCursor));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      return null;
   }

   @Override
   public boolean isBackgroundOverlay() {
      return true;
   }

   @Override
   public void onActivate() {
      updateCursor();
   }

   private void updateCursor() {
      Cursor cursor = switch (echogramSettings.zoomSubMode.getValue()) {
         case ZOOM -> LsssCursors.ZOOM;
         case PAN -> Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
      };
      setCursor(cursor);
   }

   @Override
   public void mousePressed(MouseEvent mouseEvent) {
      zoom = switch (echogramSettings.zoomSubMode.getValue()) {
         case ZOOM -> new ZoomEngine(mouseEvent, getInterpretationSettings(), getPingSettings(), getZSettings(), this::zoomUpdated);
         case PAN -> new ZoomDragEngine(mouseEvent, getInterpretationSettings(), getZSettings());
      };
      setDisplayData(zoom);
   }

   @Override
   public void mouseReleased(MouseEvent mouseEvent) {
      if (zoom == null) {
         return;
      }
      zoom.update(mouseEvent);
      zoom.doZoom();
      cancel();
   }

   @Override
   public void mouseDragged(MouseEvent mouseEvent) {
      if (zoom == null) {
         return;
      }
      zoom.update(mouseEvent);
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      switch (keyEvent.getKeyCode()) {
         case KeyEvent.VK_ESCAPE -> {
            cancel();
         }
         case KeyEvent.VK_SPACE -> {
            if (zoom != null) {
               return zoom.keyPressed(keyEvent);
            } else {
               echogramSettings.zoomSubMode.shiftValue(1);
            }
         }
         default -> {
            return zoom != null && zoom.keyPressed(keyEvent);
         }
      }
      return true;
   }

   private void cancel() {
      if (zoom != null) {
         repaint();
         getInterpretationSettings().setZoomRange(PingRange.EMPTY_RANGE);
         zoom = null;
         getEchogramModule().setSelectionRectangle(null);
         setDisplayData(null);
         echogramSettings.useDefaultIfNotSticky();
      } else {
         echogramSettings.useDefault();
      }
   }

   private void zoomUpdated(@Nullable EchogramRectangle echogramRectangle) {
      if (echogramRectangle != null) {
         getInterpretationSettings().setZoomRange(echogramRectangle.pingRange());
      }
      getEchogramModule().setSelectionRectangle(echogramRectangle);
      repaint();
   }
}
