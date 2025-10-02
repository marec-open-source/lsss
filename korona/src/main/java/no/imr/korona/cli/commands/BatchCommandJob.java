package no.imr.korona.cli.commands;

import no.imr.korona.Korona;
import no.imr.korona.apps.relay.KoronaRelay;
import no.imr.korona.cli.CliCommandJob;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.computation.io.WriterModule;
import no.imr.korona.data.formats.ek60.EK60DataFormatPlugin;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.util.realtime.app.ProcessorConfig;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

final class BatchCommandJob extends CliCommandJob {
   private final Path source;
   private final Path destination;
   private final int maxParallel;
   private final Korona korona = new Korona();
   private final ProcessorConfig processorConfig = new ProcessorConfig(korona);
   private final AsyncHandle asyncHandle = new AsyncHandle();
   private final AtomicReference<@Nullable Exception> error = new AtomicReference<>();

   BatchCommandJob(@Nullable Path cfs, Path source, Path destination, int maxParallel) {
      this.source = source;
      this.destination = destination;
      this.maxParallel = maxParallel;

      processorConfig.cfsFile.setFile(cfs);
      processorConfig.sourceDirectory.setFile(source);
      processorConfig.destinationDirectory.setFile(destination);
   }

   @Override
   public void run(InputStream in, PrintStream out) throws Exception {
      Log.global.info("Processing from " + source + " to " + destination);

      List<SegmentHandle> segmentHandles = korona.getDataFormatManager().createSegmentHandlesInDirectory(source, asyncHandle);
      FileUtils.createDirectories(destination);

      AtomicInteger counter = new AtomicInteger();

      Semaphore semaphore = new Semaphore(maxParallel);
      for (SegmentHandle segmentHandle : segmentHandles) {
         semaphore.acquireUninterruptibly();
         Exec.LONG_RUNNING_THREAD_POOL.execute(asyncHandle.createManagedRunnable(() -> {
            try {
               Log.global.info("Processing " + counter.incrementAndGet() + "/" + segmentHandles.size() + " " + segmentHandle.getDisplayName());
               process(segmentHandle);
            } finally {
               semaphore.release();
            }
         }));
      }
      asyncHandle.waitUntilFinished();
      Exception exception = error.get();
      if (exception != null) {
         throw exception;
      }
   }

   private void process(SegmentHandle segmentHandle) {
      try (PingReader pingReader = segmentHandle.createPingReader()) {

         ModuleContainer moduleContainer = processorConfig.getCfsManager().loadModuleContainer();

         WriterModule writerModule = moduleContainer.addModule(new WriterModule());
         writerModule.setExtraSuffix(KoronaRelay.NEW_SUFFIX);
         writerModule.fileName.setValue(segmentHandle.getBaseName() + KoronaRelay.KORONA_SUFFIX + EK60DataFormatPlugin.RAW_SUFFIX);
         writerModule.directory.setFile(destination);

         try (ModuleContainerComputation computation = moduleContainer.createComputation(pingReader, asyncHandle)) {
            while (true) {
               Ping ping = computation.nextPing();
               if (ping == null) {
                  break;
               }
            }
         }

         String[] suffixes = {EK60DataFormatPlugin.RAW_SUFFIX, EK60DataFormatPlugin.IDX_SUFFIX, EK60DataFormatPlugin.BOT_SUFFIX};
         for (String suffix : suffixes) {
            String name = segmentHandle.getBaseName() + KoronaRelay.KORONA_SUFFIX + suffix;
            Path file = destination.resolve(name + KoronaRelay.NEW_SUFFIX);
            FileUtils.move(file, file.resolveSibling(name));
         }
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error processing " + segmentHandle.getMainFile(), e);
         error.compareAndSet(null, e);
      }
   }
}
