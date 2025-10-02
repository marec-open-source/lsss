package no.imr.lsss.modules.test;

import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.MouseAndKeyAdapter;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

final class MouseRateListener extends MouseAndKeyAdapter {
   private long lastTime;
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
      long t = System.nanoTime();
      int nano = 1000000000;
      if (t - lastTime > nano) {
         double sec = (t - lastTime) / (double) nano;
         Log.global.info("Mouse rate:"
               + " Movement: " + Utils.format("%5.1f", mouseMoveCount / sec)
               + ",   Scroll wheel: " + Utils.format("%5.1f", mouseWheelCount / sec));
         mouseMoveCount = 0;
         mouseWheelCount = 0;
         lastTime = t;
      }
   }
}
