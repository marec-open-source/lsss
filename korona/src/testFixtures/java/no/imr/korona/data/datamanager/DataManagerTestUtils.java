package no.imr.korona.data.datamanager;

import no.imr.korona.data.DataException;
import no.imr.korona.data.track.SegmentHandle;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class DataManagerTestUtils {
   private DataManagerTestUtils() {
   }

   public static DataManager testDataManager() {
      return new DataManager(new DefaultDataConfiguration());
   }

   public static void open(DataManager dataManager, SegmentHandle... segmentHandles) {
      FileOpenRequest fileOpenRequest = testFileOpenRequest(segmentHandles);
      dataManager.asyncOpenFiles(fileOpenRequest);
      fileOpenRequest.getAsyncHandle().waitUntilFinished();
   }

   public static FileOpenRequest testFileOpenRequest(SegmentHandle... segmentHandles) {
      return new FileOpenRequest(List.of(segmentHandles), true, new FileOpenRequest.RequestObserver() {
         @Override
         public void handleDataException(SegmentHandle segmentHandle, DataException dataException) {
            fail(segmentHandle + ": " + dataException);
         }

         @Override
         public void handleIncompatibleDataFile(SegmentHandle segmentHandle, String incompatibilityReason) {
            fail(segmentHandle + ": " + incompatibilityReason);
         }
      });
   }
}
