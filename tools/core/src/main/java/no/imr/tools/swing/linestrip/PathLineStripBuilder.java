package no.imr.tools.swing.linestrip;

import no.marec.lsss.api.util.LineStripBuilder;

import java.awt.geom.Path2D;

/**
 * Adds line strips to a {@link Path2D}.
 */
public final class PathLineStripBuilder implements LineStripBuilder {
   private final Path2D path;
   private boolean initialized;
   private boolean empty = true;

   public PathLineStripBuilder(Path2D path) {
      this.path = path;
   }

   @Override
   public boolean isEmpty() {
      return empty;
   }

   @Override
   public void addPoint(double x, double y) {
      if (!initialized) {
         initialized = true;
         empty = false;
         path.moveTo(x, y);
         return;
      }

      path.lineTo(x, y);
   }

   @Override
   public void endLineStrip() {
      initialized = false;
   }
}
