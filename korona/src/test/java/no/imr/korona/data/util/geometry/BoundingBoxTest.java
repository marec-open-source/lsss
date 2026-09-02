package no.imr.korona.data.util.geometry;

import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.range.FloatRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class BoundingBoxTest {
   private PingIndex idx0;
   private PingIndex idx1;
   private PingIndex idx2;
   private PingIndex idx3;
   private PingIndex idx4;

   private BoundingBox box;

   @BeforeEach
   void beforeEach() {
      idx0 = new Idx0Datagram(Instant.ofEpochSecond(0), 0, 0, null, 0);
      idx1 = new Idx0Datagram(Instant.ofEpochSecond(1), 50, 1, null, 0);
      idx2 = new Idx0Datagram(Instant.ofEpochSecond(2), 67, 2, null, 0);
      idx3 = new Idx0Datagram(Instant.ofEpochSecond(3), 200, 3, null, 0);
      idx4 = new Idx0Datagram(Instant.ofEpochSecond(4), 290, 4, null, 0);

      box = new BoundingBox(PingRange.of(idx1, idx3), FloatRange.of(0, 500));
   }

   @Test
   void equalsAndHashCode() {
      assertEquals(box, box);
      assertEquals(box, new BoundingBox(box.pingRange(), box.depthRange()));
      assertNotEquals(box, new BoundingBox(box.pingRange(), FloatRange.of(0, 501)));
      assertNotEquals(box, new BoundingBox(PingRange.of(idx0, idx3), box.depthRange()));

      assertNotEquals(box, new Object());

      assertEquals(box.hashCode(), new BoundingBox(box.pingRange(), box.depthRange()).hashCode());
   }

   @Test
   void contains() {
      assertFalse(box.pingRange().contains(idx0));
      assertTrue(box.pingRange().contains(idx1));
      assertTrue(box.pingRange().contains(idx2));
      assertFalse(box.pingRange().contains(idx3));
      assertFalse(box.pingRange().contains(idx4));

      assertFalse(box.depthRange().contains(-1));
      assertFalse(box.depthRange().contains(-0.01f));
      assertTrue(box.depthRange().contains(0));
      assertTrue(box.depthRange().contains(1));
      assertTrue(box.depthRange().contains(499.9f));
      assertFalse(box.depthRange().contains(500));
      assertFalse(box.depthRange().contains(501));

      assertFalse(box.contains(idx0, 0));

      assertFalse(box.contains(idx1, -1));
      assertTrue(box.contains(idx1, 0));
      assertTrue(box.contains(idx1, 499.9f));
      assertFalse(box.contains(idx1, 500));

      assertTrue(box.contains(idx2, 0));

      assertFalse(box.contains(idx3, 0));
      assertFalse(box.contains(idx4, 0));
   }

   @Test
   void intersects() {
      assertTrue(box.intersects(box));
      assertTrue(box.intersects(new BoundingBox(box.pingRange(), box.depthRange())));

      assertFalse(box.intersects(new BoundingBox(PingRange.of(idx0, idx1), FloatRange.of(300, 600))));
      assertTrue(box.intersects(new BoundingBox(PingRange.of(idx1, idx2), FloatRange.of(300, 600))));
      assertFalse(box.intersects(new BoundingBox(PingRange.of(idx3, idx4), FloatRange.of(300, 600))));
      assertFalse(box.intersects(new BoundingBox(PingRange.of(idx0, idx4), FloatRange.of(500, 600))));
   }

   @Test
   void union() {
      assertEquals(box, BoundingBox.union(box, box));
      assertEquals(box, BoundingBox.union(BoundingBox.EMPTY_BOX, box));
      assertEquals(box, BoundingBox.union(box, BoundingBox.EMPTY_BOX));

      assertEquals(box, BoundingBox.union(
            new BoundingBox(PingRange.of(idx1, idx2), FloatRange.of(0, 200)),
            new BoundingBox(PingRange.of(idx2, idx3), FloatRange.of(300, 500))));
      assertNotEquals(box, BoundingBox.union(
            new BoundingBox(PingRange.of(idx0, idx2), FloatRange.of(0, 200)),
            new BoundingBox(PingRange.of(idx2, idx3), FloatRange.of(300, 500))));
      assertNotEquals(box, BoundingBox.union(
            new BoundingBox(PingRange.of(idx1, idx2), FloatRange.of(0, 200)),
            new BoundingBox(PingRange.of(idx2, idx3), FloatRange.of(300, 400))));
   }
}
