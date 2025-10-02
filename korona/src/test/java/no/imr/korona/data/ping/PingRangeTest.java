package no.imr.korona.data.ping;

import no.imr.korona.data.datagrams.Idx0Datagram;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class PingRangeTest {
   private PingRange totalRange;
   private PingRange firstHalf;
   private PingRange secondHalf;

   @BeforeEach
   void beforeEach() {
      PingIndex a = new Idx0Datagram(5, 50, 50, null, 0);
      PingIndex b = new Idx0Datagram(7, 66, 51, null, 0);
      PingIndex c = new Idx0Datagram(8, 81, 57, null, 0);

      totalRange = PingRange.of(a, c);
      firstHalf = PingRange.of(a, b);
      secondHalf = PingRange.of(b, c);
   }

   @Test
   void of() {
      assertEquals(PingRange.EMPTY_RANGE, PingRange.of(totalRange.end(), totalRange.begin()));
   }

   @Test
   void ofUnsorted() {
      assertEquals(totalRange, PingRange.ofUnsorted(totalRange.end(), totalRange.begin()));
   }

   @Test
   void testIsEmpty() {
      assertFalse(totalRange.isEmpty());
      assertFalse(firstHalf.isEmpty());
      assertFalse(secondHalf.isEmpty());

      assertTrue(PingRange.EMPTY_RANGE.isEmpty());
      assertTrue(PingRange.of(totalRange.begin(), totalRange.begin()).isEmpty());
   }

   @Test
   void testEquals() {
      assertEquals(totalRange, totalRange);
      assertEquals(totalRange, PingRange.of(totalRange.begin(), totalRange.end()));
      assertNotEquals(PingRange.EMPTY_RANGE, PingRange.of(new DefaultPingIndex(), new DefaultPingIndex()));
      assertEquals(PingRange.EMPTY_RANGE, PingRange.EMPTY_RANGE);
      assertNotEquals(PingRange.EMPTY_RANGE, totalRange);
      assertNotEquals(PingRange.EMPTY_RANGE, new Object());
      assertNotEquals(totalRange, new Object());
   }

   @Test
   void testPingCount() {
      assertEquals(0, PingRange.EMPTY_RANGE.getPingCount());
      assertEquals(0, PingRange.of(totalRange.begin(), totalRange.begin()).getPingCount());

      assertEquals(totalRange.getPingCount(), (int) (totalRange.end().getPingNumber() - totalRange.begin().getPingNumber()));
      assertEquals(firstHalf.getPingCount(), (int) (firstHalf.end().getPingNumber() - firstHalf.begin().getPingNumber()));
      assertEquals(secondHalf.getPingCount(), (int) (secondHalf.end().getPingNumber() - secondHalf.begin().getPingNumber()));

      assertEquals(totalRange.getPingCount(), firstHalf.getPingCount() + secondHalf.getPingCount());
   }

   @Test
   void testContains() {
      assertTrue(totalRange.contains(firstHalf));
      assertFalse(firstHalf.contains(totalRange));

      assertTrue(totalRange.contains(secondHalf));
      assertFalse(secondHalf.contains(totalRange));
   }

   @Test
   void testIntersection() {
      assertTrue(totalRange.intersects(totalRange));

      assertTrue(totalRange.intersects(firstHalf));
      assertTrue(firstHalf.intersects(totalRange));

      assertTrue(totalRange.intersects(secondHalf));
      assertTrue(secondHalf.intersects(totalRange));

      assertFalse(firstHalf.intersects(secondHalf));
      assertFalse(secondHalf.intersects(firstHalf));

      assertTrue(firstHalf.intersection(secondHalf).isEmpty());
      assertEquals(firstHalf, totalRange.intersection(firstHalf));
      assertEquals(secondHalf, totalRange.intersection(secondHalf));
   }

   @Test
   void testUnion() {
      assertEquals(totalRange, totalRange.union(totalRange));
      assertEquals(totalRange, PingRange.EMPTY_RANGE.union(totalRange));
      assertEquals(totalRange, totalRange.union(secondHalf));
      assertEquals(totalRange, firstHalf.union(secondHalf));
   }

   @Test
   void testClamp() {
      assertEquals(totalRange.begin(), totalRange.clamp(totalRange.begin()));
      assertEquals(secondHalf.begin(), totalRange.clamp(secondHalf.begin()));
      assertEquals(secondHalf.begin(), secondHalf.clamp(totalRange.begin()));
      assertEquals(firstHalf.end(), firstHalf.clamp(totalRange.end()));
   }
}
