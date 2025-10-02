package no.imr.tools.swing.linestrip;

import no.imr.tools.math.linalg.Vec2;
import no.marec.lsss.api.util.LineStripBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class PiecewiseHorizontalLineStripBuilderTest {
   @Test
   void test() {
      ListBuilder listBuilder = new ListBuilder();
      LineStripBuilder lineStripBuilder = new PiecewiseHorizontalLineStripBuilder(listBuilder);

      lineStripBuilder.addPoint(0, 0);
      lineStripBuilder.addPoint(10, 10);
      lineStripBuilder.addPoint(20, 20);
      lineStripBuilder.addPoint(20, 30);
      lineStripBuilder.endLineStrip();

      assertEquals(List.of(List.of(
                  new Vec2(0, 0),
                  new Vec2(9, 0),
                  new Vec2(10, 10),
                  new Vec2(19, 10),
                  new Vec2(20, 20),
                  new Vec2(20, 30)
            )),
            listBuilder.getList());
   }
}
