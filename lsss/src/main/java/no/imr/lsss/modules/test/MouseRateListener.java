package no.imr.lsss.modules.test;

import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.MouseAndKeyAdapter;
import no.imr.tools.time.TimeUtils;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.time.Instant;

final class MouseRateListener extends MouseAndKeyAdapter {
   private Instant lastTime = Instant.now();
   private int mouseMoveCount;
   private int mouseWheelCount;

   MouseRateListener() {
   }

   @Override
   public void mouseWheelMoved(MouseWheelEvent e) {
      mouseWheelCount++;
      check();
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      mouseMoved(e);
   }

   @Override
   public void mouseMoved(MouseEvent e) {
      mouseMoveCount++;
      check();
   }

   private void check() {
      Instant t = Instant.now();
      double sec = TimeUtils.toSeconds(lastTime, t);
      if (sec > 1) {
         Log.global.info("Mouse rate:"
               + " Movement: " + Utils.format("%5.1f", mouseMoveCount / sec)
               + ",   Scroll wheel: " + Utils.format("%5.1f", mouseWheelCount / sec));
         mouseMoveCount = 0;
         mouseWheelCount = 0;
         lastTime = t;
      }
   }
}
