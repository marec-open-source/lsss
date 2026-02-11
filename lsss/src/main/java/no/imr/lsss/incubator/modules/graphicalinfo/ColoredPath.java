package no.imr.lsss.incubator.modules.graphicalinfo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

record ColoredPath(
      Path2D path,
      Color color,
      boolean filled,
      List<String> texts
) implements RenderedInfo {

   @Override
   public void draw(Graphics2D g2d) {
      g2d.setColor(color);
      if (filled) {
         g2d.fill(path);
      } else {
         g2d.draw(path);
      }
   }

   @Override
   public boolean intersects(Rectangle2D rectangle) {
      return path.contains(rectangle.getCenterX(), rectangle.getCenterY());
   }
}
