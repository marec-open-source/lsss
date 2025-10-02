package no.imr.tools.math;

public final class Mean {
   private long count;
   private double m;

   public Mean() {
   }

   @Override
   public String toString() {
      return "count = " + count + ", mean = " + m;
   }

   public void update(double value) {
      count++;
      m += (value - m) / count;
   }

   public void update(double value, int n) {
      count += n;
      m += n * (value - m) / count;
   }

   public long getCount() {
      return count;
   }

   public double getMean() {
      if (count == 0) {
         return Double.NaN;
      }
      return m;
   }
}
