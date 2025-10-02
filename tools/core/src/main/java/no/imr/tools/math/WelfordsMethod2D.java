package no.imr.tools.math;

import no.imr.tools.math.linalg.Vec2;

/**
 * Estimation of covariances.
 */
public final class WelfordsMethod2D {
   private long count;
   private double mx;
   private double my;
   private double sxx;
   private double syy;
   private double sxy;

   public WelfordsMethod2D() {
   }

   @Override
   public String toString() {
      return "count = " + count
            + ", mx = " + mx
            + ", my = " + my
            + ", sxx = " + sxx
            + ", sxy = " + sxy
            + ", syy = " + syy;
   }

   public void update(double x, double y) {
      count++;
      double dx = x - mx;
      double dy = y - my;
      mx += dx / count;
      my += dy / count;
      sxx += dx * (x - mx);
      syy += dy * (y - my);
      sxy += dx * (y - my);
   }

   public void update(Vec2 p) {
      update(p.x(), p.y());
   }

   public long getCount() {
      return count;
   }

   public double meanX() {
      return count > 0 ? mx : Double.NaN;
   }

   public double meanY() {
      return count > 0 ? my : Double.NaN;
   }

   public double covXX() {
      return count > 1 ? sxx / (count - 1) : Double.NaN;
   }

   public double covXY() {
      return count > 1 ? sxy / (count - 1) : Double.NaN;
   }

   public double covYY() {
      return count > 1 ? syy / (count - 1) : Double.NaN;
   }
}
