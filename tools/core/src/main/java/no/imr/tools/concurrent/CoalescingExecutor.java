package no.imr.tools.concurrent;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An executor that only tries to execute the last submitted job.
 */
public final class CoalescingExecutor {
   private final Executor executor;
   private final AtomicBoolean executing = new AtomicBoolean();
   private final AtomicReference<@Nullable Runnable> runnableReference = new AtomicReference<>();

   public CoalescingExecutor(Executor executor) {
      this.executor = executor;
   }

   public void execute(Runnable command) {
      runnableReference.set(command);
      tryExecute();
   }

   private void tryExecute() {
      if (executing.compareAndSet(false, true)) {
         executor.execute(this::run);
      }
   }

   private void run() {
      try {
         Runnable runnable = runnableReference.getAndSet(null);
         if (runnable != null) {
            runnable.run();
         }
      } finally {
         executing.set(false);
         if (runnableReference.get() != null) {
            tryExecute();
         }
      }
   }
}
