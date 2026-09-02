package no.imr.tools.swing.animatedshape;

import no.imr.tools.swing.Repaintable;

import javax.swing.Timer;
import java.awt.Graphics2D;
import java.awt.Shape;

public final class AnimatedShape {
   private final AnimatedRenderer animatedRenderer;
   private final Timer timer;

   public AnimatedShape(Repaintable repaintable, AnimatedRenderer animatedRenderer) {
      this.animatedRenderer = animatedRenderer;
      timer = new Timer(100, _ -> {
         animatedRenderer.update();
         repaintable.repaint();
      });
      timer.start();
   }

   public void stop() {
      timer.stop();
   }

   public void draw(Graphics2D g, Shape shape) {
      animatedRenderer.draw(g, shape);
   }
}
