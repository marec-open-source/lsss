package no.imr.tools.swing.animatedshape;

import java.awt.Graphics2D;
import java.awt.Shape;

public interface AnimatedRenderer {
   void update();

   void draw(Graphics2D g, Shape shape);
}
