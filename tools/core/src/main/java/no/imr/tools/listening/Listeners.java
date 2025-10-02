package no.imr.tools.listening;

import no.imr.tools.concurrent.CoalescingExecutor;
import no.imr.tools.concurrent.Exec;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class Listeners {
   private Listeners() {
   }

   public static Listener debouncing(Runnable listener) {
      AtomicReference<Future<?>> future = new AtomicReference<>(new CompletableFuture<>());
      return () -> {
         Future<?> previousFuture = future.getAndSet(Exec.schedule(listener, 250, TimeUnit.MILLISECONDS));
         previousFuture.cancel(false);
      };
   }

   public static Listener inExecutor(Executor executor, Runnable listener) {
      return () -> executor.execute(listener);
   }

   public static <T> Consumer<T> inExecutor(Executor executor, Consumer<T> listener) {
      return argument -> {
         executor.execute(() -> listener.accept(argument));
      };
   }

   public static Listener coalescingInExecutor(Executor executor, Runnable listener) {
      CoalescingExecutor coalescingExecutor = new CoalescingExecutor(executor);
      return () -> {
         coalescingExecutor.execute(listener);
      };
   }

   public static <T> Consumer<T> coalescingInExecutor(Executor executor, Consumer<T> listener) {
      CoalescingExecutor coalescingExecutor = new CoalescingExecutor(executor);
      return argument -> {
         coalescingExecutor.execute(() -> listener.accept(argument));
      };
   }

   public static Listener coalescingDelayed(long delayMillis, Listener listener) {
      AtomicLong nextTime = new AtomicLong();
      AtomicBoolean inProgress = new AtomicBoolean();
      return () -> {
         if (inProgress.compareAndSet(false, true)) {
            long now = System.currentTimeMillis();
            long delay = Math.max(0, delayMillis - (now - nextTime.get()));
            nextTime.set(now + delay);
            Exec.schedule(() -> {
               inProgress.set(false);
               listener.listen();
            }, delay, TimeUnit.MILLISECONDS);
         }
      };
   }
}
