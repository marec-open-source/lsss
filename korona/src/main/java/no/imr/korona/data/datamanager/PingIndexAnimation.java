package no.imr.korona.data.datamanager;

import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.concurrent.Exec;
import org.jspecify.annotations.Nullable;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PingIndexAnimation {
   private final Supplier<DataFileSet> dataFileSetSupplier;
   private final Supplier<@Nullable PingIndex> pingIndexSupplier;
   private final Consumer<PingIndex> pingIndexConsumer;
   private final Runnable onStop;
   private boolean running;
   private int step = 1;
   private float realtimeFactor = 1;
   private Future<?> future = CompletableFuture.completedFuture(null);

   public PingIndexAnimation(Supplier<DataFileSet> dataFileSetSupplier,
                             Supplier<@Nullable PingIndex> pingIndexSupplier,
                             Consumer<PingIndex> pingIndexConsumer,
                             Runnable onStop) {
      this.dataFileSetSupplier = dataFileSetSupplier;
      this.pingIndexSupplier = pingIndexSupplier;
      this.pingIndexConsumer = pingIndexConsumer;
      this.onStop = onStop;
   }

   public synchronized void stop() {
      if (!running) {
         return;
      }
      running = false;
      future.cancel(true);
      onStop.run();
   }

   public synchronized void start() {
      if (running) {
         return;
      }
      future.cancel(true);
      running = true;
      go();
   }

   public synchronized void setForward(boolean forward) {
      step = forward ? 1 : -1;
   }

   public synchronized void setRealtimeFactor(float realtimeFactor) {
      this.realtimeFactor = realtimeFactor;
   }

   private synchronized void go() {
      if (!running) {
         return;
      }
      PingIndex p0 = pingIndexSupplier.get();
      if (p0 == null) {
         stop();
         return;
      }
      DataFileSet dataFileSet = dataFileSetSupplier.get();
      PingIndex p1 = dataFileSet.getPingIndexOrNull(p0.getPingNumber() + step);
      if (p1 == null || p1.equals(dataFileSet.getTotalRange().end())) {
         stop();
         return;
      }
      pingIndexConsumer.accept(p1);
      PingIndex p2 = dataFileSet.getPingIndexOrNull(p1.getPingNumber() + step);
      if (p2 == null || p2.equals(dataFileSet.getTotalRange().end())) {
         stop();
         return;
      }
      long dt = Math.min(10_000, Math.round(Math.abs(p1.getInstant().until(p2.getInstant(), ChronoUnit.MILLIS)) / realtimeFactor));
      future = Exec.schedule(this::go, dt, TimeUnit.MILLISECONDS);
   }
}
