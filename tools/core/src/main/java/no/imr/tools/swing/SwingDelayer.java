package no.imr.tools.swing;

import no.imr.tools.concurrent.Exec;

import javax.swing.SwingUtilities;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * For executing tasks in the swing thread after a delay.
 */
public final class SwingDelayer {
   private static final Map<Object, Runnable> TASKS = new ConcurrentHashMap<>();

   private SwingDelayer() {
   }

   private static void process(Object key) {
      SwingUtilities.invokeLater(() -> {
         Runnable task = TASKS.remove(key);
         if (task != null) {
            task.run();
         }
      });
   }

   /**
    * Registers a task for execution.
    * If another task is registered earlier with the same key, it will be replaced by this task.
    *
    * @param key  a key associated with the task
    * @param task a task
    */
   public static void invokeLater(Object key, Runnable task) {
      if (TASKS.put(key, task) == null) {
         Exec.schedule(() -> process(key), 10, TimeUnit.MILLISECONDS);
      }
   }
}
