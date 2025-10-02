package no.imr.tools.swing;

import java.awt.Graphics2D;

@FunctionalInterface
public interface Drawable {
   void draw(Graphics2D g2d);

   static Drawable nothing() {
      return g2d -> {
      };
   }
}
