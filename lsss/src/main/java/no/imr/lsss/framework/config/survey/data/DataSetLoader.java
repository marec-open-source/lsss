package no.imr.lsss.framework.config.survey.data;

import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.datamanager.FileOpenRequest;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.concurrent.AsyncHandle;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * For loading new {@link DataFileSet}s.
 */
public final class DataSetLoader {
   private final DataManager rawDataManager;
   private AsyncHandle rawAsyncHandle = new AsyncHandle();
   private List<SegmentHandle> rawSegmentHandles = List.of();
   private final AtomicInteger rawCounter = new AtomicInteger();

   private final DataManager processedDataManager;
   private AsyncHandle processedAsyncHandle = new AsyncHandle();
   private List<SegmentHandle> processedSegmentHandles = List.of();
   private final AtomicInteger processedCounter = new AtomicInteger();

   public DataSetLoader(DataConfiguration dataConfiguration) {
      rawDataManager = new DataManager(dataConfiguration);
      processedDataManager = new DataManager(dataConfiguration);
   }

   DataFileSet getDataFileSet(DataType dataType) {
      if (dataType == DataType.RAW) {
         return rawDataManager.getDataFileSet();
      } else {
         return processedDataManager.getDataFileSet();
      }
   }

   void cancel() {
      rawAsyncHandle.cancel();
      processedAsyncHandle.cancel();
   }

   public void waitUntilFinished() {
      rawAsyncHandle.waitUntilFinished();
      processedAsyncHandle.waitUntilFinished();
   }

   AsyncHandle getRawAsyncHandle() {
      return rawAsyncHandle;
   }

   AsyncHandle getProcessedAsyncHandle() {
      return processedAsyncHandle;
   }

   AtomicInteger getRawCounter() {
      return rawCounter;
   }

   AtomicInteger getProcessedCounter() {
      return processedCounter;
   }

   List<SegmentHandle> getRawSegmentHandles() {
      return rawSegmentHandles;
   }

   List<SegmentHandle> getProcessedSegmentHandles() {
      return processedSegmentHandles;
   }

   public void asyncOpenFiles(DataType dataType, FileOpenRequest fileOpenRequest) {
      if (dataType == DataType.RAW) {
         rawAsyncHandle = fileOpenRequest.getAsyncHandle();
         rawCounter.set(0);
         rawSegmentHandles = fileOpenRequest.getSegmentHandles();
         rawDataManager.asyncOpenFiles(fileOpenRequest);
      } else {
         processedAsyncHandle = fileOpenRequest.getAsyncHandle();
         processedCounter.set(0);
         processedSegmentHandles = fileOpenRequest.getSegmentHandles();
         processedDataManager.asyncOpenFiles(fileOpenRequest);
      }
   }
}
