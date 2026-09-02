package no.imr.korona.util.realtime;

import no.imr.korona.computation.BaseModule;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.computation.io.WriterModule;
import no.imr.korona.data.formats.ek60.EK60PingReader;
import no.imr.korona.data.formats.ek60.io.EndOfInputHandler;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.util.CancellablePingReader;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileInfoComparator;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.ErrorHandler;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;

public class RealtimeProcessor {
   private final ModuleContainer moduleContainer;
   private final ProcessingStatus processingStatus;
   private final Path sourceDirectory;
   private boolean skipToEndOfFirstFile = true;
   private boolean skipToLastFileAtStart = true;
   private boolean onlyRaw;
   private ErrorHandler errorHandler = ErrorHandler.logging();
   private Comparator<FileInfo> fileComparator = FileInfoComparator.lastModified();
   private @Nullable Path lastProcessedFile;
   private @Nullable ModuleContainerComputation moduleContainerComputation;

   private boolean started;
   private final AsyncHandle asyncHandle = new AsyncHandle();

   private final class WaitTimeoutEndOfInputHandler implements EndOfInputHandler {
      private final NextFileWait nextFileWait;
      private boolean stopWaiting = false;

      private WaitTimeoutEndOfInputHandler(Path file, AsyncHandle asyncHandle) {
         nextFileWait = new NextFileWait(file, asyncHandle);
      }

      @Override
      public boolean isEndOfInput(Duration durationWaiting, Comparator<FileInfo> comparator) throws IOException {
         if (durationWaiting.toSeconds() >= 5) {
            processingStatus.setState(ProcessingStatus.State.IDLE, "Idle");
         }
         if (durationWaiting.toSeconds() >= 15) {
            stopWaiting = true;
         }
         return stopWaiting || nextFileWait.isEndOfInput(durationWaiting, fileComparator);
      }
   }

   public RealtimeProcessor(ModuleContainer moduleContainer, ProcessingStatus processingStatus, Path sourceDirectory) {
      this.moduleContainer = moduleContainer;
      this.processingStatus = processingStatus;
      this.sourceDirectory = sourceDirectory;
   }

   public RealtimeProcessor setLastProcessedFile(@Nullable Path lastProcessedFile) {
      throwIfStarted();
      this.lastProcessedFile = lastProcessedFile;
      return this;
   }

   public RealtimeProcessor setDestinationDirectory(@Nullable Path destinationDirectory) {
      throwIfStarted();
      if (destinationDirectory != null) {
         setupWriter(destinationDirectory);
      }
      return this;
   }

   public RealtimeProcessor setSkipToEndOfFirstFile(boolean skipToEndOfFirstFile) {
      throwIfStarted();
      this.skipToEndOfFirstFile = skipToEndOfFirstFile;
      return this;
   }

   public RealtimeProcessor setSkipToLastFileAtStart(boolean skipToLastFileAtStart) {
      throwIfStarted();
      this.skipToLastFileAtStart = skipToLastFileAtStart;
      return this;
   }

   public RealtimeProcessor setOnlyRaw(boolean onlyRaw) {
      throwIfStarted();
      this.onlyRaw = onlyRaw;
      return this;
   }

   public RealtimeProcessor setErrorHandler(ErrorHandler errorHandler) {
      throwIfStarted();
      this.errorHandler = errorHandler;
      return this;
   }

   public RealtimeProcessor setFileComparator(Comparator<FileInfo> fileComparator) {
      this.fileComparator = fileComparator;
      return this;
   }

   public RealtimeProcessor start() {
      throwIfStarted();
      started = true;
      Exec.CACHED_THREAD_POOL.execute(asyncHandle.createManagedRunnable(this::run));
      return this;
   }

   private void throwIfStarted() {
      if (started) {
         throw new IllegalStateException("Already started");
      }
   }

   private void setupWriter(Path directory) {
      BaseModule lastModule = moduleContainer.getModules().isEmpty() ? null : moduleContainer.getModules().getLast();
      WriterModule writerModule;
      if (lastModule instanceof WriterModule w) {
         writerModule = w;
      } else {
         // If there is no writer module, we add one.
         writerModule = moduleContainer.addModule(new WriterModule());
      }
      writerModule.directory.setFile(directory);
   }

