package no.imr.tools.concurrent;

import no.imr.tools.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Concurrent execution of tasks, but serially for tasks with same key.
 */
public final class MultiSerialExecutor {
   private final List<SerialExecutor> executors;

   public MultiSerialExecutor(Executor executor) {
      int n = Runtime.getRuntime().availableProcessors();
      executors = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
         executors.add(new SerialExecutor(executor));
      }
   }

   /**
    * Schedules a task for execution. Tasks with the same key (i.e. hash code) are executed serially.
    *
    * @param key  a key
    * @param task a task
    */
   public void execute(Object key, Runnable task) {
      SerialExecutor executor = executors.get(Utils.mod(key.hashCode(), executors.size()));
      executor.execute(task);
   }
}
