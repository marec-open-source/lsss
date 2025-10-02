package no.imr.tools.swing.animatedshape;

import no.imr.tools.swing.Repaintable;

import javax.swing.Timer;
import java.awt.Graphics2D;
import java.awt.Shape;

public final class AnimatedShape {
   private final AnimatedRendered animatedRendered;
   private final Timer timer;

   public AnimatedShape(Repaintable repaintable, AnimatedRendered animatedRendered) {
      this.animatedRendered = animatedRendered;
      timer = new Timer(100, e -> {
         animatedRendered.update();
         repaintable.repaint();
      });
      timer.start();
   }

   public void stop() {
      timer.stop();
   }

   public void draw(Graphics2D g, Shape shape) {
      animatedRendered.draw(g, shape);
   }
}