   public @Nullable ModuleContainerComputation getModuleContainerComputation() {
      return moduleContainerComputation;
   }

   private void processFile(Path file, boolean skipToEnd) throws IOException {
      EndOfInputHandler endOfInputHandler = new WaitTimeoutEndOfInputHandler(file, asyncHandle);
      EK60PingReader pingReader = onlyRaw
            ? EK60PingReader.createRawOnly(file, endOfInputHandler, moduleContainer.getKorona().getDatagramTypeManager(), true)
            : EK60PingReader.create(file, endOfInputHandler, moduleContainer.getKorona().getDatagramTypeManager(), true);
      if (skipToEnd) {
         boolean atEndOfFile = skipToEnd(pingReader);
         if (atEndOfFile) {
            return;
         }
      }
      processingStatus.getNewFileChangeChangeManager().notifyListeners(new ProcessingStatus.NewFileInformation(file, pingReader.getPingConfiguration()));

      try (PingReader adaptedPingReader = adaptPingReader(pingReader, asyncHandle);
           ModuleContainerComputation computation = moduleContainer.createComputation(adaptedPingReader, asyncHandle)) {

         moduleContainerComputation = computation;
         processCurrent(file, computation);
      } finally {
         moduleContainerComputation = null;
      }
   }

   protected PingReader adaptPingReader(PingReader pingReader, AsyncHandle asyncHandle) {
      pingReader = new CancellablePingReader(pingReader, asyncHandle);
      return pingReader;
   }

   private boolean skipToEnd(EK60PingReader pingReader) throws IOException {
      pingReader.skipToEnd(asyncHandle);
      // Discard one ping that might be missing some of its first datagrams
      Ping ping = pingReader.nextEmptyPing(asyncHandle);
      if (ping == null) {
         // If after skipping we are at the end of the file, we can assume that the file is not being written to
         pingReader.close();
         return true;
      }
      // Set time of configuration datagrams to ping time minus 0.5 seconds.
      Instant configTime = ping.getInstant().minusMillis(500);
      for (PingItem pingItem : pingReader.getPingConfiguration().getConfigurationItems()) {
         pingItem.setInstant(configTime);
      }
      return false;
   }

   private void processCurrent(Path file, ModuleContainerComputation computation) throws IOException {
      while (!asyncHandle.isCancelled()) {
         Ping ping = computation.nextPing();
         if (ping == null) {
            break;
         }
         processingStatus.getPingProcessedChangeManager().notifyListeners(ping);
      }
      processingStatus.getFileProcessedChangeManager().notifyListeners(file);
   }

   private void run() {
      boolean firstTime = true;
      boolean error = false;
      while (!asyncHandle.isCancelled()) {
         try {
            Path file;
            boolean skipToEnd = skipToEndOfFirstFile && firstTime;
            if (firstTime && lastProcessedFile == null) {
               file = FileUtils.listFilesWithAttributes(sourceDirectory, asyncHandle, KoronaUtils::isRawFile).stream()
                     .max(skipToLastFileAtStart ? fileComparator : fileComparator.reversed())
                     .map(FileInfo::file)
                     .orElse(null);
            } else {
               file = KoronaUtils.nextRawFile(sourceDirectory, lastProcessedFile, fileComparator);
               if (file == null && error) {
                  file = lastProcessedFile;
                  skipToEnd = true;
               }
            }
            firstTime = false;
            if (file == null) {
               processingStatus.setState(ProcessingStatus.State.IDLE, "Idle");
               processingStatus.getNoMoreFilesChangeManager().notifyListeners();
               asyncHandle.sleep(1000);
               continue;
            }
            lastProcessedFile = file;
            Log.global.info("Start processing " + (skipToEnd ? " from end of " : "") + file);
            processingStatus.setState(ProcessingStatus.State.RUNNING, "Running");
            processFile(file, skipToEnd);
            error = false;
         } catch (Exception e) {
            error = true;
            if (processingStatus.getState() != ProcessingStatus.State.ERROR) {
               processingStatus.setState(ProcessingStatus.State.ERROR, "Error during processing");
               errorHandler.onError("Error during processing", e);
            }
            asyncHandle.sleep(5000); // Wait a little and try again
         }
      }
   }

   public void close() {
      asyncHandle.cancel();
      asyncHandle.waitUntilFinished();
   }
}
