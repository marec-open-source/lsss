package no.imr.lsss.util.phantom.echogram.overlays;

import no.imr.korona.data.util.geometry.EchogramRectangle;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.overlays.ZoomBaseEngine;
import no.imr.lsss.modules.echogram.overlays.ZoomDragEngine;
import no.imr.lsss.modules.echogram.overlays.ZoomEngine;
import no.imr.lsss.resources.LsssCursors;
import no.imr.lsss.util.phantom.echogram.BasePhantomEchogramModule;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.swing.GuiListeners;
import org.jspecify.annotations.Nullable;

import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Used as background overlay in {@link BasePhantomEchogramModule}.
 */
public final class ZoomPhantomOverlay extends BasePhantomOverlay {
   private @Nullable ZoomBaseEngine zoom;

   public ZoomPhantomOverlay(ModuleInfo<?> moduleInfo, BasePhantomEchogramModule phantomEchogramModule) {
      super(moduleInfo, phantomEchogramModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getInterpretationSettings().getEchogramSettings().zoomSubMode,
            GuiListeners.later(this::updateCursor));
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
      Cursor cursor = switch (getInterpretationSettings().getEchogramSettings().zoomSubMode.getValue()) {
         case ZOOM -> LsssCursors.ZOOM;
         case PAN -> Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
      };
      setCursor(cursor);
   }

   @Override
   public void mousePressed(MouseEvent mouseEvent) {
      zoom = switch (getInterpretationSettings().getEchogramSettings().zoomSubMode.getValue()) {
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
               getInterpretationSettings().getEchogramSettings().zoomSubMode.shiftValue(1);
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
         zoom = null;
         setDisplayData(null);
         getInterpretationSettings().getEchogramSettings().useDefaultIfNotSticky();
      } else {
         getInterpretationSettings().getEchogramSettings().useDefault();
      }
   }

   private void zoomUpdated(@Nullable EchogramRectangle echogramRectangle) {
      repaint();
   }
}
