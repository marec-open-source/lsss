package no.imr.tools.swing.linestrip;

import no.imr.tools.math.linalg.Vec2;
import no.marec.lsss.api.util.LineStripBuilder;
import org.junit.jupiter.api.Test;

import java.awt.geom.Path2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class PathLineStripBuilderTest {
   @Test
   void test() {
      Path2D path = new Path2D.Double();
      LineStripBuilder lineStripBuilder = new PathLineStripBuilder(path);

      lineStripBuilder.addPoint(2, 1);
      lineStripBuilder.addPoint(3, 7);
      lineStripBuilder.addPoint(4, 1);
      lineStripBuilder.endLineStrip();
      lineStripBuilder.addPoint(10, 10);
      lineStripBuilder.addPoint(11, 11);
      lineStripBuilder.endLineStrip();

      assertEquals(List.of(List.of(
                  new Vec2(2, 1),
                  new Vec2(3, 7),
                  new Vec2(4, 1)
            ), List.of(
                  new Vec2(10, 10),
                  new Vec2(11, 11)
            )),
            LineStripBuilderTest.toPointLists(path));
   }
}
