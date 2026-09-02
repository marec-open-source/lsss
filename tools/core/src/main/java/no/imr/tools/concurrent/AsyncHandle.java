package no.imr.tools.concurrent;

import no.imr.tools.misc.ThrowingConsumer;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A handle for one or more asynchronous runnables.
 * If {@link #cancel()} is followed by {@link #waitUntilFinished()} then further runnables
 * created with {@link #createManagedRunnable(Runnable)} will return immediately upon execution.
 * <p>
 * Runnables given as arguments to {@link #createManagedRunnable(Runnable)} should be given a reference
 * to this AsyncHandle and frequently check {@link #isCancelled()}.
 */
public final class AsyncHandle implements no.marec.lsss.api.util.AsyncHandle, ManagedRunnableFactory {
   private final class ManagedRunnable implements Runnable {
      private final Runnable runnable;

      private ManagedRunnable(Runnable runnable) {
         this.runnable = runnable;
      }

      @Override
      public void run() {
         try {
            synchronized (lock) {
               waitingCount--;
               runningCount++;
            }

            if (!cancelled) {
               runnable.run();
            }
         } finally {
            synchronized (lock) {
               runningCount--;
               if (runningCount == 0) {
                  lock.notifyAll();
               }
            }
         }
      }
   }

   private final Object lock = new Object();
   private int waitingCount = 0;
   private int runningCount = 0;
   private volatile boolean cancelled = false;
   private volatile @Nullable Set<AsyncHandle> subAsyncHandles;

   /**
    * Creates a new AsyncHandle.
    */
   public AsyncHandle() {
   }

   @Override
   public String toString() {
      synchronized (lock) {
         return "AsyncHandle [" + (cancelled ? "Cancelled" : "Active") + ", running: " + runningCount + ", waiting: " + waitingCount + "]";
      }
   }

   /**
    * Creates a managed runnable that will execute the target runnable only if
    * this AsyncHandle is not cancelled.
    *
    * @param runnable the target runnable
    * @return a managed runnable
    */
   @Override
   public Runnable createManagedRunnable(Runnable runnable) {
      synchronized (lock) {
         waitingCount++;
         return new ManagedRunnable(runnable);
      }
   }

   /**
    * Waits until all Runnables created with {@link #createManagedRunnable(Runnable)} have completed.
    */
   public void waitUntilFinished() {
      synchronized (lock) {
         while (!isFinished()) {
            try {
               lock.wait();
            } catch (InterruptedException _) {
               Thread.currentThread().interrupt();
               break;
            }
         }
      }
   }

   public void invokeAndWait(Executor executor, Runnable runnable) {
      AtomicInteger state = new AtomicInteger(0);
      executor.execute(() -> {
         try {
            // NB: Set state _before_ checking cancelled.
            state.set(1);
            if (cancelled) {
               return;
            }
            runnable.run();
         } finally {
            synchronized (lock) {
               state.set(2);
               lock.notifyAll();
            }
         }
      });
      synchronized (lock) {
         while (true) {
            if (state.get() == 2) {
               break; // done
            }
            // NB: Check cancelled _before_ checking state.
            if (cancelled && state.get() == 0) {
               break; // cancelled and not started
            }
            try {
               lock.wait();
            } catch (InterruptedException _) {
               Thread.currentThread().interrupt();
               break;
            }
         }
      }
   }

   /**
    * Returns {@code true} if this AsyncHandle is finished.
    *
    * @return {@code true} if finished
    */
   public boolean isFinished() {
      synchronized (lock) {
         return runningCount == 0 && (waitingCount == 0 || cancelled);
      }
   }

   /**
    * Returns {@code true} if this AsyncHandle is cancelled.
    *
    * @return {@code true} if cancelled
    */
   @Override
   public boolean isCancelled() {
      return cancelled;
   }

   public int getWaitingCount() {
      synchronized (lock) {
         return waitingCount;
      }
   }

   /**
    * Cancels this AsyncHandle.
    * All Runnables created with {@link #createManagedRunnable(Runnable)} subsequently
    * will return immediately.
    */
   public void cancel() {
      if (!cancelled) {
         synchronized (lock) {
            cancelled = true;
            lock.notifyAll();
         }
         Set<AsyncHandle> subs = subAsyncHandles;
         if (subs != null) {
            subs.forEach(AsyncHandle::cancel);
         }
      }
   }

   /**
    * Causes the currently executing thread to sleep (temporarily cease execution)
    * for the specified number of milliseconds, or until this AsyncHandle is cancelled.
    *
    * @param millis the length of time to sleep in milliseconds
    */
   public void sleep(long millis) {
      long endMillis = System.currentTimeMillis() + millis;
      synchronized (lock) {
         while (!cancelled) {
            long waitMillis = endMillis - System.currentTimeMillis();
            if (waitMillis <= 0) {
               break;
            }
            try {
               lock.wait(waitMillis);
            } catch (InterruptedException _) {
               Thread.currentThread().interrupt();
               break;
            }
         }
      }
   }

   public <E extends Exception> void withSubAsyncHandle(ThrowingConsumer<AsyncHandle, E> consumer) throws E {
      Set<AsyncHandle> subs = subAsyncHandles;
      if (subs == null) {
         synchronized (lock) { // Double-checked locking with volatile field.
            subs = subAsyncHandles;
            if (subs == null) {
               subs = ConcurrentHashMap.newKeySet();
               subAsyncHandles = subs;
            }
         }
      }
      AsyncHandle subAsyncHandle = new AsyncHandle();
      subs.add(subAsyncHandle);
      try {
         if (!cancelled) {
            consumer.accept(subAsyncHandle);
         }
      } finally {
         subs.remove(subAsyncHandle);
      }
   }
}
