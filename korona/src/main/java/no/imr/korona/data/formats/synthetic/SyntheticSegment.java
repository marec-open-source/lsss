package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.track.Segment;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.korona.data.util.DataUtils;

/**
 * A segment of synthetic data.
 */
public final class SyntheticSegment extends Segment {
   public SyntheticSegment(SyntheticData syntheticData) {
      super(new SyntheticSegmentHandle(new SyntheticDataFile(syntheticData)), createPingRange(syntheticData));
   }

   public Ping expand() {
      SyntheticSegmentData segmentData = (SyntheticSegmentData) getSegmentData();
      segmentData.expand(getPingIndexShift());
      setPingRange(DataUtils.createPingRange(segmentData.getPingIndices()));

      long pingNumber = segmentData.getSyntheticData().getLastPingNumber();

      return segmentData.getSyntheticData().createPing(
            segmentData.getSyntheticData().createPingIndex(pingNumber));
   }

   private static PingRange createPingRange(SyntheticData syntheticData) {
      PingIndex begin = syntheticData.createPingIndex(syntheticData.getFirstPingNumber());
      PingIndex end = syntheticData.createPingIndex(syntheticData.getLastPingNumber() + 1);
      return PingRange.of(begin, end);
   }

   static SegmentInfo createSegmentInfo(SyntheticData syntheticData) {
      return new SegmentInfo(syntheticData.getRawFileConfiguration(), createPingRange(syntheticData));
   }
}
