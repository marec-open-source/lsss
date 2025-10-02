package no.imr.lsss.modules.echogram;

import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.lsss.framework.NavigationHistory;

import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public final class EchogramZoomMouseWheelListener implements MouseWheelListener {
   public static final double ZOOM_FACTOR = 1.1;

   private final EchogramPingSettings pingSettings;
   private final EchogramZSettings zSettings;
   private final NavigationHistory navigationHistory;

   public EchogramZoomMouseWheelListener(EchogramPingSettings pingSettings, EchogramZSettings zSettings, NavigationHistory navigationHistory) {
      this.pingSettings = pingSettings;
      this.zSettings = zSettings;
      this.navigationHistory = navigationHistory;
   }

   @Override
   public void mouseWheelMoved(MouseWheelEvent e) {
      navigationHistory.coalesceCheckPoint(this, () -> {
         switch (e.getModifiersEx()) {
            case 0 -> {
               horizontalZoom(e);
            }
            case KeyEvent.SHIFT_DOWN_MASK -> {
               verticalZoom(e);
            }
            case KeyEvent.CTRL_DOWN_MASK -> {
               horizontalZoom(e);
               verticalZoom(e);
            }
            default -> {
               // Do nothing
            }
         }
      });
   }

   private void horizontalZoom(MouseWheelEvent e) {
      pingSettings.zoom(e.getX(), Math.pow(ZOOM_FACTOR, e.getWheelRotation()));
   }

   private void verticalZoom(MouseWheelEvent e) {
      zSettings.zoom(zSettings.yToZ(e.getY()), Math.pow(ZOOM_FACTOR, e.getWheelRotation()));
   }
}
