package no.imr.tools.swing;

import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.RangeSet;

import java.awt.geom.Path2D;
import java.util.HashMap;
import java.util.Map;

/**
 * For coalescing horizontal and vertical lines.
 */
public final class GridLineBuilder {
   private final Map<Integer, RangeSet<Integer>> horizontalLines = new HashMap<>();
   private final Map<Integer, RangeSet<Integer>> verticalLines = new HashMap<>();

   public GridLineBuilder() {
   }

   public void rectangle(Integer x0, Integer y0, Integer x1, Integer y1) {
      horizontalLine(x0, x1, y0);
      horizontalLine(x0, x1, y1);
      verticalLine(x0, y0, y1);
      verticalLine(x1, y0, y1);
   }

   public void horizontalLine(Integer x0, Integer x1, Integer y) {
      horizontalLines.computeIfAbsent(y, k -> new ArrayRangeSet<>()).add(x0, x1);
   }

   public void verticalLine(Integer x, Integer y0, Integer y1) {
      verticalLines.computeIfAbsent(x, k -> new ArrayRangeSet<>()).add(y0, y1);
   }

   public Path2D build() {
      Path2D.Float path = new Path2D.Float();

      horizontalLines.forEach((y, x) -> {
         x.forEach((x0, x1) -> {
            path.moveTo(x0, y);
            path.lineTo(x1, y);
         });
      });

      verticalLines.forEach((x, y) -> {
         y.forEach((y0, y1) -> {
            path.moveTo(x, y0);
            path.lineTo(x, y1);
         });
      });

      return path;
   }
}
