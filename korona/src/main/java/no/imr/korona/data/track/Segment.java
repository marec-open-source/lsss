package no.imr.korona.data.track;

import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.ping.PingIndexShift;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.misc.ErrorHandler;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents a segment of a {@link Track}.
 */
public class Segment {
   private final SegmentHandle segmentHandle;
   private PingRange pingRange;
   private @Nullable SegmentData segmentData;
   private boolean closed;

   private PingIndexShift pingIndexShift = PingIndexShift.NO_SHIFT;

   private ErrorHandler errorHandler = ErrorHandler.logging();

   public Segment(SegmentHandle segmentHandle, PingRange pingRange) {
      this.segmentHandle = segmentHandle;
      this.pingRange = copyPingRange(pingRange);
   }

   public void setErrorHandler(ErrorHandler errorHandler) {
      this.errorHandler = errorHandler;
   }

   private static PingRange copyPingRange(PingRange pingRange) {
      return pingRange == PingRange.EMPTY_RANGE
            ? PingRange.EMPTY_RANGE
            : PingRange.of(new Idx0Datagram(pingRange.begin(), 0), new Idx0Datagram(pingRange.end(), 0));
   }

   public SegmentHandle getSegmentHandle() {
      return segmentHandle;
   }

   public Path getMainFile() {
      return segmentHandle.getMainFile();
   }

   /**
    * The range of this segment.
    * Possibly shifted to be a valid part of a {@link Track}.
    *
    * @return the shifted ping range
    */
   public PingRange getPingRange() {
      return pingRange;
   }

   protected void setPingRange(PingRange pingRange) {
      this.pingRange = copyPingRange(pingRange);
   }

   protected PingIndexShift getPingIndexShift() {
      return pingIndexShift;
   }

   synchronized void setPingIndexShift(PingIndexShift pingIndexShift) {
      this.pingIndexShift = pingIndexShift;
      pingIndexShift.apply(pingRange);

      if (segmentData != null) {
         pingIndexShift.apply(segmentData.getPingIndices());
      }
   }

   public synchronized SegmentData getSegmentData() {
      try {
         return getSegmentData(NoticeHandler.ignore(), new AsyncHandle());
      } catch (IOException e) {
         errorHandler.onError("Error accessing " + getMainFile(), e);
         return new ClosedSegmentData();
      }
   }

   private synchronized SegmentData getSegmentData(NoticeHandler noticeHandler, AsyncHandle asyncHandle) throws IOException {
      if (segmentData == null) {
         if (closed) {
            return new ClosedSegmentData();
         }
         segmentData = segmentHandle.createSegmentData(noticeHandler, asyncHandle);
         pingIndexShift.apply(segmentData.getPingIndices());
      }
      return segmentData;
   }

   protected synchronized void setSegmentData(SegmentData segmentData) {
      if (closed) {
         throw new IllegalStateException("closed");
      }
      if (this.segmentData != null) {
         throw new IllegalStateException("opened");
      }
      this.segmentData = segmentData;
   }

   public synchronized void close() {
      closed = true;
      if (segmentData != null) {
         try {
            segmentData.close();
         } catch (IOException e) {
            errorHandler.onError("Error closing " + getMainFile(), e);
         } finally {
            segmentData = null;
         }
      }
   }

   @Override
   public String toString() {
      return "{" + pingRange + ", " + pingIndexShift + "}";
   }
}
