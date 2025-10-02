package no.imr.tools.logging;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("PMD.SystemPrintln")
public final class ExpectedLogWarnings {
   private static final Level EXPECTED_WARNING = new Level("EXPECTED_WARNING", Level.WARNING.intValue() - 1) {
   };

   private static final Set<Expectation> expectations = ConcurrentHashMap.newKeySet();

   static {
      Filter previousFilter = Log.global.getFilter();
      Log.global.setFilter(previousFilter == null
            ? ExpectedLogWarnings::isLoggable
            : logRecord -> previousFilter.isLoggable(logRecord) && isLoggable(logRecord));

      Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
         if (!expectations.isEmpty()) {
            System.err.println(expectations.size() + " log expectations not checked");
            Runtime.getRuntime().halt(1);
         }
      }));
   }

   private ExpectedLogWarnings() {
   }

   private static boolean isLoggable(LogRecord logRecord) {
      if (logRecord.getLevel().intValue() >= Level.WARNING.intValue()) {
         AtomicBoolean match = new AtomicBoolean();
         expectations.stream()
               .filter(expectation -> expectation.criterion.test(logRecord))
               .forEach(expectation -> {
                  expectation.countDownLatch.countDown();
                  match.set(true);
               });
         if (match.get()) {
            logRecord.setLevel(EXPECTED_WARNING);
         }
      }
      return true;
   }

   public static Expectation expectLogRecord(Predicate<LogRecord> criterion) {
      Expectation expectation = new Expectation(criterion);
      expectations.add(expectation);
      return expectation;
   }

   public static Expectation expectMessage(Predicate<String> criterion) {
      return expectLogRecord(logRecord -> criterion.test(logRecord.getMessage()));
   }

   public static final class Expectation {
      private final Predicate<LogRecord> criterion;
      private final CountDownLatch countDownLatch = new CountDownLatch(1);

      private Expectation(Predicate<LogRecord> criterion) {
         this.criterion = criterion;
      }

      public void awaitAndCheck(int timeout, TimeUnit timeUnit) throws InterruptedException {
         assertTrue(countDownLatch.await(timeout, timeUnit));
         check();
      }

      public void check() {
         assertTrue(expectations.remove(this));
         assertEquals(0, countDownLatch.getCount());
      }
   }
}
