package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.buffer.TrackPingBuffer;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.track.Track;

public final class SyntheticTrack extends Track {
   private final TrackPingBuffer trackPingBuffer;

   public SyntheticTrack(boolean deleteAngles) {
      trackPingBuffer = new TrackPingBuffer(this, deleteAngles);
   }

   public TrackPingBuffer getTrackPingBuffer() {
      return trackPingBuffer;
   }

   public void add(SyntheticSegment syntheticSegment) {
      super.add(syntheticSegment);
      SyntheticSegmentData syntheticSegmentData = (SyntheticSegmentData) syntheticSegment.getSegmentData();
      long lastPing = syntheticSegmentData.getSyntheticData().getLastPingNumber();
      Ping ping = syntheticSegmentData.getSyntheticData().createPing(
            syntheticSegmentData.getSyntheticData().createPingIndex(lastPing));
      trackPingBuffer.newPing(ping);
   }

   public void expandLastSegment() {
      SyntheticSegment syntheticSegment = (SyntheticSegment) getLastSegment();
      if (syntheticSegment == null) {
         return;
      }
      Ping ping = syntheticSegment.expand();
      updateLastSegmentRange();
      trackPingBuffer.newPing(ping);
   }
}
