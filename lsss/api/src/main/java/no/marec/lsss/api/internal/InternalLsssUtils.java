package no.marec.lsss.api.internal;

import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * This is an implementation detail and is not part of the LSSS API.
 */
public interface InternalLsssUtils {
   void drawText(Graphics2D g, String text, int x, int y);

   boolean lineStripIntersectsRectangle(Path2D lineStrip, Rectangle2D rectangle);
}
