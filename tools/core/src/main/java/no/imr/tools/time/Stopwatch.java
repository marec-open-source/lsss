package no.imr.tools.time;

/**
 * For timing.
 * <p>
 * Elapsed time is accumulated if it is repeatedly started and stopped.
 */
public final class Stopwatch {
   private boolean running;
   private long accumulatedTime = 0;
   private long startTime = System.nanoTime();

   private Stopwatch(boolean running) {
      this.running = running;
   }

   public static Stopwatch createStarted() {
      return new Stopwatch(true);
   }

   public static Stopwatch createUnstarted() {
      return new Stopwatch(false);
   }

   /**
    * Restarts this stopwatch.
    */
   public void restart() {
      running = true;
      accumulatedTime = 0;
      startTime = System.nanoTime();
   }

   /**
    * Starts this stopwatch. Nothing happens if it is already running.
    */
   public void start() {
      if (!running) {
         startTime = System.nanoTime();
         running = true;
      }
   }

   /**
    * Stops this stopwatch.
    */
   public void stop() {
      if (running) {
         accumulatedTime += System.nanoTime() - startTime;
         running = false;
      }
   }

   /**
    * Returns the elapsed time since last reset.
    *
    * @return elapsed time in seconds
    */
   public double seconds() {
      long t = accumulatedTime;
      if (running) {
         t += System.nanoTime() - startTime;
      }
      return t * 1e-9;
   }
}
