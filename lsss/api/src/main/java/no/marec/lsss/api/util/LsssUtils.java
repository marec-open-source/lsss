package no.marec.lsss.api.util;

import no.marec.lsss.api.internal.InternalLsss;
import no.marec.lsss.api.internal.InternalLsssUtils;

import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

/**
 * Various utility functions.
 */
public final class LsssUtils {
   private static final InternalLsssUtils UTILS = InternalLsss.INSTANCE.lsssUtils();

   private static final double IMR_CONSTANT = 4 * Math.PI * 1852.0 * 1852.0;

   private LsssUtils() {
   }

   /**
    * Converts sv from a linear value to a logarithmic value.
    *
    * @param svValue a linear sv value, including the factor <code>4 π 1852<sup>2</sup></code>
    * @return a logarithmic sv value
    */
   public static double svToLogSv(double svValue) {
      return 10 * Math.log10(svValue / IMR_CONSTANT);
   }

   /**
    * Converts sv from a logarithmic value to a linear value.
    *
    * @param logSvValue a logarithmic sv value
    * @return a linear sv value, including the factor <code>4 π 1852<sup>2</sup></code>
    */
   public static double logSvToSv(double logSvValue) {
      return IMR_CONSTANT * Math.pow(10, logSvValue / 10);
   }

   /**
    * Draws text at the specified location.
    *
    * @param g    the graphics context in which to paint
    * @param text the text
    * @param x    the x-coordinate
    * @param y    the y coordinate
    */
   public static void drawText(Graphics2D g, String text, int x, int y) {
      UTILS.drawText(g, text, x, y);
   }

   /**
    * Test for intersection between line strips and a rectangle.
    *
    * @param lineStrip a path containing only {@link PathIterator#SEG_MOVETO}, {@link PathIterator#SEG_LINETO} and {@link PathIterator#SEG_CLOSE}
    * @param rectangle  a rectangle
    * @return {@code true} if intersection
    */
   public static boolean lineStripIntersectsRectangle(Path2D lineStrip, Rectangle2D rectangle) {
      return UTILS.lineStripIntersectsRectangle(lineStrip, rectangle);
   }
}
