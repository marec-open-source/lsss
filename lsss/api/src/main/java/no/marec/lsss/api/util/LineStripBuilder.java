package no.marec.lsss.api.util;

import no.marec.lsss.api.DoNotImplement;

import java.awt.geom.Point2D;

/**
 * Builds multiple line strips by adding points.
 * After each line strip, {@link #endLineStrip()} must be called.
 */
@DoNotImplement
public interface LineStripBuilder {
   /**
    * Adds a point to the current line strip.
    *
    * @param x x-coordinate of the point to add
    * @param y y-coordinate of the point to add
    */
   void addPoint(double x, double y);

   /**
    * Adds a point to the current line strip.
    *
    * @param point the point to add
    */
   default void addPoint(Point2D point) {
      addPoint(point.getX(), point.getY());
   }

   /**
    * Ends the current line strip.
    * <p>
    * Must be called after the last point of each line strip has been added.
    */
   void endLineStrip();

   /**
    * Tests if any line strips have been added.
    * <p>
    * Should only be called after first calling {@link #endLineStrip()}.
    *
    * @return {@code true} if no line strips have been added
    */
   boolean isEmpty();
}
