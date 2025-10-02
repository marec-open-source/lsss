package no.imr.tools.concurrent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An executor that makes sure submitted jobs are executed serially by another executor.
 */
public final class SerialExecutor implements Executor {
   private final Executor executor;
   private final AtomicBoolean executing = new AtomicBoolean();
   private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();

   public SerialExecutor(Executor executor) {
      this.executor = executor;
   }

   @Override
   public void execute(Runnable command) {
      queue.add(command);
      tryExecute();
   }

   private void tryExecute() {
      if (executing.compareAndSet(false, true)) {
         executor.execute(this::run);
      }
   }

   public boolean isIdle() {
      return queue.isEmpty() && !executing.get();
   }

   public void discardWaitingJobs() {
      queue.clear();
   }

   private void run() {
      try {
         Runnable runnable = queue.poll();
         if (runnable != null) {
            runnable.run();
         }
      } finally {
         executing.set(false);
         if (!queue.isEmpty()) {
            tryExecute();
         }
      }
   }
}
