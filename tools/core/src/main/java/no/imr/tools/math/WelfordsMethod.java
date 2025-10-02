package no.imr.tools.math;

/**
 * Method for estimating the variance. Numerically more stable than accumulating the sum of squares.
 */
public final class WelfordsMethod {
   private long count;
   private double m;
   private double s;

   public WelfordsMethod() {
   }

   @Override
   public String toString() {
      return "count = " + count + ", mean = " + m + ", s = " + s;
   }

   public void update(double value) {
      count++;
      double delta = value - m;
      m += delta / count;
      s += delta * (value - m);
   }

   public void update(WelfordsMethod other) {
      if (other.count == 0) {
         return;
      }
      double delta = other.m - m;
      long countSum = count + other.count;
      s += other.s + delta * delta * count * other.count / countSum;
      m += delta * other.count / countSum;
      count = countSum;
   }

   public long getCount() {
      return count;
   }

   public double getMean() {
      return count > 0 ? m : Double.NaN;
   }

   public double getVariance() {
      return count > 1 ? s / (count - 1) : Double.NaN;
   }

   public double getStdDev() {
      return Math.sqrt(getVariance());
   }

   public double getStdErr() {
      return Math.sqrt(getVariance() / count);
   }
}
