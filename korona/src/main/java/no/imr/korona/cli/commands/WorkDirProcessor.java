package no.imr.korona.cli.commands;

import no.imr.korona.Korona;
import no.imr.korona.cli.CliCommandJob;
import no.imr.korona.data.DataException;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.datamanager.DefaultDataConfiguration;
import no.imr.korona.data.datamanager.FileOpenRequest;
import no.imr.korona.data.formats.ek60.MissingIdxFileHandler;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.SimpleRegionConfiguration;
import no.imr.korona.region.WorkData;
import no.imr.korona.region.WorkFile;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.DirectoryListing;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

final class WorkDirProcessor extends CliCommandJob {
   private final Path dataDir;
   private final Path workDir;
   private final Path outputDir;
   private final int maxParallel;
   private final Korona korona = new Korona();
   private final WorkData workData = new WorkData();
   private final AsyncHandle asyncHandle = new AsyncHandle();
   private final AtomicInteger errorCounter = new AtomicInteger();
   private final WorkFileProcessor workFileProcessor;

   WorkDirProcessor(Path dataDir, Path workDir, Path outputDir, int maxParallel, WorkFileProcessor workFileProcessor) {
      this.dataDir = dataDir;
      this.workDir = workDir;
      this.outputDir = outputDir;
      this.maxParallel = maxParallel;
      this.workFileProcessor = workFileProcessor;
   }

   @Override
   public void run(InputStream in, PrintStream out) throws Exception {
      Log.global.info("Processing work files from " + workDir + " to " + outputDir);

      MissingIdxFileHandler.setDatagramTypeManager(korona.getDatagramTypeManager());

      List<SegmentHandle> segmentHandles = korona.getDataFormatManager().createSegmentHandlesInDirectory(dataDir, asyncHandle);

      DirectoryListing workDirListing = DirectoryListing.of(workDir, asyncHandle);

      Path workDataFile = workDir.resolve(WorkFile.WORK_DATA_FILE_NAME);
      if (workDirListing.exists(workDataFile)) {
         try {
            workData.fromXml(XmlUtils.readDocument(workDataFile).getRootElement());
         } catch (Exception e) {
            Log.global.log(Level.WARNING, "Error reading " + workDataFile, e);
            errorCounter.incrementAndGet();
         }
      }

      FileUtils.createDirectories(outputDir);

      AtomicInteger counter = new AtomicInteger();

      Set<SegmentHandle> needSecondPass = new ConcurrentSkipListSet<>();

      Semaphore semaphore = new Semaphore(maxParallel);
      for (SegmentHandle segmentHandle : segmentHandles) {
         semaphore.acquireUninterruptibly();
         Exec.LONG_RUNNING_THREAD_POOL.execute(asyncHandle.createManagedRunnable(() -> {
            try {
               Log.global.info("Processing " + counter.incrementAndGet() + "/" + segmentHandles.size() + " " + segmentHandle.getDisplayName());
               boolean done = process(segmentHandle, workDirListing, true);
               if (!done) {
                  needSecondPass.add(segmentHandle);
               }
            } finally {
               semaphore.release();
            }
         }));
      }
      asyncHandle.waitUntilFinished();

      if (!needSecondPass.isEmpty()) {
         Log.global.info("Some work files contain regions without object number. Processing these last.");
         counter.set(0);
         for (SegmentHandle segmentHandle : needSecondPass) {
            Log.global.info("Second pass processing " + counter.incrementAndGet() + "/" + needSecondPass.size() + " " + segmentHandle.getDisplayName());
            process(segmentHandle, workDirListing, false);
         }
      }

      try {
         workFileProcessor.end(workData);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error processing workData", e);
         errorCounter.incrementAndGet();
      }

      int errorCount = errorCounter.get();
      if (errorCount != 0) {
         throw new IOException("Processing finished with " + errorCount + " errors");
      }
   }

   private boolean process(SegmentHandle segmentHandle, DirectoryListing workDirListing, boolean firstPass) {
      String workFileBaseName = WorkFile.getWorkFileBaseName(segmentHandle);
      Path workFile = WorkFile.getExistingWorkFile(workDir, workDirListing, workFileBaseName);
      if (workFile == null) {
         Log.global.log(Level.INFO, "No work file for " + segmentHandle);
      }
      try {
         DataManager dataManager = new DataManager(new DefaultDataConfiguration());
         AtomicReference<@Nullable DataException> dataExceptionRef = new AtomicReference<>();
         FileOpenRequest fileOpenRequest = new FileOpenRequest(List.of(segmentHandle), false, new FileOpenRequest.RequestObserver() {
            @Override
            public void handleDataException(SegmentHandle segmentHandle, DataException dataException) {
               dataExceptionRef.set(dataException);
            }
         });
         dataManager.asyncOpenFiles(fileOpenRequest);
         fileOpenRequest.getAsyncHandle().waitUntilFinished();
         DataException dataException = dataExceptionRef.get();
         if (dataException != null) {
            throw dataException;
         }

         SimpleRegionConfiguration regionConfiguration = new SimpleRegionConfiguration(dataManager)
               .setNextObjectNumberSupplier(workData::nextObjectNumber);
         RegionManager regionManager = new RegionManager(regionConfiguration);
         Element originalXml;
         if (workFile != null) {
            originalXml = XmlUtils.readDocument(workFile).getRootElement();
            Element upgradedXml = WorkFile.upgrade(originalXml);
            regionManager.fromXml(upgradedXml);
         } else {
            originalXml = null;
            regionManager.setupDefaultBoundaries(__ -> 0, __ -> 0);
         }

         if (firstPass) {
            regionManager.regionStream()
                  .filter(Region::hasObjectNumber)
                  .mapToInt(Region::getObjectNumber)
                  .max()
                  .ifPresent(workData::possiblyAdjustObjectNumber);
            boolean allRegionsHasObjectNumber = regionManager.regionStream().allMatch(Region::hasObjectNumber);
            if (!allRegionsHasObjectNumber) {
               return false;
            }
         }

         workFileProcessor.process(dataManager, regionManager, workFileBaseName, originalXml);
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error processing " + segmentHandle + ", work file: " + workFile, e);
         errorCounter.incrementAndGet();
      }
      return true;
   }
}
