package no.imr.korona.data.datamanager;

import no.imr.korona.data.DataException;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * A request for asynchronously opening files.
 *
 * @see DataManager#asyncOpenFiles(FileOpenRequest)
 */
public final class FileOpenRequest {
   private final List<SegmentHandle> segmentHandles;
   private final boolean checkCompatibility;
   private final RequestObserver observer;
   private OnTheFlyProcessing onTheFlyProcessing = (segmentData, segmentHandle) -> segmentData;
   private final AsyncHandle asyncHandle = new AsyncHandle();

   public FileOpenRequest(List<SegmentHandle> segmentHandles) {
      this(segmentHandles, true, RequestObserver.ignore());
   }

   public FileOpenRequest(List<SegmentHandle> segmentHandles, boolean checkCompatibility, RequestObserver observer) {
      this.segmentHandles = segmentHandles;
      this.checkCompatibility = checkCompatibility;
      this.observer = observer;
   }

   public List<SegmentHandle> getSegmentHandles() {
      return segmentHandles;
   }

   public boolean getCheckCompatibility() {
      return checkCompatibility;
   }

   public RequestObserver getObserver() {
      return observer;
   }

   public OnTheFlyProcessing getOnTheFlyProcessing() {
      return onTheFlyProcessing;
   }

   public void setOnTheFlyProcessing(OnTheFlyProcessing onTheFlyProcessing) {
      this.onTheFlyProcessing = onTheFlyProcessing;
   }

   public AsyncHandle getAsyncHandle() {
      return asyncHandle;
   }

   public interface RequestObserver {
      default void progressReportAfterOpen(SegmentHandle segmentHandle, @Nullable DataFile dataFile) {
      }

      default void handleDataException(SegmentHandle segmentHandle, DataException dataException) {
      }

      default void handleIncompatibleDataFile(SegmentHandle segmentHandle, String incompatibilityReason) {
      }

      static RequestObserver ignore() {
         return new RequestObserver() {
         };
      }
   }

   @FunctionalInterface
   public interface OnTheFlyProcessing {
      SegmentData process(SegmentData segmentData, SegmentHandle segmentHandle) throws IOException;

      default OnTheFlyProcessing andThen(OnTheFlyProcessing after) {
         return (segmentData, segmentHandle) -> {
            return after.process(process(segmentData, segmentHandle), segmentHandle);
         };
      }
   }
}
