package no.imr.korona.data.ping;

import no.imr.korona.data.datagrams.Idx0Datagram;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class PingRangeTest {
   private PingRange totalRange;
   private PingRange firstHalf;
   private PingRange secondHalf;

   @BeforeEach
   void beforeEach() {
      PingIndex a = new Idx0Datagram(Instant.ofEpochSecond(5), 50, 50, null, 0);
      PingIndex b = new Idx0Datagram(Instant.ofEpochSecond(7), 66, 51, null, 0);
      PingIndex c = new Idx0Datagram(Instant.ofEpochSecond(8), 81, 57, null, 0);

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
   void isEmpty() {
      assertFalse(totalRange.isEmpty());
      assertFalse(firstHalf.isEmpty());
      assertFalse(secondHalf.isEmpty());

      assertTrue(PingRange.EMPTY_RANGE.isEmpty());
      assertTrue(PingRange.of(totalRange.begin(), totalRange.begin()).isEmpty());
   }

   @Test
   void equalsTest() {
      assertEquals(totalRange, totalRange);
      assertEquals(totalRange, PingRange.of(totalRange.begin(), totalRange.end()));
      assertEquals(PingRange.EMPTY_RANGE, PingRange.of(EmptyPingIndex.INSTANCE, EmptyPingIndex.INSTANCE));
      assertEquals(PingRange.EMPTY_RANGE, PingRange.EMPTY_RANGE);
      assertNotEquals(PingRange.EMPTY_RANGE, totalRange);
      assertNotEquals(PingRange.EMPTY_RANGE, new Object());
      assertNotEquals(totalRange, new Object());
   }

   @Test
   void pingCount() {
      assertEquals(0, PingRange.EMPTY_RANGE.getPingCount());
      assertEquals(0, PingRange.of(totalRange.begin(), totalRange.begin()).getPingCount());

      assertEquals(totalRange.getPingCount(), (int) (totalRange.end().getPingNumber() - totalRange.begin().getPingNumber()));
      assertEquals(firstHalf.getPingCount(), (int) (firstHalf.end().getPingNumber() - firstHalf.begin().getPingNumber()));
      assertEquals(secondHalf.getPingCount(), (int) (secondHalf.end().getPingNumber() - secondHalf.begin().getPingNumber()));

      assertEquals(totalRange.getPingCount(), firstHalf.getPingCount() + secondHalf.getPingCount());
   }

   @Test
   void contains() {
      assertTrue(totalRange.contains(firstHalf));
      assertFalse(firstHalf.contains(totalRange));

      assertTrue(totalRange.contains(secondHalf));
      assertFalse(secondHalf.contains(totalRange));
   }

   @Test
   void containsInstant() {
      assertFalse(totalRange.containsInstant(totalRange.begin().getInstant().plusNanos(-1)));
      assertTrue(totalRange.containsInstant(totalRange.begin().getInstant()));
      assertTrue(totalRange.containsInstant(totalRange.begin().getInstant().plusNanos(1)));
      assertTrue(totalRange.containsInstant(totalRange.end().getInstant().plusNanos(-1)));
      assertFalse(totalRange.containsInstant(totalRange.end().getInstant()));
      assertFalse(totalRange.containsInstant(totalRange.end().getInstant().plusNanos(1)));
   }

   @Test
   void intersectsVesselDistanceRange() {
      double a = totalRange.begin().getVesselDistance();
      double b = totalRange.end().getVesselDistance();
      assertTrue(totalRange.intersectsVesselDistanceRange(a - 0.1, a + 0.1));
      assertTrue(totalRange.intersectsVesselDistanceRange(b - 0.1, b + 0.1));
      assertFalse(totalRange.intersectsVesselDistanceRange(a, a));
      assertFalse(totalRange.intersectsVesselDistanceRange((a + b) / 2, (a + b) / 2));
      assertFalse(totalRange.intersectsVesselDistanceRange(b, b));
   }

   @Test
   void intersects() {
      assertTrue(totalRange.intersects(totalRange));

      assertTrue(totalRange.intersects(firstHalf));
      assertTrue(firstHalf.intersects(totalRange));

      assertTrue(totalRange.intersects(secondHalf));
      assertTrue(secondHalf.intersects(totalRange));

      assertFalse(firstHalf.intersects(secondHalf));
      assertFalse(secondHalf.intersects(firstHalf));
   }

   @Test
   void intersection() {
      assertTrue(firstHalf.intersection(secondHalf).isEmpty());
      assertEquals(firstHalf, totalRange.intersection(firstHalf));
      assertEquals(secondHalf, totalRange.intersection(secondHalf));
   }

   @Test
   void union() {
      assertEquals(totalRange, totalRange.union(totalRange));
      assertEquals(totalRange, PingRange.EMPTY_RANGE.union(totalRange));
      assertEquals(totalRange, totalRange.union(secondHalf));
      assertEquals(totalRange, firstHalf.union(secondHalf));
   }

   @Test
   void clamp() {
      assertEquals(totalRange.begin(), totalRange.clamp(totalRange.begin()));
      assertEquals(secondHalf.begin(), totalRange.clamp(secondHalf.begin()));
      assertEquals(secondHalf.begin(), secondHalf.clamp(totalRange.begin()));
      assertEquals(firstHalf.end(), firstHalf.clamp(totalRange.end()));
   }
}
