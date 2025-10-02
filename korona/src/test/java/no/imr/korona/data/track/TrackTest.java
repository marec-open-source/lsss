package no.imr.korona.data.track;

import no.imr.korona.data.formats.missing.MissingPingIndex;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.formats.synthetic.SyntheticSegment;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.test.data.ConstantSyntheticData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class TrackTest {
   @Test
   void test() {
      Track track = new Track();
      track.add(new SyntheticSegment(createSyntheticData(1, 10)));
      track.add(new SyntheticSegment(createSyntheticData(1, 10)));

      assertEquals(20, track.getTotalRange().getPingCount());

      track.add(new SyntheticSegment(createSyntheticData(31, 40)));

      assertEquals(40, track.getTotalRange().getPingCount());
      assertInstanceOf(MissingPingIndex.class, track.getContainingPingIndex(25, PingMapping.NUMBER));
   }

   private static SyntheticData createSyntheticData(int firstPingNumber, int lastPingNumber) {
      ConstantSyntheticData syntheticData = new ConstantSyntheticData();
      syntheticData.setFirstAndLastPingNumber(firstPingNumber, lastPingNumber);
      return syntheticData;
   }

   @Test
   void testRemoveFirstSegment() {
      Track track = new Track();
      track.add(new SyntheticSegment(createSyntheticData(11, 20)));
      track.add(new SyntheticSegment(createSyntheticData(21, 30)));
      assertNotNull(track.getContainingPingIndex(15, PingMapping.NUMBER));

      track.removeFirstSegment();
      assertNull(track.getContainingPingIndex(15, PingMapping.NUMBER));
      assertEquals(21, track.getTotalRange().begin().getPingNumber());

      track.removeFirstSegment();
      assertEquals(PingRange.EMPTY_RANGE, track.getTotalRange());

      track.add(new SyntheticSegment(createSyntheticData(11, 20)));
      track.add(new SyntheticSegment(createSyntheticData(31, 40)));
      track.removeFirstSegment();
      assertEquals(31, track.getTotalRange().begin().getPingNumber());

      track.removeFirstSegment();
      assertEquals(PingRange.EMPTY_RANGE, track.getTotalRange());
   }

   @Test
   void expansionOfLastSegment() {
      Track track = new Track();
      track.add(new SyntheticSegment(createSyntheticData(11, 20)));
      SyntheticSegment segment = new SyntheticSegment(createSyntheticData(21, 30));
      track.add(segment);
      assertEquals(31, track.getTotalRange().end().getPingNumber());
      assertNull(track.getContainingPingIndex(31, PingMapping.NUMBER));

      segment.expand();
      track.updateLastSegmentRange();
      assertEquals(32, track.getTotalRange().end().getPingNumber());
      PingIndex pingIndex = track.getContainingPingIndex(31, PingMapping.NUMBER);
      assertNotNull(pingIndex);
      assertEquals(31, pingIndex.getPingNumber());
      assertNull(track.getContainingPingIndex(32, PingMapping.NUMBER));
   }
}
