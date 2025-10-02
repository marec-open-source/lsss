package no.imr.tools.swing.linestrip;

import no.imr.tools.math.linalg.Vec2;
import no.marec.lsss.api.util.LineStripBuilder;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class ShiftedLineStripBuilderTest {
   /**
    * Test for line like this, with offset = 2.
    * <pre>
    *            / (4,4)
    *    (2,2) /
    *         |
    * (0,0) __| (2,0)
    * </pre>
    */
   @Test
   void test1() {
      ListBuilder listBuilder = new ListBuilder();
      LineStripBuilder lineStripBuilder = new ShiftedLineStripBuilder(listBuilder, 2);
      lineStripBuilder.addPoint(0, 0);
      lineStripBuilder.addPoint(2, 0);
      lineStripBuilder.addPoint(2, 2);
      lineStripBuilder.addPoint(4, 4);
      lineStripBuilder.endLineStrip();

      List<Point2D.Double> expectedPoints = List.of(
            new Point2D.Double(0., -2),
            new Point2D.Double(2 + 2 * Math.sin(Math.atan2(2, 2)), -2 * Math.cos(Math.atan2(2, 2))),
            new Point2D.Double(2 + 2 * Math.sin(Math.atan2(4, 2)), 2 - 2 * Math.cos(Math.atan2(4, 2))),
            new Point2D.Double(4 + 2 * Math.sin(Math.atan2(2, 2)), 4 - 2 * Math.cos(Math.atan2(2, 2)))
      );

      List<List<Vec2>> list = listBuilder.getList();
      assertEquals(1, list.size());
      List<Vec2> actualPoints = list.getFirst();
      assertEquals(expectedPoints.size(), actualPoints.size());
      for (int i = 0; i < expectedPoints.size(); i++) {
         Point2D.Double expectedPoint = expectedPoints.get(i);
         Vec2 actualPoint = actualPoints.get(i);
         assertEquals(expectedPoint.x, actualPoint.x(), 0.000001, "X coord wrong for point " + i);
         assertEquals(expectedPoint.y, actualPoint.y(), 0.000001, "Y coord wrong for point " + i);
      }
   }
}
