package no.imr.korona.data.buffer;

import no.imr.korona.Korona;
import no.imr.korona.data.formats.ek60.EK60PingReader;
import no.imr.korona.data.formats.ek60.io.EndOfInputHandler;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.FileInfoComparator;
import no.imr.tools.logging.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Reads files from a directory into a {@link PingBuffer}.
 */
public final class PingBufferReader {
   private final PingBuffer pingBuffer;
   private final AsyncHandle asyncHandle = new AsyncHandle();

   public PingBufferReader(PingBuffer pingBuffer) {
      this.pingBuffer = pingBuffer;
   }

   public void stop() {
      asyncHandle.cancel();
      asyncHandle.waitUntilFinished();
   }

   public void read(Path directory, Korona korona) {
      ExecutorService executor = Executors.newSingleThreadExecutor(Exec.newThreadFactory("PingBufferReader"));
      executor.execute(asyncHandle.createManagedRunnable(() -> {
         Path currentRawFile = null;
         while (!asyncHandle.isCancelled()) {
            Path nextRawFile;
            try {
               nextRawFile = KoronaUtils.nextRawFile(directory, currentRawFile, FileInfoComparator.path());
            } catch (IOException _) {
               asyncHandle.sleep(1000);
               continue;
            }
            if (nextRawFile == null) {
               asyncHandle.sleep(1000);
               continue;
            }

            currentRawFile = nextRawFile;

            try (EK60PingReader pingReader = EK60PingReader.create(currentRawFile, new EndOfInputHandler.NextFileWait(currentRawFile, asyncHandle), korona.getDatagramTypeManager(), false)) {
               while (!asyncHandle.isCancelled()) {
                  Ping ping = pingReader.nextPing(asyncHandle);
                  if (asyncHandle.isCancelled() || ping == null) {
                     break;
                  }
                  pingBuffer.newPing(ping);
                  asyncHandle.sleep(1);
               }
            } catch (IOException e) {
               Log.global.log(Level.WARNING, "Error reading " + currentRawFile, e);
            }
         }
      }));
   }
}
