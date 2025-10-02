package no.imr.tools.swing.linestrip;

import no.imr.tools.math.linalg.Vec2;
import no.marec.lsss.api.util.LineStripBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class CoalescingLineStripBuilderTest {
   /*
   The tree middle points lies on the same line, so final path should have four points.
    */
   @Test
   void test1() {
      ListBuilder listBuilder = new ListBuilder();
      LineStripBuilder lineStripBuilder = new CoalescingLineStripBuilder(listBuilder);

      lineStripBuilder.addPoint(2, 1);
      lineStripBuilder.addPoint(2, 2);
      lineStripBuilder.addPoint(4, 4);
      lineStripBuilder.addPoint(6, 6);
      lineStripBuilder.addPoint(5, 1);

      lineStripBuilder.endLineStrip();

      assertEquals(List.of(List.of(
                  new Vec2(2, 1),
                  new Vec2(2, 2),
                  new Vec2(6, 6),
                  new Vec2(5, 1)
            )),
            listBuilder.getList());
   }

   @Test
   void test2() {
      ListBuilder listBuilder = new ListBuilder();
      LineStripBuilder lineStripBuilder = new CoalescingLineStripBuilder(listBuilder);

      lineStripBuilder.addPoint(0, 1);
      lineStripBuilder.addPoint(0, 0);
      lineStripBuilder.addPoint(0, 3);
      lineStripBuilder.addPoint(0, 4);
      lineStripBuilder.addPoint(0, 2);

      lineStripBuilder.addPoint(1, 2);
      lineStripBuilder.addPoint(1, 3);
      lineStripBuilder.addPoint(1, 6);
      lineStripBuilder.addPoint(1, 4);
      lineStripBuilder.addPoint(1, 1);

      lineStripBuilder.addPoint(2, 1);
      lineStripBuilder.addPoint(2, 3);
      lineStripBuilder.addPoint(2, 0);
      lineStripBuilder.addPoint(2, 4);
      lineStripBuilder.addPoint(2, 2);
      lineStripBuilder.addPoint(2, 5);

      lineStripBuilder.endLineStrip();

      assertEquals(List.of(List.of(
                  new Vec2(0, 1),
                  new Vec2(0, 0),
                  new Vec2(0, 4),
                  new Vec2(0, 2),

                  new Vec2(1, 2),
                  new Vec2(1, 6),
                  new Vec2(1, 1),

                  new Vec2(2, 1),
                  new Vec2(2, 0),
                  new Vec2(2, 5)
            )),
            listBuilder.getList());
   }
}
