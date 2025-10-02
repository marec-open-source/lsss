package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.formats.ek60.io.EndOfInputHandler;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.track.Segment;
import no.imr.korona.data.util.DataUtils;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * A segment created from a raw file being written to.
 */
public final class EK60ExpandingSegment extends Segment {
   public EK60ExpandingSegment(EK60SegmentHandle ek60SegmentHandle, EndOfInputHandler endOfInputHandler, DatagramTypeManager datagramTypeManager) throws IOException {
      this(ek60SegmentHandle, new EK60ExpandingSegmentData(ek60SegmentHandle.getEK60FileSet(), endOfInputHandler, datagramTypeManager));
   }

   private EK60ExpandingSegment(EK60SegmentHandle ek60SegmentHandle, EK60ExpandingSegmentData ek60ExpandingSegmentData) {
      super(ek60SegmentHandle, DataUtils.createPingRange(ek60ExpandingSegmentData.getPingIndices()));

      setSegmentData(ek60ExpandingSegmentData);
   }

   public @Nullable PingIndex expand(AsyncHandle asyncHandle) throws IOException {
      EK60ExpandingSegmentData segmentData = (EK60ExpandingSegmentData) getSegmentData();
      PingIndex pingIndex = segmentData.expand(asyncHandle);
      if (pingIndex != null) {
         getPingIndexShift().apply(pingIndex);
         setPingRange(DataUtils.createPingRange(segmentData.getPingIndices()));
      } else {
         segmentData.closePingReader();
      }
      return pingIndex;
   }
}
