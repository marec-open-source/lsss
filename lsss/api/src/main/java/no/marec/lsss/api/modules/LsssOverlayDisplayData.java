package no.marec.lsss.api.modules;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 * The data to be displayed by a {@link LsssOverlay}.
 */
public interface LsssOverlayDisplayData {
   /**
    * For painting graphics, but not text.
    *
    * @param g the graphics context in which to paint
    */
   default void draw(Graphics2D g) {
   }

   /**
    * For painting text.
    *
    * @param g the graphics context in which to paint
    */
   default void drawText(Graphics2D g) {
   }

   /**
    * Tests if this overlay intersects with a rectangle.
    *
    * @param rectangle a rectangle
    * @return {@code true} if some part of this overlay is inside the rectangle
    */
   default boolean intersects(Rectangle2D rectangle) {
      return false;
   }
}
