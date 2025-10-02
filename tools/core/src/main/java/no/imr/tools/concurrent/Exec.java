package no.imr.tools.concurrent;

import no.imr.tools.Utils;
import no.imr.tools.logging.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class Exec {
   public static final ForkJoinPool FORK_JOIN_POOL = new AdaptedForkJoinPool();

   public static final ExecutorService LONG_RUNNING_THREAD_POOL = newExecutor(
         Runtime.getRuntime().availableProcessors(), newThreadFactory("Exec.LONG_RUNNING"));

   public static final ExecutorService CACHED_THREAD_POOL = Executors.newCachedThreadPool(
         newThreadFactory("Exec.CACHED"));

   public static final ExecutorService LOW_PRIORITY_CACHED_THREAD_POOL = Executors.newCachedThreadPool(
         newThreadFactory("Exec.LOW_PRIORITY_CACHED", builder -> builder.priority(Thread.MIN_PRIORITY)));

   /**
    * Private to force use of the methods taking a {@link #newScheduledRunnable} so that uncaught exceptions are not silently swallowed.
    */
   private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1, newThreadFactory("Exec.SCHEDULER"));

   private Exec() {
   }

   public static ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
      return SCHEDULER.schedule(newScheduledRunnable(runnable), delay, timeUnit);
   }

   public static ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long delay, TimeUnit timeUnit) {
      return scheduleWithFixedDelay(runnable, delay, delay, timeUnit);
   }

   public static ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnit) {
      return SCHEDULER.scheduleWithFixedDelay(newScheduledRunnable(runnable), initialDelay, delay, timeUnit);
   }

   private static ExecutorService newExecutor(int poolSize, ThreadFactory threadFactory) {
      ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(poolSize, poolSize,
            60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), threadFactory);
      threadPoolExecutor.allowCoreThreadTimeOut(true);
      return threadPoolExecutor;
   }

   public static ThreadFactory newThreadFactory(String namePrefix) {
      return newThreadFactory(namePrefix, Utils.emptyConsumer());
   }

   public static ThreadFactory newThreadFactory(String namePrefix, Consumer<Thread.Builder.OfPlatform> adapter) {
      Thread.Builder.OfPlatform builder = Thread.ofPlatform()
            .name(namePrefix + "-", 0)
            .daemon()
            .uncaughtExceptionHandler(Log.UNCAUGHT_EXCEPTION_HANDLER);
      adapter.accept(builder);
      return builder.factory();
   }

   public static boolean shutDownAndWait() {
      FORK_JOIN_POOL.shutdownNow();
      LONG_RUNNING_THREAD_POOL.shutdownNow();
      CACHED_THREAD_POOL.shutdownNow();
      LOW_PRIORITY_CACHED_THREAD_POOL.shutdownNow();
      SCHEDULER.shutdownNow();

      try {
         return FORK_JOIN_POOL.awaitTermination(10, TimeUnit.SECONDS)
               && LONG_RUNNING_THREAD_POOL.awaitTermination(10, TimeUnit.SECONDS)
               && CACHED_THREAD_POOL.awaitTermination(10, TimeUnit.SECONDS)
               && LONG_RUNNING_THREAD_POOL.awaitTermination(10, TimeUnit.SECONDS)
               && SCHEDULER.awaitTermination(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         Log.global.log(Level.WARNING, "Error awaiting termination", e);
         return false;
      }
   }

   private static Runnable newScheduledRunnable(Runnable runnable) {
      return () -> {
         try {
            runnable.run();
         } catch (Throwable e) {
            Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            throw e; // Rethrow so that scheduled repeating tasks stop.
         }
      };
   }
}
