package no.imr.tools.test;

import no.imr.tools.Utils;
import no.imr.tools.time.Stopwatch;

@SuppressWarnings("PMD.SystemPrintln")
public final class BenchmarkTimer {
   private final Runnable runnable;

   public BenchmarkTimer(Runnable runnable) {
      this.runnable = runnable;
      run();
   }

   private void run() {
      int n = 1;
      System.out.println("n = " + n);
      for (int i = 0; i < 100; i++) {
         Stopwatch stopwatch = Stopwatch.createStarted();
         run(n);
         double s = stopwatch.seconds();
         if (s > 0.95) {
            break;
         }
         double avg = s / n;
         n = (int) Math.ceil(1 / avg);
         System.out.println("n = " + n);
      }

      int runCount = 10;
      double secondSum = 0;
      for (int i = 0; i < runCount; i++) {
         Stopwatch stopwatch = Stopwatch.createStarted();
         run(n);
         double s = stopwatch.seconds();
         secondSum += s;
         System.out.print(Utils.format("%2d", i + 1) + ": ");
         print(n, s);
      }
      System.out.println("---");
      System.out.println("Average:");
      print(runCount * n, secondSum);
   }

   private void run(int n) {
      for (int i = 0; i < n; i++) {
         runnable.run();
      }
   }

   private static void print(int iterations, double seconds) {
      System.out.println("Iterations per second: " + Utils.format("%.2f", iterations / seconds) + ",  Seconds per iteration: " + seconds / iterations);
   }
}
