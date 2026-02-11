package no.imr.tools.swing.linestrip;

import no.marec.lsss.api.util.LineStripBuilder;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

public final class LineStripBuilders {
   private LineStripBuilders() {
   }

   public static LineStripBuilder coalescing(Path2D path) {
      return coalescing(new PathLineStripBuilder(path));
   }

   public static LineStripBuilder coalescing(LineStripBuilder path) {
      return new CoalescingLineStripBuilder(path);
   }

   public static LineStripBuilder coalescing(Path2D path, Rectangle2D bounds) {
      return coalescing(new PathLineStripBuilder(path), bounds);
   }

   public static LineStripBuilder coalescing(LineStripBuilder path, Rectangle2D bounds) {
      return new BoundedLineStripBuilder(coalescing(path), bounds);
   }

   public static LineStripBuilder piecewiseHorizontal(Path2D path) {
      return piecewiseHorizontal(new PathLineStripBuilder(path));
   }

   public static LineStripBuilder piecewiseHorizontal(LineStripBuilder path) {
      return new PiecewiseHorizontalLineStripBuilder(coalescing(path));
   }

   public static LineStripBuilder piecewiseHorizontal(Path2D path, Rectangle2D bounds) {
      return piecewiseHorizontal(new PathLineStripBuilder(path), bounds);
   }

   public static LineStripBuilder piecewiseHorizontal(LineStripBuilder path, Rectangle2D bounds) {
      return new PiecewiseHorizontalLineStripBuilder(coalescing(path, bounds));
   }
}
