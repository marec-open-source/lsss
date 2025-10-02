package no.imr.tools.concurrent;

import java.util.concurrent.Executor;

/**
 * An executor using an {@link ExecutorObservation}.
 */
public final class ObservingExecutor implements Executor {
   private final Executor executor;
   private final ExecutorObservation executorObservation;

   public ObservingExecutor(Executor executor, ExecutorObservation executorObservation) {
      this.executor = executor;
      this.executorObservation = executorObservation;
   }

   @Override
   public void execute(Runnable command) {
      executorObservation.execute(executor, command);
   }
}
