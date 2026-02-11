package no.imr.korona.data.track;

import no.imr.korona.data.formats.missing.MissingPingIndex;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.formats.synthetic.SyntheticSegment;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.test.data.ConstantSyntheticData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class TrackTest {
   @Test
   void test() {
      Track track = new Track();
      track.add(createSyntheticSegment(1, 10));
      track.add(createSyntheticSegment(1, 10));

      assertEquals(20, track.getTotalRange().getPingCount());

      track.add(createSyntheticSegment(31, 40));

      assertEquals(40, track.getTotalRange().getPingCount());
      assertInstanceOf(MissingPingIndex.class, track.getPingIndexOrNullExcludingEnd(25));
   }

   private static SyntheticSegment createSyntheticSegment(int firstPingNumber, int lastPingNumber) {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData().withFirstAndLastPingNumber(firstPingNumber, lastPingNumber);
      return new SyntheticSegment(syntheticDataFile);
   }

   @Test
   void testRemoveFirstSegment() {
      Track track = new Track();
      track.add(createSyntheticSegment(11, 20));
      track.add(createSyntheticSegment(21, 30));
      assertNotNull(track.getPingIndexOrNullExcludingEnd(15));

      track.removeFirstSegment();
      assertNull(track.getPingIndexOrNullExcludingEnd(15));
      assertEquals(21, track.getTotalRange().begin().getPingNumber());

      track.removeFirstSegment();
      assertEquals(PingRange.EMPTY_RANGE, track.getTotalRange());

      track.add(createSyntheticSegment(11, 20));
      track.add(createSyntheticSegment(31, 40));
      track.removeFirstSegment();
      assertEquals(31, track.getTotalRange().begin().getPingNumber());

      track.removeFirstSegment();
      assertEquals(PingRange.EMPTY_RANGE, track.getTotalRange());
   }

   @Test
   void expansionOfLastSegment() {
      Track track = new Track();
      track.add(createSyntheticSegment(11, 20));
      SyntheticSegment segment = createSyntheticSegment(21, 30);
      track.add(segment);
      assertEquals(31, track.getTotalRange().end().getPingNumber());
      assertNull(track.getPingIndexOrNullExcludingEnd(31));

      segment.expand();
      track.updateLastSegmentRange();
      assertEquals(32, track.getTotalRange().end().getPingNumber());
      PingIndex pingIndex = track.getPingIndexOrNullExcludingEnd(31);
      assertNotNull(pingIndex);
      assertEquals(31, pingIndex.getPingNumber());
      assertNull(track.getPingIndexOrNullExcludingEnd(32));
   }
}
