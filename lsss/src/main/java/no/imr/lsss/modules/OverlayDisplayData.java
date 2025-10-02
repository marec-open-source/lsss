package no.imr.lsss.modules;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public abstract class OverlayDisplayData {
   protected OverlayDisplayData() {
   }

   /**
    * Draw on top of the overlaid module.
    * Text should be drawn in {@link #drawText(Graphics2D)}.
    * Note: A new graphic context is created for each overlay.
    *
    * @param g2d the Graphics2D
    */
   public void draw(Graphics2D g2d) {
   }

   /**
    * Draw text on top of the overlaid module.
    * Non-textual graphics should be drawn in {@link #draw(Graphics2D)}.
    * Note: The same graphic context is used for all overlays.
    *
    * @param g2d the Graphics2D
    */
   public void drawText(Graphics2D g2d) {
   }

   /**
    * Tests for intersection between a rectangle and this overlay's graphics.
    *
    * @param rectangle a rectangle
    * @return {@code true} if there is an intersection
    */
   public boolean intersects(Rectangle2D rectangle) {
      return false;
   }
}
