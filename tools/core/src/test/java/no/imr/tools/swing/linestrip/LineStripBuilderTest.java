package no.imr.tools.swing.linestrip;

import no.imr.tools.math.linalg.Vec2;
import no.marec.lsss.api.util.LineStripBuilder;
import org.junit.jupiter.api.Test;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class LineStripBuilderTest {
   @Test
   void createPiecewiseHorizontalBounded() {
      Path2D path = new Path2D.Double();
      LineStripBuilder lineStripBuilder = LineStripBuilders.piecewiseHorizontal(path, new Rectangle2D.Double(0, 0, 10, 10));
      lineStripBuilder.addPoint(-10, 8);
      lineStripBuilder.addPoint(2, 20);
      lineStripBuilder.addPoint(12, 1);
      lineStripBuilder.endLineStrip();

      assertEquals(List.of(List.of(
                  new Vec2(-10, 8),
                  new Vec2(1, 8),
                  new Vec2(2, 20)
            )),
            toPointLists(path));
   }

   static List<List<Vec2>> toPointLists(Path2D path) {
      double[] a = new double[2];
      List<List<Vec2>> result = new ArrayList<>();
      List<Vec2> currentPointList = new ArrayList<>();

      for (PathIterator pathIterator = path.getPathIterator(null); !pathIterator.isDone(); pathIterator.next()) {
         int type = pathIterator.currentSegment(a);
         switch (type) {
            case PathIterator.SEG_MOVETO -> {
               if (!currentPointList.isEmpty()) {
                  result.add(currentPointList);
                  currentPointList = new ArrayList<>();
               }
               currentPointList.add(new Vec2((float) a[0], (float) a[1]));
            }
            case PathIterator.SEG_LINETO -> {
               currentPointList.add(new Vec2((float) a[0], (float) a[1]));
            }
            default -> {
               throw new IllegalArgumentException("Illegal segment type: " + type);
            }
         }
      }
      if (!currentPointList.isEmpty()) {
         result.add(currentPointList);
      }
      return result;
   }
}
