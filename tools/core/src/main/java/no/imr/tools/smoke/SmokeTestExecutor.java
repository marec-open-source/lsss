package no.imr.tools.smoke;

import no.imr.tools.InMemoryPreferencesFactory;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.logging.LoggingManager;
import no.imr.tools.logging.MaxLevelHandler;
import no.imr.tools.logging.OneLineFormatter;
import no.imr.tools.misc.ThreadDump;
import no.imr.tools.misc.test.UniqueTmpDir;
import org.jspecify.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Executes smoke tests.
 */
@SuppressWarnings("PMD.SystemPrintln")
public final class SmokeTestExecutor {
   static {
      InMemoryPreferencesFactory.install();
      LoggingManager.initTmpApplicationDataDir();
   }

   private SmokeTestExecutor() {
   }

   public static void execute(@Nullable LoggingManager loggingManager, SmokeTestRunnable... smokeTests) {
      if (!isAssertEnabled()) {
         System.err.println("Assert is not enabled");
         System.exit(1);
      }

      if (loggingManager != null) {
         if (!FileUtils.isInDir(loggingManager.getApplicationDataDir(), UniqueTmpDir.get())) {
            System.err.println("ApplicationDataDir not in UniqueTmpDir: " + loggingManager.getApplicationDataDir());
            System.exit(1);
         }
         loggingManager.setUseWindowHandler(false);
         loggingManager.startLogging();
      }

      try {
         Log.addHandler(new MaxLevelHandler() {
            @Override
            protected void newMaxLevel(LogRecord logRecord) {
               if (logRecord.getLevel().intValue() >= Level.WARNING.intValue()) {
                  System.err.println("Smoke test: log >= warning");
                  System.err.println(new OneLineFormatter().format(logRecord));
                  if (logRecord.getThrown() == null) {
                     new SmokeTestException(logRecord.getLevel().getName()).printStackTrace(System.err);
                  }
                  Thread.ofVirtual().name("maxLogLevel").start(() -> {
                     // Run in a different thread, in case logging is done via an executor
                     // that should be stopped in a shutdown hhok.
                     System.exit(1);
                  });
               }
            }
         });

         for (SmokeTestRunnable smokeTest : smokeTests) {
            smokeTest.run();
         }
      } catch (Throwable e) {
         System.err.println("Smoke test: exception");
         e.printStackTrace(System.err);
         System.exit(1);
      }
      waitForShutdown();
   }

   private static boolean isAssertEnabled() {
      try {
         assert System.lineSeparator().isEmpty(); // Should always fail.
         return false;
      } catch (AssertionError _) {
         return true;
      }
   }

   private static void waitForShutdown() {
      Thread.ofVirtual().name("waitForShutdown").start(() -> {
         Utils.sleep(60_000);
         shutdownFailed();
      });
   }

   private static void shutdownFailed() {
      System.err.println("Smoke test: shutdown failed");
      ThreadDump.print(System.err);
      System.exit(1);
   }
}
