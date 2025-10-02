package no.imr.tools.concurrent;

import no.imr.tools.logging.ExpectedLogWarnings;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

final class ExecTest {
   @Test
   void exceptionInForkJoinPoolExecuteRunnable() throws InterruptedException {
      RuntimeException expectedException = new RuntimeException("Expected");
      ExpectedLogWarnings.Expectation expectation = ExpectedLogWarnings.expectLogRecord(logRecord -> {
         return logRecord.getThrown() == expectedException;
      });
      Exec.FORK_JOIN_POOL.execute(() -> {
         throw expectedException;
      });
      expectation.awaitAndCheck(10, TimeUnit.SECONDS);
   }
}
