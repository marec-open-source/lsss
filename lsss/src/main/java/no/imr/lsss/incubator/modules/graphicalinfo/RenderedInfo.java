package no.imr.lsss.incubator.modules.graphicalinfo;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

interface RenderedInfo {
   void draw(Graphics2D g2d);

   boolean intersects(Rectangle2D rectangle);

   List<String> texts();
}
