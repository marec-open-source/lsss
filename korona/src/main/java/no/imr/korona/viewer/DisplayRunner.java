package no.imr.korona.viewer;

import no.imr.korona.computation.ModuleException;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.concurrent.SerialExecutor;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.time.RealtimeSyncer;
import no.imr.tools.time.Stopwatch;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.logging.Level;

/**
 * "Runs" display module.
 */
final class DisplayRunner {
   private final KoronaPlaybox koronaPlaybox;
   private final PingSource pingSource;
   private final Executor executor = new SerialExecutor(Exec.CACHED_THREAD_POOL);
   private AsyncHandle asyncHandle = new AsyncHandle();
   private volatile boolean running;
   private volatile boolean endOfInput;

   DisplayRunner(KoronaPlaybox koronaPlaybox, PingSource pingSource) {
      this.koronaPlaybox = koronaPlaybox;
      this.pingSource = pingSource;
   }

   void stop() {
      asyncHandle.cancel();
      asyncHandle.waitUntilFinished();
   }

   boolean isRunning() {
      return running;
   }

   boolean isEndOfInput() {
      return endOfInput;
   }

   void start() {
      if (!running) {
         running = true;
         asyncHandle = new AsyncHandle();
         executor.execute(asyncHandle.createManagedRunnable(this::run));
      }
   }

   private void run() {
      try {
         drainPingSource();
      } catch (ModuleException e) {
         SwingUtilities.invokeLater(() -> {
            GuiUtils.showErrorDialog(koronaPlaybox.getComponent(), "Processing error in module \"" + e.getModule().getDisplayName() + "\":" +
                  "\n\n" + e.getMessage(), e);
         });
      } catch (IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
      } finally {
         running = false;
         SwingUtilities.invokeLater(koronaPlaybox::displayRunnerDone);
      }
   }

   private void drainPingSource() throws IOException {
      RealtimeSyncer realtimeSyncer = new RealtimeSyncer();
      Stopwatch stopwatch = Stopwatch.createStarted();
      while (!asyncHandle.isCancelled()) {
         Ping ping = pingSource.nextPing(asyncHandle);
         if (ping == null) {
            endOfInput = true;
            break;
         }
         realtimeSyncer.sync(asyncHandle, ping.getInstant(), koronaPlaybox.getRealtimeFactor(), koronaPlaybox.isFullSpeed());
         SwingUtilities.invokeLater(() -> {
            if (!asyncHandle.isCancelled()) {
               koronaPlaybox.displayRunnerTime(ping.getInstant());
            }
         });
      }
      Log.global.info("Processing for " + stopwatch.seconds() + " seconds");
   }
}
