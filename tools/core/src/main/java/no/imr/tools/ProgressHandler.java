package no.imr.tools;

import no.imr.tools.listening.Listener;

import java.util.concurrent.atomic.AtomicLong;

/**
 * For monitoring progress of a task.
 */
@FunctionalInterface
public interface ProgressHandler {
   void setProgress(double fraction);

   default ProgressHandler subHandlerForPart(int partIndex, int partCount) {
      return subHandlerForRange(partIndex / (double) partCount, (partIndex + 1) / (double) partCount);
   }

   default ProgressHandler subHandlerForRange(double min, double max) {
      return fraction -> setProgress(min + fraction * (max - min));
   }

   default Listener asCountingListener(long totalCount) {
      AtomicLong counter = new AtomicLong();
      return () -> setProgress((double) counter.incrementAndGet() / totalCount);
   }

   static ProgressHandler ignore() {
      return _ -> {
      };
   }
}
