package no.imr.tools.math;

public final class Sum {
   private double sum;
   private long count;

   public Sum() {
   }

   public void add(double value) {
      sum += value;
      count++;
   }

   public double getSum() {
      return sum;
   }

   public long getCount() {
      return count;
   }

   public double getAverage() {
      return sum / count;
   }
}
