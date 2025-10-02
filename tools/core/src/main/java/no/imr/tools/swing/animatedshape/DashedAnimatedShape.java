package no.imr.tools.swing.animatedshape;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;

public final class DashedAnimatedShape implements AnimatedRendered {
   private final Stroke backgroundStroke = new BasicStroke(4);
   private Stroke animatedStroke = backgroundStroke;
   private int phase;

   public DashedAnimatedShape() {
   }

   @Override
   public void update() {
      float[] dash = {9, 9};
      phase = (phase + 1) % (int) (dash[0] + dash[1]);
      animatedStroke = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dash, phase);
   }

   @Override
   public void draw(Graphics2D g, Shape shape) {
      g.setColor(Color.BLACK);
      g.setStroke(backgroundStroke);
      g.draw(shape);

      g.setColor(Color.WHITE);
      g.setStroke(animatedStroke);
      g.draw(shape);
   }
}
