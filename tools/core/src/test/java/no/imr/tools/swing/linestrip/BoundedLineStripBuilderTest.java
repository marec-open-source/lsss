package no.imr.tools.swing.linestrip;

import no.imr.tools.math.linalg.Vec2;
import no.marec.lsss.api.util.LineStripBuilder;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class BoundedLineStripBuilderTest {
   @Test
   void test1() {
      ListBuilder listBuilder = new ListBuilder();
      LineStripBuilder lineStripBuilder = new BoundedLineStripBuilder(listBuilder, new Rectangle2D.Double(10, 10, 10, 10));

      lineStripBuilder.addPoint(0, 0);
      lineStripBuilder.addPoint(0, 30);
      lineStripBuilder.addPoint(30, 30);
      lineStripBuilder.addPoint(30, 0);
      lineStripBuilder.endLineStrip();

      assertEquals(List.of(), listBuilder.getList());
   }

   @Test
   void test2() {
      ListBuilder listBuilder = new ListBuilder();
      LineStripBuilder lineStripBuilder = new BoundedLineStripBuilder(listBuilder, new Rectangle2D.Double(10, 10, 10, 10));

      lineStripBuilder.addPoint(0, 0);
      lineStripBuilder.addPoint(1, 1);
      lineStripBuilder.addPoint(15, 0);
      lineStripBuilder.addPoint(15, 15);
      lineStripBuilder.addPoint(100, 15);
      lineStripBuilder.addPoint(200, 15);
      lineStripBuilder.addPoint(15, 100);
      lineStripBuilder.addPoint(15, 0);
      lineStripBuilder.endLineStrip();

      assertEquals(List.of(
                  List.of(
                        new Vec2(15, 0),
                        new Vec2(15, 15),
                        new Vec2(100, 15)
                  ),
                  List.of(
                        new Vec2(15, 100),
                        new Vec2(15, 0)
                  )),
            listBuilder.getList());
   }
}
