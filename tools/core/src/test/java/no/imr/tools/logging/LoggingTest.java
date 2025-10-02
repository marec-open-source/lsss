package no.imr.tools.logging;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("PMD.SystemPrintln")
final class LoggingTest {
   static {
      Log.addHandler(new HandlerAdapter() {
         @Override
         public void publish(LogRecord record) {
            if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
               fail(new OneLineFormatter().format(Log.getMaxLevelLogRecord()));
            }
         }
      });
      Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
         if (Log.getMaxLevel().intValue() >= Level.WARNING.intValue()) {
            System.err.println("Max log level: " + Log.getMaxLevel());
            Runtime.getRuntime().halt(1);
         }
      }));
   }

   @Test
   void test() {
      JUnitUtils.assertMaxLogLevel();
   }
}
