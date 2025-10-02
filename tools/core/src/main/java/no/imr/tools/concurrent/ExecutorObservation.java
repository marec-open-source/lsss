package no.imr.tools.concurrent;

import no.imr.tools.listening.ChangeManager;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Observation of an executor used by {@link ObservingExecutor}.
 */
public final class ExecutorObservation {
   private final AtomicInteger counter = new AtomicInteger();
   private final ChangeManager changeManager = new ChangeManager();

   public ExecutorObservation() {
   }

   public int getCount() {
      return counter.get();
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   private void increment() {
      if (counter.getAndIncrement() == 0) {
         changeManager.notifyListeners();
      }
   }

   private void decrement() {
      if (counter.decrementAndGet() == 0) {
         changeManager.notifyListeners();
      }
   }

   public void execute(Executor executor, Runnable runnable) {
      increment();
      executor.execute(() -> {
         try {
            runnable.run();
         } finally {
            decrement();
         }
      });
   }
}
