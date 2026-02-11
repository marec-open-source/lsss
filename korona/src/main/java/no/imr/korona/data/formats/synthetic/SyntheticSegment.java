package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.track.Segment;
import no.imr.korona.data.util.DataUtils;

/**
 * A segment of synthetic data.
 */
public final class SyntheticSegment extends Segment {
   public SyntheticSegment(SyntheticDataFile syntheticDataFile) {
      super(new SyntheticSegmentHandle(syntheticDataFile.toFile(), syntheticDataFile), syntheticDataFile.toPingRange());
   }

   public Ping expand() {
      SyntheticSegmentData segmentData = (SyntheticSegmentData) getSegmentData();
      segmentData.expand(getPingIndexShift());
      setPingRange(DataUtils.createPingRange(segmentData.getPingIndices()));

      long pingNumber = segmentData.getSyntheticDataFile().getLastPingNumber();
      PingIndex pingIndex = segmentData.getSyntheticDataFile().createPingIndex(pingNumber);
      return segmentData.getSyntheticDataFile().createPing(pingIndex);
   }
}
