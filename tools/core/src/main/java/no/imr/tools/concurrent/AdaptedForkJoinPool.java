package no.imr.tools.concurrent;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Sends uncaught exceptions from {@link ForkJoinPool#execute(Runnable)}
 * to {@link Thread#getUncaughtExceptionHandler()}.
 */
final class AdaptedForkJoinPool extends ForkJoinPool {
   AdaptedForkJoinPool() {
   }

   @Override
   public void execute(Runnable task) {
      super.execute(new AdaptedRecursiveAction(task));
   }

   private static final class AdaptedRecursiveAction extends RecursiveAction {
      private final Runnable task;

      private AdaptedRecursiveAction(Runnable task) {
         this.task = task;
      }

      @Override
      protected void compute() {
         try {
            task.run();
         } catch (Throwable e) {
            Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
         }
      }
   }
}
